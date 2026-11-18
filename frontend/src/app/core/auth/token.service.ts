import { Injectable, signal } from '@angular/core';

const TOKEN_KEY = 'neo4flix.token';

@Injectable({ providedIn: 'root' })
export class TokenService {
  private readonly token = signal<string | null>(sessionStorage.getItem(TOKEN_KEY));

  get(): string | null {
    return this.token();
  }

  set(token: string): void {
    sessionStorage.setItem(TOKEN_KEY, token);
    this.token.set(token);
  }

  clear(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    this.token.set(null);
  }

  isLoggedIn(): boolean {
    const token = this.token();
    if (!token) return false;
    try {
      const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
      return typeof payload.exp === 'number' && payload.exp * 1000 > Date.now();
    } catch {
      return false;
    }
  }
}