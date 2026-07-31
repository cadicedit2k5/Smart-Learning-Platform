import type { NavigationItem } from '@/portals/types'
import { LayoutDashboard } from 'lucide-vue-next'
import { markRaw } from 'vue'

export const adminNavigation: NavigationItem[] = [
   {
    label: 'Dashboard',
    routeName: 'admin-dashboard',
    icon: markRaw(LayoutDashboard),
  },
]