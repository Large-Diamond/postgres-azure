#!/bin/bash

set -e  # Exit immediately if a command exits with a non-zero status

mkdir -p .kube

az keyvault secret show --vault-name p05 --name backup-namespace-k8s-user-config --query value --output tsv > .kube/config

chmod 0600 .kube/config
