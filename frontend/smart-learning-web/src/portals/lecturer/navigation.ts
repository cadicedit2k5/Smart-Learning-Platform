import { BookOpenCheck, CircleUserRound, LayoutDashboard } from 'lucide-vue-next'
import { markRaw } from 'vue'

import type { NavigationItem } from '@/portals/types'

export const lecturerNavigation: NavigationItem[] = [
  {
    label: 'Tổng quan',
    routeName: 'lecturer-dashboard',
    icon: markRaw(LayoutDashboard),
  },
  {
    label: 'Khóa học của tôi',
    routeName: 'lecturer-courses',
    icon: markRaw(BookOpenCheck),
  },
  {
    label: 'Tài khoản',
    routeName: 'lecturer-profile',
    icon: markRaw(CircleUserRound),
  },
]
