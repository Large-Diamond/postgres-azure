import { Component, ElementRef, inject, ViewChild } from '@angular/core';
import { SelectedDatabaseService } from '../database/selected-database.service';
import { DatabasesService } from '../databases/databases.service';
import { BackupsService } from '../backups/backups.service';
import { olderThenOneDay } from '../utils/dateUtils';
import { UserProfileService } from '../user-profile.service';
import { RelativeTimePipe } from '../utils/relativeTime.pipe';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RelativeTimePipe, RouterLink],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css',
})
export class HeaderComponent {
  selectedDatabaseService = inject(SelectedDatabaseService);
  databaseName = this.selectedDatabaseService.databaseName;
  databases = inject(DatabasesService).databases;
  lastBackupTime = inject(BackupsService).lastBackupTime;
  olderThenOneDay = olderThenOneDay;
  profile = inject(UserProfileService).profile;

  @ViewChild('popover') popover!: ElementRef;
}
