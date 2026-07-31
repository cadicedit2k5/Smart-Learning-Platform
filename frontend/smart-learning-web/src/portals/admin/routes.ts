import type { RouteRecordRaw } from 'vue-router'

export const adminRoutes: RouteRecordRaw[] = [
  {
    path: 'dashboard',
    name: 'admin-dashboard',
    component: () => import("@/App.vue"),
    meta: {
      title: 'Dashboard',
    },
  },
]