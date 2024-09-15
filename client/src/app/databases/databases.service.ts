import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  combineLatest,
  map,
  Observable,
  shareReplay,
  switchMap,
  tap
} from 'rxjs';
import { environment } from '../../environments/environment';
import { Database } from '../../types';
import { BackupsService } from '../backups/backups.service';
import { TablesService } from '../tables/tables.service';
import { handleError } from '../utils/handleError';

@Injectable({
  providedIn: 'root',
})
export class DatabasesService {
  private $databases: Observable<Database[]>;
  private readonly loading = signal(true);

  constructor(
    private readonly http: HttpClient,
    tableService: TablesService,
    backupsService: BackupsService
  ) {
    this.$databases = combineLatest([
      tableService.getTableMutations(),
      backupsService.getBackupMutations(),
    ]).pipe(
      tap(() => this.loading.set(true)),
      switchMap(() =>
        this.http
          .get<Database[]>(environment.apiContextPath + '/databases')
          .pipe(
            handleError('Could not fetch databases'),
            map((databases) =>
              databases.map((db) => ({
                ...db,
                lastBackupTime:
                  db.lastBackupTime && new Date(db.lastBackupTime),
              }))
            ),
            tap(() => this.loading.set(false))
          )
      ),
      shareReplay(1)
    );
  }

  getDatabases() {
    return toSignal(this.$databases);
  }

  isLoading() {
    return this.loading;
  }
}
