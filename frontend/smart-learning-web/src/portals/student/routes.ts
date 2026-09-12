import type { RouteRecordRaw } from 'vue-router'

export const studentRoutes: RouteRecordRaw[] = [
  {
    path: '',
    redirect: { name: 'student-public-courses' },
  },
  {
    path: 'dashboard',
    name: 'student-dashboard',
    component: () => import('./pages/StudentDashboardPage.vue'),
    meta: { title: 'Trang chủ' },
  },
  {
    path: 'discover',
    name: 'student-public-courses',
    component: () => import('./pages/StudentPublicCoursePage.vue'),
    meta: { title: 'Khám phá khóa học' },
  },
  {
    path: 'discover/:courseId',
    name: 'student-public-course-detail',
    component: () => import('./pages/StudentPublicCourseDetailPage.vue'),
    meta: { title: 'Tổng quan khóa học' },
  },
  {
    path: 'courses',
    name: 'student-courses',
    component: () => import('./pages/StudentCoursesPage.vue'),
    meta: { title: 'Khóa học của tôi' },
  },
  {
    path: 'courses/:courseId',
    name: 'student-course-detail',
    component: () => import('./pages/StudentCourseDetailPage.vue'),
    meta: { title: 'Chi tiết khóa học' },
  },
  {
    path: 'profile',
    name: 'student-profile',
    component: () => import('@/features/auth/pages/ProfilePage.vue'),
    meta: { title: 'Tài khoản của tôi' },
  },
]