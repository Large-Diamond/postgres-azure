# postgres-azure-backup

Simple PostgreSQL backup tool to Azure with UI

![PostgreSQL backup tool screenshot](docs/postrgress-backup-tool-1.png)

## Features

- Supports multiple databases
- List tables and records of actual database
- Show last backup time
- Create backups with retention period
- Cleanup expired backups
- Restore backups
- Exclude tables from backup
- Can be used without UI as REST API. For example using a cronjob and curl
- Fully covered with E2E Selenium tests
- Compatible with PostgreSQL 16
- Azure cloud native

## Stack

- Java 21
- Spring Boot 3
- Angular
- Azure

## Required environment variables

- `AZURE_CLIENT_ID`
- `AZURE_TENANT_ID`
- `SPRING_ACTUATOR_PORT`
- `BLOBSTORAGE_ENDPOINT_URL`
- `DATABASES_CONFIG_PATH`
- `UI_CLIENT_ID`

## Database config file

```json
[
  {
    "name": "db1",
    "host": "db1",
    "port": 5432,
    "database": "test",
    "username": "postgres",
    "password": "postgres",
    "excludeTables": ["passwords", "secrets"]
  },
  {
    "name": "db2",
    "host": "db2",
    "port": 5432,
    "database": "test",
    "username": "postgres",
    "password": "postgres",
    "excludeTables": ["passwords", "secrets"]
  }
]
```

## Deployment with Helm

```bash
host=$(...)
blobStorageEndpointUrl=$(...)
apiClientId=$(...)
spaClientId=$(...)

helm repo add mucsi96 https://mucsi96.github.io/k8s-helm-charts
helm install mucsi96/spring-app \
    --namespace backup \
    --set image=mucsi96/postgres-azure-backup:latest \
    --set host=backup.$host \
    --set clientId=$apiClientId \
    --set serviceAccountName=postgres-azure-backup \
    --set env.BLOBSTORAGE_ENDPOINT_URL=$blobStorageEndpointUrl \
    --set env.DATABASES_CONFIG_PATH=/app/databases_config.json \
    --set env.UI_CLIENT_ID=$spaClientId \
    --set configFile[0].name=databases_config.json \
    --set configFile[0].mountPath=/app/databases_config.json \
    --set "configFile[0].data=$(cat scripts/databases_config.json | base64)" \
    --wait
```

## Resources

- https://github.com/kananindzya/hello-world-aws-sdk-r2/blob/master/src/main/java/com/example/aws/api/r2/App.java
- https://github.com/esfandiar/vs-code-spring-boot-setup
- https://gist.github.com/valferon/4d6ebfa8a7f3d4e84085183609d10f14
- https://cwienczek.com/2020/06/simple-backup-of-postgres-database-in-kubernetes/
- https://developers.cloudflare.com/r2/examples/aws/boto3/
- https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/s3.html
- https://florianbuchner.com/kubernetes-curl-cronjob-for-internal-service/

- https://flowbite.com/docs/components/tables/
- https://hslpicker.com/
- https://learn.microsoft.com/en-us/azure/developer/java/spring-framework/spring-security-support?tabs=SpringCloudAzure5x#accessing-a-resource-server
