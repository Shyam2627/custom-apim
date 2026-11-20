# Azure deployment with GitHub Actions and GHCR

## Prerequisites

- GitHub account and repository
- Azure for Students subscription
- Azure CLI with the `containerapp` extension
- Docker Engine or Docker Desktop
- GitHub Container Registry access
- Microsoft Entra app registration for JWT validation

This setup does not require Azure Container Registry, Azure Files, PostgreSQL, or an H2 sidecar.

## 1. Create Azure resources

In PowerShell:

```powershell
$env:RESOURCE_GROUP="rg-custom-apim"
$env:LOCATION="eastus"
$env:CONTAINERAPPS_ENV="cae-custom-apim"
$env:ALLOW_AZURE_CHARGES="YES"

bash infrastructure/azure/create-resources.sh
```

This creates only:

- One resource group
- One Azure Container Apps environment

## 2. Configure Microsoft Entra

Create an app registration that represents the gateway API. Configure these values:

```text
Issuer URI:
https://login.microsoftonline.com/<tenant-id>/v2.0

JWK set URI:
https://login.microsoftonline.com/<tenant-id>/discovery/v2.0/keys

Scopes:
api.read
order.read
external.read
apim.admin
```

Set the values locally:

```powershell
$env:AZURE_ENTRA_ISSUER_URI="https://login.microsoftonline.com/<tenant-id>/v2.0"
$env:AZURE_ENTRA_JWK_SET_URI="https://login.microsoftonline.com/<tenant-id>/discovery/v2.0/keys"
```

Never enable `SPRING_PROFILES_ACTIVE=local` in Azure.

## 3. Push source to GitHub

```powershell
git init
git add .
git commit -m "Initial custom APIM setup"
git branch -M main
git remote add origin https://github.com/<github-owner>/<github-repository>.git
git push -u origin main
```

## 4. Configure GitHub repository variables

Open **Settings → Secrets and variables → Actions → Variables** and add:

- `AZURE_CLIENT_ID`
- `AZURE_TENANT_ID`
- `AZURE_SUBSCRIPTION_ID`
- `RESOURCE_GROUP=rg-custom-apim`
- `CONTAINERAPPS_ENVIRONMENT=cae-custom-apim`
- `AZURE_ENTRA_ISSUER_URI`
- `AZURE_ENTRA_JWK_SET_URI`

No Docker Hub or Azure Container Registry secret is required.

## 5. Configure GHCR packages

The workflow builds three packages:

```text
ghcr.io/<github-owner>/<github-repository>-user:<git-sha>
ghcr.io/<github-owner>/<github-repository>-order:<git-sha>
ghcr.io/<github-owner>/<github-repository>-gateway:<git-sha>
```

Push the images by running the workflow. For Azure to pull them without credentials, open each package in GitHub and set **Package settings → Visibility → Public**.

A private GHCR package requires Azure Container Apps registry credentials; public packages are simpler for a student project.

## 6. Deploy through GitHub Actions

Open **Actions → Deploy to Azure Container Apps → Run workflow**. The workflow:

1. Builds the Java project.
2. Logs into GHCR using `${{ secrets.GITHUB_TOKEN }}`.
3. Builds and pushes all three container images.
4. Creates or updates internal user and order apps.
5. Creates or updates the external gateway app.

The workflow runs automatically after pushes to `main` or `master`. A manual workflow run does not deploy unless `allow_azure_charges=true`.

## 7. Verify the deployment

Get the gateway URL:

```powershell
az containerapp show --name custom-apim --resource-group rg-custom-apim --query properties.configuration.ingress.fqdn -o tsv
```

Test health:

```powershell
curl https://<gateway-fqdn>/actuator/health
```

API requests require Entra JWT access:

```text
Authorization: Bearer <entra-access-token>
```

Expected status behavior:

- Missing or invalid JWT: `401`
- Valid JWT but insufficient scope: `403`
- Valid JWT with required scope: request forwarded

## 8. Cost control

- Use Azure Container Apps Consumption mode.
- Keep `minReplicas=0` and `maxReplicas=1`.
- Do not create Azure Container Registry or Azure Files.
- H2 API definitions disappear when the gateway restarts or scales to zero.
- GHCR package storage and pulls are separate from Azure credit; public packages avoid Azure pull credentials.
- Create a Cost Management budget for notifications, but remember it does not stop resources.

## 9. Registry summary

GHCR supplies the gateway, user, and order images. GitHub Actions authenticates with `${{ secrets.GITHUB_TOKEN }}`; no separate container registry account or token is required for public packages.
