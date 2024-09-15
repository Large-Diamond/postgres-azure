import { Component, signal, Signal } from '@angular/core';
import { Router } from '@angular/router';
import { Database } from '../../types';
import { BackupsService } from '../backups/backups.service';
import { olderThenOneDay } from '../utils/dateUtils';
import { RelativeTimePipe } from '../utils/relativeTime.pipe';
import { DatabasesService } from './databases.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-databases',
  standalone: true,
  imports: [RelativeTimePipe, FormsModule],
  templateUrl: './databases.component.html',
  styleUrl: './databases.component.css',
})
export class DatabasesComponent {
  databases: Signal<Database[] | undefined>;
  loading: Signal<boolean>;
  olderThenOneDay = olderThenOneDay;
  retentionPeriod = signal(1);
  processing: Signal<boolean>;

  constructor(
    private readonly databasesService: DatabasesService,
    private readonly backupsService: BackupsService,
    private readonly router: Router
  ) {
    this.databases = this.databasesService.getDatabases();
    this.loading = this.databasesService.isLoading();
    this.processing = this.backupsService.isProcessing();
  }

  selectDatabase(database: Database) {
    this.router.navigate(['/database', database.name]);
  }

  createBackup() {
    this.backupsService.createBackup(this.retentionPeriod());
  }

  cleanupBackups() {
    this.backupsService.cleanupBackups();
  }
}
