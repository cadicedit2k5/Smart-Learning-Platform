import { httpClient, type ApiResponse } from '@/shared/api'
import type { TopicContent } from '@/shared/course-content'
import type { Discussion, DiscussionCreateInput } from './types'

const path = (courseId: string) => `/courses/${courseId}/discussions`

export const getDiscussions = async (
  courseId: string,
): Promise<Discussion[]> => {
  const response = await httpClient.get<ApiResponse<Discussion[]>>(
    path(courseId),
  )

  return response.data.data
}

export const createDiscussion = async (
  courseId: string,
  input: DiscussionCreateInput,
): Promise<Discussion> => {
  const response = await httpClient.post<ApiResponse<Discussion>>(
    path(courseId),
    input,
  )

  return response.data.data
}

export const createDiscussionReply = async (
  courseId: string,
  discussionId: string,
  content: TopicContent,
): Promise<Discussion> => {
  const response = await httpClient.post<ApiResponse<Discussion>>(
    `${path(courseId)}/${discussionId}/replies`,
    { content },
  )

  return response.data.data
}