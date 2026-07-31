import type { RouteRecordRaw } from 'vue-router'

export const lecturerRoutes: RouteRecordRaw[] = [
  {
    path: '',
    redirect: {
      name: 'lecturer-dashboard',
    },
  },
  {
    path: 'dashboard',
    name: 'lecturer-dashboard',
    component: () => import('./pages/LecturerDashboardPage.vue'),
  },
]