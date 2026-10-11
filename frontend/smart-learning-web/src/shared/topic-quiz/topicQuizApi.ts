
import { httpClient, type ApiResponse } from '@/shared/api'

export type QuizQuestionType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE'
export type QuizStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

export interface QuizQuestionInput {
  type: QuizQuestionType
  content: string
  explanation: string | null
  options: Array<{ content: string; correct: boolean }>
}

export interface EditorQuiz {
  id: string
  topicId: string
  title: string
  description: string | null
  version: number
  status: QuizStatus
  hasAttempts: boolean
  publishedAt: string | null
  questions: Array<{
    id: string
    type: QuizQuestionType
    content: string
    explanation: string | null
    options: Array<{ id: string; content: string; correct: boolean }>
  }>
}

export interface StudentQuiz {
  id: string
  topicId: string
  title: string
  description: string | null
  version: number
  questions: Array<{
    id: string
    type: QuizQuestionType
    content: string
    options: Array<{ id: string; content: string }>
  }>
}

export interface QuizAttemptSummary {
  id: string
  assessmentId: string
  version: number
  score: number
  maxScore: number
  submittedAt: string
}

export interface QuizAttemptResult extends QuizAttemptSummary {
  answers: Array<{
    questionId: string
    type: QuizQuestionType
    content: string
    options: Array<{ id: string; content: string }>
    selectedOptionIds: string[]
    correctOptionIds: string[]
    awardedScore: number
    explanation: string | null
  }>
}

const path = (courseId: string, topicId: string) =>
  `/courses/${courseId}/topics/${topicId}/quizzes`

export const getQuizVersions = async (courseId: string, topicId: string) => {
  const response = await httpClient.get<ApiResponse<EditorQuiz[]>>(`${path(courseId, topicId)}/manage`)
  return response.data.data
}

export const createQuizDraft = async (
  courseId: string, topicId: string,
  data: { title: string; description: string | null; questions: QuizQuestionInput[] },
) => {
  const response = await httpClient.post<ApiResponse<EditorQuiz>>(`${path(courseId, topicId)}/draft`, data)
  return response.data.data
}

export const updateQuizQuestions = async (
  courseId: string, topicId: string, quizId: string, questions: QuizQuestionInput[],
) => {
  const response = await httpClient.put<ApiResponse<EditorQuiz>>(
    `${path(courseId, topicId)}/${quizId}/questions`, { questions },
  )
  return response.data.data
}

export const updateQuizDetails = async (
  courseId: string, topicId: string, quizId: string,
  data: {
    title?: string
    description?: string
    explanations?: Array<{ questionId: string; explanation: string | null }>
  },
) => {
  const response = await httpClient.patch<ApiResponse<EditorQuiz>>(
    `${path(courseId, topicId)}/${quizId}/details`, data,
  )
  return response.data.data
}

export const createQuizVersion = async (courseId: string, topicId: string, quizId: string) => {
  const response = await httpClient.post<ApiResponse<EditorQuiz>>(
    `${path(courseId, topicId)}/${quizId}/next-version`,
  )
  return response.data.data
}

export const publishQuiz = async (courseId: string, topicId: string, quizId: string) => {
  const response = await httpClient.post<ApiResponse<EditorQuiz>>(
    `${path(courseId, topicId)}/${quizId}/publish`,
  )
  return response.data.data
}

export const getCurrentQuiz = async (courseId: string, topicId: string) => {
  const response = await httpClient.get<ApiResponse<StudentQuiz | null>>(
    `${path(courseId, topicId)}/current`,
  )
  return response.data.data
}

export const submitQuiz = async (
  courseId: string, topicId: string, quizId: string,
  answers: Array<{ questionId: string; selectedOptionIds: string[] }>,
) => {
  const response = await httpClient.post<ApiResponse<QuizAttemptResult>>(
    `${path(courseId, topicId)}/${quizId}/attempts`, { answers },
  )
  return response.data.data
}

export const getQuizHistory = async (courseId: string, topicId: string) => {
  const response = await httpClient.get<ApiResponse<QuizAttemptSummary[]>>(
    `${path(courseId, topicId)}/attempts`,
  )
  return response.data.data
}

export const getQuizAttempt = async (courseId: string, topicId: string, attemptId: string) => {
  const response = await httpClient.get<ApiResponse<QuizAttemptResult>>(
    `${path(courseId, topicId)}/attempts/${attemptId}`,
  )
  return response.data.data
}
