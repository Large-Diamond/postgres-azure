import { Component, ElementRef, Signal, ViewChild } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { Database } from '../types';
import { BackupsService } from './backups/backups.service';
import { SelectedDatabaseService } from './database/selected-database.service';
import { DatabasesService } from './databases/databases.service';
import { olderThenOneDay } from './utils/dateUtils';
import { RelativeTimePipe } from './utils/relativeTime.pipe';
import { UserProfileService } from './user-profile.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RelativeTimePipe, RouterLink],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  databaseName: Signal<string | undefined>;
  databases: Signal<Database[] | undefined>;
  lastBackupTime: Signal<Date | undefined>;
  olderThenOneDay = olderThenOneDay;
  profile: Signal<{ name: string; initials: string } | undefined>;

  @ViewChild('popover') popover!: ElementRef;

  constructor(
    private readonly databasesService: DatabasesService,
    private readonly selectedDatabaseService: SelectedDatabaseService,
    private readonly backupsService: BackupsService,
    private readonly userProfileService: UserProfileService
  ) {
    this.databases = this.databasesService.getDatabases();
    this.databaseName =
      this.selectedDatabaseService.getSelectedDatabaseSignal();
    this.lastBackupTime = this.backupsService.getLastBackupTime();
    this.profile = this.userProfileService.getProfile();
  }

  resetSelectedDatabase() {
    this.selectedDatabaseService.resetSelectedDatabase();
  }
}
