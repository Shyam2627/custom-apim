package com.example.apim.service;

import com.example.apim.config.GatewayServiceProperties;
import com.example.apim.model.ApiDefinitionEntity;
import com.example.apim.model.ApiDefinitionRequest;
import com.example.apim.repository.ApiAllowedHostRepository;
import com.example.apim.repository.ApiDefinitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ApiDefinitionService {

    private static final Logger log = LoggerFactory.getLogger(ApiDefinitionService.class);

    private static final Pattern ID_PATTERN = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9_-]{0,63}");
    private static final Pattern PATH_PATTERN = Pattern.compile("/api/v1/[a-zA-Z0-9_-]+/\\*\\*");
    private static final Pattern SCOPE_PATTERN = Pattern.compile("[a-zA-Z0-9._-]{1,128}");
    private static final Pattern CONTEXT_PATTERN = Pattern.compile("[a-zA-Z0-9_-]{1,64}");

    private final ApiDefinitionRepository apiDefinitions;
    private final ApiAllowedHostRepository allowedHosts;
    private final ApplicationEventPublisher eventPublisher;

    public ApiDefinitionService(
            ApiDefinitionRepository apiDefinitions,
            ApiAllowedHostRepository allowedHosts,
            ApplicationEventPublisher eventPublisher) {
        this.apiDefinitions = apiDefinitions;
        this.allowedHosts = allowedHosts;
        this.eventPublisher = eventPublisher;
    }

    public Flux<ApiDefinitionEntity> findAll() {
        return apiDefinitions.findAll().sort((first, second) -> first.getId().compareTo(second.getId()));
    }

    public Mono<ApiDefinitionEntity> findById(String id) {
        return apiDefinitions.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    public Mono<ApiDefinitionEntity> create(ApiDefinitionRequest request) {
        String upstreamHost = validate(request);
        ApiDefinitionRequest normalized = normalizeRequest(request);
        return requireAllowedHost(upstreamHost)
            .then(apiDefinitions.existsById(normalized.id()))
                .flatMap(idExists -> idExists
                        ? Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "API id already exists"))
                        : apiDefinitions.findByPath(normalized.path())
                                .flatMap(existing -> Mono.<ApiDefinitionEntity>error(
                                        new ResponseStatusException(HttpStatus.CONFLICT, "API path already exists")))
                                .switchIfEmpty(Mono.defer(() -> apiDefinitions.save(toEntity(normalized)))))
                .doOnSuccess(saved -> {
                    log.debug("API definition created id={} gatewayContext={} upstreamContext={} host={}", saved.getId(),
                            saved.getGatewayContextPath(), saved.getUpstreamContextPath(), saved.getUri());
                    refreshRoutes();
                })
                .doOnError(error -> log.error("Failed to create API definition id={} gatewayContext={} host={} reason={}",
                        normalized.id(), normalized.gatewayContextPath(), upstreamHost, error.getMessage(), error));
    }

    public Mono<ApiDefinitionEntity> update(String id, ApiDefinitionRequest request) {
        if (request == null || !id.equals(request.id())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path id and body id must match"));
        }
        String upstreamHost = validate(request);
        ApiDefinitionRequest normalized = normalizeRequest(request);

        return requireAllowedHost(upstreamHost)
            .then(apiDefinitions.findById(id))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .flatMap(existing -> apiDefinitions.findByPath(normalized.path())
                        .filter(pathMatch -> !id.equals(pathMatch.getId()))
                        .flatMap(pathMatch -> Mono.<ApiDefinitionEntity>error(
                                new ResponseStatusException(HttpStatus.CONFLICT, "API path already exists")))
                        .switchIfEmpty(Mono.defer(() -> {
                            existing.setPath(normalized.path());
                            existing.setGatewayContextPath(normalized.gatewayContextPath());
                            existing.setUri(normalized.uri());
                            existing.setUpstreamContextPath(normalized.upstreamContextPath());
                            existing.setStripPrefix(normalized.stripPrefix());
                            existing.setRequiredScope(normalized.requiredScope());
                            existing.setRemoveAuthorizationHeader(normalized.removeAuthorizationHeader());
                            return apiDefinitions.save(existing);
                        })))
                .doOnSuccess(saved -> {
                    log.debug("API definition updated id={} gatewayContext={} upstreamContext={} host={}",
                            saved.getId(), saved.getGatewayContextPath(), saved.getUpstreamContextPath(), saved.getUri());
                    refreshRoutes();
                })
                .doOnError(error -> log.error("Failed to update API definition id={} gatewayContext={} host={} reason={}",
                        id, normalized.gatewayContextPath(), upstreamHost, error.getMessage(), error));
    }

    public Mono<Void> delete(String id) {
        return apiDefinitions.existsById(id)
                .flatMap(exists -> exists
                        ? apiDefinitions.deleteById(id).doOnSuccess(ignored -> {
                            refreshRoutes();
                            log.debug("API definition deleted id={}", id);
                        })
                        : Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .doOnError(error -> log.error("Failed to delete API definition id={} reason={}", id, error.getMessage(), error));
    }

    private ApiDefinitionEntity toEntity(ApiDefinitionRequest request) {
        return new ApiDefinitionEntity(request.id(), request.path(), request.gatewayContextPath(),
                request.uri(), request.upstreamContextPath(), request.stripPrefix(),
                request.requiredScope(), request.removeAuthorizationHeader());
    }

    private ApiDefinitionRequest normalizeRequest(ApiDefinitionRequest request) {
        String gatewayContext = request.gatewayContextPath();
        if (gatewayContext == null || gatewayContext.isBlank()) {
            String[] parts = request.path() == null ? new String[0] : request.path().split("/");
            gatewayContext = parts.length > 3 ? parts[3] : "";
        }
        gatewayContext = gatewayContext.trim().replace("/", "").toLowerCase(Locale.ROOT);

        String upstreamContext = request.upstreamContextPath();
        if (upstreamContext == null || upstreamContext.isBlank()) {
            upstreamContext = "";
        } else {
            upstreamContext = normalizeContext(upstreamContext);
        }

        String path = "/api/v1/" + gatewayContext + "/**";
        return new ApiDefinitionRequest(request.id(), path, gatewayContext, request.uri(),
                upstreamContext, request.stripPrefix(), request.requiredScope(),
                request.removeAuthorizationHeader());
    }

    private String normalizeContext(String contextPath) {
        if (contextPath == null || contextPath.isBlank() || "/".equals(contextPath.trim())) {
            return "";
        }
        String normalized = "/" + contextPath.trim().replace("\\", "/").replaceAll("^/+|/+$", "");
        if (!normalized.matches("(?:/[a-zA-Z0-9_-]+)+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid upstream context path");
        }
        return normalized;
    }

    private String validate(ApiDefinitionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid API definition");
        }
        ApiDefinitionRequest normalized = normalizeRequest(request);
        if (request.id() == null
                || !ID_PATTERN.matcher(request.id()).matches()
                || request.requiredScope() == null
                || !SCOPE_PATTERN.matcher(request.requiredScope()).matches()
                || !CONTEXT_PATTERN.matcher(normalized.gatewayContextPath()).matches()
                || request.stripPrefix() < 0
                || request.stripPrefix() > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid API definition");
        }

        URI upstream;
        try {
            upstream = URI.create(normalized.uri());
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid upstream URI");
        }
        String scheme = upstream.getScheme();
        if (upstream.getHost() == null
                || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                || upstream.getRawUserInfo() != null
                || upstream.getRawQuery() != null
                || upstream.getRawFragment() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid upstream URI");
        }
        return upstream.getHost().toLowerCase(Locale.ROOT);
    }

    private Mono<Void> requireAllowedHost(String host) {
        return allowedHosts.existsById(host)
                .flatMap(allowed -> allowed
                        ? Mono.empty()
                        : Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "The upstream host is not approved. Add and approve it in the admin host list, then try again.")));
    }

    private void refreshRoutes() {
        eventPublisher.publishEvent(new RefreshRoutesEvent(this));
    }
}