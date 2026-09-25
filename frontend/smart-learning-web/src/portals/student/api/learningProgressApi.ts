import { httpClient, type ApiResponse } from '@/shared/api'

export type LearningProgressStatus = 'IN_PROGRESS' | 'COMPLETED'

export interface TopicLearningProgress {
  topicId: string
  status: LearningProgressStatus
  activeSeconds: number
  startedAt: string
  completedAt: string | null
  lastAccessedAt: string
}

export interface CourseLearningProgress {
  courseId: string
  totalTopics: number
  completedTopics: number
  progressPercentage: number
  lastTopicId: string | null
  topics: TopicLearningProgress[]
}

const learningPath = (courseId: string) => `/courses/${courseId}/learning`

export const startTopicActivity = async (courseId: string, topicId: string): Promise<TopicLearningProgress> => {
  const response = await httpClient.post<ApiResponse<TopicLearningProgress>>(
    `${learningPath(courseId)}/topics/${topicId}/activity/start`,
  )
  return response.data.data
}

export const recordTopicActivity = async (courseId: string, topicId: string): Promise<TopicLearningProgress> => {
  const response = await httpClient.post<ApiResponse<TopicLearningProgress>>(
    `${learningPath(courseId)}/topics/${topicId}/activity`,
  )
  return response.data.data
}

export const completeTopic = async (courseId: string, topicId: string): Promise<TopicLearningProgress> => {
  const response = await httpClient.post<ApiResponse<TopicLearningProgress>>(
    `${learningPath(courseId)}/topics/${topicId}/complete`,
  )
  return response.data.data
}

export const getCourseProgress = async (courseId: string): Promise<CourseLearningProgress> => {
  const response = await httpClient.get<ApiResponse<CourseLearningProgress>>(
    `${learningPath(courseId)}/progress/me`,
  )
  return response.data.data
}