import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthApi } from '../../core/auth/auth.api';
import { PASSWORD_RULES, passwordPolicyValidator } from '../../core/auth/password-policy';
import { problemMessage } from '../../core/problem-detail';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
})
export class Register {
  private readonly authApi = inject(AuthApi);
  private readonly router = inject(Router);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    displayName: ['', Validators.required],
    password: ['', passwordPolicyValidator()],
  });

  private readonly password = toSignal(this.form.controls.password.valueChanges, { initialValue: '' });
  protected readonly rules = computed(() =>
    PASSWORD_RULES.map((rule) => ({ id: rule.id, label: rule.label, ok: rule.test(this.password()) })),
  );

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.authApi.register(this.form.getRawValue()).subscribe({
      next: () => this.router.navigate(['/login'], { queryParams: { registered: 1 } }),
      error: (err) => {
        this.loading.set(false);
        this.error.set(problemMessage(err, 'Registration failed.'));
      },
    });
  }
}