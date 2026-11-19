import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { TokenService } from '../../core/auth/token.service';
import { problemMessage } from '../../core/problem-detail';
import { UserApi } from '../../core/user/user.api';
import { UserProfile } from '../../core/user/user.models';

@Component({
  selector: 'app-account',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './account.html',
})
export class Account {
  private readonly userApi = inject(UserApi);
  private readonly tokens = inject(TokenService);
  private readonly router = inject(Router);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly profile = signal<UserProfile | null>(null);
  protected readonly saving = signal(false);
  protected readonly saved = signal(false);
  protected readonly confirmingDelete = signal(false);
  protected readonly deleting = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.group({
    displayName: ['', [Validators.required, Validators.maxLength(50), Validators.pattern(/\S/)]],
  });

  constructor() {
    this.userApi.getMe().subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.form.setValue({ displayName: profile.displayName });
      },
      error: (err) => this.error.set(problemMessage(err, 'Could not load your account.')),
    });
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.saved.set(false);
    this.error.set(null);
    this.userApi.updateMe({ displayName: this.form.getRawValue().displayName.trim() }).subscribe({
      next: (profile) => {
        this.saving.set(false);
        this.saved.set(true);
        this.profile.set(profile);
        this.form.setValue({ displayName: profile.displayName });
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(problemMessage(err, 'Could not save your name.'));
      },
    });
  }

  protected deleteAccount(): void {
    this.deleting.set(true);
    this.error.set(null);
    this.userApi.deleteMe().subscribe({
      next: () => {
        this.tokens.clear();
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.deleting.set(false);
        this.error.set(problemMessage(err, 'Could not delete your account.'));
      },
    });
  }
}
