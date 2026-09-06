import type {
  CourseStatus,
  CourseVisibility,
} from './types'

export const courseStatusLabel: Record<CourseStatus, string> = {
    DRAFT: 'Bản nháp',
    PUBLISHED: 'Đã xuất bản',
    ARCHIVED: 'Đã lưu trữ',
  }

export const courseVisibilityLabel:Record<CourseVisibility, string> = {
    PUBLIC: 'Công khai',
    PRIVATE: 'Riêng tư',
    INVITE_ONLY: 'Chỉ người được mời',
  }

export type CourseStatusTone =  | 'warning' | 'success' | 'neutral';

export const courseStatusTone:Record<CourseStatus, CourseStatusTone> = {
    DRAFT: 'warning',
    PUBLISHED: 'success',
    ARCHIVED: 'neutral',
  }