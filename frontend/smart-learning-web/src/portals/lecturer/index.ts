import { USER_ROLES } from '@/features/auth/role'

import { lecturerNavigation } from './navigation'
import { lecturerRoutes } from './routes'

import type { PortalDefinition } from '@/portals/type'

export const lecturerPortal: PortalDefinition = {
  role: USER_ROLES.LECTURER,
  basePath: '/lecturer',
  routes: lecturerRoutes,
  navigation: lecturerNavigation,
}