# Custom API Management Platform

## Overview
This project implements a custom API Management layer using Spring Cloud Gateway, Microsoft Entra ID, embedded H2, and internal backend services for users and orders. API definitions can be registered at runtime through a protected admin API.

## Features
- API routing for users and orders
- Runtime API registration backed by embedded H2
- JWT validation with Microsoft Entra ID
- Scope-based authorization
- Correlation ID propagation
- Request/response logging
- In-memory rate limiting for single-replica gateway deployments
- Timeout, retry, and circuit breaker behavior
- Containerized local deployment with Docker Compose
- Azure Container Apps deployment using GitHub Actions and OIDC

## Architecture

```text
GitHub -> GitHub Actions -> GHCR -> Azure Container Apps
             |
             +--> APIM Gateway -> User Service / Order Service
                          |
                          +--> Microsoft Entra ID
```

## Technology Stack
- Java 21
- Spring Boot 3.4.1
- Spring Cloud 2024.0.0
- Spring Cloud Gateway
- Spring Security
- Spring OAuth2 Resource Server
- Spring Data R2DBC and H2
- Microsoft Entra ID
- Maven
- Docker
- Docker Compose
- Azure Container Apps Consumption
- GitHub Actions

## Local Setup
1. Ensure Java 21 and Maven are installed.
2. Build the project:
   ```bash
   mvn clean verify
   ```
3. Start services:
   ```bash
   docker compose up --build
   ```
4. Access the gateway at http://localhost:8080.
5. API definitions are held in memory and reset when the gateway restarts.
6. Open http://localhost:8080/admin.html and connect with `local-admin-token`.

## API Examples
- GET http://localhost:8080/actuator/health
- GET http://localhost:8080/api/v1/users
- GET http://localhost:8080/api/v1/orders

## Azure Deployment
The deployment scripts in the infrastructure folder target Azure Container Apps with GitHub Actions, OIDC, and GHCR. The design keeps the gateway public while user and order services remain internal.

## Cost Control
- Use Azure for Students free allowances when available.
- The H2 database is in memory; no database or storage resource is required.
- Monitor Container Apps and GHCR usage.
- Avoid Azure API Management, Azure SQL, Redis, and NAT Gateway.

## Security
- JWT validation is enabled by the gateway.
- Request scopes such as `SCOPE_api.read` and `SCOPE_order.read` are enforced.
- Health endpoints remain public.
- No secrets are committed to Git.

## Future Enhancements
- Redis distributed rate limiting
- Centralized API consumer management
- Developer portal
- OpenAPI aggregation
- Distributed tracing and metrics

## GitHub and GHCR deployment

Configure repository variables:

- `AZURE_CLIENT_ID`
- `AZURE_TENANT_ID`
- `AZURE_SUBSCRIPTION_ID`
- `RESOURCE_GROUP`
- `CONTAINERAPPS_ENVIRONMENT`
- `AZURE_ENTRA_ISSUER_URI`
- `AZURE_ENTRA_JWK_SET_URI`

The workflow uses `${{ secrets.GITHUB_TOKEN }}` and `packages: write`; no custom GHCR secret is required.

After the first workflow run, set the three GHCR packages to public if Azure should pull them without registry credentials.
