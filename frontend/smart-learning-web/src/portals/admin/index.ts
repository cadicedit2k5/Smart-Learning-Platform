import { USER_ROLES } from '@/features/auth/role'


import type { PortalDefinition } from '@/portals/types'
import { adminRoutes } from './routes'
import { adminNavigation } from './navigation'

export const adminPortal: PortalDefinition = {
  role: USER_ROLES.ADMIN,
  portalName: 'Admin Portal',
  basePath: '/admin',
  homeRouteName: 'admin-dashboard',
  routes: adminRoutes,
  navigation: adminNavigation,
  topbar: {
    search: {
      placeholder: 'Search users, courses, or system resources...',
    },
  },
}