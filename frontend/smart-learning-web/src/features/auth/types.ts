import type { UserRole } from "./role"

export interface AuthUser {
  id: string
  name: string
  roles: UserRole[]
  permissions: string[]
}