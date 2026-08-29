import { httpClient, type ApiResponse } from '@/shared/api'

export type TopicContent = Record<string, unknown>

export interface CourseChapter {
  id: string
  courseId: string
  title: string
  description: string | null
  learningObjectives: string | null
  orderIndex: number
  createdAt: string
  updatedAt: string
}

export interface CourseTopic {
  id: string
  courseId: string
  chapterId: string
  title: string
  description: string | null
  orderIndex: number
  estimatedMinutes: number | null
  content: TopicContent | null
  createdAt: string
  updatedAt: string
}

const chaptersPath = (courseId: string) =>
  `/courses/${courseId}/chapters`

const topicsPath = (
  courseId: string,
  chapterId: string,
) =>
  `${chaptersPath(courseId)}/${chapterId}/topics`

export const getChapters = async (
  courseId: string,
): Promise<CourseChapter[]> => {
  const response =
    await httpClient.get<ApiResponse<CourseChapter[]>>(
      chaptersPath(courseId),
    )

  return response.data.data
}

export const getTopics = async (
  courseId: string,
  chapterId: string,
): Promise<CourseTopic[]> => {
  const response =
    await httpClient.get<ApiResponse<CourseTopic[]>>(
      topicsPath(courseId, chapterId),
    )

  return response.data.data
}