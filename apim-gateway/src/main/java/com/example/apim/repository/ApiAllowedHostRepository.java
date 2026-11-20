package com.example.apim.repository;

import com.example.apim.model.ApiAllowedHostEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface ApiAllowedHostRepository extends ReactiveCrudRepository<ApiAllowedHostEntity, String> {
}