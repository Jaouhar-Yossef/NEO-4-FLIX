import { TestBed } from '@angular/core/testing';
import { TokenService } from './token.service';

function fakeJwt(expSecondsFromNow: number): string {
  const payload = btoa(JSON.stringify({ sub: 'u1', exp: Math.floor(Date.now() / 1000) + expSecondsFromNow }));
  return `x.${payload}.y`;
}

describe('TokenService', () => {
  let service: TokenService;

  beforeEach(() => {
    sessionStorage.clear();
    service = TestBed.inject(TokenService);
  });

  it('is logged out with no token', () => expect(service.isLoggedIn()).toBe(false));

  it('is logged in with an unexpired token', () => {
    service.set(fakeJwt(3600));
    expect(service.isLoggedIn()).toBe(true);
  });

  it('is logged out with an expired token', () => {
    service.set(fakeJwt(-10));
    expect(service.isLoggedIn()).toBe(false);
  });

  it('is logged out with a malformed token', () => {
    service.set('not-a-jwt');
    expect(service.isLoggedIn()).toBe(false);
  });

  it('clear removes the token', () => {
    service.set(fakeJwt(3600));
    service.clear();
    expect(service.get()).toBeNull();
  });
});