import { Injectable } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class SelectedDatabaseService {
  private readonly $databaseName = new BehaviorSubject<string | undefined>(
    undefined
  );

  getSelectedDatabase() {
    return this.$databaseName;
  }

  getSelectedDatabaseSignal() {
    return toSignal(this.$databaseName);
  }

  setDatabaseName(name: string | undefined) {
    this.$databaseName.next(name);
  }

  resetSelectedDatabase() {
    this.$databaseName.next(undefined);
  }
}
