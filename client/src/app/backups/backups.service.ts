import { HttpClient } from '@angular/common/http';
import { inject, Injectable, resource, signal } from '@angular/core';
import {
  ErrorNotificationEvent,
  SuccessNotificationEvent,
} from '@mucsi96/ui-elements';
import { environment } from '../../environments/environment';
import { Backup } from '../../types';
import { SelectedDatabaseService } from '../database/selected-database.service';
import { fetchJson } from '../utils/fetchJson';

@Injectable({
  providedIn: 'root',
})
export class BackupsService {
  private readonly http = inject(HttpClient);
  private readonly selectedDatabaseService = inject(SelectedDatabaseService);
  readonly processing = signal(false);
  readonly backups = resource<Backup[], { databaseName?: string }>({
    request: () => ({
      databaseName: this.selectedDatabaseService.databaseName(),
    }),
    loader: async ({ request: { databaseName } }) => {
      if (!databaseName) {
        return [];
      }
      try {
        const backups = await fetchJson<Backup[]>(
          this.http,
          environment.apiContextPath + `/database/${databaseName}/backups`
        );

        return backups.map((backup) => ({
          ...backup,
          lastModified: new Date(backup.lastModified),
        }));
      } catch (error) {
        dispatchEvent(new ErrorNotificationEvent('Could not get backups.'));
        return [];
      }
    },
  });
  readonly lastBackupTime = resource<
    Date | undefined,
    { databaseName?: string }
  >({
    request: () => ({
      databaseName: this.selectedDatabaseService.databaseName(),
    }),
    loader: async ({ request: { databaseName } }) => {
      if (!databaseName) {
        return undefined;
      }
      try {
        const lastBackupTime = await fetchJson<Date | undefined>(
          this.http,
          environment.apiContextPath +
            `/database/${databaseName}/last-backup-time`
        );

        return lastBackupTime && new Date(lastBackupTime);
      } catch (error) {
        dispatchEvent(
          new ErrorNotificationEvent('Could not get last backup time.')
        );
        return undefined;
      }
    },
  });

  async createBackup(retentionPeriod: number) {
    try {
      this.processing.set(true);
      await fetchJson<void>(
        this.http,
        environment.apiContextPath +
          `/backup?retention_period=${retentionPeriod}`,
        { method: 'post' }
      );
      document.dispatchEvent(new SuccessNotificationEvent('Backup created'));
    } catch (error) {
      document.dispatchEvent(
        new ErrorNotificationEvent('Could not create backup.')
      );
    }
    this.processing.set(false);
    this.backups.reload();
    this.lastBackupTime.reload();
  }

  async cleanupBackups() {
    try {
      this.processing.set(true);
      await fetchJson<void>(
        this.http,
        environment.apiContextPath + `/cleanup`,
        { method: 'post' }
      );
      document.dispatchEvent(
        new SuccessNotificationEvent('Backup cleanup finished')
      );
    } catch (error) {
      document.dispatchEvent(
        new ErrorNotificationEvent('Could not cleanup backups')
      );
    }
    this.processing.set(false);
    this.backups.reload();
    this.lastBackupTime.reload();
  }
}
