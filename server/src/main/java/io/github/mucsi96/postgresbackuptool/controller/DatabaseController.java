package io.github.mucsi96.postgresbackuptool.controller;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.mucsi96.postgresbackuptool.model.Database;
import io.github.mucsi96.postgresbackuptool.model.DatabaseInfo;
import io.github.mucsi96.postgresbackuptool.service.BackupService;
import io.github.mucsi96.postgresbackuptool.service.DatabaseService;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class DatabaseController {
  private final DatabaseService databaseService;
  private final BackupService backupService;

  @GetMapping("/databases")
  @ResponseBody
  public List<Database> getDatabases() {
    return databaseService.getDatabases().stream().map(databaseName -> {
      DatabaseInfo databaseInfo = databaseService.getDatabaseInfo(databaseName);
      Optional<Instant> lastBackupTime = backupService
          .getLastBackupTime(databaseName);
      return Database.builder().name(databaseName)
          .totalRowCount(databaseInfo.getTotalRowCount())
          .tablesCount(databaseInfo.getTables().size())
          .backupsCount(backupService.getBackups(databaseName).size())
          .lastBackupTime(lastBackupTime.orElse(null)).build();
    }).toList();
  }

  @GetMapping("/database/{database_name}/tables")
  @ResponseBody
  public DatabaseInfo getDatabaseInfo(@PathVariable("database_name") String databaseName) {
    return databaseService.getDatabaseInfo(databaseName);
  }
}
