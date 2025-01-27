import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { map, of } from 'rxjs';
import { environment } from '../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class UserProfileService {
  private readonly http = inject(HttpClient);
  $profile = environment.mockAuth
    ? of({ name: 'Test User', initials: 'TU' })
    : this.http
        .get<{ displayName: string }>('https://graph.microsoft.com/v1.0/me')
        .pipe(
          map(({ displayName }) => ({
            name: displayName,
            initials: this.getInitials(displayName),
          }))
        );

  getProfile() {
    return toSignal(this.$profile);
  }

  private getInitials(name: string | undefined): string {
    if (!name) return '';
    const initials = name
      .split(' ')
      .map((n) => n[0])
      .join('');
    return initials.toUpperCase();
  }
}
