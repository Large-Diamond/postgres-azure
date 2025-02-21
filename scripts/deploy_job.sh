#!/bin/bash

set -e  # Exit immediately if a command exits with a non-zero status

apiClientId=$(az keyvault secret show --vault-name p05 --name backup-api-client-id --query value --output tsv)
cronJobClientId=$(az keyvault secret show --vault-name p05 --name backup-cron-job-client-id --query value --output tsv)

helm upgrade postgres-azure-backup-cron-job cron-jobs/chart \
    --install \
    --force \
    --kubeconfig .kube/config \
    --namespace backup \
    --set clientId=$cronJobClientId \
    --set env.API_CLIENT_ID=$apiClientId \
    --wait
