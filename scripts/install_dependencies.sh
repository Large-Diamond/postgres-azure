#!/bin/sh

blobStorageEndpointUrl=$(az storage account show --name ibari --resource-group ibari --query "primaryEndpoints.blob" --output tsv)
spaClientId=$(az keyvault secret show --vault-name p05 --name backup-spa-client-id --query value -o tsv)
apiClientId=$(az keyvault secret show --vault-name p05 --name backup-api-client-id --query value -o tsv)
tenantId=$(az keyvault secret show --vault-name p05 --name tenant-id --query value -o tsv)

echo "BLOBSTORAGE_ENDPOINT_URL=$blobStorageEndpointUrl" > .env
echo "DATABASES_CONFIG_PATH=../scripts/databases_config.json" >> .env
echo "AUTH_ISSUER_URI=https://login.microsoftonline.com/$tenantId/v2.0" >> .env
echo "AUTH_JWK_SET_URI=https://login.microsoftonline.com/$tenantId/discovery/v2.0/keys" >> .env

echo "NG_APP_TENANT_ID=$tenantId" > client/.env
echo "NG_APP_CLIENT_ID=$spaClientId" >> client/.env
echo "NG_APP_API_CLIENT_ID=$apiClientId" >> client/.env

pip install -r requirements.txt

cd server && mvn clean install && cd ..
cd client && npm install && cd ..

playwright install --with-deps chromium

