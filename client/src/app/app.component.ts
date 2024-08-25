import { Component, signal, Signal } from '@angular/core';
import { ActivatedRoute, RouterLink, RouterOutlet } from '@angular/router';
import { BackupsService } from './backups/backups.service';
import { RelativeTimePipe } from './utils/relativeTime.pipe';
import { DatabasesService } from './databases/databases.service';
import { Database } from '../types';
import { olderThenOneDay } from './utils/dateUtils';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RelativeTimePipe, RouterLink],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  databaseName = signal<string | undefined>(undefined);
  databases: Signal<Database[] | undefined>;
  lastBackupTime: Signal<Date | undefined>;
  olderThenOneDay = olderThenOneDay

  constructor(
    private readonly route: ActivatedRoute,
    private readonly databasesService: DatabasesService,
    private readonly backupsService: BackupsService
  ) {
    this.route.params.subscribe((params) => {
      if (params['name']) {
        this.databaseName.set(params['name']);
      }
    });
    this.databases = this.databasesService.getDatabases();
    this.databaseName = this.databasesService.getSelectedDatabase();
    this.lastBackupTime = this.backupsService.getLastBackupTime();
  }
}
