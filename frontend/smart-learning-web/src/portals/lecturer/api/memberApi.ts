import { httpClient, type ApiResponse, type PaginatedData } from '@/shared/api'

export interface CourseMember {
  id: string
  courseId: string
  userId: string
  role: 'OWNER' | 'STUDENT'
  status: 'PENDING' | 'ACTIVE' | 'REJECTED' | 'REMOVED'
  joinedAt: string | null
  invitedBy: string | null
  removedAt: string | null
  createdAt: string
}

export interface UserLookup {
  id: string
  email: string
  fullName: string
  avatar: string | null
  role: {
    id: number
    code: string
    name: string
  } | null
}

export const getCourseMembers = async (courseId: string): Promise<CourseMember[]> => {
  const response = await httpClient.get<ApiResponse<CourseMember[]>>(`/courses/${courseId}/members`)

  return response.data.data
}

export const searchStudents = async (
  keyword: string,
  page = 1,
): Promise<PaginatedData<UserLookup>> => {
  const response = await httpClient.get<ApiResponse<PaginatedData<UserLookup>>>('/users', {
    params: {
      keyword,
      roleCode: 'STUDENT',
      page,
    },
  })

  return response.data.data
}

export const getUserLookup = async (userId: string): Promise<UserLookup> => {
  const response = await httpClient.get<ApiResponse<UserLookup>>(`/users/${userId}`)

  return response.data.data
}

export const addStudent = async (courseId: string, userId: string): Promise<CourseMember> => {
  const response = await httpClient.post<ApiResponse<CourseMember>>(
    `/courses/${courseId}/members`,
    { userId },
  )

  return response.data.data
}

export const removeCourseMember = async (courseId: string, memberId: string): Promise<void> => {
  await httpClient.delete(`/courses/${courseId}/members/${memberId}`)
}
