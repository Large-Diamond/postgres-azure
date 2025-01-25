import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class UserProfileService {
  private readonly http = inject(HttpClient);
  $profile = this.http
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
