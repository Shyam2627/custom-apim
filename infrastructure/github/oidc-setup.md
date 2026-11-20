# GitHub Actions + Azure OIDC + GHCR setup

## 1. Create Azure OIDC credentials

1. Create or use an Entra app registration for GitHub Actions.
2. Add a federated credential:
   - Issuer: `https://token.actions.githubusercontent.com`
   - Audience: `api://AzureADTokenExchange`
   - Subject: `repo:<github-owner>/<github-repository>:ref:refs/heads/main`
3. Grant the app registration `Contributor` on `rg-custom-apim` or the resource group used in GitHub variables.

## 2. Configure GitHub repository variables

In GitHub: **Settings → Secrets and variables → Actions → Variables**, add:

- `AZURE_CLIENT_ID`
- `AZURE_TENANT_ID`
- `AZURE_SUBSCRIPTION_ID`
- `RESOURCE_GROUP=rg-custom-apim`
- `CONTAINERAPPS_ENVIRONMENT=cae-custom-apim`
- `AZURE_ENTRA_ISSUER_URI=https://login.microsoftonline.com/<tenant-id>/v2.0`
- `AZURE_ENTRA_JWK_SET_URI=https://login.microsoftonline.com/<tenant-id>/discovery/v2.0/keys`

No Docker Hub variables or secrets are needed. GHCR authentication uses the workflow GITHUB_TOKEN.

## 3. Grant GHCR package access

The workflow uses `${{ secrets.GITHUB_TOKEN }}` and `packages: write`.

For Azure to pull the images without credentials:

1. Open each package: `<github-repository>-user`, `<github-repository>-order`, `<github-repository>-gateway`.
2. Open **Package settings**.
3. Set visibility to **Public**.

Alternatively, configure Azure Container Apps registry credentials for private GHCR packages. Public packages are the simplest project demo setup.

## 4. Enable deployment

The deploy workflow runs on pushes to `main` or `master`, and can also be started manually.

Manual deployment is allowed only when `allow_azure_charges=true`.

## 5. Push source to GitHub

```bash
git init
git add .
git commit -m "Initial project setup"
git branch -M main
git remote add origin https://github.com/<github-owner>/<github-repository>.git
git push -u origin main
```

A push to `main` builds and pushes GHCR images, then creates or updates the Azure Container Apps.

Do not commit Azure credentials, Entra client secrets, or JWT tokens.
