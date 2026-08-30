import { httpClient, type ApiResponse, type PaginatedData } from '@/shared/api'
import type { Course, CourseVisibility } from '@/shared/course'

export {
  getDocument,
  getDocuments,
  uploadDocument,
  updateDocument,
  downloadDocument,
  deleteDocument,
} from './documentApi'
export type {
  CourseDocument,
  CourseDocumentVersion,
  DocumentFilters,
  DocumentProcessingStatus,
  DocumentUpdateInput,
  DocumentUploadInput,
} from './documentApi'
export {
  addStudent,
  getCourseMembers,
  getUserLookup,
  removeCourseMember,
  searchStudents,
  getJoinRequests,
  approveJoinRequest,
  rejectJoinRequest
} from './memberApi'
export type { CourseMember, UserLookup } from './memberApi'



export interface CourseInput {
  title: string
  description: string
  level: string
  visibility: CourseVisibility
}

export interface AiCitation {
  label: string
  chunkId: string
  documentId: string
  documentVersionId: string
  locator: Record<string, unknown>
}

export interface AiMessage {
  id: string
  role: 'USER' | 'ASSISTANT'
  accessScope: 'PREVIEW' | 'FULL'
  content: string
  citations: AiCitation[]
  createdAt: string
}

export interface AiConversation {
  id: string
  title: string
  lastMessageAt: string
  createdAt: string
}

export interface AiChatTurn {
  conversationId: string
  userMessage: AiMessage
  assistantMessage: AiMessage
}

const coursesPath = '/courses'

export const createCourse = async (input: CourseInput): Promise<Course> => {
  const response = await httpClient.post<ApiResponse<Course>>(coursesPath, input)

  return response.data.data
}

export const updateCourse = async (courseId: string, input: CourseInput): Promise<Course> => {
  const response = await httpClient.patch<ApiResponse<Course>>(`${coursesPath}/${courseId}`, {
    ...input,
    // Backend currently validates title as required on every PATCH.
    title: input.title,
  })

  return response.data.data
}

export const publishCourse = async (courseId: string): Promise<Course> => {
  const response = await httpClient.post<ApiResponse<Course>>(`${coursesPath}/${courseId}/publish`)

  return response.data.data
}

export const deleteCourse = async (courseId: string): Promise<void> => {
  await httpClient.delete(`${coursesPath}/${courseId}`)
}

export const getConversations = async (
  courseId: string,
  page = 1,
): Promise<PaginatedData<AiConversation>> => {
  const response = await httpClient.get<ApiResponse<PaginatedData<AiConversation>>>(
    `${coursesPath}/${courseId}/ai/conversations`,
    {
      params: { page },
    },
  )

  return response.data.data
}

export const getConversationMessages = async (
  courseId: string,
  conversationId: string,
  page = 1,
): Promise<PaginatedData<AiMessage>> => {
  const response = await httpClient.get<ApiResponse<PaginatedData<AiMessage>>>(
    `${coursesPath}/${courseId}/ai/conversations/${conversationId}/messages`,
    { params: { page } },
  )

  return response.data.data
}

export const createConversation = async (
  courseId: string,
  content: string,
): Promise<AiChatTurn> => {
  const response = await httpClient.post<ApiResponse<AiChatTurn>>(
    `${coursesPath}/${courseId}/ai/conversations`,
    { content },
  )

  return response.data.data
}

export const sendConversationMessage = async (
  courseId: string,
  conversationId: string,
  content: string,
): Promise<AiChatTurn> => {
  const response = await httpClient.post<ApiResponse<AiChatTurn>>(
    `${coursesPath}/${courseId}/ai/conversations/${conversationId}/messages`,
    { content },
  )

  return response.data.data
}
