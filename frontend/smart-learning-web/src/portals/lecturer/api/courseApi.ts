import { httpClient, type ApiResponse, type PaginatedData } from '@/shared/api'

export type CourseVisibility = 'PUBLIC' | 'PRIVATE' | 'INVITE_ONLY'
export type CourseStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

export interface Course {
  id: string
  title: string
  description: string | null
  level: string | null
  visibility: CourseVisibility
  status: CourseStatus
  createdBy: string
  publishedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface CourseInput {
  title: string
  description: string
  level: string
  visibility: CourseVisibility
}

export interface CourseMember {
  id: string
  courseId: string
  userId: string
  role: 'OWNER' | 'LECTURER' | 'STUDENT'
  status: 'PENDING' | 'ACTIVE' | 'REJECTED' | 'REMOVED'
  joinedAt: string | null
  invitedBy: string | null
  removedAt: string | null
  createdAt: string
}

export interface AccessCode {
  id: string
  courseId: string
  code: string
  expiresAt: string | null
  active: boolean
  createdAt: string
}

export type DocumentProcessingStatus = 'UPLOADED' | 'QUEUED' | 'PROCESSING' | 'INDEXED' | 'FAILED'

export interface CourseDocumentVersion {
  id: string
  versionNumber: number
  fileName: string
  fileSize: number
  mimeType: string
  checksumSha256: string | null
  processingStatus: DocumentProcessingStatus
  uploadedBy: string
  createdAt: string
}

export interface CourseDocument {
  id: string
  courseId: string
  chapterId: string | null
  topicId: string | null
  title: string
  description: string | null
  lifecycleStatus: 'ACTIVE' | 'ARCHIVED'
  uploadedBy: string
  version: CourseDocumentVersion
  createdAt: string
  updatedAt: string
}

export interface DocumentUploadInput {
  title: string
  description?: string
  file: File
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

export const getMyCourses = async (): Promise<Course[]> => {
  const response = await httpClient.get<ApiResponse<Course[]>>(`${coursesPath}/me`)

  return response.data.data
}

export const getCourse = async (courseId: string): Promise<Course> => {
  const response = await httpClient.get<ApiResponse<Course>>(`${coursesPath}/${courseId}`)

  return response.data.data
}

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

export const addStudent = async (courseId: string, userId: string): Promise<CourseMember> => {
  const response = await httpClient.post<ApiResponse<CourseMember>>(
    `${coursesPath}/${courseId}/members`,
    {
      userId,
      role: 'STUDENT',
    },
  )

  return response.data.data
}

export const createAccessCode = async (
  courseId: string,
  expiresAt: string | null,
): Promise<AccessCode> => {
  const response = await httpClient.post<ApiResponse<AccessCode>>(
    `${coursesPath}/${courseId}/access-codes`,
    { expiresAt },
  )

  return response.data.data
}

export const revokeAccessCode = async (courseId: string, codeId: string): Promise<void> => {
  await httpClient.delete(`${coursesPath}/${courseId}/access-codes/${codeId}`)
}

export const getDocuments = async (
  courseId: string,
  page = 1,
): Promise<PaginatedData<CourseDocument>> => {
  const response = await httpClient.get<ApiResponse<PaginatedData<CourseDocument>>>(
    `${coursesPath}/${courseId}/documents`,
    {
      params: { page },
    },
  )

  return response.data.data
}

export const uploadDocument = async (
  courseId: string,
  input: DocumentUploadInput,
): Promise<CourseDocument> => {
  const formData = new FormData()

  formData.append('title', input.title)
  formData.append('description', input.description ?? '')
  formData.append('file', input.file)

  // Do not set Content-Type: the browser must add the multipart boundary.
  const response = await httpClient.post<ApiResponse<CourseDocument>>(
    `${coursesPath}/${courseId}/documents`,
    formData,
  )

  return response.data.data
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
