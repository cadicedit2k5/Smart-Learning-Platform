import { httpClient, type ApiResponse, type PaginatedData } from '@/shared/api'
import type { CourseAnnouncement } from './types'

const path = (courseId: string) => `/courses/${courseId}/announcements`

export const getAnnouncements = async (
  courseId: string,
  page = 1,
): Promise<PaginatedData<CourseAnnouncement>> => {
  const response = await httpClient.get<ApiResponse<PaginatedData<CourseAnnouncement>>>(
    path(courseId),
    {
      params: { page },
    },
  )

  return response.data.data
}