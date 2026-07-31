import type { RouteRecordRaw } from 'vue-router'

export const studentRoutes: RouteRecordRaw[] = [
  {
    path: 'dashboard',
    name: 'student-dashboard',
    component: () => import("@/App.vue"),
    meta: {
      title: 'Dashboard',
    },
  },
]