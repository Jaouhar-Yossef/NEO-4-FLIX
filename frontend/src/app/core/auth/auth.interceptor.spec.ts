import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { TokenService } from './token.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let tokens: TokenService;
  let router: Router;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    tokens = TestBed.inject(TokenService);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => backend.verify());

  it('adds the Bearer token to /api calls', () => {
    tokens.set('abc');
    http.get('/api/movies').subscribe();
    expect(backend.expectOne('/api/movies').request.headers.get('Authorization')).toBe('Bearer abc');
  });

  it('does not add a token to non-/api calls', () => {
    tokens.set('abc');
    http.get('/assets/x.json').subscribe();
    expect(backend.expectOne('/assets/x.json').request.headers.has('Authorization')).toBe(false);
  });

  it('on 401 clears the token and goes to /login', () => {
    tokens.set('abc');
    http.get('/api/movies').subscribe({ error: () => {} });
    backend.expectOne('/api/movies').flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(tokens.get()).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login'], expect.anything());
  });

  it('on 401 from /api/auth/* (wrong password) does not redirect', () => {
    http.post('/api/auth/login', {}).subscribe({ error: () => {} });
    backend.expectOne('/api/auth/login').flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(router.navigate).not.toHaveBeenCalled();
  });
});