import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { environment } from '../environments/environment';
import { DatabaseComponent } from './database/database.component';
import { DatabasesComponent } from './databases/databases.component';

export const routes: Routes = [
  {
    path: '',
    component: DatabasesComponent,
    canActivate: environment.mockAuth ? [] : [MsalGuard],
  },
  {
    path: 'database/:name',
    component: DatabaseComponent,
    canActivate: environment.mockAuth ? [] : [MsalGuard],
  },
];
