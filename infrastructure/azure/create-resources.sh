#!/usr/bin/env bash
set -euo pipefail

RESOURCE_GROUP="${RESOURCE_GROUP:-rg-custom-apim}"
LOCATION="${LOCATION:-eastus}"
CONTAINERAPPS_ENV="${CONTAINERAPPS_ENV:-cae-custom-apim}"

if [[ "${ALLOW_AZURE_CHARGES:-NO}" != "YES" ]]; then
    echo "This script creates billable Azure resources."
    echo "No Azure resources were created. Set ALLOW_AZURE_CHARGES=YES after reviewing Azure pricing."
    exit 1
fi

az login
az account set --subscription "${AZURE_SUBSCRIPTION_ID:-$(az account show --query id -o tsv)}"
az group create --name "$RESOURCE_GROUP" --location "$LOCATION"
az containerapp env create --name "$CONTAINERAPPS_ENV" --resource-group "$RESOURCE_GROUP" --location "$LOCATION"

echo "Resource group: $RESOURCE_GROUP"
echo "Container Apps environment: $CONTAINERAPPS_ENV"
echo "No Azure Container Registry was created."
