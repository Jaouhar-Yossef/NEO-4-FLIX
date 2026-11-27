import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, of, switchMap, tap } from 'rxjs';

import { MovieApi } from '../../core/movie/movie.api';
// Aliased: the component class is already called MovieDetails (used by the route).
import { MovieDetails as Movie } from '../../core/movie/movie.models';
import { problemMessage } from '../../core/problem-detail';
import { RatingApi } from '../../core/rating/rating.api';

@Component({
  imports: [RouterLink, DecimalPipe],
  selector: 'app-movie-details',
  styleUrl: './movie-details.css',
  templateUrl: './movie-details.html',
})
export class MovieDetails {
  private readonly movieApi = inject(MovieApi);
  private readonly ratingApi = inject(RatingApi);
  private readonly destroyRef = inject(DestroyRef);

  /** Route param :id. */
  readonly id = input.required<string>();
  /** ?page= of the home page the user came from (set by the home card). */
  readonly page = input<string>();

  protected readonly stars = [1, 2, 3, 4, 5];
  protected readonly movie = signal<Movie | null>(null);
  protected readonly loading = signal(true);
  protected readonly notFound = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);

  /** Only a valid page above 1 goes back into the link; page 1 is the bare home URL. */
  protected readonly backPage = computed(() => {
    const n = Number(this.page());
    return Number.isInteger(n) && n > 1 ? n : null;
  });

  constructor() {
    // switchMap: opening another movie cancels the request for the previous one.
    toObservable(this.id)
      .pipe(
        tap(() => {
          this.loading.set(true);
          this.notFound.set(false);
          this.error.set(null);
          this.saveError.set(null);
        }),
        switchMap((id) =>
          this.movieApi.getById(id).pipe(
            catchError((err) => {
              if (err instanceof HttpErrorResponse && err.status === 404) {
                this.notFound.set(true);
              } else {
                this.error.set(problemMessage(err, 'Could not load the movie.'));
              }
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((movie) => {
        this.movie.set(movie);
        this.loading.set(false);
      });
  }

  protected rate(value: number): void {
    const movie = this.movie();
    // Ignore clicks during a save (no parallel requests, no double POST) and on the current
    // rating (a PUT that changes nothing).
    if (!movie || this.saving() || value === movie.myRating) return;
    this.saving.set(true);
    this.saveError.set(null);

    // First rating creates it (POST); later changes update it (PUT). POST twice would be a 409.
    const save =
      movie.myRating === null
        ? this.ratingApi.create(movie.id, value)
        : this.ratingApi.update(movie.id, value);

    save
      .pipe(
        // Keep the saved rating even if the refresh below fails, so the next click sends PUT.
        tap((saved) => this.movie.update((m) => m && { ...m, myRating: saved.rating })),
        // Average and count come from the server; never computed in the browser.
        switchMap(() =>
          this.movieApi.getById(movie.id).pipe(
            catchError(() => {
              this.saveError.set('Rating saved, but the average could not be refreshed.');
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (fresh) => {
          if (fresh) this.movie.set(fresh);
          this.saving.set(false);
        },
        error: (err) => {
          // rating-service errors are not ProblemDetail yet, so this is usually the fallback.
          this.saveError.set(problemMessage(err, 'Could not save your rating. Try again.'));
          this.saving.set(false);
        },
      });
  }
}
