export type CourseVisibility = 'PUBLIC' | 'INVITE_ONLY'
export type CourseStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
export type CourseMemberRole = 'OWNER' | 'STUDENT'

export interface CourseFeatureConfig {
  announcements: boolean
  content: boolean
  assignments: boolean
  documents: boolean
  discussion: boolean
  aiTutor: boolean
}

export interface Course {
  id: string
  title: string
  description: string | null
  imageUrl: string | null
  level: string | null
  visibility: CourseVisibility
  status: CourseStatus
  createdBy: string
  publishedAt: string | null
  createdAt: string
  updatedAt: string
  currentUserRole: CourseMemberRole | null
  featureConfig: CourseFeatureConfig
}