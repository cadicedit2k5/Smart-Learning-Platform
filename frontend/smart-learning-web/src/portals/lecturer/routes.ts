import type { RouteRecordRaw } from 'vue-router'

export const lecturerRoutes: RouteRecordRaw[] = [
  {
    path: 'dashboard',
    name: 'lecturer-dashboard',
    component: () => import("@/App.vue"),
    meta: {
      title: 'Dashboard',
    },
  },
]