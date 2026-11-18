import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE } from '../api';
import {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
  TokenResponse,
  TwoFactorSetupResponse,
  TwoFactorVerifyRequest,
} from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE}/auth`;

  register(body: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.base}/register`, body);
  }

  login(body: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.base}/login`, body);
  }

  verifyTwoFactor(body: TwoFactorVerifyRequest): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${this.base}/2fa/verify`, body);
  }

  setupTwoFactor(): Observable<TwoFactorSetupResponse> {
    return this.http.post<TwoFactorSetupResponse>(`${this.base}/2fa/setup`, {});
  }

  enableTwoFactor(code: string): Observable<void> {
    return this.http.post<void>(`${this.base}/2fa/enable`, { code });
  }
}