import { httpClient, type ApiResponse, type PaginatedData } from '@/shared/api'
import type { CourseDocument } from '@/shared/document/types'

export interface DocumentFilters {
  keyword?: string
  chapterId?: string
  topicId?: string
  page?: number
}

export interface DocumentUploadInput {
  title: string
  description?: string
  file: File
  chapterId?: string
  topicId?: string
}

export interface DocumentUpdateInput {
  title: string
  description?: string
  chapterId?: string
  topicId?: string
}

const documentsPath = (courseId: string) => `/courses/${courseId}/documents`

export const getDocuments = async (
  courseId: string,
  filters: DocumentFilters | number = {},
): Promise<PaginatedData<CourseDocument>> => {
  const normalizedFilters = typeof filters === 'number' ? { page: filters } : filters
  const response = await httpClient.get<ApiResponse<PaginatedData<CourseDocument>>>(
    documentsPath(courseId),
    {
      params: {
        keyword: normalizedFilters.keyword || undefined,
        chapterId: normalizedFilters.chapterId || undefined,
        topicId: normalizedFilters.topicId || undefined,
        page: normalizedFilters.page ?? 1,
      },
    },
  )

  return response.data.data
}

export const getDocument = async (
  courseId: string,
  documentId: string,
): Promise<CourseDocument> => {
  const response = await httpClient.get<ApiResponse<CourseDocument>>(
    `${documentsPath(courseId)}/${documentId}`,
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
  if (input.chapterId) formData.append('chapterId', input.chapterId)
  if (input.topicId) formData.append('topicId', input.topicId)

  const response = await httpClient.post<ApiResponse<CourseDocument>>(
    documentsPath(courseId),
    formData,
  )
  return response.data.data
}

export const updateDocument = async (
  courseId: string,
  documentId: string,
  input: DocumentUpdateInput,
): Promise<CourseDocument> => {
  const response = await httpClient.patch<ApiResponse<CourseDocument>>(
    `${documentsPath(courseId)}/${documentId}`,
    input,
  )
  return response.data.data
}

export const downloadDocument = async (courseId: string, documentId: string): Promise<Blob> => {
  const response = await httpClient.get(`${documentsPath(courseId)}/${documentId}/download`, {
    responseType: 'blob',
  })
  return response.data as Blob
}

export const deleteDocument = async (courseId: string, documentId: string): Promise<void> => {
  await httpClient.delete(`${documentsPath(courseId)}/${documentId}`)
}
