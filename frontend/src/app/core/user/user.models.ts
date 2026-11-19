export interface UserProfile {
  id: string;
  email: string;
  displayName: string;
  twoFactorEnabled: boolean;
}

export interface UpdateProfileRequest {
  displayName: string;
}
