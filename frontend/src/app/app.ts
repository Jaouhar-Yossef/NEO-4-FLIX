import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TokenService } from './core/auth/token.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly tokens = inject(TokenService);
  private readonly router = inject(Router);

  logout(): void {
    this.tokens.clear();
    this.router.navigate(['/login']);
  }
}