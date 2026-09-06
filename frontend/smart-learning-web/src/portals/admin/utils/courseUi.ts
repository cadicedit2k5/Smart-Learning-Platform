import type {
  CourseStatus,
  CourseVisibility,
} from '@/shared/course'

export const courseStatusLabel = (status: CourseStatus): string => {
  switch (status) {
    case 'DRAFT':
      return 'Bản nháp'

    case 'PUBLISHED':
      return 'Đã xuất bản'

    case 'ARCHIVED':
      return 'Đã lưu trữ'
  }
}

export const courseStatusTone = (status: CourseStatus,) => {
  switch (status) {
    case 'PUBLISHED':
      return 'success' as const

    case 'DRAFT':
      return 'warning' as const

    case 'ARCHIVED':
      return 'neutral' as const
  }
}

export const visibilityLabel = (visibility: CourseVisibility): string => {
  switch (visibility) {
    case 'PUBLIC':
      return 'Công khai'

    case 'PRIVATE':
      return 'Riêng tư'

    case 'INVITE_ONLY':
      return 'Chỉ lời mời'
  }
}