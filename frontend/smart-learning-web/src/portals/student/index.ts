import { USER_ROLES } from '@/features/auth/role'

import type { PortalDefinition } from '@/portals/types'
import { studentRoutes } from './routes'
import { studentNavigation } from './navigation'

export const studentPortal: PortalDefinition = {
  role: USER_ROLES.STUDENT,
  portalName: 'Student Portal',
  basePath: '/student',
  homeRouteName: 'student-dashboard',
  routes: studentRoutes,
  navigation: studentNavigation,
   topbar: {
    search: {
      placeholder: 'Search courses and learning materials...',
    },
  },
}