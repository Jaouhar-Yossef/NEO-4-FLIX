import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE } from '../api';
import { MyRating, UpdateProfileRequest, UserProfile } from './user.models';

/** docs/api.md §3: the logged-in user's own account. */
@Injectable({ providedIn: 'root' })
export class UserApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE}/users/me`;

  getMe(): Observable<UserProfile> {
    return this.http.get<UserProfile>(this.base);
  }

  updateMe(body: UpdateProfileRequest): Observable<UserProfile> {
    return this.http.put<UserProfile>(this.base, body);
  }

  getMyRatings(): Observable<MyRating[]> {
    return this.http.get<MyRating[]>(`${this.base}/ratings`);
  }

  deleteMe(): Observable<void> {
    return this.http.delete<void>(this.base);
  }
}
