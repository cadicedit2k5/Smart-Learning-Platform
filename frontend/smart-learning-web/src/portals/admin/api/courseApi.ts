import { httpClient, type ApiResponse, type PaginatedData } from '@/shared/api'

import type { Course, CourseStatus, CourseVisibility } from '@/shared/course'

const ADMIN_COURSES_PATH = '/admin/courses'

export interface CoursesPage {
    content: Course[]
    pageable: {
        page: number
        size: number
        totalElements: number
        totalPages: number
    }
}

export interface CourseListParams {
  page: number
  keyword?: string
  status?: CourseStatus
  visibility?: CourseVisibility
  updatedAtOrder?: 'ASC' | 'DESC'
}

export interface AdminUpdateCourseRequest {
  title?: string
  description?: string
  level?: string
  visibility?: CourseVisibility
}

export const getCourses = async (params: CourseListParams): Promise<CoursesPage> => {
  const response = await httpClient.get<ApiResponse<CoursesPage>>(
      ADMIN_COURSES_PATH,
      {
        params: {
          page: params.page || 1,
          keyword: params.keyword || undefined,
          status: params.status || undefined,
          visibility: params.visibility || undefined,
          'orders[updatedAt]': params.updatedAtOrder || undefined,
        },
      },
    )

  return response.data.data
}

export const getCourse = async (courseId: string): Promise<Course> => {
  const response = await httpClient.get<ApiResponse<Course>>(
      `${ADMIN_COURSES_PATH}/${courseId}`)

  return response.data.data;
}

export const updateCourse = async (
  courseId: string,
  request: AdminUpdateCourseRequest,
): Promise<Course> => {
  const response = await httpClient.patch<ApiResponse<Course>>(
      `${ADMIN_COURSES_PATH}/${courseId}`,
      request)

  return response.data.data;
}

export const deleteCourse = async (courseId: string): Promise<void> => {
  await httpClient.delete(`${ADMIN_COURSES_PATH}/${courseId}`,)
}