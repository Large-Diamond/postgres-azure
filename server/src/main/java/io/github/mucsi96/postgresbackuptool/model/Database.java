package io.github.mucsi96.postgresbackuptool.model;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Database {
  private String name;
  private int tablesCount;
  private int totalRowCount;
  private int backupsCount;
  private Instant lastBackupTime;
}