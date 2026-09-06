import type { UserRole } from "./role"

export interface Permission {
  id: number
  code: string
  description: string
};

export interface AuthRole {
  id: number
  code: UserRole
  name: string
  permissions: Permission[]
};

export type UserStatus = | 'ACTIVE' | 'DELETED';

export interface AuthUser {
  id: string
  email: string
  fullName: string
  avatar: string | null
  role: AuthRole
  status: UserStatus
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

export interface UpdateCurrentUserRequest {
  email?: string
  password?: string
  fullName?: string
  avatar?: File
}