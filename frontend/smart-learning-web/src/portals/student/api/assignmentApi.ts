import {
  httpClient,
  type ApiResponse,
} from '@/shared/api'

import type {
  Assignment,
  AssignmentSubmission,
} from '@/shared/assignment/types'

const path = (courseId: string) =>
  `/courses/${courseId}/assignments`

export const getAssignments = async (
  courseId: string,
): Promise<Assignment[]> => {
  const response =
    await httpClient.get<
      ApiResponse<Assignment[]>
    >(path(courseId))

  return response.data.data
}

export const getMySubmissions = async (
  courseId: string,
): Promise<AssignmentSubmission[]> => {
  const response =
    await httpClient.get<
      ApiResponse<AssignmentSubmission[]>
    >(
      `${path(courseId)}/my-submissions`,
    )

  return response.data.data
}

export const submitAssignment = async (
  courseId: string,
  assignmentId: string,
  content: string,
  file?: File | null,
): Promise<AssignmentSubmission> => {
  const formData = new FormData()

  if (content.trim()) {
    formData.append(
      'content',
      content.trim(),
    )
  }

  if (file) {
    formData.append('file', file)
  }

  const response =
    await httpClient.post<
      ApiResponse<AssignmentSubmission>
    >(
      `${path(courseId)}/${assignmentId}/submission`,
      formData,
    )

  return response.data.data
}