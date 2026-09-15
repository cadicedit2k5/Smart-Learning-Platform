import { httpClient, type ApiResponse, type PaginatedData } from '@/shared/api'
import type { CourseMemberRole } from '@/shared/course'

export type CourseMemberStatus = 'PENDING' | 'ACTIVE' | 'REJECTED' | 'REMOVED'

export interface CourseMembership {
  id: string
  courseId: string
  userId: string
  role: CourseMemberRole
  status: CourseMemberStatus
  joinedAt: string | null
  invitedBy: string | null
  removedAt: string | null
  createdAt: string
}

export interface PublicCourse {
  id: string
  title: string
  description: string | null
  imageUrl: string | null
  level: string | null
  publishedAt: string | null
  currentUserMembershipStatus: CourseMemberStatus | null
}

export interface PublicCourseTopicOutline {
  id: string
  title: string
  description: string | null
  orderIndex: number
  estimatedMinutes: number | null
}

export interface PublicCourseChapterOutline {
  id: string
  title: string
  description: string | null
  learningObjectives: string | null
  orderIndex: number
  topics: PublicCourseTopicOutline[]
}

export interface PublicCourseDetail extends PublicCourse {
  chapters: PublicCourseChapterOutline[]
}

export interface PublicCourseParams {
  page?: number
  keyword?: string
}

export const getCurrentMembership = async (courseId: string): Promise<CourseMembership> => {
  const response = await httpClient.get<ApiResponse<CourseMembership>>(`/courses/${courseId}/members/me`)
  return response.data.data
}

export const getPublicCourses = async (
  params: PublicCourseParams = {},
): Promise<PaginatedData<PublicCourse>> => {
  const response = await httpClient.get<ApiResponse<PaginatedData<PublicCourse>>>('/courses/public', { params })
  return response.data.data
}

export const getPublicCourseDetail = async (courseId: string): Promise<PublicCourseDetail> => {
  const response = await httpClient.get<ApiResponse<PublicCourseDetail>>(`/courses/public/${courseId}`)
  return response.data.data
}

export const requestToJoinCourse = async (courseId: string): Promise<CourseMembership> => {
  const response = await httpClient.post<ApiResponse<CourseMembership>>(`/courses/${courseId}/join-requests`)
  return response.data.data
}