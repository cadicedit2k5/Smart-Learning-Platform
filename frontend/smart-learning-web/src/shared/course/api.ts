import { httpClient, type ApiResponse, type PaginatedData } from "../api"
import type { Course, CourseStatus } from "./types"

const coursesPath = '/courses'

export interface MyCourseParams {
  page?: number
  keyword?: string
  status?: CourseStatus
}

export const getMyCourses = async (params: MyCourseParams = {}): Promise<PaginatedData<Course>> => {
  const response = await httpClient.get<ApiResponse<PaginatedData<Course>>>(
      `${coursesPath}/me`,
      {
        params: {
          page: params.page,
          keyword: params.keyword,
          status: params.status,
        },
      },
    )

  return response.data.data;
}

export const getCourse = async (courseId: string): Promise<Course> => {
  const response = await httpClient.get<ApiResponse<Course>>(`${coursesPath}/${courseId}`)

  return response.data.data;
}