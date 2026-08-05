import type { UserRole } from '@/features/auth/role'
import 'vue-router'

export {}

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    portal?: UserRole
    requiresAuth?: boolean
    role?: UserRole
    permissions?: string[]
  }
}