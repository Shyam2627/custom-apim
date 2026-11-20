package com.example.apim.exception;

import com.example.apim.filter.CorrelationIdFilter;
import com.example.apim.model.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException ex,
            ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        String correlationId = exchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID);
        String reason = ex.getReason() == null ? "Unexpected error" : ex.getReason();

        if (ex.getStatusCode().is5xxServerError()) {
            log.error("Request failed status={} path={} correlationId={} reason={}",
                    ex.getStatusCode().value(), path, correlationId, reason, ex);
        } else {
            log.debug("Request rejected status={} path={} correlationId={} reason={}",
                    ex.getStatusCode().value(), path, correlationId, reason);
        }

        return ResponseEntity.status(ex.getStatusCode())
                .body(new ApiErrorResponse(
                        Instant.now(),
                        ex.getStatusCode().value(),
                        ex.getStatusCode().is4xxClientError() ? "CLIENT_ERROR" : "SERVER_ERROR",
                        reason,
                        path,
                        correlationId == null ? "" : correlationId
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(
            Exception ex,
            ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        String correlationId = exchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID);

        log.error("Unhandled gateway error path={} correlationId={} reason={}",
                path, correlationId, ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(
                        Instant.now(),
                        500,
                        "INTERNAL_SERVER_ERROR",
                        "An unexpected error occurred",
                        path,
                        correlationId == null ? "" : correlationId
                ));
    }
}
