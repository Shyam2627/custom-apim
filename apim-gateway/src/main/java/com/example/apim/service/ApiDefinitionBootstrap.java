package com.example.apim.service;

import com.example.apim.config.GatewayServiceProperties;
import com.example.apim.model.ApiAllowedHostEntity;
import com.example.apim.model.ApiDefinitionEntity;
import com.example.apim.repository.ApiAllowedHostRepository;
import com.example.apim.repository.ApiDefinitionRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Locale;

@Component
public class ApiDefinitionBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ApiDefinitionBootstrap.class);

    private final ApiDefinitionRepository apiDefinitions;
    private final ApiAllowedHostRepository allowedHosts;
    private final GatewayServiceProperties serviceProperties;
    private final ApplicationEventPublisher eventPublisher;

    public ApiDefinitionBootstrap(
            ApiDefinitionRepository apiDefinitions,
            ApiAllowedHostRepository allowedHosts,
            GatewayServiceProperties serviceProperties,
            ApplicationEventPublisher eventPublisher) {
        this.apiDefinitions = apiDefinitions;
        this.allowedHosts = allowedHosts;
        this.serviceProperties = serviceProperties;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting runtime API and host registry bootstrap");
        try {
            Flux.fromIterable(serviceProperties.getAllowedHosts())
                    .map(host -> host.toLowerCase(Locale.ROOT))
                    .concatMap(host -> allowedHosts.existsById(host)
                            .flatMap(exists -> exists
                                    ? reactor.core.publisher.Mono.empty()
                                    : allowedHosts.save(new ApiAllowedHostEntity(host)).doOnSuccess(saved ->
                                            log.debug("Bootstrapped allowed host host={}", saved.getHost()))))
                    .thenMany(Flux.fromIterable(serviceProperties.getServices())
                            .filter(service -> service.getUri() != null && !service.getUri().isBlank())
                            .concatMap(service -> apiDefinitions.existsById(service.getId())
                                    .flatMap(exists -> exists
                                            ? reactor.core.publisher.Mono.empty()
                                            : apiDefinitions.save(new ApiDefinitionEntity(
                                                    service.getId(), service.getPath(), service.getGatewayContextPath(),
                                                    service.getUri(), service.getUpstreamContextPath(), service.getStripPrefix(),
                                                    service.getRequiredScope(), service.isRemoveAuthorizationHeader()))
                                                    .doOnSuccess(saved -> log.debug("Bootstrapped API definition id={} gatewayContext={}",
                                                            saved.getId(), saved.getGatewayContextPath())))))
                    .then()
                    .block(Duration.ofSeconds(30));
            eventPublisher.publishEvent(new RefreshRoutesEvent(this));
            log.info("Runtime API and host registry bootstrap completed");
        } catch (RuntimeException error) {
            log.error("Runtime API and host registry bootstrap failed reason={}", error.getMessage(), error);
            throw error;
        }
    }
}