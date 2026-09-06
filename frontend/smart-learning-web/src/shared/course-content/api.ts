import { httpClient, type ApiResponse } from "../api"
import type { CourseChapter, CourseTopic } from "./types"

const chaptersPath = (courseId: string) => `/courses/${courseId}/chapters`

const topicsPath = (courseId: string, chapterId: string) =>
  `${chaptersPath(courseId)}/${chapterId}/topics`

export const getChapters = async (courseId: string): Promise<CourseChapter[]> => {
  const response = await httpClient.get<ApiResponse<CourseChapter[]>>(chaptersPath(courseId))
  return response.data.data
}


export const getTopics = async (courseId: string, chapterId: string): Promise<CourseTopic[]> => {
  const response = await httpClient.get<ApiResponse<CourseTopic[]>>(topicsPath(courseId, chapterId))
  return response.data.data
}