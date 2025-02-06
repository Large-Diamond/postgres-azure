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

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class BackupConfiguration {

  @Bean
  public BlobServiceClient blobServiceClient(
      @Value("${blobstorage.baseUrl}") String baseUrl,
      @Value("${blobstorage.connectionString}") String connectionString) {

    log.info("baseUrl: {}", baseUrl);
    log.info("connectionString: {}", connectionString);

    if (connectionString != null && !connectionString.isEmpty()) {
      log.info("Using connection string to authenticate");
      return new BlobServiceClientBuilder().connectionString(connectionString)
          .buildClient();
    }

    log.info("Using managed identity to authenticate");

    TokenCredential tokenCredential = new DefaultAzureCredentialBuilder()
        .build();

    BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
        .endpoint(baseUrl).credential(tokenCredential).buildClient();

    return blobServiceClient;
  }

  @Bean
  public DateTimeFormatter backupDateTimeFormat() {
    return DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        .withZone(ZoneOffset.UTC);
  }
}
