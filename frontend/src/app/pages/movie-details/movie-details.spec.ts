import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { MovieDetails as Movie } from '../../core/movie/movie.models';
import { MovieDetails } from './movie-details';

const MOVIE: Movie = {
  id: 'movie-001',
  title: 'The Matrix',
  releaseYear: 1999,
  genres: ['Action', 'Science Fiction'],
  averageRating: 4.333,
  ratingCount: 3,
  myRating: null,
  inWatchlist: false,
};

const RATING = {
  movieId: 'movie-001',
  userId: 'user-1',
  createdAt: '2026-11-27T10:00:00Z',
  updatedAt: '2026-11-27T10:00:00Z',
};

describe('MovieDetails', () => {
  let backend: HttpTestingController;
  let harness: RouterTestingHarness;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        // Same input binding as app.config.ts: :id and ?page= reach the inputs.
        provideRouter([{ path: 'movies/:id', component: MovieDetails }], withComponentInputBinding()),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    backend = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => backend.verify());

  const el = () => harness.routeNativeElement as HTMLElement;
  const stars = () => [...el().querySelectorAll<HTMLButtonElement>('.stars button')];

  /** Opens the url and answers the movie request with the movie. */
  async function open(url: string, movie: Movie): Promise<void> {
    await harness.navigateByUrl(url);
    backend.expectOne('/api/movies/movie-001').flush(movie);
    await harness.fixture.whenStable();
  }

  /** Answers the reload that follows a successful save. */
  async function refresh(movie: Movie): Promise<void> {
    const req = backend.expectOne('/api/movies/movie-001');
    expect(req.request.method).toBe('GET');
    req.flush(movie);
    await harness.fixture.whenStable();
  }

  it('renders the movie details and the average rating', async () => {
    await open('/movies/movie-001', { ...MOVIE, myRating: 4 });

    const text = el().textContent ?? '';
    expect(el().querySelector('h1')?.textContent).toBe('The Matrix');
    expect(text).toContain('1999');
    expect(text).toContain('Action, Science Fiction');
    expect(text).toContain('4.3');
    expect(text).toContain('3 ratings');
    expect(text).toContain('You rated it 4/5.');
    expect(stars()[3].getAttribute('aria-pressed')).toBe('true');
    expect(stars().filter((b) => b.classList.contains('on')).length).toBe(4);
  });

  it('shows "Not rated yet" for a movie without ratings', async () => {
    await open('/movies/movie-001', { ...MOVIE, averageRating: null, ratingCount: 0 });

    expect(el().textContent).toContain('Not rated yet');
    expect(el().textContent).toContain("You haven't rated this movie yet.");
    expect(stars().some((b) => b.classList.contains('on'))).toBe(false);
  });

  it('links back to the home page the user came from', async () => {
    await open('/movies/movie-001?page=3', MOVIE);
    expect(el().querySelector('a.back')?.getAttribute('href')).toBe('/?page=3');
  });

  it('links back to the bare home page for page 1 or a bad ?page=', async () => {
    await open('/movies/movie-001?page=abc', MOVIE);
    expect(el().querySelector('a.back')?.getAttribute('href')).toBe('/');
  });

  it('shows "Movie not found" with a link home for an unknown id', async () => {
    await harness.navigateByUrl('/movies/missing');
    backend
      .expectOne('/api/movies/missing')
      .flush(
        { status: 404, title: 'Movie Not Found', detail: 'Movie not found with id: missing' },
        { status: 404, statusText: 'Not Found' },
      );
    await harness.fixture.whenStable();

    expect(el().querySelector('h1')?.textContent).toBe('Movie not found');
    expect(el().querySelector('.stars')).toBeNull();
  });

  it('shows other load errors through the ProblemDetail message', async () => {
    await harness.navigateByUrl('/movies/movie-001');
    backend
      .expectOne('/api/movies/movie-001')
      .flush(
        { status: 503, title: 'Service Unavailable', detail: 'service unavailable, try again later' },
        { status: 503, statusText: 'Service Unavailable' },
      );
    await harness.fixture.whenStable();

    expect(el().querySelector('[role="alert"]')?.textContent).toContain('service unavailable, try again later');
  });

  it('creates the first rating (POST), then shows the average from the server', async () => {
    await open('/movies/movie-001', MOVIE);

    stars()[4].click();
    const req = backend.expectOne('/api/ratings');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ movieId: 'movie-001', rating: 5 });
    req.flush({ ...RATING, rating: 5 }, { status: 201, statusText: 'Created' });
    await refresh({ ...MOVIE, myRating: 5, averageRating: 4.5, ratingCount: 4 });

    expect(el().textContent).toContain('You rated it 5/5.');
    expect(el().textContent).toContain('4.5');
    expect(el().textContent).toContain('4 ratings');
  });

  it('updates an existing rating (PUT)', async () => {
    await open('/movies/movie-001', { ...MOVIE, myRating: 4 });

    stars()[1].click();
    const req = backend.expectOne('/api/ratings/movie-001');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ rating: 2 });
    req.flush({ ...RATING, rating: 2 });
    await refresh({ ...MOVIE, myRating: 2, averageRating: 3.667 });

    expect(el().textContent).toContain('You rated it 2/5.');
    expect(el().textContent).toContain('3.7');
  });

  it('sends nothing when the user clicks their current rating', async () => {
    await open('/movies/movie-001', { ...MOVIE, myRating: 4 });

    stars()[3].click();
    backend.expectNone('/api/ratings/movie-001');
  });

  it('ignores clicks while a save is running', async () => {
    await open('/movies/movie-001', MOVIE);

    stars()[4].click();
    await harness.fixture.whenStable();
    expect(stars().every((b) => b.disabled)).toBe(true);
    stars()[2].click();

    // Only the first click reached the server: one POST, no PUT.
    backend.expectOne('/api/ratings').flush({ ...RATING, rating: 5 }, { status: 201, statusText: 'Created' });
    backend.expectNone('/api/ratings/movie-001');
    await refresh({ ...MOVIE, myRating: 5 });
  });

  it('shows a save error next to the rating control and keeps the old rating', async () => {
    await open('/movies/movie-001', { ...MOVIE, myRating: 4 });

    stars()[0].click();
    // rating-service error body is not ProblemDetail, so the fallback message is shown.
    backend
      .expectOne('/api/ratings/movie-001')
      .flush(
        { status: 500, error: 'Internal Server Error', message: 'An unexpected error occurred' },
        { status: 500, statusText: 'Internal Server Error' },
      );
    await harness.fixture.whenStable();

    expect(el().querySelector('.my-rating [role="alert"]')?.textContent).toContain(
      'Could not save your rating. Try again.',
    );
    expect(el().textContent).toContain('You rated it 4/5.');
    expect(stars().every((b) => !b.disabled)).toBe(true);
  });

  it('sends PUT next time when the save worked but the refresh failed', async () => {
    await open('/movies/movie-001', MOVIE);

    stars()[2].click();
    backend.expectOne('/api/ratings').flush({ ...RATING, rating: 3 }, { status: 201, statusText: 'Created' });
    backend.expectOne('/api/movies/movie-001').flush(null, { status: 503, statusText: 'Service Unavailable' });
    await harness.fixture.whenStable();

    expect(el().textContent).toContain('Rating saved, but the average could not be refreshed.');
    expect(el().textContent).toContain('You rated it 3/5.');

    stars()[3].click();
    expect(backend.expectOne('/api/ratings/movie-001').request.method).toBe('PUT');
  });
});
