package io.github.mucsi96.postgresbackuptool.service;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;

import io.github.mucsi96.postgresbackuptool.model.Backup;
import io.github.mucsi96.postgresbackuptool.model.BackupType;

@Service
public class BackupService {
    private final BlobServiceClient blobServiceClient;
    private final DateTimeFormatter dateTimeFormatter;
    private final String containerName;

    public BackupService(BlobServiceClient blobServiceClient,
            DateTimeFormatter dateTimeFormatter,
            @Value("${blobstorage.containerName}") String containerName) {
        this.blobServiceClient = blobServiceClient;
        this.dateTimeFormatter = dateTimeFormatter;
        this.containerName = containerName;
    }

    public List<Backup> getBackups(String prefix, boolean hasPlainDump) {
        BlobContainerClient blobContainerClient = blobServiceClient
                .getBlobContainerClient(containerName);

        if (!blobContainerClient.exists()) {
            return Collections.emptyList();
        }

        return blobContainerClient
                .listBlobs(new ListBlobsOptions().setPrefix(prefix + "/"), null)
                .stream().filter(blob -> !blob.getName().endsWith(".sql"))
                .map(blob -> {
                    String name = getBackupName(prefix, blob);
                    return Backup.builder().name(getBackupName(prefix, blob))
                            .lastModified(dateTimeFormatter.parse(
                                    name.substring(0, 15), Instant::from))
                            .size(blob.getProperties().getContentLength())
                            .totalRowCount(getTotalCountFromName(prefix, blob))
                            .retentionPeriod(
                                    getRetentionPeriodFromName(prefix, blob))
                            .hasPlainDump(hasPlainDump).build();
                }).sorted((a, b) -> b.getLastModified()
                        .compareTo(a.getLastModified()))
                .toList();
    }

    public void createBackup(String prefix, File dumpFile) {
        BlobContainerClient blobContainerClient = blobServiceClient
                .getBlobContainerClient(containerName);

        blobContainerClient.getBlobClient(prefix + "/" + dumpFile.getName())
                .uploadFromFile(dumpFile.getAbsolutePath());
    }

    public File downloadBackup(String prefix, String key) throws IOException {
        BlobContainerClient blobContainerClient = blobServiceClient
                .getBlobContainerClient(containerName);

        blobContainerClient.getBlobClient(prefix + "/" + key)
                .downloadToFile(key);

        return new File(key);
    }

    public String getBackupUrl(String prefix, String key, BackupType type)
            throws IOException {
        BlobContainerClient blobContainerClient = blobServiceClient
                .getBlobContainerClient(containerName);

        BlobSasPermission permission = new BlobSasPermission()
                .setReadPermission(true);
        OffsetDateTime expiryTime = OffsetDateTime.now().plusMinutes(2);
        BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(
                expiryTime, permission).setStartTime(OffsetDateTime.now());

        UserDelegationKey userDelegationKey = blobServiceClient
                .getUserDelegationKey(OffsetDateTime.now(), expiryTime);

        String fileName = type == BackupType.ARCHIVE ? key
                : key.replaceAll("\\.[^.]+$", "") + ".sql";
        BlobClient blobClient = blobContainerClient
                .getBlobClient(prefix + "/" + fileName);
        return blobClient.getBlobUrl() + "?" + blobClient
                .generateUserDelegationSas(values, userDelegationKey);
    }

    public void cleanup(String prefix) {
        BlobContainerClient blobContainerClient = blobServiceClient
                .getBlobContainerClient(containerName);

        blobContainerClient
                .listBlobs(new ListBlobsOptions().setPrefix(prefix + "/"), null)
                .stream().filter(blobItem -> shouldCleanup(prefix, blobItem))
                .forEach(blobItem -> blobContainerClient
                        .getBlobClient(blobItem.getName()).delete());
    }

    public Optional<Instant> getLastBackupTime(String prefix,
            boolean hasPlainDump) {
        return getBackups(prefix, hasPlainDump).stream().findFirst()
                .map(backup -> backup.getLastModified());
    }

    private int getTotalCountFromName(String prefix, BlobItem backup) {
        return Integer.parseInt(getBackupName(prefix, backup).split("\\.")[1]);
    }

    private int getRetentionPeriodFromName(String prefix, BlobItem backup) {
        return Integer.parseInt(getBackupName(prefix, backup).split("\\.")[2]);
    }

    private boolean shouldCleanup(String prefix, BlobItem backup) {
        Backup b = Backup.builder().name(backup.getName())
                .lastModified(dateTimeFormatter.parse(
                        getBackupName(prefix, backup).substring(0, 15),
                        Instant::from))
                .retentionPeriod(getRetentionPeriodFromName(prefix, backup))
                .build();
        Instant cleanupDate = b.getLastModified()
                .plus(Duration.ofDays(b.getRetentionPeriod()));

        return cleanupDate.isBefore(Instant.now());
    }

    private static String getBackupName(String prefix, BlobItem backup) {
        return backup.getName().substring(prefix.length() + 1);
    }
}
