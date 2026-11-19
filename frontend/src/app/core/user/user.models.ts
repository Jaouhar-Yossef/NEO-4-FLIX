import { MovieSummary } from '../movie/movie.models';

export interface MyRating {
  movie: MovieSummary;
  score: number;
  updatedAt: string;
}

export interface UserProfile {
  id: string;
  email: string;
  displayName: string;
  twoFactorEnabled: boolean;
}

export interface UpdateProfileRequest {
  displayName: string;
}
