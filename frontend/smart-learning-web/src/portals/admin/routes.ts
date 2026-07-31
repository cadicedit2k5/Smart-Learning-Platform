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
]