import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AuthApi } from '../../core/auth/auth.api';
import { problemMessage } from '../../core/problem-detail';

@Component({
  selector: 'app-two-factor-setup',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './two-factor-setup.html',
})
export class TwoFactorSetup {
  private readonly authApi = inject(AuthApi);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly secret = signal<string | null>(null);
  protected readonly done = signal(false);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly codeForm = this.fb.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  constructor() {
    this.authApi.setupTwoFactor().subscribe({
      next: ({ otpauthUri }) => this.secret.set(new URL(otpauthUri).searchParams.get('secret')),
      error: (err) => this.error.set(problemMessage(err, 'Could not start 2FA setup.')),
    });
  }

  protected confirm(): void {
    if (this.codeForm.invalid) {
      this.codeForm.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.authApi.enableTwoFactor(this.codeForm.getRawValue().code).subscribe({
      next: () => this.done.set(true),
      error: (err) => {
        this.loading.set(false);
        this.error.set(problemMessage(err, 'Wrong code.'));
      },
    });
  }
}