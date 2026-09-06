import type { DocumentProcessingStatus,} from './types'

export const documentStatusLabel: Record<DocumentProcessingStatus,string> = {
  UPLOADED: 'Đã tải lên',
  QUEUED: 'Đang chờ xử lý',
  INDEXED: 'Sẵn sàng cho AI',
  FAILED: 'Xử lý thất bại',
}

export const documentStatusTone = {
  UPLOADED: 'warning',
  QUEUED: 'warning',
  INDEXED: 'success',
  FAILED: 'danger',
} as const satisfies Record<DocumentProcessingStatus, 'warning' | 'success' | 'danger'>

export const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) {
    return `${bytes} B`
  }

  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`
  }

  return `${(bytes /(1024 * 1024)).toFixed(1)} MB`
}