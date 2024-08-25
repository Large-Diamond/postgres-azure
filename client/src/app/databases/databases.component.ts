import { Component, Signal } from '@angular/core';
import { Router } from '@angular/router';
import { DatabasesService } from './databases.service';
import { Database } from '../../types';
import { RelativeTimePipe } from '../utils/relativeTime.pipe';

@Component({
  selector: 'app-databases',
  standalone: true,
  imports: [RelativeTimePipe],
  templateUrl: './databases.component.html',
  styleUrl: './databases.component.css',
})
export class DatabasesComponent {
  databases: Signal<Database[] | undefined>;
  loading: Signal<boolean>;

  constructor(
    private readonly databasesService: DatabasesService,
    private readonly router: Router
  ) {
    this.databases = this.databasesService.getDatabases();
    this.loading = this.databasesService.isLoading();
  }

  selectDatabase(database: Database) {
    this.router.navigate(['/database', database.name]);
  }
}
