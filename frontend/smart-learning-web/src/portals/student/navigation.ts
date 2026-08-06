import type { NavigationItem } from '@/portals/types'
import { LayoutDashboard } from 'lucide-vue-next'
import { markRaw } from 'vue'

export const studentNavigation: NavigationItem[] = [
   {
    label: 'Dashboard',
    routeName: 'student-dashboard',
    icon: markRaw(LayoutDashboard),
  },
]