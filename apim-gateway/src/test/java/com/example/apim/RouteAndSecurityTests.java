package com.example.apim;

import com.example.apim.security.ApiScopeAuthorizationManager;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class RouteAndSecurityTests {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private Map<String, RouteLocator> routeLocators;

    @Autowired
    private ApiScopeAuthorizationManager apiScopeAuthorizationManager;

    @Test
    void seededApiDefinitionsAreLoadedAsGatewayRoutes() {
        Boolean routeLoaded = Flux.fromIterable(routeLocators.values())
                .flatMap(RouteLocator::getRoutes)
                .any(route -> "user-service".equals(route.getId()))
                .block();

        assertTrue(Boolean.TRUE.equals(routeLoaded));
    }

//    @Test
//    void unauthorizedRequestWithoutJwt() {
//        webTestClient.get()
//                .uri("/api/v1/users")
//                .exchange()
//                .expectStatus().isUnauthorized();
//    }

        @Test
        void correctScopeIsGrantedForDatabaseRoute() {
        AuthorizationContext context = new AuthorizationContext(
            MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/users").build()), Map.of());
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
            "test-user", "", List.of(new SimpleGrantedAuthority("SCOPE_api.read")));

        AuthorizationDecision decision = apiScopeAuthorizationManager
            .check(Mono.just(authentication), context)
            .block();

        assertTrue(decision != null && decision.isGranted());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_order.read")
    void forbiddenRequestWhenWrongScope() {
        webTestClient.get()
                .uri("/api/v1/users")
                .exchange()
                .expectStatus().isForbidden();
    }

//    @Test
//    void externalApiRequiresJwt() {
//        webTestClient.get()
//                .uri("/api/v1/external/status")
//                .exchange()
//                .expectStatus().isUnauthorized();
//    }

    @Test
    @WithMockUser(authorities = "SCOPE_api.read")
    void externalApiRequiresExternalScope() {
        webTestClient.get()
                .uri("/api/v1/external/status")
                .exchange()
                .expectStatus().isForbidden();
    }

        @Test
        void adminConsoleIsPublicButAdminApiRequiresScope() {
        webTestClient.get()
            .uri("/admin.html")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML);

        webTestClient.get()
            .uri("/admin/apis")
            .exchange()
            .expectStatus().isUnauthorized();
        }

        @Test
        @WithMockUser(authorities = "SCOPE_apim.admin")
        void adminCanRegisterApiAndItsScopeAppliesImmediately() {
        webTestClient.post()
                                .uri("/admin/apis")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue("""
                                                {
                                                    "id": "weather-api",
                                                    "path": "/api/v1/weather/**",
                                                    "uri": "https://api.example.com",
                                                    "stripPrefix": 3,
                                                    "requiredScope": "weather.read",
                                                    "removeAuthorizationHeader": true
                                                }
                                                """)
                                .exchange()
                                .expectStatus().isCreated();

                            Boolean dynamicRouteLoaded = Flux.fromIterable(routeLocators.values())
                                .flatMap(RouteLocator::getRoutes)
                                .any(route -> "weather-api".equals(route.getId()))
                                .block();
                            assertTrue(Boolean.TRUE.equals(dynamicRouteLoaded));

        webTestClient.get()
                                .uri("/api/v1/weather/current")
                                .exchange()
                                .expectStatus().isForbidden();
        }
}
