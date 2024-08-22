package io.github.mucsi96.postgresbackuptool.configuration;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

@Configuration
public class BackupConfiguration {

  @Profile("!test")
  @Bean
  public BlobServiceClient blobServiceClient(
      @Value("${blobstorage.endpoint}") String endpointUrl) {
    TokenCredential tokenCredential = new DefaultAzureCredentialBuilder()
        .build();

    BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
        .endpoint(endpointUrl).credential(tokenCredential).buildClient();

    return blobServiceClient;
  }

  @Profile("test")
  @Bean
  public BlobServiceClient mockBlobServiceClient(
      @Value("${blobstorage.connectionString}") String connectionString) {
    BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
        .connectionString(connectionString).buildClient();

    return blobServiceClient;
  }

  @Bean
  public DateTimeFormatter backupDateTimeFormat() {
    return DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        .withZone(ZoneOffset.UTC);
  }
}
