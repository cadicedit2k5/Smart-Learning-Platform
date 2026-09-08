import { httpClient, type ApiResponse, type PaginatedData } from "../api"
import type { Course } from "./types"

const coursesPath = '/courses'

export const getMyCourses = async (page = 1): Promise<PaginatedData<Course>> => {
  const response = await httpClient.get<ApiResponse<PaginatedData<Course>>>(
      `${coursesPath}/me`,
      {
        params: {
          page,
        },
      },
    )

  return response.data.data;
}

export const getCourse = async (courseId: string): Promise<Course> => {
  const response = await httpClient.get<ApiResponse<Course>>(`${coursesPath}/${courseId}`)

  return response.data.data;
}