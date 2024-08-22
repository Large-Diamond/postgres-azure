const POSTGRES_DB = "postgres-azure-backup";
const POSTGRES_USER = "postgres";
const POSTGRES_PASSWORD = "postgres";

const config = {
  volumes: {
    "postgres-data": {},
  },
  services: {
    app: {
      build: ".",
      environment: {
        SERVER_SERVLET_CONTEXT_PATH: "/db",
        SPRING_ACTUATOR_PORT: "8082",
        POSTGRES_HOSTNAME: "db",
        POSTGRES_PORT: "5432",
        POSTGRES_DB,
        POSTGRES_PASSWORD,
        POSTGRES_USER,
        BLOBSTORAGE_ENDPOINT_URL: "http://blobstorage:10000",
        BLOBSTORAGE_CONTAINER: "test-bucket",
        EXCLUDE_TABLES: "passwords,secrets",
      },
      ports: ["8080:8080"],
    },
    blobstorage: {
      image: "mcr.microsoft.com/azure-storage/azurite",
      command: "azurite-blob --blobHost 0.0.0.0",
    },
    db: {
      image: "postgres:16.2-bullseye",
      environment: {
        POSTGRES_DB,
        POSTGRES_PASSWORD,
        POSTGRES_USER,
      },
      volumes: [
        `./db/create_tables.sql:/docker-entrypoint-initdb.d/create_tables.sql`,
      ],
    },
  },
};

console.log(JSON.stringify(config));
