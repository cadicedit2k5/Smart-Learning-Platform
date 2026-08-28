import { httpClient, type ApiResponse , type PaginatedData} from '@/shared/api'

export type CourseVisibility = 'PUBLIC' | 'PRIVATE' | 'INVITE_ONLY'
export type CourseStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
export type CourseMemberRole = 'OWNER' | 'STUDENT'
export type CourseMemberStatus = 'PENDING' | 'ACTIVE' | 'REJECTED' | 'REMOVED'

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
  level: string | null
  publishedAt: string | null
  currentUserMembershipStatus: CourseMemberStatus | null
}

export const getMyCourses = async (): Promise<Course[]> => {
  const response = await httpClient.get<ApiResponse<Course[]>>('/courses/me')
  return response.data.data;
}

export const getCourse = async (courseId: string): Promise<Course> => {
  const response = await httpClient.get<ApiResponse<Course>>(`/courses/${courseId}`)
  return response.data.data;
}

export const getCurrentMembership = async (courseId: string): Promise<CourseMembership> => {
  const response = await httpClient.get<ApiResponse<CourseMembership>>(
    `/courses/${courseId}/members/me`,
  )
  return response.data.data;
}

export const getPublicCourses = async (page = 1): Promise<PaginatedData<PublicCourse>> => {
  const response = await httpClient.get<ApiResponse<PaginatedData<PublicCourse>>>('/courses/public', {
    params: { page },
  });

  return response.data.data;
}

export const requestToJoinCourse = async (courseId: string): Promise<CourseMembership> => {
  const response = await httpClient.post<ApiResponse<CourseMembership>>(`/courses/${courseId}/join-requests`)

  return response.data.data;
}
