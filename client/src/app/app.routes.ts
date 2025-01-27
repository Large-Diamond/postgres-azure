import { Routes } from '@angular/router';
import { DatabasesComponent } from './databases/databases.component';
import { DatabaseComponent } from './database/database.component';
import { MsalGuard, MsalRedirectComponent } from '@azure/msal-angular';
import { environment } from '../environments/environment';

export const routes: Routes = [
  {
    path: 'auth',
    pathMatch: 'full',
    component: MsalRedirectComponent,
  },
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
