import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { RatingApi } from './rating.api';
import { Rating } from './rating.models';

const RATING: Rating = {
  movieId: 'movie-001',
  userId: 'user-1',
  rating: 4,
  createdAt: '2026-11-27T10:00:00Z',
  updatedAt: '2026-11-27T10:00:00Z',
};

describe('RatingApi', () => {
  let api: RatingApi;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(RatingApi);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('create posts the movie id and the rating', () => {
    let result: Rating | undefined;
    api.create('movie-001', 4).subscribe((r) => (result = r));

    const req = backend.expectOne('/api/ratings');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ movieId: 'movie-001', rating: 4 });
    req.flush(RATING, { status: 201, statusText: 'Created' });

    expect(result).toEqual(RATING);
  });

  it('update puts the rating on the encoded movie id', () => {
    let result: Rating | undefined;
    api.update('movie 001/x', 5).subscribe((r) => (result = r));

    const req = backend.expectOne('/api/ratings/movie%20001%2Fx');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ rating: 5 });
    req.flush({ ...RATING, rating: 5 });

    expect(result?.rating).toBe(5);
  });

  it('create passes a 409 through when the movie is already rated', () => {
    let status = 0;
    api.create('movie-001', 3).subscribe({ error: (err) => (status = err.status) });

    // rating-service errors are not ProblemDetail (see step 0 notes): { status, error, message }.
    backend
      .expectOne('/api/ratings')
      .flush(
        { status: 409, error: '409 CONFLICT', message: 'You have already rated this movie' },
        { status: 409, statusText: 'Conflict' },
      );

    expect(status).toBe(409);
  });
});
