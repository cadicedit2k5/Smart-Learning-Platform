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
  {
    path: 'roles',
    name: 'admin-roles',
    component: () =>
      import(
        './pages/AdminRolesPage.vue'
      ),
    meta: {
      title: 'Vai trò & phân quyền',
    },
  },
  {
    path: 'courses',
    name: 'admin-courses',
    component: () =>
      import(
        './pages/AdminCoursesPage.vue'
      ),
    meta: {
      title: 'Quản lý môn học',
    },
  },
    {
    path: 'documents',
    name: 'admin-documents',
    component: () =>
      import(
        './pages/AdminDocumentsPage.vue'
      ),
    meta: {
      title: 'Quản lý tài liệu',
    },
  },
  {
    path: 'profile',
    name: 'admin-profile',
    component: () =>
      import('@/features/auth/pages/ProfilePage.vue'),
    meta: {
      title: 'Tài khoản của tôi',
    },
  },
  ]