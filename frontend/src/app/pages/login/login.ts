import { Component, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthApi } from '../../core/auth/auth.api';
import { TokenService } from '../../core/auth/token.service';
import { problemMessage } from '../../core/problem-detail';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
})
export class Login {
  private readonly authApi = inject(AuthApi);
  private readonly tokens = inject(TokenService);
  private readonly router = inject(Router);
  private readonly fb = inject(NonNullableFormBuilder);

  readonly returnUrl = input<string>();
  readonly registered = input<string>();

  protected readonly step = signal<'credentials' | 'code'>('credentials');
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  private tempToken = ''; 

  protected readonly credentialsForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  protected readonly codeForm = this.fb.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  protected submitCredentials(): void {
    if (this.credentialsForm.invalid) {
      this.credentialsForm.markAllAsTouched();
      return;
    }
    this.start();
    this.authApi.login(this.credentialsForm.getRawValue()).subscribe({
      next: (res) => {
        this.loading.set(false);
        if ('token' in res) {
          this.finish(res.token);
        } else {
          this.tempToken = res.tempToken;
          this.step.set('code');
        }
      },
      error: (err) => this.fail(err, 'Login failed.'),
    });
  }

  protected submitCode(): void {
    if (this.codeForm.invalid) {
      this.codeForm.markAllAsTouched();
      return;
    }
    this.start();
    this.authApi
      .verifyTwoFactor({ tempToken: this.tempToken, code: this.codeForm.getRawValue().code })
      .subscribe({
        next: (res) => {
          this.loading.set(false);
          this.finish(res.token);
        },
        error: (err) => this.fail(err, 'Verification failed.'),
      });
  }

  private start(): void {
    this.loading.set(true);
    this.error.set(null);
  }

  private fail(err: unknown, fallback: string): void {
    this.loading.set(false);
    this.error.set(problemMessage(err, fallback));
  }

  private finish(token: string): void {
    this.tokens.set(token);
    this.router.navigateByUrl(this.safeReturnUrl());
  }

  private safeReturnUrl(): string {
    const url = this.returnUrl();
    return url && url.startsWith('/') && !url.startsWith('//') ? url : '/';
  }
}