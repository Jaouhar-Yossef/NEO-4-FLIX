import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { authGuard } from './auth.guard';
import { TokenService } from './token.service';

function fakeJwt(expSecondsFromNow: number): string {
  const payload = btoa(JSON.stringify({ sub: 'u1', exp: Math.floor(Date.now() / 1000) + expSecondsFromNow }));
  return `x.${payload}.y`;
}

describe('authGuard', () => {
  const run = (url: string) =>
    TestBed.runInInjectionContext(() => authGuard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot));

  beforeEach(() => sessionStorage.clear());

  it('lets a logged-in user through', () => {
    TestBed.inject(TokenService).set(fakeJwt(3600));
    expect(run('/watchlist')).toBe(true);
  });

  it('sends a logged-out user to /login with returnUrl', () => {
    const result = run('/watchlist') as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(result)).toBe('/login?returnUrl=%2Fwatchlist');
  });
});