import type { UserRole } from "./role"

export interface AuthRole {
  id: number
  code: UserRole
  name: string
};

export interface AuthUser {
  id: string
  email: string
  fullName: string
  avatar: string | null
  role: AuthRole
  status: string
  createdAt: string
  updatedAt: string
};

export interface LoginRequest {
  email: string
  password: string
};

export interface LoginResponse {
  accessToken: string
  expiresIn: number
};

export interface RegisterRequest {
  email: string
  password: string
  fullName: string
  avatar?: File
};