package io.github.mucsi96.postgresbackuptool.controller;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.mucsi96.postgresbackuptool.configuration.DatabaseConfiguration;
import io.github.mucsi96.postgresbackuptool.model.Database;
import io.github.mucsi96.postgresbackuptool.model.DatabaseInfo;
import io.github.mucsi96.postgresbackuptool.service.BackupService;
import io.github.mucsi96.postgresbackuptool.service.DatabaseService;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE, path = "/api")
@RequiredArgsConstructor
public class DatabaseController {
    private final DatabaseService databaseService;
    private final BackupService backupService;

    @PreAuthorize("hasAuthority('APPROLE_DatabaseBackupsReader') && hasAuthority('SCOPE_readBackups')")
    @GetMapping("/databases")
    @ResponseBody
    public List<Database> getDatabases() {
        return databaseService.getDatabaseNames().stream().map(databaseName -> {
            DatabaseInfo databaseInfo = databaseService
                    .getDatabaseInfo(databaseName);
            DatabaseConfiguration databaseConfiguration = databaseService
                    .getDatabaseConfiguration(databaseName);
            Optional<Instant> lastBackupTime = backupService.getLastBackupTime(
                    databaseConfiguration.getPrefix(),
                    databaseConfiguration.isCreatePlainDump());
            return Database.builder().name(databaseName)
                    .totalRowCount(databaseInfo.getTotalRowCount())
                    .tablesCount(databaseInfo.getTables().size())
                    .backupsCount(backupService
                            .getBackups(databaseConfiguration.getPrefix(),
                                    databaseConfiguration.isCreatePlainDump())
                            .size())
                    .lastBackupTime(lastBackupTime.orElse(null)).build();
        }).toList();
    }

    @PreAuthorize("hasAuthority('APPROLE_DatabaseBackupsReader') and hasAuthority('SCOPE_readBackups')")
    @GetMapping("/database/{database_name}/tables")
    @ResponseBody
    public DatabaseInfo getDatabaseInfo(
            @PathVariable("database_name") String databaseName) {
        return databaseService.getDatabaseInfo(databaseName);
    }
}
