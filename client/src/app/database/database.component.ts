import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BackupsComponent } from '../backups/backups.component';
import { TablesComponent } from '../tables/tables.component';
import { SelectedDatabaseService } from './selected-database.service';

@Component({
  selector: 'app-database',
  standalone: true,
  imports: [TablesComponent, BackupsComponent],
  templateUrl: './database.component.html',
  styleUrl: './database.component.css',
})
export class DatabaseComponent {
  constructor(
    private readonly route: ActivatedRoute,
    selectedDatabaseService: SelectedDatabaseService
  ) {
    this.route.params.subscribe((params) =>
      selectedDatabaseService.setDatabaseName(params['name'])
    );
  }
}
