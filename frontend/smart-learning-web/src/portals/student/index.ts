import { USER_ROLES } from '@/features/auth/role'

import type { PortalDefinition } from '@/portals/types'
import { studentRoutes } from './routes'
import { studentNavigation } from './navigation'

export const studentPortal: PortalDefinition = {
  role: USER_ROLES.STUDENT,
  basePath: '/student',
  routes: studentRoutes,
  navigation: studentNavigation,
  topbar: {
    search: {
      placeholder: 'Tìm khóa học và tài liệu...',
    },
  },
}
