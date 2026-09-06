import { USER_ROLES } from '@/features/auth/role'

import { lecturerNavigation } from './navigation'
import { lecturerRoutes } from './routes'

import type { PortalDefinition } from '@/portals/types'

export const lecturerPortal: PortalDefinition = {
  role: USER_ROLES.LECTURER,
  basePath: '/lecturer',
  routes: lecturerRoutes,
  navigation: lecturerNavigation,
  topbar: {
    search: {
      placeholder: 'Tìm khóa học, học viên hoặc nội dung...',
    },

    // primaryAction: {
    //   label: 'AI Assistant',
    //   routeName: 'lecturer-ai-assistant',
    //   icon: markRaw(Sparkles),
    // },
  },
}
