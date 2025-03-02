import { Routes } from '@angular/router';
import { DatabaseComponent } from './database/database.component';
import { DatabasesComponent } from './databases/databases.component';

export const routes: Routes = [
  {
    path: '',
    component: DatabasesComponent,
  },
  {
    path: 'database/:name',
    component: DatabaseComponent,
  },
];
