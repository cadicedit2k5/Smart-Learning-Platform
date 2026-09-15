export interface Assignment {
  id: string
  courseId: string
  title: string
  description: string | null
  dueAt: string
  maxScore: number
  createdBy: string
  expired: boolean
  createdAt: string
  updatedAt: string
}

export type AssignmentSubmissionStatus =
  | 'SUBMITTED'
  | 'GRADED'

export interface AssignmentSubmission {
  id: string
  assignmentId: string
  studentId: string
  content: string | null

  status: AssignmentSubmissionStatus

  submittedAt: string
  late: boolean

  originalFileName: string | null
  attachmentUrl: string | null

  score: number | null
  feedback: string | null
  gradedAt: string | null
  gradedBy: string | null
}