import { httpClient, type ApiResponse } from '@/shared/api'
import type { Course, CourseFeatureConfig, CourseVisibility } from '@/shared/course'

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
  rejectJoinRequest,
} from './memberApi'

export type { CourseMember, UserLookup } from './memberApi'

export interface CourseInput {
  title: string
  description: string
  level: string
  visibility: CourseVisibility
  image: File | null
}

const coursesPath = '/courses'

const toCourseFormData = (input: CourseInput): FormData => {
  const formData = new FormData()

  const course = {
    title: input.title,
    description: input.description,
    level: input.level,
    visibility: input.visibility,
  }

  formData.append('course', new Blob([JSON.stringify(course)], { type: 'application/json' }))

  if (input.image) {
    formData.append('image', input.image)
  }

  return formData
}

export const createCourse = async (input: CourseInput): Promise<Course> => {
  const response = await httpClient.post<ApiResponse<Course>>(coursesPath, toCourseFormData(input))
  return response.data.data
}

export const updateCourseFeatureConfig = async (
  courseId: string,
  config: CourseFeatureConfig,
): Promise<Course> => {
  const response = await httpClient.patch<ApiResponse<Course>>(
    `${coursesPath}/${courseId}/feature-config`,
    config,
  )

  return response.data.data
}

export const updateCourse = async (courseId: string, input: CourseInput): Promise<Course> => {
  const response = await httpClient.patch<ApiResponse<Course>>(
    `${coursesPath}/${courseId}`,
    toCourseFormData(input),
  )

  return response.data.data
}

export const publishCourse = async (courseId: string): Promise<Course> => {
  const response = await httpClient.post<ApiResponse<Course>>(`${coursesPath}/${courseId}/publish`)
  return response.data.data
}

export const deleteCourse = async (courseId: string): Promise<void> => {
  await httpClient.delete(`${coursesPath}/${courseId}`)
}