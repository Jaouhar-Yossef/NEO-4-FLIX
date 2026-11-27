import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE } from '../api';
import { MovieDetails, MoviePage } from './movie.models';

/** docs/api.md §5: movie list, paged (sorted by title by default). */
@Injectable({ providedIn: 'root' })
export class MovieApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE}/movies`;

  list(query: { page: number; size: number }): Observable<MoviePage> {
    const params = new HttpParams().set('page', query.page).set('size', query.size);
    return this.http.get<MoviePage>(this.base, { params });
  }

  /** 404 ProblemDetail for an unknown id. The id is encoded: it can come from a shared link. */
  getById(id: string): Observable<MovieDetails> {
    return this.http.get<MovieDetails>(`${this.base}/${encodeURIComponent(id)}`);
  }
}
