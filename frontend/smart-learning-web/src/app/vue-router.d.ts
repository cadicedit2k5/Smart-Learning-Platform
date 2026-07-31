import type { UserRole } from '@/features/auth/role'
import 'vue-router'

export {}

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    portal?: UserRole
    requiresAuth?: boolean
    roles?: UserRole[]
    permissions?: string[]
  }
}