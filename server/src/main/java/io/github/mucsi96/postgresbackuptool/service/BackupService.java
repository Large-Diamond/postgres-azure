package io.github.mucsi96.postgresbackuptool.service;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;

import io.github.mucsi96.postgresbackuptool.model.Backup;

@Service
public class BackupService {
  private final BlobServiceClient blobServiceClient;
  private final String containerName;
  private final DateTimeFormatter dateTimeFormatter;

  public BackupService(BlobServiceClient blobServiceClient,
      @Value("${blobstorage.container}") String containerName,
      DateTimeFormatter dateTimeFormatter) {
    this.blobServiceClient = blobServiceClient;
    this.containerName = containerName;
    this.dateTimeFormatter = dateTimeFormatter;
  }

  public List<Backup> getBackups() {
    BlobContainerClient blobContainerClient = blobServiceClient
        .getBlobContainerClient(containerName);

    if (!blobContainerClient.exists()) {
      return Collections.emptyList();
    }

    return blobContainerClient.listBlobs().stream()
        .map(s3Object -> Backup.builder().name(s3Object.getName())
            .lastModified(dateTimeFormatter
                .parse(s3Object.getName().substring(0, 15), Instant::from))
            .size(s3Object.getProperties().getContentLength())
            .totalRowCount(getTotalCountFromName(s3Object))
            .retentionPeriod(getRetentionPeriodFromName(s3Object)).build())
        .sorted((a, b) -> b.getLastModified().compareTo(a.getLastModified()))
        .toList();
  }

  public void createBackup(File dumpFile) {
    BlobContainerClient blobContainerClient = blobServiceClient
        .getBlobContainerClient(containerName);

    if (!blobContainerClient.exists()) {
      blobContainerClient.create();
    }

    blobContainerClient.getBlobClient(dumpFile.getName())
        .uploadFromFile(dumpFile.getAbsolutePath());
  }

  public File downloadBackup(String key) throws IOException {
    BlobContainerClient blobContainerClient = blobServiceClient
        .getBlobContainerClient(containerName);

    if (!blobContainerClient.exists()) {
      throw new IOException("Container does not exist");
    }

    blobContainerClient.getBlobClient(key).downloadToFile(key);

    return new File(key);
  }

  public void cleanup() {
    BlobContainerClient blobContainerClient = blobServiceClient
        .getBlobContainerClient(containerName);

    if (!blobContainerClient.exists()) {
      return;
    }

    blobContainerClient.listBlobs().stream().filter(this::shouldCleanup)
        .forEach(blobItem -> blobContainerClient
            .getBlobClient(blobItem.getName()).delete());
  }

  public Optional<Instant> getLastBackupTime() {
    return getBackups().stream().findFirst()
        .map(backup -> backup.getLastModified());
  }

  private int getTotalCountFromName(BlobItem backup) {
    return Integer.parseInt(backup.getName().split("\\.")[1]);
  }

  private int getRetentionPeriodFromName(BlobItem backup) {
    return Integer.parseInt(backup.getName().split("\\.")[2]);
  }

  private boolean shouldCleanup(BlobItem backup) {
    Backup b = Backup.builder().name(backup.getName())
        .lastModified(dateTimeFormatter.parse(backup.getName().substring(0, 15),
            Instant::from))
        .retentionPeriod(getRetentionPeriodFromName(backup)).build();
    Instant cleanupDate = b.getLastModified()
        .plus(Duration.ofDays(b.getRetentionPeriod()));

    return cleanupDate.isBefore(Instant.now());
  }
}
