#!/bin/bash

set -e  # Exit immediately if a command exits with a non-zero status

host=$(az keyvault secret show --vault-name p05 --name hostname --query value --output tsv)
storageAccountBlobUrl=$(az storage account show --name ibari --resource-group ibari --query "primaryEndpoints.blob" --output tsv)
apiClientId=$(az keyvault secret show --vault-name p05 --name backup-api-client-id --query value --output tsv)
spaClientId=$(az keyvault secret show --vault-name p05 --name backup-spa-client-id --query value --output tsv)

helm upgrade postgres-azure-backup mucsi96/spring-app \
    --install \
    --force \
    --kubeconfig .kube/config \
    --namespace backup \
    --set image=mucsi96/postgres-azure-backup:25 \
    --set host=backup.$host \
    --set clientId=$apiClientId \
    --set serviceAccountName=postgres-azure-backup \
    --set env.STORAGE_ACCOUNT_BLOB_URL=$storageAccountBlobUrl \
    --set env.STORAGE_ACCOUNT_CONTAINER_NAME=backups \
    --set env.DATABASES_CONFIG_PATH=/app/databases_config.json \
    --set env.UI_CLIENT_ID=$spaClientId \
    --set configFile[0].name=databases_config.json \
    --set configFile[0].mountPath=/app/databases_config.json \
    --set "configFile[0].data=$(cat scripts/databases_config.json | base64)" \
    --wait
