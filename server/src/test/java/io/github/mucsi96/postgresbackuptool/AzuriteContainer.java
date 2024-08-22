package io.github.mucsi96.postgresbackuptool;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class AzuriteContainer extends GenericContainer<AzuriteContainer> {
  public AzuriteContainer() {
    super("mcr.microsoft.com/azure-storage/azurite");
    withExposedPorts(10000).waitingFor(Wait.forLogMessage(
        ".*Azurite Blob service successfully listens on http://0.0.0.0:10000*\n",
        1)).withCommand("azurite-blob --blobHost 0.0.0.0");
  }

  public String getConnectionString() {
    return "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;"
        + "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;"
        + "BlobEndpoint=http://127.0.0.1:" + getMappedPort(10000)
        + "/devstoreaccount1;";
  }
}
