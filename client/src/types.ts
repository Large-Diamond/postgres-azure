export type Database = {
  name: string;
  tablesCount: number;
  totalRowCount: number;
  backupsCount: number;
  lastBackupTime: Date;
};

export type Table = {
  name: string;
  rowCount: number;
};

export type Backup = {
  name: string;
  lastModified: Date;
  totalRowCount: number;
  size: number;
  retentionPeriod: number;
};
