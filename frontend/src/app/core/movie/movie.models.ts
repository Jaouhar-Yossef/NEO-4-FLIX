export interface MovieSummary {
  id: string;
  title: string;
  releaseYear: number | null;
  genres: string[];
  averageRating: number | null;
  ratingCount: number;
}

export interface MoviePage {
  content: MovieSummary[];
  page: number;
  size: number;
  totalElements: number;
}
