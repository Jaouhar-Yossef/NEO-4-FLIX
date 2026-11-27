import { DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, of, switchMap, tap } from 'rxjs';

import { MovieApi } from '../../core/movie/movie.api';
import { MoviePage } from '../../core/movie/movie.models';
import { problemMessage } from '../../core/problem-detail';

const PAGE_SIZE = 20;
// The backend returns 500 on huge page numbers (skip = page * size overflows).
const MAX_PAGE = 10_000;

/** Reads ?page= (1-based in the URL, 0-based in the API), never sends a bad value. */
function readPage(value: string | null): number {
  const n = Number(value);
  return Number.isInteger(n) && n >= 1 ? Math.min(n, MAX_PAGE) : 1;
}

@Component({
  imports: [RouterLink, DecimalPipe],
  selector: 'app-home',
  styleUrl: './home.css',
  templateUrl: './home.html',
})
export class Home {
  private readonly movieApi = inject(MovieApi);
  private readonly route = inject(ActivatedRoute);

  protected readonly page = signal(1);
  protected readonly result = signal<MoviePage | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  constructor() {
    // switchMap: a new page cancels the request for the previous one.
    this.route.queryParamMap
      .pipe(
        tap((params) => {
          this.page.set(readPage(params.get('page')));
          this.loading.set(true);
          this.error.set(null);
        }),
        switchMap(() =>
          this.movieApi.list({ page: this.page() - 1, size: PAGE_SIZE }).pipe(
            catchError((err) => {
              this.error.set(problemMessage(err, 'Could not load the movies.'));
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((result) => {
        this.result.set(result);
        this.loading.set(false);
      });
  }

  protected totalPages(result: MoviePage): number {
    return Math.max(1, Math.ceil(result.totalElements / result.size));
  }
}
