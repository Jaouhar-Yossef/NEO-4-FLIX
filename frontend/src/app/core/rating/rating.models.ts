/** POST /api/ratings and PUT /api/ratings/{movieId} response. rating is 1 to 5. */
export interface Rating {
  movieId: string;
  userId: string;
  rating: number;
  createdAt: string;
  updatedAt: string;
}
