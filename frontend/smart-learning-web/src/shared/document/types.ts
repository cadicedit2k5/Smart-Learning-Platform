export type DocumentProcessingStatus = 'UPLOADED' | 'QUEUED' | 'INDEXED' | 'FAILED'

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
  courseTitle: string
  title: string
  description: string | null
  uploadedBy: string
  version: CourseDocumentVersion
  createdAt: string
  updatedAt: string
}