#!/usr/bin/env bash
set -euo pipefail

RESOURCE_GROUP="${RESOURCE_GROUP:-rg-custom-apim}"
CONTAINERAPPS_ENV="${CONTAINERAPPS_ENV:-cae-custom-apim}"
GHCR_OWNER="${GHCR_OWNER:-}"
GHCR_REPOSITORY="${GHCR_REPOSITORY:-custom-apim}"
IMAGE_TAG="${IMAGE_TAG:-latest}"

if [[ "${ALLOW_AZURE_CHARGES:-NO}" != "YES" ]]; then
    echo "This script deploys Azure resources that may consume student credit."
    echo "No deployment started. Set ALLOW_AZURE_CHARGES=YES after reviewing Azure pricing."
    exit 1
fi

if [[ -z "$GHCR_OWNER" ]]; then
    echo "GHCR_OWNER must be set to the GitHub owner or organization name."
    exit 1
fi

if [[ -z "${GITHUB_TOKEN:-}" ]]; then
    echo "GITHUB_TOKEN must be set for pushing or pulling private GHCR packages."
    exit 1
fi

if [[ -z "${AZURE_ENTRA_ISSUER_URI:-}" || -z "${AZURE_ENTRA_JWK_SET_URI:-}" ]]; then
    echo "AZURE_ENTRA_ISSUER_URI and AZURE_ENTRA_JWK_SET_URI must be set for Azure deployment."
    exit 1
fi

az login
az account set --subscription "${AZURE_SUBSCRIPTION_ID:-$(az account show --query id -o tsv)}"
GHCR_OWNER_LOWER="$(printf '%s' "$GHCR_OWNER" | tr '[:upper:]' '[:lower:]')"
GHCR_REPOSITORY_LOWER="$(printf '%s' "$GHCR_REPOSITORY" | tr '[:upper:]' '[:lower:]')"
echo "$GITHUB_TOKEN" | docker login ghcr.io --username "$GHCR_OWNER_LOWER" --password-stdin

USER_IMAGE="ghcr.io/${GHCR_OWNER_LOWER}/${GHCR_REPOSITORY_LOWER}-user:${IMAGE_TAG}"
ORDER_IMAGE="ghcr.io/${GHCR_OWNER_LOWER}/${GHCR_REPOSITORY_LOWER}-order:${IMAGE_TAG}"
GATEWAY_IMAGE="ghcr.io/${GHCR_OWNER_LOWER}/${GHCR_REPOSITORY_LOWER}-gateway:${IMAGE_TAG}"

docker build -f docker/user.Dockerfile -t "$USER_IMAGE" .
docker build -f docker/order.Dockerfile -t "$ORDER_IMAGE" .
docker build -f docker/apim.Dockerfile -t "$GATEWAY_IMAGE" .
docker push "$USER_IMAGE"
docker push "$ORDER_IMAGE"
docker push "$GATEWAY_IMAGE"

deploy_internal_app() {
    local name="$1"
    local image="$2"
    local port="$3"
    if az containerapp show --name "$name" --resource-group "$RESOURCE_GROUP" >/dev/null 2>&1; then
        az containerapp update --name "$name" --resource-group "$RESOURCE_GROUP" --image "$image"
    else
        az containerapp create --name "$name" --resource-group "$RESOURCE_GROUP" --environment "$CONTAINERAPPS_ENV" --image "$image" --ingress internal --target-port "$port" --min-replicas 0 --max-replicas 1 --env-vars SERVER_PORT="$port"
    fi
}

deploy_internal_app user-service "$USER_IMAGE" 8081
deploy_internal_app order-service "$ORDER_IMAGE" 8082

if az containerapp show --name custom-apim --resource-group "$RESOURCE_GROUP" >/dev/null 2>&1; then
    az containerapp update --name custom-apim --resource-group "$RESOURCE_GROUP" --image "$GATEWAY_IMAGE" --set-env-vars \
        USER_SERVICE_URL=http://user-service \
        ORDER_SERVICE_URL=http://order-service \
        API_ALLOWED_HOSTS=user-service,order-service \
        AZURE_ENTRA_ISSUER_URI="$AZURE_ENTRA_ISSUER_URI" \
        AZURE_ENTRA_JWK_SET_URI="$AZURE_ENTRA_JWK_SET_URI"
else
    az containerapp create --name custom-apim --resource-group "$RESOURCE_GROUP" --environment "$CONTAINERAPPS_ENV" --image "$GATEWAY_IMAGE" --ingress external --target-port 8080 --min-replicas 0 --max-replicas 1 --env-vars \
        USER_SERVICE_URL=http://user-service \
        ORDER_SERVICE_URL=http://order-service \
        API_ALLOWED_HOSTS=user-service,order-service \
        AZURE_ENTRA_ISSUER_URI="$AZURE_ENTRA_ISSUER_URI" \
        AZURE_ENTRA_JWK_SET_URI="$AZURE_ENTRA_JWK_SET_URI"
fi

echo "Deployed gateway and internal user/order services."
echo "Gateway image: $GATEWAY_IMAGE"
