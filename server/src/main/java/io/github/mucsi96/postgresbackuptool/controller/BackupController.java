package io.github.mucsi96.postgresbackuptool.controller;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.mucsi96.postgresbackuptool.configuration.DatabaseConfiguration;
import io.github.mucsi96.postgresbackuptool.model.Backup;
import io.github.mucsi96.postgresbackuptool.model.BackupUrl;
import io.github.mucsi96.postgresbackuptool.service.BackupService;
import io.github.mucsi96.postgresbackuptool.service.DatabaseService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE, path = "/api")
@RequiredArgsConstructor
public class BackupController {
    private final BackupService backupService;
    private final DatabaseService databaseService;

    @PreAuthorize("hasAuthority('APPROLE_DatabaseBackupCreator') and hasAuthority('SCOPE_createBackup')")
    @PostMapping("/backup")
    @ResponseBody
    void create(
            @RequestParam("retention_period") @Min(1) @Max(356) int retentionPeriod)
            throws IOException, InterruptedException {
        databaseService.getDatabases().forEach(databaseConfiguration -> {
            try {
                createDump(retentionPeriod, databaseConfiguration.getName(),
                        databaseConfiguration.getPrefix(),
                        databaseConfiguration.getDumpFormat().getValue());

                if (databaseConfiguration.isCreatePlainDump()) {
                    createDump(retentionPeriod, databaseConfiguration.getName(),
                            databaseConfiguration.getPrefix(), "plain");
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void createDump(int retentionPeriod, String name, String prefix,
            String dumpFormat) throws IOException, InterruptedException {
        File dumpFile = databaseService.createDump(name, retentionPeriod,
                dumpFormat);
        backupService.createBackup(prefix, dumpFile);

        dumpFile.delete();
    }

    @PreAuthorize("hasAuthority('APPROLE_DatabaseBackupCleaner') and hasAuthority('SCOPE_cleanupBackups')")
    @PostMapping("/cleanup")
    @ResponseBody
    void cleanup() {
        databaseService.getDatabases().stream()
                .map(DatabaseConfiguration::getPrefix)
                .forEach(backupService::cleanup);
    }

    @PreAuthorize("hasAuthority('APPROLE_DatabaseBackupsReader') and hasAuthority('SCOPE_readBackups')")
    @GetMapping("/database/{database_name}/backups")
    @ResponseBody
    List<Backup> list(@PathVariable("database_name") String databaseName) {
        DatabaseConfiguration databaseConfiguration = databaseService
                .getDatabaseConfiguration(databaseName);
        return backupService.getBackups(databaseConfiguration.getPrefix());
    }

    @PreAuthorize("hasAuthority('APPROLE_DatabaseBackupDownloader') and hasAuthority('SCOPE_downloadBackup')")
    @GetMapping("/database/{database_name}/backup/{key}")
    @ResponseBody
    BackupUrl download(@PathVariable("database_name") String databaseName,
            @PathVariable String key) throws IOException, InterruptedException {
        DatabaseConfiguration databaseConfiguration = databaseService
                .getDatabaseConfiguration(databaseName);
        String url = backupService
                .getBackupUrl(databaseConfiguration.getPrefix(), key);

        return BackupUrl.builder().url(url).build();
    }

    @PreAuthorize("hasAuthority('APPROLE_DatabaseBackupRestorer') and hasAuthority('SCOPE_restoreBackup')")
    @PostMapping("/database/{database_name}/restore/{key}")
    @ResponseBody
    void restore(@PathVariable("database_name") String databaseName,
            @PathVariable String key) throws IOException, InterruptedException {
        DatabaseConfiguration databaseConfiguration = databaseService
                .getDatabaseConfiguration(databaseName);
        File dumpFile = backupService
                .downloadBackup(databaseConfiguration.getPrefix(), key);
        databaseService.restoreDump(databaseName, dumpFile);

        dumpFile.delete();
    }

    @PreAuthorize("hasAuthority('APPROLE_DatabaseBackupsReader') and hasAuthority('SCOPE_readBackups')")
    @GetMapping("/database/{database_name}/last-backup-time")
    @ResponseBody
    Optional<Instant> lastBackupTime(
            @PathVariable("database_name") String databaseName) {
        DatabaseConfiguration databaseConfiguration = databaseService
                .getDatabaseConfiguration(databaseName);
        return backupService
                .getLastBackupTime(databaseConfiguration.getPrefix());
    }
}
