package com.example.apim.controller;

import com.example.apim.model.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

@RestController
public class FallbackController {

    private static final Logger logger = LoggerFactory.getLogger(FallbackController.class);

    @GetMapping("/fallback/service-unavailable")
    public ResponseEntity<ApiErrorResponse> serviceUnavailable(
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        logger.error("Service unavailable error occurred. Correlation ID: {}", correlationId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponse(
                        Instant.now(),
                        503,
                        "SERVICE_UNAVAILABLE",
                        "Backend service is temporarily unavailable",
                        "/fallback/service-unavailable",
                        correlationId != null ? correlationId : ""
                ));
    }
}
