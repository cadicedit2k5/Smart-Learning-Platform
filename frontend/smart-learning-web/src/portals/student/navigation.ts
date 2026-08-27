import type { NavigationItem } from '@/portals/types'
import { BookOpenCheck, LayoutDashboard } from 'lucide-vue-next'
import { markRaw } from 'vue'

export const studentNavigation: NavigationItem[] = [
  {
    label: 'Tổng quan',
    routeName: 'student-dashboard',
    icon: markRaw(LayoutDashboard),
  },
  {
    label: 'Khóa học của tôi',
    routeName: 'student-courses',
    icon: markRaw(BookOpenCheck),
  },
]
