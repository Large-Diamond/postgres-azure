package io.github.mucsi96.postgresbackuptool.controller;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.mucsi96.postgresbackuptool.model.Backup;
import io.github.mucsi96.postgresbackuptool.service.BackupService;
import io.github.mucsi96.postgresbackuptool.service.DatabaseService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class BackupController {
  private final BackupService backupService;
  private final DatabaseService databaseService;

  @GetMapping("/database/{database_name}/backups")
  @ResponseBody
  List<Backup> list(@PathVariable("database_name") String databaseName) {
    return backupService.getBackups(databaseName);
  }

  @PostMapping("/database/{database_name}/backup")
  @ResponseBody
  void create(@PathVariable("database_name") String databaseName,
      @RequestParam("retention_period") @Min(1) @Max(356) int retentionPeriod)
      throws IOException, InterruptedException {
    File dumpFile = databaseService.createDump(databaseName, retentionPeriod);
    backupService.createBackup(databaseName, dumpFile);

    dumpFile.delete();
  }

  @PostMapping("/database/{database_name}/restore/{key}")
  @ResponseBody
  void restore(@PathVariable("database_name") String databaseName,
      @PathVariable String key) throws IOException, InterruptedException {
    File dumpFile = backupService.downloadBackup(databaseName, key);
    databaseService.restoreDump(databaseName, dumpFile);

    dumpFile.delete();
  }

  @PostMapping("/database/{database_name}/cleanup")
  @ResponseBody
  void cleanup(@PathVariable("database_name") String databaseName) {
    backupService.cleanup(databaseName);
  }

  @GetMapping("/database/{database_name}/last-backup-time")
  @ResponseBody
  Optional<Instant> lastBackupTime(
      @PathVariable("database_name") String databaseName) {
    return backupService.getLastBackupTime(databaseName);
  }
}
