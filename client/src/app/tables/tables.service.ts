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
  Subject,
  switchMap,
  tap,
} from 'rxjs';
import { environment } from '../../environments/environment';
import { Table } from '../../types';
import { handleError } from '../utils/handleError';

@Injectable({
  providedIn: 'root',
})
export class TablesService {
  $databaseName = new BehaviorSubject<string | undefined>(undefined);
  $tables: Observable<{
    tables: Table[];
    totalRowCount: number;
  }>;
  $tableMutations = new BehaviorSubject<void>(undefined);
  loading = signal(true);
  processing = signal(false);

  constructor(private readonly http: HttpClient) {
    this.$tables = this.$databaseName.pipe(
      switchMap((databaseName) =>
        this.$tableMutations.pipe(
          filter(() => !!databaseName),
          tap(() => this.loading.set(true)),
          switchMap(() =>
            this.http
              .get<{
                tables: Table[];
                totalRowCount: number;
              }>(
                environment.apiContextPath + `/database/${databaseName}/tables`
              )
              .pipe(
                handleError('Could not fetch tables.'),
                shareReplay(1),
                finalize(() => this.loading.set(false))
              )
          )
        )
      )
    );
  }

  setDatabaseName(name: string) {
    this.$databaseName.next(name);
  }

  getTables() {
    return toSignal(this.$tables.pipe(map((data) => data.tables)));
  }

  getTotalRowCount() {
    return toSignal(this.$tables.pipe(map((data) => data.totalRowCount)));
  }

  isLoading() {
    return this.loading;
  }

  restoreBackup(selectedBackup: string) {
    this.$databaseName
      .pipe(
        tap(() => this.processing.set(true)),
        switchMap((databaseName) =>
          this.http
            .post<void>(
              environment.apiContextPath +
                `/database/${databaseName}/restore/${selectedBackup}`,
              {}
            )
            .pipe(
              handleError('Could not restore backup.'),
              tap(() => {
                document.dispatchEvent(
                  new SuccessNotificationEvent('Backup restored')
                );
                this.$tableMutations.next();
              }),
              finalize(() => this.processing.set(false))
            )
        )
      )
      .subscribe();
  }

  isProcessing() {
    return this.processing;
  }
}
