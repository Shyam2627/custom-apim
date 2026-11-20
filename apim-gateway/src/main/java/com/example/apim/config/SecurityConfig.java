package com.example.apim.config;

import com.example.apim.security.ApiScopeAuthorizationManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ApiScopeAuthorizationManager apiScopeAuthorizationManager) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .pathMatchers("/fallback/**").permitAll()
                    .pathMatchers("/admin.html").permitAll()
                        .pathMatchers("/admin/apis", "/admin/apis/**").hasAuthority("SCOPE_apim.admin")
                        .pathMatchers("/admin/hosts", "/admin/hosts/**").hasAuthority("SCOPE_apim.admin")
                        .pathMatchers("/api/v1/**").access(apiScopeAuthorizationManager)
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    @Profile("test")
    @ConditionalOnMissingBean(ReactiveJwtDecoder.class)
    public ReactiveJwtDecoder jwtDecoder() {
        return token -> Mono.just(Jwt.withTokenValue(token)
                .header("alg", "none")
                .header("typ", "JWT")
                .claim("scp", "api.read")
                .claim("aud", "apim-gateway")
                .issuer("https://localhost")
                .subject("test-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .notBefore(Instant.now())
                .build());
    }

    @Bean
    @Profile("local")
    @ConditionalOnMissingBean(ReactiveJwtDecoder.class)
    public ReactiveJwtDecoder localJwtDecoder(
            @Value("${LOCAL_ADMIN_TOKEN:local-admin-token}") String localAdminToken) {
        return token -> {
            if (!localAdminToken.equals(token)) {
                return Mono.error(new BadJwtException("Invalid local development token"));
            }
            return Mono.just(Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .header("typ", "JWT")
                    .claim("scp", "api.read order.read external.read apim.admin")
                    .claim("aud", "apim-gateway")
                    .issuer("https://localhost")
                    .subject("local-admin")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .notBefore(Instant.now())
                    .build());
        };
    }

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("SCOPE_");
        authoritiesConverter.setAuthoritiesClaimName("scp");
        delegate.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return new Converter<Jwt, Mono<AbstractAuthenticationToken>>() {
            @Override
            public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
                return Mono.just(delegate.convert(jwt));
            }
        };
    }
}
