package com.example.apim.repository;

import com.example.apim.model.ApiDefinitionEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ApiDefinitionRepository extends ReactiveCrudRepository<ApiDefinitionEntity, String> {

    Mono<ApiDefinitionEntity> findByPath(String path);
}