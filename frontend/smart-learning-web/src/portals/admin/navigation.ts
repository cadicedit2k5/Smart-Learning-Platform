import type { NavigationItem } from '@/portals/types'
import { BookOpen, CircleUserRound, LayoutDashboard, ShieldCheck, UserIcon } from 'lucide-vue-next'
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
  {
    label: 'Vai trò & phân quyền',
    routeName: 'admin-roles',
    icon: markRaw(ShieldCheck),
  },
  {
    label: 'Quản lý môn học',
    routeName: 'admin-courses',
    icon: markRaw(BookOpen),
  },
  {
    label: 'Tài khoản',
    routeName: 'admin-profile',
    icon: markRaw(CircleUserRound),
  },
]