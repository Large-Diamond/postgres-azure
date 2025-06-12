#!/bin/bash

set -e  # Exit immediately if a command exits with a non-zero status

host=$(az keyvault secret show --vault-name p05 --name hostname --query value --output tsv)
apiClientId=$(az keyvault secret show --vault-name p05 --name backup-api-client-id --query value --output tsv)
cronJobClientId=$(az keyvault secret show --vault-name p05 --name backup-cron-job-client-id --query value --output tsv)
latestTag=$(curl -s "https://registry.hub.docker.com/v2/repositories/mucsi96/postgres-azure-backup-job/tags/" | jq -r '.results |  map(select(.name != "latest")) | sort_by(.last_updated) | reverse | .[0].name')

echo "Deploying mucsi96/postgres-azure-backup-job:$latestTag to backup.$host"

helm upgrade postgres-azure-backup-cron-job backup-job/chart \
    --install \
    --force \
    --kubeconfig .kube/config \
    --namespace backup \
    --set image=mucsi96/postgres-azure-backup-job:$latestTag \
    --set clientId=$cronJobClientId \
    --set serviceAccountName=postgres-azure-backup-cron-job-workload-identity \
    --set env.API_CLIENT_ID=$apiClientId \
    --wait
