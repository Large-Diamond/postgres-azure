#!/bin/bash

set -e  # Exit immediately if a command exits with a non-zero status

# Detect if running on Ubuntu
if [ "$(uname -s)" = "Linux" ] && [ -f /etc/os-release ]; then
    . /etc/os-release
    if [ "$ID" = "ubuntu" ]; then
        echo "Running on Ubuntu. Checking dependencies..."

        # Check and install jq
        if ! command -v jq &> /dev/null; then
            echo "Installing jq..."
            sudo apt-get install -y jq
        else
            echo "jq is already installed."
        fi
    fi
fi

host=$(az keyvault secret show --vault-name p05 --name hostname --query value --output tsv)
storageAccountBlobUrl=$(az storage account show --name ibari --resource-group ibari --query "primaryEndpoints.blob" --output tsv)
apiClientId=$(az keyvault secret show --vault-name p05 --name backup-api-client-id --query value --output tsv)
spaClientId=$(az keyvault secret show --vault-name p05 --name backup-spa-client-id --query value --output tsv)
latestTag=$(curl -s "https://registry.hub.docker.com/v2/repositories/mucsi96/postgres-azure-backup/tags/" | jq -r '.results |  map(select(.name != "latest")) | sort_by(.last_updated) | reverse | .[0].name')

echo "Deploying mucsi96/postgres-azure-backup:$latestTag to https://backup.$host"

helm repo update

helm upgrade postgres-azure-backup mucsi96/spring-app \
    --install \
    --force \
    --kubeconfig .kube/config \
    --namespace backup \
    --set image=mucsi96/postgres-azure-backup:$latestTag \
    --set host=backup.$host \
    --set clientId=$apiClientId \
    --set serviceAccountName=postgres-azure-backup-api-workload-identity \
    --set env.STORAGE_ACCOUNT_BLOB_URL=$storageAccountBlobUrl \
    --set env.STORAGE_ACCOUNT_CONTAINER_NAME=backups \
    --set env.DATABASES_CONFIG_PATH=/app/databases_config.json \
    --set env.UI_CLIENT_ID=$spaClientId \
    --set configFile[0].name=databases_config.json \
    --set configFile[0].mountPath=/app/databases_config.json \
    --set "configFile[0].data=$(cat scripts/databases_config.json | base64)" \
    --wait
