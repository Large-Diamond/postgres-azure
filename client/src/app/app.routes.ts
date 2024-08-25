import { Routes } from '@angular/router';
import { DatabasesComponent } from './databases/databases.component';
import { DatabaseComponent } from './database/database.component';

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
