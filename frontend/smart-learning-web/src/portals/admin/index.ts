import { USER_ROLES } from '@/features/auth/role'


import type { PortalDefinition } from '@/portals/type'
import { adminRoutes } from './routes'
import { adminNavigation } from './navigation'

export const adminPortal: PortalDefinition = {
  role: USER_ROLES.ADMIN,
  basePath: '/admin',
  routes: adminRoutes,
  navigation: adminNavigation,
}