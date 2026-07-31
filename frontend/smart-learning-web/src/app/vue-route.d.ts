import 'vue-router'

import type { UserRole } from '@/shared/constants/userRole'

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