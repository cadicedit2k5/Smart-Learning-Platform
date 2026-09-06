import { httpClient, type ApiResponse, type PaginatedData } from '@/shared/api'

export type DocumentLifecycleStatus = 'ACTIVE' | 'ARCHIVED'
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
  lifecycleStatus: DocumentLifecycleStatus
  uploadedBy: string
  version: CourseDocumentVersion
  createdAt: string
  updatedAt: string
}

export interface DocumentFilters {
  keyword?: string
  page?: number
}

const documentsPath = (courseId: string) => `/courses/${courseId}/documents`

export const getDocuments = async (
  courseId: string,
  filters: DocumentFilters = {},
): Promise<PaginatedData<CourseDocument>> => {
  const response = await httpClient.get<ApiResponse<PaginatedData<CourseDocument>>>(
    documentsPath(courseId),
    {
      params: {
        keyword: filters.keyword || undefined,
        page: filters.page ?? 1,
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

export const downloadDocument = async (courseId: string, documentId: string): Promise<Blob> => {
  const response = await httpClient.get(`${documentsPath(courseId)}/${documentId}/download`, {
    responseType: 'blob',
  })
  return response.data as Blob
}
