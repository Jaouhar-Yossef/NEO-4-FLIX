export interface RegisterRequest {
  email: string;
  displayName: string;
  password: string;
}

export interface RegisterResponse {
  id: string;
  email: string;
  displayName: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export type LoginResponse =
  | { token: string }
  | { twoFactorRequired: true; tempToken: string };

export interface TwoFactorVerifyRequest {
  tempToken: string;
  code: string;
}

export interface TokenResponse {
  token: string;
}

export interface TwoFactorSetupResponse {
  otpauthUri: string;
}

export interface ProblemDetail {
  status: number;
  title: string;
  detail?: string;
  instance?: string;
}