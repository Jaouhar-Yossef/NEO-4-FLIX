import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';

import { TokenService } from '../../core/auth/token.service';
import { UserProfile } from '../../core/user/user.models';
import { Account } from './account';

const ALICE: UserProfile = {
  id: 'user-1',
  email: 'alice@example.com',
  displayName: 'Alice',
  twoFactorEnabled: false,
};

describe('Account', () => {
  let backend: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      imports: [Account],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    backend = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => backend.verify());

  async function render(profile: UserProfile = ALICE) {
    const fixture = TestBed.createComponent(Account);
    backend.expectOne({ method: 'GET', url: '/api/users/me' }).flush(profile);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    return { fixture, el };
  }

  function button(el: HTMLElement, label: string): HTMLButtonElement {
    const found = [...el.querySelectorAll('button')].find((b) => b.textContent?.trim() === label);
    if (!found) throw new Error(`no button "${label}"`);
    return found;
  }

  it('shows the profile and fills the name field', async () => {
    const { el } = await render();
    expect(el.textContent).toContain('alice@example.com');
    expect(el.querySelector('input')!.value).toBe('Alice');
  });

  it('links to the 2FA setup when 2FA is off', async () => {
    const { el } = await render();
    expect(el.querySelector('a[href="/account/2fa"]')).not.toBeNull();
  });

  it('does not offer 2FA setup when it is already on', async () => {
    const { el } = await render({ ...ALICE, twoFactorEnabled: true });
    expect(el.textContent).toContain('2FA is on.');
    expect(el.querySelector('a[href="/account/2fa"]')).toBeNull();
  });

  it('saves the trimmed display name with PUT', async () => {
    const { fixture, el } = await render();
    const input = el.querySelector('input')!;
    input.value = '  Alice B.  ';
    input.dispatchEvent(new Event('input'));
    el.querySelector('form')!.dispatchEvent(new Event('submit'));

    const req = backend.expectOne({ method: 'PUT', url: '/api/users/me' });
    expect(req.request.body).toEqual({ displayName: 'Alice B.' });
    req.flush({ ...ALICE, displayName: 'Alice B.' });
    await fixture.whenStable();
    expect(el.textContent).toContain('Saved.');
  });

  it('does not send a blank name', async () => {
    const { fixture, el } = await render();
    const input = el.querySelector('input')!;
    input.value = '   ';
    input.dispatchEvent(new Event('input'));
    el.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    backend.expectNone({ method: 'PUT', url: '/api/users/me' });
    expect(el.textContent).toContain('Enter a name');
  });

  it('asks for confirmation, then deletes, drops the token and goes to /login', async () => {
    TestBed.inject(TokenService).set('abc');
    const { fixture, el } = await render();

    button(el, 'Delete my account').click();
    await fixture.whenStable();
    backend.expectNone({ method: 'DELETE', url: '/api/users/me' });

    button(el, 'Yes, delete my account').click();
    backend.expectOne({ method: 'DELETE', url: '/api/users/me' }).flush(null, { status: 204, statusText: 'No Content' });

    expect(TestBed.inject(TokenService).get()).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('shows the error and keeps the token when delete fails', async () => {
    TestBed.inject(TokenService).set('abc');
    const { fixture, el } = await render();

    button(el, 'Delete my account').click();
    await fixture.whenStable();
    button(el, 'Yes, delete my account').click();
    backend
      .expectOne({ method: 'DELETE', url: '/api/users/me' })
      .flush({ status: 500, title: 'Internal Server Error', detail: 'unexpected error' }, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(el.textContent).toContain('unexpected error');
    expect(TestBed.inject(TokenService).get()).toBe('abc');
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
