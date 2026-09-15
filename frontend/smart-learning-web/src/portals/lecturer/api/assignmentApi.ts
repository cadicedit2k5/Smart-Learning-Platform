import {
  httpClient,
  type ApiResponse,
} from '@/shared/api'

import type {
  Assignment,
  AssignmentSubmission,
} from '@/shared/assignment/types'

export interface AssignmentInput {
  title: string
  description?: string
  dueAt: string
  maxScore: number
}

export interface GradeInput {
  score: number
  feedback?: string
}

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

export const createAssignment = async (
  courseId: string,
  input: AssignmentInput,
): Promise<Assignment> => {
  const response =
    await httpClient.post<
      ApiResponse<Assignment>
    >(path(courseId), input)

  return response.data.data
}

export const updateAssignment = async (
  courseId: string,
  assignmentId: string,
  input: Partial<AssignmentInput>,
): Promise<Assignment> => {
  const response =
    await httpClient.patch<
      ApiResponse<Assignment>
    >(
      `${path(courseId)}/${assignmentId}`,
      input,
    )

  return response.data.data
}

export const deleteAssignment = async (
  courseId: string,
  assignmentId: string,
) => {
  await httpClient.delete(
    `${path(courseId)}/${assignmentId}`,
  )
}

export const getSubmissions = async (
  courseId: string,
  assignmentId: string,
): Promise<AssignmentSubmission[]> => {
  const response =
    await httpClient.get<
      ApiResponse<AssignmentSubmission[]>
    >(
      `${path(courseId)}/${assignmentId}/submissions`,
    )

  return response.data.data
}

export const gradeSubmission = async (
  courseId: string,
  assignmentId: string,
  submissionId: string,
  input: GradeInput,
): Promise<AssignmentSubmission> => {
  const response =
    await httpClient.patch<
      ApiResponse<AssignmentSubmission>
    >(
      `${path(courseId)}/${assignmentId}/submissions/${submissionId}/grade`,
      input,
    )

  return response.data.data
}