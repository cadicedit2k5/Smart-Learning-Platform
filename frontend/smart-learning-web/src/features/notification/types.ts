export type NotificationType =
  | 'ANNOUNCEMENT_CREATED'
  | 'ASSIGNMENT_CREATED'
  | 'ASSIGNMENT_GRADED'
  | 'COURSE_JOIN_APPROVED'

export interface Notification {
  id: string
  type: NotificationType
  title: string
  message: string
  courseId: string
  read: boolean
  createdAt: string
}

export interface UnreadCount {
  count: number
}