export type CourseVisibility = 'PUBLIC'| 'INVITE_ONLY'
export type CourseStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
export type CourseMemberRole = 'OWNER' | 'STUDENT'

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
  currentUserRole: CourseMemberRole | null
}