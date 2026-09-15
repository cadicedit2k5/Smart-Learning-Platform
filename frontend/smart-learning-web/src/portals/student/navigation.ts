import type { NavigationItem } from '@/portals/types'
import { BookOpenCheck, CircleUserRound, Compass, LayoutDashboard } from 'lucide-vue-next'
import { markRaw } from 'vue'

export const studentNavigation: NavigationItem[] = [
  {
    label: 'Tổng quan',
    routeName: 'student-dashboard',
    icon: markRaw(LayoutDashboard),
  },
  {
    label: 'Khám phá khóa học',
    routeName: 'student-public-courses',
    icon: markRaw(Compass),
  },
  {
    label: 'Khóa học của tôi',
    routeName: 'student-courses',
    icon: markRaw(BookOpenCheck),
  },
  {
    label: 'Tài khoản',
    routeName: 'student-profile',
    icon: markRaw(CircleUserRound),
  },
]
