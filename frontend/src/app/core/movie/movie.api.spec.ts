import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { MovieApi } from './movie.api';
import { MoviePage } from './movie.models';

const PAGE: MoviePage = {
  content: [
    {
      id: 'movie-005',
      title: 'Blade Runner',
      releaseYear: 1982,
      genres: ['Science Fiction', 'Thriller'],
      averageRating: null,
      ratingCount: 0,
    },
  ],
  page: 1,
  size: 20,
  totalElements: 50,
};

describe('MovieApi', () => {
  let api: MovieApi;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(MovieApi);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('list sends page and size and returns the page', () => {
    let result: MoviePage | undefined;
    api.list({ page: 1, size: 20 }).subscribe((page) => (result = page));

    const req = backend.expectOne((r) => r.url === '/api/movies');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('20');
    req.flush(PAGE);

    expect(result).toEqual(PAGE);
  });

  it('list passes a ProblemDetail error through', () => {
    let status = 0;
    api.list({ page: 0, size: 500 }).subscribe({ error: (err) => (status = err.status) });

    backend
      .expectOne((r) => r.url === '/api/movies')
      .flush(
        { status: 400, title: 'Invalid Request', detail: 'Size must be between 1 and 100' },
        { status: 400, statusText: 'Bad Request' },
      );

    expect(status).toBe(400);
  });
});
