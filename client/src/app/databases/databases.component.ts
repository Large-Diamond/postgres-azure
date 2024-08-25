import { Component, Signal } from '@angular/core';
import { Router } from '@angular/router';
import { DatabasesService } from './databases.service';

@Component({
  selector: 'app-databases',
  standalone: true,
  imports: [],
  templateUrl: './databases.component.html',
  styleUrl: './databases.component.css',
})
export class DatabasesComponent {
  databases: Signal<string[] | undefined>;
  loading: Signal<boolean>;

  constructor(
    private readonly databasesService: DatabasesService,
    private readonly router: Router
  ) {
    this.databases = this.databasesService.getDatabases();
    this.loading = this.databasesService.isLoading();
  }

  selectDatabase(name: string) {
    this.router.navigate(['/database', name]);
  }
}
