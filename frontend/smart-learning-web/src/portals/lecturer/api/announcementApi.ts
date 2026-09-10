import { httpClient, type ApiResponse } from '@/shared/api'
import type { CourseAnnouncement } from '@/shared/announcement/types'

export { getAnnouncements } from '@/shared/announcement/api'

export interface AnnouncementInput {
  title: string
  content: string
}

const path = (courseId: string) => `/courses/${courseId}/announcements`

export const createAnnouncement = async (
  courseId: string,
  input: AnnouncementInput,
): Promise<CourseAnnouncement> => {
  const response = await httpClient.post<ApiResponse<CourseAnnouncement>>(
    path(courseId),
    input,
  )

  return response.data.data
}

export const updateAnnouncement = async (
  courseId: string,
  announcementId: string,
  input: Partial<AnnouncementInput>,
): Promise<CourseAnnouncement> => {
  const response = await httpClient.patch<ApiResponse<CourseAnnouncement>>(
    `${path(courseId)}/${announcementId}`,
    input,
  )

  return response.data.data
}

export const deleteAnnouncement = async (
  courseId: string,
  announcementId: string,
): Promise<void> => {
  await httpClient.delete(`${path(courseId)}/${announcementId}`)
}