import { Component, input } from '@angular/core';

@Component({
  imports: [],
  selector: 'app-movie-details',
  styleUrl: './movie-details.css',
  templateUrl: './movie-details.html',
})
export class MovieDetails {
  readonly id = input.required<string>();
}