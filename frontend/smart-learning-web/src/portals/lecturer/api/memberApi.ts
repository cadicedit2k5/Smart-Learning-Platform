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

export interface CourseMemberUser {
  id: string
  email: string
  fullName: string
  avatar: string | null
}

export interface CourseMemberDetail extends CourseMember {
  user: CourseMemberUser | null
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

export interface StudentListParams {
  page?: number
  keyword?: string
}

export const getCourseMembers = async (courseId: string, page = 1): Promise<PaginatedData<CourseMemberDetail>> => {
  const response = await httpClient.get<ApiResponse<PaginatedData<CourseMemberDetail>>>(`/courses/${courseId}/members`, {
    params: {
      page,
      'orders[createdAt]': 'ASC',
    },
  })

  return response.data.data;
}

export const getStudents = async ({
  page = 1,
  keyword,
}: StudentListParams = {}): Promise<PaginatedData<UserLookup>> => {
  const response = await httpClient.get<ApiResponse<PaginatedData<UserLookup>>>('/users', {
    params: {
      page,
      roleCode: 'STUDENT',
      keyword: keyword?.trim() || undefined,
    },
  })

  return response.data.data
}

export const getUserLookup = async (userId: string): Promise<UserLookup> => {
  const response = await httpClient.get<ApiResponse<UserLookup>>(`/users/${userId}`)

  return response.data.data;
}

export const addStudent = async (courseId: string, userId: string): Promise<CourseMember> => {
  const response = await httpClient.post<ApiResponse<CourseMember>>(
    `/courses/${courseId}/members`,
    { userId },
  )

  return response.data.data;
}

export const removeCourseMember = async (courseId: string, memberId: string): Promise<void> => {
  await httpClient.delete(`/courses/${courseId}/members/${memberId}`)
}


export const getJoinRequests = async (courseId: string, page = 1): Promise<PaginatedData<CourseMemberDetail>> => {
  const response = await httpClient.get<ApiResponse<PaginatedData<CourseMemberDetail>>>(
    `/courses/${courseId}/join-requests`,
    {
      params: {
        page,
        'orders[createdAt]': 'ASC'
      },
    }
  )

  return response.data.data;
}

export const approveJoinRequest = async (courseId: string, memberId: string): Promise<CourseMember> => {
  const response = await httpClient.post<ApiResponse<CourseMember>>(
    `/courses/${courseId}/join-requests/${memberId}/approve`,
  )

  return response.data.data;
}

export const rejectJoinRequest = async (courseId: string, memberId: string): Promise<CourseMember> => {
  const response = await httpClient.post<ApiResponse<CourseMember>>(
    `/courses/${courseId}/join-requests/${memberId}/reject`,
  )

  return response.data.data;
}