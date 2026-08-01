import { USER_ROLES } from '@/features/auth/role'

import { lecturerNavigation } from './navigation'
import { lecturerRoutes } from './routes'

import type { PortalDefinition } from '@/portals/types'

export const lecturerPortal: PortalDefinition = {
  role: USER_ROLES.LECTURER,
  basePath: '/lecturer',
  homeRouteName: 'lecturer-dashboard',
  routes: lecturerRoutes,
  navigation: lecturerNavigation,
  topbar: {
    search: {
      placeholder: 'Search students, courses, or insights...',
    },

    // primaryAction: {
    //   label: 'AI Assistant',
    //   routeName: 'lecturer-ai-assistant',
    //   icon: markRaw(Sparkles),
    // },
  },
}