import type { UserRole } from "./role"

export interface AuthUser {
  id: string
  name: string
  roles: UserRole[]
  permissions: string[]
}

export interface LoginCredentials {
  email: string
  password: string
}

export interface LoginResponse {
  token: string
}