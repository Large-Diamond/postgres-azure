import { inject, Injectable, signal } from '@angular/core';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import {
  AuthenticationResult,
  EventMessage,
  EventType,
  InteractionStatus,
} from '@azure/msal-browser';
import { filter } from 'rxjs';
import { environment } from '../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  readonly isAuthenticated = signal(!environment.mockAuth);
  readonly msalService = !environment.mockAuth
    ? inject(MsalService)
    : undefined;
  readonly msalBroadcastService = !environment.mockAuth
    ? inject(MsalBroadcastService)
    : undefined;

  constructor() {
    this.msalBroadcastService?.msalSubject$
      .pipe(
        filter((msg: EventMessage) => msg.eventType === EventType.LOGIN_SUCCESS)
      )
      .subscribe((result: EventMessage) => {
        console.log(result);
        const payload = result.payload as AuthenticationResult;
        this.msalService?.instance.setActiveAccount(payload.account);
      });

    this.msalBroadcastService?.inProgress$
      .pipe(
        filter((status: InteractionStatus) => status === InteractionStatus.None)
      )
      .subscribe(() => {
        if (
          this.msalService &&
          this.msalService.instance.getAllAccounts().length > 0
        ) {
          this.isAuthenticated.set(true);
        }
      });
  }
}
