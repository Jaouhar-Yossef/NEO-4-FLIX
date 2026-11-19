import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { problemMessage } from '../../core/problem-detail';
import { UserApi } from '../../core/user/user.api';
import { MyRating } from '../../core/user/user.models';

@Component({
  imports: [RouterLink, DatePipe, DecimalPipe],
  selector: 'app-my-ratings',
  styleUrl: './my-ratings.css',
  templateUrl: './my-ratings.html',
})
export class MyRatings {
  private readonly userApi = inject(UserApi);

  protected readonly ratings = signal<MyRating[] | null>(null);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.userApi.getMyRatings().subscribe({
      next: (ratings) => this.ratings.set(ratings),
      error: (err) => this.error.set(problemMessage(err, 'Could not load your ratings.')),
    });
  }

  protected stars(score: number): string {
    return '★'.repeat(score) + '☆'.repeat(5 - score);
  }
}
