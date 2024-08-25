import { Component } from '@angular/core';
import { TablesComponent } from "../tables/tables.component";
import { BackupsComponent } from "../backups/backups.component";

@Component({
  selector: 'app-database',
  standalone: true,
  imports: [TablesComponent, BackupsComponent],
  templateUrl: './database.component.html',
  styleUrl: './database.component.css'
})
export class DatabaseComponent {

}
