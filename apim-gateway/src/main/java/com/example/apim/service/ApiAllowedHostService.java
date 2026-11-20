package com.example.apim.service;

import com.example.apim.config.GatewayServiceProperties;
import com.example.apim.model.ApiAllowedHostEntity;
import com.example.apim.model.ApiAllowedHostRequest;
import com.example.apim.repository.ApiAllowedHostRepository;
import com.example.apim.repository.ApiDefinitionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ApiAllowedHostService {

    private static final Logger log = LoggerFactory.getLogger(ApiAllowedHostService.class);

    private static final Pattern DNS_NAME = Pattern.compile(
            "(?=.{1,253}$)(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,63}");
    private static final String[] RESERVED_SUFFIXES = {
            ".localhost", ".local", ".internal", ".lan", ".home.arpa"
    };

    private final ApiAllowedHostRepository allowedHosts;
    private final ApiDefinitionRepository apiDefinitions;
    private final GatewayServiceProperties serviceProperties;

    public ApiAllowedHostService(
            ApiAllowedHostRepository allowedHosts,
            ApiDefinitionRepository apiDefinitions,
            GatewayServiceProperties serviceProperties) {
        this.allowedHosts = allowedHosts;
        this.apiDefinitions = apiDefinitions;
        this.serviceProperties = serviceProperties;
    }

    public Flux<ApiAllowedHostEntity> findAll() {
        return allowedHosts.findAll().sort((first, second) -> first.getHost().compareTo(second.getHost()));
    }

    public Mono<ApiAllowedHostEntity> add(ApiAllowedHostRequest request) {
        String host = normalizeAndValidate(request);
        return allowedHosts.existsById(host)
                .flatMap(exists -> exists
                        ? Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Host is already allowed"))
                        : allowedHosts.save(new ApiAllowedHostEntity(host)))
                .doOnSuccess(saved -> log.debug("Approved upstream host host={}", saved.getHost()))
                .doOnError(error -> log.error("Failed to approve upstream host host={} reason={}", host, error.getMessage(), error));
    }

    public Mono<Void> delete(String requestedHost) {
        if (requestedHost == null || requestedHost.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "A host is required"));
        }
        String host = normalize(requestedHost.trim());
        if (serviceProperties.getAllowedHosts().stream()
                .map(this::normalize)
                .anyMatch(host::equals)) {
            return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Configured service hosts cannot be removed"));
        }
        String finalHost = normalizeAndValidate(new ApiAllowedHostRequest(host));

        return apiDefinitions.findAll()
                .filter(definition -> usesHost(definition.getUri(), finalHost))
                .hasElements()
                .flatMap(inUse -> inUse
                        ? Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Host is used by an API definition"))
                        : allowedHosts.deleteById(finalHost))
                .doOnSuccess(ignored -> log.debug("Removed approved upstream host host={}", finalHost))
                .doOnError(error -> log.error("Failed to remove approved upstream host host={} reason={}", finalHost, error.getMessage(), error));
    }

    private String normalizeAndValidate(ApiAllowedHostRequest request) {
        if (request == null || request.host() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A host is required");
        }
        String host = normalize(request.host().trim());
        if (!DNS_NAME.matcher(host).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a fully qualified DNS host name");
        }
        for (String suffix : RESERVED_SUFFIXES) {
            if (host.endsWith(suffix)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Local and internal host names are not allowed");
            }
        }
        return host;
    }

    private String normalize(String host) {
        return host.toLowerCase(Locale.ROOT);
    }

    private boolean usesHost(String uri, String host) {
        try {
            return host.equals(normalize(URI.create(uri).getHost()));
        } catch (RuntimeException exception) {
            return false;
        }
    }
}