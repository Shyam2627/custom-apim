package com.example.apim.controller;

import com.example.apim.model.ApiAllowedHostEntity;
import com.example.apim.model.ApiAllowedHostRequest;
import com.example.apim.service.ApiAllowedHostService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/admin/hosts")
public class ApiAllowedHostController {

    private final ApiAllowedHostService allowedHostService;
    private static final Logger log = LoggerFactory.getLogger(ApiAllowedHostController.class);

    public ApiAllowedHostController(ApiAllowedHostService allowedHostService) {
        this.allowedHostService = allowedHostService;
    }

    @GetMapping
    public Flux<ApiAllowedHostEntity> findAll() {
        log.debug("Request to fetch all allowed hosts");
        return allowedHostService.findAll()
            .doOnError(e -> log.error("Error fetching allowed hosts: {}", e.getMessage()))
            .doOnComplete(() -> log.debug("Successfully fetched all allowed hosts"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiAllowedHostEntity> add(@RequestBody ApiAllowedHostRequest request) {
        log.debug("Request to add new allowed host: {}", request.host());
        return allowedHostService.add(request)
            .doOnError(e -> log.error("Error adding allowed host: {}", e.getMessage()))
            .doOnSuccess(entity -> log.debug("Successfully added allowed host: {}", entity.getHost()));
    }

    @DeleteMapping("/{host}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable("host") String host) {
        log.debug("Request to delete allowed host: {}", host);
        return allowedHostService.delete(host)
            .doOnError(e -> log.error("Error deleting allowed host: {}", e.getMessage()))
            .doOnSuccess(aVoid -> log.debug("Successfully deleted allowed host: {}", host));
    }
}