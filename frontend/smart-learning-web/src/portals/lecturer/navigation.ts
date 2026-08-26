import { BookOpenCheck, LayoutDashboard } from 'lucide-vue-next'
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
  // {
  //   label: 'Learning Analytics',
  //   routeName: 'lecturer-analytics',
  //   icon: markRaw(BarChart3),
  // },
  // {
  //   label: 'AI Insights',
  //   routeName: 'lecturer-ai-insights',
  //   icon: markRaw(BrainCircuit),
  // },
  // {
  //   label: 'Settings',
  //   routeName: 'lecturer-settings',
  //   icon: markRaw(Settings),
  // },
]
