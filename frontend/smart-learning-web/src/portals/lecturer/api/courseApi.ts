import { httpClient, type ApiResponse, type PaginatedData } from '@/shared/api'
import type { Course, CourseVisibility } from '@/shared/course'

export {
  getDocument,
  getDocuments,
  uploadDocument,
  updateDocument,
  downloadDocument,
  deleteDocument,
} from './documentApi'
export type {
  DocumentFilters,
  DocumentUpdateInput,
  DocumentUploadInput,
} from './documentApi'
export {
  addStudent,
  getCourseMembers,
  getUserLookup,
  removeCourseMember,
  getStudents,
  getJoinRequests,
  approveJoinRequest,
  rejectJoinRequest
} from './memberApi'
export type { CourseMember, UserLookup } from './memberApi'



export interface CourseInput {
  title: string
  description: string
  level: string
  visibility: CourseVisibility
}



const coursesPath = '/courses'

export const createCourse = async (input: CourseInput): Promise<Course> => {
  const response = await httpClient.post<ApiResponse<Course>>(coursesPath, input)

  return response.data.data
}

export const updateCourse = async (courseId: string, input: CourseInput): Promise<Course> => {
  const response = await httpClient.patch<ApiResponse<Course>>(`${coursesPath}/${courseId}`, {
    ...input,
    // Backend currently validates title as required on every PATCH.
    title: input.title,
  })

  return response.data.data
}

export const publishCourse = async (courseId: string): Promise<Course> => {
  const response = await httpClient.post<ApiResponse<Course>>(`${coursesPath}/${courseId}/publish`)

  return response.data.data
}

export const deleteCourse = async (courseId: string): Promise<void> => {
  await httpClient.delete(`${coursesPath}/${courseId}`)
}