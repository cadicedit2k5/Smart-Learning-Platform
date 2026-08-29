export type TopicContent = Record<string, unknown>

export interface CourseChapter {
  id: string
  courseId: string
  title: string
  description: string | null
  learningObjectives: string | null
  orderIndex: number
  createdAt: string
  updatedAt: string
}

export interface CourseTopic {
  id: string
  courseId: string
  chapterId: string
  title: string
  description: string | null
  orderIndex: number
  estimatedMinutes: number | null
  content: TopicContent | null
  createdAt: string
  updatedAt: string
}