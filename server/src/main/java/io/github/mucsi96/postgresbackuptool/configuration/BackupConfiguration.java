package io.github.mucsi96.postgresbackuptool.configuration;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

@Configuration
public class BackupConfiguration {

  @Bean
  public BlobServiceClient blobServiceClient(
      @Value("${blobstorage.endpoint}") String endpointUrl,
      @Value("${blobstorage.connectionString}") String connectionString) {

    if (connectionString != null && !connectionString.isEmpty()) {
      return new BlobServiceClientBuilder().connectionString(connectionString)
          .buildClient();
    }

    TokenCredential tokenCredential = new DefaultAzureCredentialBuilder()
        .build();

    BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
        .endpoint(endpointUrl).credential(tokenCredential).buildClient();

    return blobServiceClient;
  }

  @Bean
  public DateTimeFormatter backupDateTimeFormat() {
    return DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        .withZone(ZoneOffset.UTC);
  }
}
