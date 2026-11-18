import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  // Public
  { path: 'login', loadComponent: () => import('./pages/login/login').then((m) => m.Login) },
  { path: 'register', loadComponent: () => import('./pages/register/register').then((m) => m.Register) },

  // Logged in only
  {
    path: '',
    canActivateChild: [authGuard],
    children: [
      { path: '', loadComponent: () => import('./pages/home/home').then((m) => m.Home) },
      { path: 'movies/:id', loadComponent: () => import('./pages/movie-details/movie-details').then((m) => m.MovieDetails) },
      { path: 'my-ratings', loadComponent: () => import('./pages/my-ratings/my-ratings').then((m) => m.MyRatings) },
      { path: 'recommendations', loadComponent: () => import('./pages/recommendations/recommendations').then((m) => m.Recommendations) },
      { path: 'watchlist', loadComponent: () => import('./pages/watchlist/watchlist').then((m) => m.Watchlist) },
      { path: 'shared-with-me', loadComponent: () => import('./pages/shared-with-me/shared-with-me').then((m) => m.SharedWithMe) },
    ],
  },

  { path: '**', redirectTo: '' },
];