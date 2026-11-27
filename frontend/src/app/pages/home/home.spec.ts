import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { MoviePage } from '../../core/movie/movie.models';
import { Home } from './home';

function moviePage(page: number, totalElements = 50): MoviePage {
  return {
    content: [
      {
        id: 'movie-001',
        title: 'The Matrix',
        releaseYear: 1999,
        genres: ['Action', 'Science Fiction'],
        averageRating: 4.333,
        ratingCount: 3,
      },
      {
        id: 'movie-006',
        title: 'Alien',
        releaseYear: 1979,
        genres: ['Horror'],
        averageRating: null,
        ratingCount: 0,
      },
    ],
    page,
    size: 20,
    totalElements,
  };
}

describe('Home', () => {
  let backend: HttpTestingController;
  let harness: RouterTestingHarness;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: '', component: Home }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    backend = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => backend.verify());

  /** Opens the url and answers the movie request; returns the page index the app sent. */
  async function open(url: string, body: MoviePage): Promise<string | null> {
    await harness.navigateByUrl(url);
    const req = backend.expectOne((r) => r.url === '/api/movies');
    req.flush(body);
    await harness.fixture.whenStable();
    return req.request.params.get('page');
  }

  const el = () => harness.routeNativeElement as HTMLElement;

  it('renders one card per movie, linking to its details page', async () => {
    await open('/', moviePage(0));

    const cards = el().querySelectorAll<HTMLAnchorElement>('a.card');
    expect(cards.length).toBe(2);
    expect(cards[0].getAttribute('href')).toBe('/movies/movie-001');
    expect(cards[0].textContent).toContain('The Matrix');
    expect(cards[0].textContent).toContain('1999');
    expect(cards[0].textContent).toContain('Action, Science Fiction');
    expect(cards[0].textContent).toContain('4.3');
    expect(cards[0].textContent).toContain('3 ratings');
    expect(cards[1].textContent).toContain('Not rated');
  });

  it('reads ?page= as 1-based and asks the API for the 0-based page', async () => {
    expect(await open('/?page=2', moviePage(1))).toBe('1');
    expect(el().textContent).toContain('Page 2 of 3');
  });

  it('falls back to page 1 for an invalid ?page=', async () => {
    expect(await open('/?page=abc', moviePage(0))).toBe('0');
  });

  it('caps a huge ?page= so the API never overflows', async () => {
    expect(await open('/?page=999999999', moviePage(9999))).toBe('9999');
  });

  it('shows previous and next links in the middle pages', async () => {
    await open('/?page=2', moviePage(1));

    const links = [...el().querySelectorAll<HTMLAnchorElement>('.pager a')];
    expect(links.map((a) => a.getAttribute('href'))).toEqual(['/?page=1', '/?page=3']);
  });

  it('disables previous on the first page and next on the last', async () => {
    await open('/', moviePage(0));
    expect(el().querySelector('.pager .disabled')?.textContent).toContain('Previous');

    await open('/?page=3', moviePage(2));
    expect(el().querySelector('.pager .disabled')?.textContent).toContain('Next');
  });

  it('loads the next page when next is clicked', async () => {
    await open('/', moviePage(0));

    el().querySelector<HTMLAnchorElement>('.pager a')!.click();
    await harness.fixture.whenStable();

    const req = backend.expectOne((r) => r.url === '/api/movies');
    expect(req.request.params.get('page')).toBe('1');
    req.flush(moviePage(1));
  });

  it('shows the empty state when there are no movies', async () => {
    await open('/', { content: [], page: 0, size: 20, totalElements: 0 });
    expect(el().textContent).toContain('No movies yet.');
    expect(el().querySelector('.pager')).toBeNull();
  });

  it('offers the first page when the page is past the end', async () => {
    await open('/?page=9', { content: [], page: 8, size: 20, totalElements: 50 });
    expect(el().textContent).toContain('This page is empty.');
  });

  it('shows the ProblemDetail message on error', async () => {
    await harness.navigateByUrl('/');
    backend
      .expectOne((r) => r.url === '/api/movies')
      .flush(
        { status: 400, title: 'Invalid Request', detail: 'Size must be between 1 and 100' },
        { status: 400, statusText: 'Bad Request' },
      );
    await harness.fixture.whenStable();

    expect(el().querySelector('[role="alert"]')?.textContent).toContain(
      'Size must be between 1 and 100',
    );
    expect(el().querySelector('.grid')).toBeNull();
  });
});
