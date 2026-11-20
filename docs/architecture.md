# Architecture

The solution contains an APIM gateway, user service, order service, and an embedded H2 API registry.

- APIM gateway is the only public entry point.
- User and order services are internal and not directly exposed.
- Microsoft Entra ID validates JWTs before requests are forwarded.
- The gateway enforces route, scope, rate limit, and resilience policies.
- API definitions and required scopes are stored in the gateway's in-memory H2 database.
- Admin changes publish a route refresh event; gateway restarts are not required.

## Request flow
1. Client sends request to the public API gateway.
2. Correlation ID and rate limit filters run.
3. Security filter validates the JWT and resolves the API's required scope from H2.
4. Gateway routes the request using the current database-backed route definitions.
5. Retry and circuit breaker protect backend calls.
6. Response is returned to the client with correlation ID in headers.

## Runtime onboarding
Administrators create, update, list, and delete API definitions through `/admin/apis`.
The gateway stores each definition and required scope in H2, then refreshes its routes.
Requests to `/api/v1/**` use the matching database definition for scope authorization.
Definitions are lost when the gateway process stops or its Container App replica scales down.
