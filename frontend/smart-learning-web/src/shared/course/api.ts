import { httpClient, type ApiResponse } from "../api"
import type { Course } from "./types"

const coursesPath = '/courses'

export const getMyCourses = async (): Promise<Course[]> => {
  const response = await httpClient.get<ApiResponse<Course[]>>(`${coursesPath}/me`)

  return response.data.data;
}

export const getCourse = async (courseId: string): Promise<Course> => {
  const response = await httpClient.get<ApiResponse<Course>>(`${coursesPath}/${courseId}`)

  return response.data.data;
}