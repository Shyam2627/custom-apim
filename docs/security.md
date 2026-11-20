# Security

## Entra ID configuration
1. Create an application registration in Microsoft Entra ID.
2. Expose an API and define scopes such as `api.read` and `order.read`.
3. Configure your front-end or API client to request the relevant scopes.
4. Set the gateway JWT issuer and JWK URI through environment variables.

## Gateway behavior
- Validates JWT signature.
- Checks issuer and audience claims.
- Applies scope-based authorization.
- Rejects requests without valid JWT with HTTP 401.
- Rejects malformed scope matches with HTTP 403.
