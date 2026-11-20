package com.example.apim.security;

import com.example.apim.model.ApiDefinitionEntity;
import com.example.apim.repository.ApiDefinitionRepository;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

@Component
public class ApiScopeAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private static final Logger log = LoggerFactory.getLogger(ApiScopeAuthorizationManager.class);

    private final ApiDefinitionRepository apiDefinitions;
    private final PathPatternParser pathPatternParser = new PathPatternParser();

    public ApiScopeAuthorizationManager(ApiDefinitionRepository apiDefinitions) {
        this.apiDefinitions = apiDefinitions;
    }

    @Override
    public Mono<AuthorizationDecision> check(
            Mono<Authentication> authentication,
            AuthorizationContext context) {
        String requestPath = context.getExchange().getRequest().getPath().pathWithinApplication().value();

        return authentication
            .filter(auth -> auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken))
                .flatMap(authenticated -> apiDefinitions.findAll()
                        .filter(definition -> matches(definition, requestPath))
                        .collectList()
                        .map(definitions -> {
                            if (definitions.isEmpty()) {
                                log.warn("No API definition found for request path: {}", requestPath);
                            } else {
                                log.info("Matched API definitions for request path {}: {}", requestPath, definitions);
                            }
                            return definitions.stream()
                                    .max(Comparator.comparingInt(definition -> definition.getPath().length()))
                                    .map(definition -> hasScope(authenticated, definition))
                                    .orElse(false);
                        }))
                .doOnSuccess(authorized -> {
                    if (authorized) {
                        log.info("Authorization granted for request path: {}", requestPath);
                    } else {
                        log.warn("Authorization denied for request path: {}", requestPath);
                    }
                })
                .map(AuthorizationDecision::new)
                .defaultIfEmpty(new AuthorizationDecision(false))
                .doOnError(error -> log.error("Error occurred during authorization check for request path: {}", requestPath, error));
    }

    private boolean matches(ApiDefinitionEntity definition, String requestPath) {
        PathPattern pattern = pathPatternParser.parse(definition.getPath());
        return pattern.matches(org.springframework.http.server.PathContainer.parsePath(requestPath));
    }

    private boolean hasScope(Authentication authentication, ApiDefinitionEntity definition) {
        String requiredAuthority = "SCOPE_" + definition.getRequiredScope();
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> requiredAuthority.equals(authority.getAuthority()));
    }
}