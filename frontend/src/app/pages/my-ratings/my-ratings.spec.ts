import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { MyRating } from '../../core/user/user.models';
import { MyRatings } from './my-ratings';

const MATRIX: MyRating = {
  movie: {
    id: 'movie-001',
    title: 'The Matrix',
    releaseYear: 1999,
    genres: ['Science Fiction'],
    averageRating: 4.5,
    ratingCount: 2,
  },
  score: 4,
  updatedAt: '2026-11-15T10:00:00Z',
};

describe('MyRatings', () => {
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [MyRatings],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  async function render(flush: (req: ReturnType<HttpTestingController['expectOne']>) => void) {
    const fixture = TestBed.createComponent(MyRatings);
    flush(backend.expectOne({ method: 'GET', url: '/api/users/me/ratings' }));
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('lists each rated movie with a link, my score and the average', async () => {
    const el = await render((req) => req.flush([MATRIX]));

    const link = el.querySelector('a.title') as HTMLAnchorElement;
    expect(link.textContent).toBe('The Matrix');
    expect(link.getAttribute('href')).toBe('/movies/movie-001');
    expect(el.textContent).toContain('(1999)');
    expect(el.textContent).toContain('Science Fiction');
    expect(el.querySelector('.stars')!.textContent).toBe('★★★★☆');
    expect(el.querySelector('.stars')!.getAttribute('aria-label')).toBe('4 out of 5');
    expect(el.textContent).toContain('average 4.5 (2)');
  });

  it('shows an empty state with a link to the movies', async () => {
    const el = await render((req) => req.flush([]));

    expect(el.textContent).toContain("You haven't rated any movies yet.");
    expect(el.querySelector('a[href="/"]')).not.toBeNull();
  });

  it('shows the error message when loading fails', async () => {
    const el = await render((req) =>
      req.flush(
        { status: 500, title: 'Internal Server Error', detail: 'unexpected error' },
        { status: 500, statusText: 'Server Error' },
      ),
    );

    expect(el.querySelector('[role="alert"]')!.textContent).toContain('unexpected error');
  });
});
