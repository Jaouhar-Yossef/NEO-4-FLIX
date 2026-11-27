import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE } from '../api';
import { Rating } from './rating.models';

/**
 * docs/api.md §6. The user comes from the JWT (the gateway sets X-User-Id).
 * create is for a first rating (409 if one exists), update for a later change (404 if none).
 */
@Injectable({ providedIn: 'root' })
export class RatingApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE}/ratings`;

  create(movieId: string, rating: number): Observable<Rating> {
    return this.http.post<Rating>(this.base, { movieId, rating });
  }

  update(movieId: string, rating: number): Observable<Rating> {
    return this.http.put<Rating>(`${this.base}/${encodeURIComponent(movieId)}`, { rating });
  }
}
