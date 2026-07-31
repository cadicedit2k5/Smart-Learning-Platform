import type { RouteRecordRaw } from 'vue-router'

export const studentRoutes: RouteRecordRaw[] = [
 {
    path: '',
    redirect: {
      name: 'student-dashboard',
    },
  },
  {
    path: 'dashboard',
    name: 'student-dashboard',
    component: () => import('./pages/StudentDashboardPage.vue'),
  },
]