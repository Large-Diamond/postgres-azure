import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { SuccessNotificationEvent } from '@mucsi96/ui-elements';
import {
  BehaviorSubject,
  filter,
  finalize,
  map,
  Observable,
  shareReplay,
  skip,
  switchMap,
  take,
  tap,
} from 'rxjs';
import { environment } from '../../environments/environment';
import { Backup } from '../../types';
import { handleError } from '../utils/handleError';
import { SelectedDatabaseService } from '../database/selected-database.service';

@Injectable({
  providedIn: 'root',
})
export class BackupsService {
  private $lastBackupTime: Observable<Date | undefined>;
  private $backups: Observable<Backup[]>;
  private readonly $backupMutations = new BehaviorSubject<void>(undefined);
  private readonly loading = signal(true);
  private readonly processing = signal(false);

  constructor(
    private readonly http: HttpClient,
    private readonly selectedDatabaseService: SelectedDatabaseService
  ) {
    this.$backups = this.selectedDatabaseService.getSelectedDatabase().pipe(
      filter((databaseName) => !!databaseName),
      switchMap((databaseName) =>
        this.$backupMutations.pipe(
          tap(() => this.loading.set(true)),
          switchMap(() =>
            this.http
              .get<Backup[]>(
                environment.apiContextPath + `/database/${databaseName}/backups`
              )
              .pipe(
                map((backups) =>
                  backups.map((backup) => ({
                    ...backup,
                    lastModified: new Date(backup.lastModified),
                  }))
                ),
                handleError('Could not fetch backups.'),
                finalize(() => this.loading.set(false))
              )
          )
        )
      ),
      shareReplay(1)
    );
    this.$lastBackupTime = this.selectedDatabaseService
      .getSelectedDatabase()
      .pipe(
        filter((databaseName) => !!databaseName),
        switchMap((databaseName) =>
          this.$backupMutations.pipe(
            switchMap(() =>
              this.http
                .get<Date | undefined>(
                  environment.apiContextPath +
                    `/database/${databaseName}/last-backup-time`
                )
                .pipe(
                  map(
                    (lastBackupTime) =>
                      lastBackupTime && new Date(lastBackupTime)
                  ),
                  handleError('Unable to fetch last backup time')
                )
            )
          )
        ),
        shareReplay(1)
      );
  }

  getLastBackupTime() {
    return toSignal(this.$lastBackupTime);
  }

  getBackups() {
    return toSignal(this.$backups);
  }

  isLoading() {
    return this.loading;
  }

  createBackup(retentionPeriod: number) {
    this.http
      .post<void>(
        environment.apiContextPath +
          `/backup?retention_period=${retentionPeriod}`,
        {}
      )
      .pipe(
        handleError('Could not create backup.'),
        tap(() => {
          document.dispatchEvent(
            new SuccessNotificationEvent('Backup created')
          );
          this.$backupMutations.next();
        }),
        finalize(() => this.processing.set(false)),
        take(1)
      )
      .subscribe();
  }

  cleanupBackups() {
    this.http
      .post<void>(
        environment.apiContextPath + `/cleanup`,
        {}
      )
      .pipe(
        handleError('Could not cleanup backups'),
        tap(() => {
          document.dispatchEvent(
            new SuccessNotificationEvent('Backup cleanup finished')
          );
          this.$backupMutations.next();
        }),
        finalize(() => this.processing.set(false)),
        take(1)
      )
      .subscribe();
  }

  isProcessing() {
    return this.processing;
  }

  getBackupMutations() {
    return this.$backupMutations;
  }
}
