import {
    httpClient,
    type ApiResponse,
    type PaginatedData,
} from '@/shared/api'
import type { CourseDocument, DocumentProcessingStatus } from '@/shared/document'

const ADMIN_DOCUMENTS_PATH = '/admin/documents'

export interface AdminDocumentListParams {
    page: number
    keyword?: string
    courseKeyword?: string
    courseId?: string
    processingStatus?: DocumentProcessingStatus
    createdAtOrder?: 'ASC' | 'DESC'
}

export const getDocuments = async (params: AdminDocumentListParams): Promise<
    PaginatedData<CourseDocument>> => {
    const response = await httpClient.get<ApiResponse<PaginatedData<CourseDocument>>>(
        ADMIN_DOCUMENTS_PATH,
        {
            params: {
                page: params.page || 1,
                keyword: params.keyword || undefined,
                courseKeyword: params.courseKeyword || undefined,
                courseId: params.courseId || undefined,
                processingStatus: params.processingStatus || undefined,
                'orders[createdAt]': params.createdAtOrder || undefined,
            },
        },
    )

    return response.data.data
}

export const getDocument = async (documentId: string): Promise<CourseDocument> => {
    const response = await httpClient.get<ApiResponse<CourseDocument>>(
        `${ADMIN_DOCUMENTS_PATH}/${documentId}`)

    return response.data.data;
}

export const downloadDocument = async (documentId: string,): Promise<Blob> => {
    const response = await httpClient.get(`${ADMIN_DOCUMENTS_PATH}/${documentId}/download`, {
          responseType: 'blob',
        },
      )

    return response.data as Blob;
  }

export const deleteDocument = async (documentId: string,): Promise<void> => {
    await httpClient.delete(`${ADMIN_DOCUMENTS_PATH}/${documentId}`,)
}