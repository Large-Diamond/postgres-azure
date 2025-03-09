import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class SelectedDatabaseService {
  readonly databaseName = signal<string | undefined>(undefined);

  setDatabaseName(name: string | undefined) {
    this.databaseName.set(name);
  }

  resetSelectedDatabase() {
    this.databaseName.set(undefined);
  }
}
