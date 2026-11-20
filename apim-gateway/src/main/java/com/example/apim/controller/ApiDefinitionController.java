package com.example.apim.controller;

import com.example.apim.model.ApiDefinitionEntity;
import com.example.apim.model.ApiDefinitionRequest;
import com.example.apim.service.ApiDefinitionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/admin/apis")
public class ApiDefinitionController {

    private static final Logger log = LoggerFactory.getLogger(ApiDefinitionController.class);
    private final ApiDefinitionService apiDefinitionService;

    public ApiDefinitionController(ApiDefinitionService apiDefinitionService) {
        this.apiDefinitionService = apiDefinitionService;
    }

    @GetMapping
    public Flux<ApiDefinitionEntity> findAll() {
        log.debug("Admin API list request received");
        return apiDefinitionService.findAll()
                .doOnNext(definition -> log.trace("Returning API definition id={} gatewayContext={}", definition.getId(), definition.getGatewayContextPath()))
                .doOnError(error -> log.error("Failed to list API definitions reason={}", error.getMessage(), error));
    }

    @GetMapping("/{id}")
    public Mono<ApiDefinitionEntity> findById(@PathVariable("id") String id) {
        log.debug("Admin API get request id={}", id);
        return apiDefinitionService.findById(id)
                .doOnError(error -> log.error("Failed to get API definition id={} reason={}", id, error.getMessage(), error));
    }

    @PostMapping
    public Mono<ResponseEntity<ApiDefinitionEntity>> create(@RequestBody ApiDefinitionRequest request) {
        log.debug("Admin API create request id={} gatewayContext={} host={}", request.id(), request.gatewayContextPath(), request.uri());
        return apiDefinitionService.create(request)
                .map(created -> ResponseEntity.created(URI.create("/admin/apis/" + created.getId())).body(created))
                .doOnError(error -> log.error("Admin API create failed id={} reason={}", request.id(), error.getMessage(), error));
    }

    @PutMapping("/{id}")
    public Mono<ApiDefinitionEntity> update(
            @PathVariable("id") String id,
            @RequestBody ApiDefinitionRequest request) {
        log.debug("Admin API update request id={} gatewayContext={} host={}", id, request.gatewayContextPath(), request.uri());
        return apiDefinitionService.update(id, request)
                .doOnError(error -> log.error("Admin API update failed id={} reason={}", id, error.getMessage(), error));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable("id") String id) {
        log.debug("Admin API delete request id={}", id);
        return apiDefinitionService.delete(id)
                .then(Mono.defer(() -> Mono.<ResponseEntity<Void>>just(ResponseEntity.noContent().build())))
                .doOnError(error -> log.error("Admin API delete failed id={} reason={}", id, error.getMessage(), error));
    }
}