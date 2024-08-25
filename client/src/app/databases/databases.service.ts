import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Observable, shareReplay, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { handleError } from '../utils/handleError';

@Injectable({
  providedIn: 'root',
})
export class DatabasesService {
  $databases: Observable<string[]>;
  selectedDatabase = signal<string | undefined>(undefined);
  loading = signal(true);

  constructor(private readonly http: HttpClient) {
    this.$databases = this.http
      .get<string[]>(environment.apiContextPath + '/databases')
      .pipe(
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
