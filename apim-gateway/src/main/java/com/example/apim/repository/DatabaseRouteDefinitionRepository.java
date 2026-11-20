package com.example.apim.repository;

import com.example.apim.model.ApiDefinitionEntity;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Repository
public class DatabaseRouteDefinitionRepository implements RouteDefinitionRepository {

    private static final Logger log = LoggerFactory.getLogger(DatabaseRouteDefinitionRepository.class);

    private final ApiDefinitionRepository apiDefinitions;
    private final ApplicationEventPublisher eventPublisher;

    public DatabaseRouteDefinitionRepository(
            ApiDefinitionRepository apiDefinitions,
            ApplicationEventPublisher eventPublisher) {
        this.apiDefinitions = apiDefinitions;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return apiDefinitions.findAll()
                .doOnNext(api -> log.debug("Loading Gateway route id={} gatewayContext={} upstreamContext={} host={}",
                        api.getId(), api.getGatewayContextPath(), api.getUpstreamContextPath(), api.getUri()))
                .map(this::toRouteDefinition)
                .doOnError(error -> log.error("Failed to load Gateway routes from H2 reason={}", error.getMessage(), error));
    }

    @Override
    public Mono<Void> save(Mono<RouteDefinition> routeDefinitions) {
        return routeDefinitions
                .map(this::toApiDefinition)
                .flatMap(incoming -> apiDefinitions.findById(incoming.getId())
                        .flatMap(existing -> {
                            existing.setPath(incoming.getPath());
                            existing.setGatewayContextPath(incoming.getGatewayContextPath());
                            existing.setUri(incoming.getUri());
                            existing.setUpstreamContextPath(incoming.getUpstreamContextPath());
                            existing.setStripPrefix(incoming.getStripPrefix());
                            existing.setRequiredScope(incoming.getRequiredScope());
                            existing.setRemoveAuthorizationHeader(incoming.isRemoveAuthorizationHeader());
                            return apiDefinitions.save(existing);
                        })
                        .switchIfEmpty(Mono.defer(() -> apiDefinitions.save(incoming))))
                .doOnSuccess(saved -> refreshRoutes())
                .doOnError(error -> log.error("Failed to save route definition reason={}", error.getMessage(), error))
                .then();
    }

    @Override
    public Mono<Void> delete(Mono<String> routeIds) {
        return routeIds
                .flatMap(id -> apiDefinitions.deleteById(id).thenReturn(id))
                .doOnNext(ignored -> refreshRoutes())
                .doOnNext(ignored -> log.debug("Gateway route deleted routeId={} refreshRequested=true", ignored))
                .doOnError(error -> log.error("Failed to delete route definition reason={}", error.getMessage(), error))
                .then();
    }

    private RouteDefinition toRouteDefinition(ApiDefinitionEntity api) {
        RouteDefinition route = new RouteDefinition();
        route.setId(api.getId());
        route.setUri(hostOnlyUri(api.getUri()));
        route.setPredicates(List.of(predicate("Path", api.getPath())));

        List<FilterDefinition> filters = new ArrayList<>();
        if (api.getStripPrefix() > 0) {
            filters.add(filter("StripPrefix", Map.of("_genkey_0", String.valueOf(api.getStripPrefix()))));
        }
        if (api.getUpstreamContextPath() != null && !api.getUpstreamContextPath().isBlank()) {
            filters.add(filter("PrefixPath", Map.of("_genkey_0", api.getUpstreamContextPath())));
        }
        if (api.isRemoveAuthorizationHeader()) {
            filters.add(filter("RemoveRequestHeader", Map.of("_genkey_0", "Authorization")));
        }
        filters.add(filter("CircuitBreaker", Map.of(
                "name", api.getId() + "CircuitBreaker",
                "fallbackUri", "forward:/fallback/service-unavailable")));
        route.setFilters(filters);
        route.getMetadata().put("requiredScope", api.getRequiredScope());
        return route;
    }

    private URI hostOnlyUri(String configuredUri) {
        URI uri = URI.create(configuredUri);
        if (uri.getHost() == null) {
            return uri;
        }
        try {
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), null, null, null);
        } catch (java.net.URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid upstream URI", exception);
        }
    }

    private ApiDefinitionEntity toApiDefinition(RouteDefinition route) {
        String path = route.getPredicates().stream()
                .filter(predicate -> "Path".equals(predicate.getName()))
                .map(predicate -> predicate.getArgs().get("_genkey_0"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Route requires a Path predicate"));
        int stripPrefix = route.getFilters().stream()
                .filter(filter -> "StripPrefix".equals(filter.getName()))
                .map(filter -> Integer.parseInt(filter.getArgs().getOrDefault("_genkey_0", "0")))
                .findFirst()
                .orElse(0);
        String upstreamContextPath = route.getFilters().stream()
                .filter(filter -> "PrefixPath".equals(filter.getName()))
                .map(filter -> filter.getArgs().get("_genkey_0"))
                .findFirst()
                .orElse("");
        boolean removeAuthorizationHeader = route.getFilters().stream()
                .filter(filter -> "RemoveRequestHeader".equals(filter.getName()))
                .anyMatch(filter -> filter.getArgs().containsValue("Authorization"));
        Object scope = route.getMetadata().get("requiredScope");
        if (scope == null) {
            throw new IllegalArgumentException("Route metadata must include requiredScope");
        }
        String[] pathParts = path.split("/");
        String gatewayContextPath = pathParts.length > 3 ? pathParts[3] : "";
        return new ApiDefinitionEntity(route.getId(), path, gatewayContextPath,
                route.getUri().toString(), upstreamContextPath, stripPrefix,
                scope.toString(), removeAuthorizationHeader);
    }

    private PredicateDefinition predicate(String name, String argument) {
        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName(name);
        predicate.setArgs(Map.of("_genkey_0", argument));
        return predicate;
    }

    private FilterDefinition filter(String name, Map<String, String> arguments) {
        FilterDefinition filter = new FilterDefinition();
        filter.setName(name);
        filter.setArgs(arguments);
        return filter;
    }

    private void refreshRoutes() {
        log.debug("Publishing Gateway route refresh event");
        eventPublisher.publishEvent(new RefreshRoutesEvent(this));
    }
}