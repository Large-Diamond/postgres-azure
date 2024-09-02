#!/bin/sh

brew update && brew upgrade && brew install libpq miniconda

blobstorage_endpoint_url=$(az keyvault secret show --vault-name p02 --name demo-db-backup-endpoint-url --query value -o tsv)

echo "BLOBSTORAGE_ENDPOINT_URL=$blobstorage_endpoint_url" > .env

cd client && npm install && cd ..

if [ ! -d ".conda" ]; then
    conda init
    conda create --prefix .conda --yes python=3.12
fi

conda activate $PWD/.conda
pip install -r requirements.txt