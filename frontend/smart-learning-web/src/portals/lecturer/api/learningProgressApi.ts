import { httpClient, type ApiResponse, type PaginatedData } from '@/shared/api'

export type LecturerTopicProgressStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED'

export interface StudentProgressSummary {
  studentId: string
  fullName: string
  email: string | null
  totalTopics: number
  completedTopics: number
  inProgressTopics: number
  progressPercentage: number
}

export interface LecturerCourseProgress {
  courseId: string
  totalStudents: number
  totalTopics: number
  averageProgressPercentage: number
  completedStudents: number
  students: PaginatedData<StudentProgressSummary>
}

export interface StudentTopicProgress {
  topicId: string
  title: string
  orderIndex: number
  status: LecturerTopicProgressStatus
  startedAt: string | null
  completedAt: string | null
  lastAccessedAt: string | null
}

export interface StudentChapterProgress {
  chapterId: string
  title: string
  orderIndex: number
  totalTopics: number
  completedTopics: number
  progressPercentage: number
  topics: StudentTopicProgress[]
}

export interface StudentProgressDetail {
  studentId: string
  fullName: string
  email: string | null
  totalTopics: number
  completedTopics: number
  inProgressTopics: number
  progressPercentage: number
  chapters: StudentChapterProgress[]
}

const learningPath = (courseId: string) => `/courses/${courseId}/learning`

export const getCourseStudentProgress = async (
  courseId: string,
  page = 1,
): Promise<LecturerCourseProgress> => {
  const response = await httpClient.get<ApiResponse<LecturerCourseProgress>>(
    `${learningPath(courseId)}/progress/students`,
    {
      params: {
        page,
        'orders[createdAt]': 'ASC',
      },
    },
  )

  return response.data.data
}

export const getStudentProgress = async (
  courseId: string,
  studentId: string,
): Promise<StudentProgressDetail> => {
  const response = await httpClient.get<ApiResponse<StudentProgressDetail>>(
    `${learningPath(courseId)}/progress/students/${studentId}`,
  )

  return response.data.data
}