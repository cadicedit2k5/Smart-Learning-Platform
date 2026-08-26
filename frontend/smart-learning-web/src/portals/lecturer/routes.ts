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
  {
    path: 'courses',
    name: 'lecturer-courses',
    component: () => import('./pages/LecturerCoursesPage.vue'),
    meta: {
      title: 'Khóa học của tôi',
    },
  },
  {
    path: 'courses/:courseId',
    name: 'lecturer-course-detail',
    component: () => import('./pages/LecturerCourseDetailPage.vue'),
    meta: {
      title: 'Chi tiết khóa học',
    },
  },
]
