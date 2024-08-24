#!/bin/bash

brew update && brew upgrade && brew install libpq

blobstorage_endpoint_url=$(az keyvault secret show --vault-name p02 --name demo-db-backup-endpoint-url --query value -o tsv)

echo "BLOBSTORAGE_ENDPOINT_URL=$blobstorage_endpoint_url" > .env

cd client && npm install && cd ..