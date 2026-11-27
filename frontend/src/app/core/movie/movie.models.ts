export interface MovieSummary {
  id: string;
  title: string;
  releaseYear: number | null;
  genres: string[];
  averageRating: number | null;
  ratingCount: number;
}

/** GET /api/movies/{id}: myRating is null until the user rates the movie. */
export interface MovieDetails extends MovieSummary {
  myRating: number | null;
  inWatchlist: boolean;
}

export interface MoviePage {
  content: MovieSummary[];
  page: number;
  size: number;
  totalElements: number;
}
