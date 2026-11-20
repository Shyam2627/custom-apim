# API documentation

## Protected routes
- GET /api/v1/users
- GET /api/v1/users/{id}
- GET /api/v1/orders
- GET /api/v1/orders/{id}

Default route scopes:
- `SCOPE_api.read` for user routes
- `SCOPE_order.read` for order routes

Each runtime API definition specifies its own required scope. Admin endpoints require
`SCOPE_apim.admin`.

## Runtime API onboarding
Open `/admin.html` in the gateway to use the browser-based API registry. The page itself
is public, but reading or changing definitions still requires an Entra token with
`apim.admin`; the UI keeps the token in memory and does not persist it.

For local Docker Compose only, the `local` Spring profile accepts the fixed development
token `local-admin-token`. Paste it into the admin page to connect. Override it with
`LOCAL_ADMIN_TOKEN` in your local environment if desired. Never activate the `local`
profile in Azure or another shared environment; use Entra-issued tokens there.

New API definitions and approved upstream hosts are stored in embedded H2 and can be
added while the gateway runs. Approve an external hostname in the admin page before
creating an API that points to it; this does not require a restart. The host endpoints
require `SCOPE_apim.admin`:

- `GET /admin/hosts`
- `POST /admin/hosts` with `{"host":"httpbin.org"}`
- `DELETE /admin/hosts/{host}` (only when no API uses the host)

API definition endpoints also require `SCOPE_apim.admin`:

- `GET /admin/apis`
- `POST /admin/apis`
- `GET /admin/apis/{id}`
- `PUT /admin/apis/{id}`
- `DELETE /admin/apis/{id}`

Example request:

```json
{
  "id": "weather-api",
  "path": "/api/v1/weather/**",
  "uri": "https://api.example.com",
  "stripPrefix": 3,
  "requiredScope": "weather.read",
  "removeAuthorizationHeader": true
}
```

Approve `api.example.com` in the host list before submitting this example. The gateway
accepts fully qualified DNS names and rejects local/internal-style names. Approve only
trusted upstream hosts. External APIs should remove the caller's Authorization header;
provider credentials are not stored in the API definition and must be configured
separately. User and order routes are inserted from `application.yml` only when their
database records are absent; their service hostnames are pre-approved at startup.

Submit the definition with an Entra access token containing `apim.admin`:

```bash
curl -X POST http://localhost:8080/admin/apis \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  --data @api-definition.json
```

## H2 storage
The gateway uses an in-memory H2 database. It requires no separate database container,
storage mount, or database credentials. API definitions are lost when the gateway
restarts or is replaced with a new Container Apps revision; the configured user and
order routes are seeded again at startup. Use persistent database storage if runtime
definitions must survive restarts.

## Health endpoints
- /actuator/health
- /actuator/info
- /actuator/metrics

These should be exposed selectively, with only health and info left public by default.

## Context paths
Each API has two independent context paths:

- `gatewayContextPath`: unique public name under `/api/v1`; for example `users` creates `/api/v1/users/**`.
- `upstreamContextPath`: base path sent upstream; for example `/users` sends `/users/100` to the user service.

Example internal service:

```json
{
  "id": "user-service",
  "gatewayContextPath": "users",
  "uri": "http://user-service:8081",
  "upstreamContextPath": "/users",
  "stripPrefix": 3,
  "requiredScope": "api.read"
}
```

Example external service:

```json
{
  "id": "httpbin-test",
  "gatewayContextPath": "httpbin",
  "uri": "https://httpbin.org",
  "upstreamContextPath": "",
  "stripPrefix": 3,
  "requiredScope": "external.read",
  "removeAuthorizationHeader": true
}
```

A gateway context must be unique. Approve the upstream host separately in `/admin/hosts` before adding or updating the API.
