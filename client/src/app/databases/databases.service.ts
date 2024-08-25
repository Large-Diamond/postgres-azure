import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { map, Observable, shareReplay, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { handleError } from '../utils/handleError';
import { Database } from '../../types';

@Injectable({
  providedIn: 'root',
})
export class DatabasesService {
  $databases: Observable<Database[]>;
  selectedDatabase = signal<string | undefined>(undefined);
  loading = signal(true);

  constructor(private readonly http: HttpClient) {
    this.$databases = this.http
      .get<Database[]>(environment.apiContextPath + '/databases')
      .pipe(
        map((databases) =>
          databases.map((db) => ({
            ...db,
            lastBackupTime: new Date(db.lastBackupTime),
          }))
        ),
        tap(() => this.loading.set(false)),
        handleError('Could not fetch databases'),
        shareReplay(1)
      );
  }

  getSelectedDatabase() {
    return this.selectedDatabase;
  }

  setDatabaseName(name: string) {
    this.selectedDatabase.set(name);
  }

  getDatabases() {
    return toSignal(this.$databases);
  }

  isLoading() {
    return this.loading;
  }
}
