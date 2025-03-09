import { HttpClient, HttpHeaders } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export async function fetchJson<T>(
  http: HttpClient,
  url: string,
  options: { body?: any; method?: string } = {}
) {
  const { body, method = 'get' } = options;
  const response = await firstValueFrom(
    http.request(method, url, {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
        Accept: 'application/json',
      }),
      body,
      responseType: 'json',
    })
  );
  return response as T;
}
