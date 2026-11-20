package com.example.apim.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class InMemoryRateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(InMemoryRateLimitFilter.class);

    private final Map<String, Deque<Long>> requestTimestamps = new ConcurrentHashMap<>();
    private final int maxRequestsPerMinute;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InMemoryRateLimitFilter(@Value("${app.rate-limit.requests-per-minute:100}") int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientId = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "anonymous";

        Deque<Long> timestamps = requestTimestamps.computeIfAbsent(clientId, key -> new ArrayDeque<>());
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000L;
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxRequestsPerMinute) {
            log.warn("Rate limit exceeded clientId={} path={} limit={} correlationId={}",
                    clientId, exchange.getRequest().getPath().value(), maxRequestsPerMinute,
                    exchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID));
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            response.getHeaders().add(CorrelationIdFilter.CORRELATION_ID,
                    exchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID));
            String body;
            try {
                body = objectMapper.writeValueAsString(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", 429,
                        "error", "TOO_MANY_REQUESTS",
                        "message", "Rate limit exceeded. Please try again later.",
                        "path", exchange.getRequest().getPath().value(),
                        "correlationId", exchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID)
                ));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize rate-limit response path={} reason={}",
                        exchange.getRequest().getPath().value(), e.getMessage(), e);
                body = "{\"status\":429,\"error\":\"TOO_MANY_REQUESTS\"}";
            }
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());
            return response.writeWith(Mono.just(buffer));
        }

        timestamps.addLast(now);
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
