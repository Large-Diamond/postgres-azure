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
import { Table } from '../../types';
import { handleError } from '../utils/handleError';
import { SelectedDatabaseService } from '../database/selected-database.service';

@Injectable({
  providedIn: 'root',
})
export class TablesService {
  private $tables: Observable<{
    tables: Table[];
    totalRowCount: number;
  }>;
  private readonly $tableMutations = new BehaviorSubject<void>(undefined);
  private readonly loading = signal(true);
  private readonly processing = signal(false);

  constructor(
    private readonly http: HttpClient,
    private readonly selectedDatabaseService: SelectedDatabaseService
  ) {
    this.$tables = this.selectedDatabaseService.getSelectedDatabase().pipe(
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
                finalize(() => this.loading.set(false))
              )
          )
        )
      ),
      shareReplay(1)
    );
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
    this.selectedDatabaseService
      .getSelectedDatabase()
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
        ),
        take(1)
      )
      .subscribe();
  }

  isProcessing() {
    return this.processing;
  }

  getTableMutations() {
    return this.$tableMutations.pipe(skip(1));
  }
}
