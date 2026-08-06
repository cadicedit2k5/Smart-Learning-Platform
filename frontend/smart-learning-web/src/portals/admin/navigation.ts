import type { NavigationItem } from '@/portals/types'
import { LayoutDashboard, UserIcon } from 'lucide-vue-next'
import { markRaw } from 'vue'

export const adminNavigation: NavigationItem[] = [
   {
    label: 'Dashboard',
    routeName: 'admin-dashboard',
    icon: markRaw(LayoutDashboard),
  },
  {
    label: 'Quản lý người dùng',
    routeName: 'admin-users',
    icon: markRaw(UserIcon),
  },
]