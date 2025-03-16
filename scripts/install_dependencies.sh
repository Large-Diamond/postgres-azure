#!/bin/sh

set -e  # Exit immediately if a command exits with a non-zero status

storageAccountBlobUrl=$(az storage account show --name ibari --resource-group ibari --query "primaryEndpoints.blob" --output tsv)
spaClientId=$(az keyvault secret show --vault-name p05 --name backup-spa-client-id --query value -o tsv)
apiClientId=$(az keyvault secret show --vault-name p05 --name backup-api-client-id --query value -o tsv)
tenantId=$(az keyvault secret show --vault-name p05 --name tenant-id --query value -o tsv)

echo "STORAGE_ACCOUNT_BLOB_URL=$storageAccountBlobUrl" > .env
echo "STORAGE_ACCOUNT_CONTAINER_NAME=backups" > .env
echo "DATABASES_CONFIG_PATH=../scripts/databases_config.json" >> .env
echo "AZURE_TENANT_ID=$tenantId" >> .env
echo "AZURE_CLIENT_ID=$apiClientId" >> .env
echo "UI_CLIENT_ID=$spaClientId" >> .env
echo "SPRING_ACTUATOR_PORT=8082" >> .env
echo "SPRING_PROFILES_ACTIVE=prod" >> .env



echo "NG_APP_TENANT_ID=$tenantId" > client/.env
echo "NG_APP_CLIENT_ID=$spaClientId" >> client/.env
echo "NG_APP_API_CLIENT_ID=$apiClientId" >> client/.env

pip install -r requirements.txt

cd server && mvn clean install && cd ..
cd client && npm install && cd ..

playwright install --with-deps chromium

