import type { RouteRecordRaw } from 'vue-router'

export const adminRoutes: RouteRecordRaw[] = [
{
    path: '',
    redirect: {
      name: 'admin-dashboard',
    },
  },
  {
    path: 'dashboard',
    name: 'admin-dashboard',
    component: () => import('./pages/AdminDashboardPage.vue'),
  },
  {
    path: 'users',
    name: 'admin-users',
    component: () =>
      import('./pages/AdminUsersPage.vue'),
    meta: {
      title: 'Quản lý người dùng',
    },
  },
  ]