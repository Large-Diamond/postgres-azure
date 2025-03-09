import { Component, inject, signal, Signal } from '@angular/core';
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
  private readonly databasesService = inject(DatabasesService);
  private readonly backupsService = inject(BackupsService);
  private readonly router = inject(Router);
  databases = this.databasesService.databases;
  olderThenOneDay = olderThenOneDay;
  retentionPeriod = signal(1);
  processing = this.backupsService.processing;

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
