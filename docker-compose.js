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
        BLOBSTORAGE_CONNECTION_STRING: Object.entries({
          DefaultEndpointsProtocol: "http",
          AccountName: "devstoreaccount1",
          AccountKey:
            "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==",
          BlobEndpoint: "http://blobstorage:10000/devstoreaccount1",
        })
          .map(([key, value]) => `${key}=${value}`)
          .join(";"),
        DATABASES_CONFIG_PATH: "/app/databases_config.json",
      },
      ports: ["8080:8080"],
      volumes: ["./test/databases_config.json:/app/databases_config.json"],
    },
    blobstorage: {
      image: "mcr.microsoft.com/azure-storage/azurite",
      command: "azurite-blob --blobHost 0.0.0.0",
    },
    db1: {
      image: "postgres:16.2-bullseye",
      environment: {
        POSTGRES_DB: "test",
        POSTGRES_PASSWORD,
        POSTGRES_USER,
      },
      volumes: [
        `./test/create_tables_1.sql:/docker-entrypoint-initdb.d/create_tables.sql`,
      ],
    },
    db2: {
      image: "postgres:16.2-bullseye",
      environment: {
        POSTGRES_DB: "test",
        POSTGRES_PASSWORD,
        POSTGRES_USER,
      },
      volumes: [
        `./test/create_tables_2.sql:/docker-entrypoint-initdb.d/create_tables.sql`,
      ],
    },
  },
};

console.log(JSON.stringify(config));
