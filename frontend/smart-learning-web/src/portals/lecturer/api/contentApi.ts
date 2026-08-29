import { httpClient, type ApiResponse } from '@/shared/api'
import type { CourseChapter, CourseTopic, TopicContent } from '@/shared/course-content'

export interface ChapterInput {
  title: string
  description?: string
  learningObjectives?: string
  orderIndex?: number
}

export interface TopicInput {
  title: string
  description?: string
  orderIndex?: number
  estimatedMinutes?: number
  content: TopicContent
}

const chaptersPath = (courseId: string) => `/courses/${courseId}/chapters`

export const createChapter = async (
  courseId: string,
  input: ChapterInput,
): Promise<CourseChapter> => {
  const response = await httpClient.post<ApiResponse<CourseChapter>>(chaptersPath(courseId), input)
  return response.data.data
}

export const updateChapter = async (
  courseId: string,
  chapterId: string,
  input: Partial<ChapterInput>,
): Promise<CourseChapter> => {
  const response = await httpClient.patch<ApiResponse<CourseChapter>>(
    `${chaptersPath(courseId)}/${chapterId}`,
    input,
  )
  return response.data.data
}

export const deleteChapter = async (courseId: string, chapterId: string): Promise<void> => {
  await httpClient.delete(`${chaptersPath(courseId)}/${chapterId}`)
}

const topicsPath = (courseId: string, chapterId: string) =>
  `${chaptersPath(courseId)}/${chapterId}/topics`

export const createTopic = async (
  courseId: string,
  chapterId: string,
  input: TopicInput,
): Promise<CourseTopic> => {
  const response = await httpClient.post<ApiResponse<CourseTopic>>(
    topicsPath(courseId, chapterId),
    input,
  )
  return response.data.data
}

export const updateTopic = async (
  courseId: string,
  chapterId: string,
  topicId: string,
  input: Partial<TopicInput>,
): Promise<CourseTopic> => {
  const response = await httpClient.patch<ApiResponse<CourseTopic>>(
    `${topicsPath(courseId, chapterId)}/${topicId}`,
    input,
  )
  return response.data.data
}

export const deleteTopic = async (
  courseId: string,
  chapterId: string,
  topicId: string,
): Promise<void> => {
  await httpClient.delete(`${topicsPath(courseId, chapterId)}/${topicId}`)
}
