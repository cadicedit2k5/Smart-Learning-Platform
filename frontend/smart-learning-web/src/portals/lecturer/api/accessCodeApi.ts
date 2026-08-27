import { httpClient, type ApiResponse } from '@/shared/api'

export interface AccessCode {
  id: string
  courseId: string
  code: string
  expiresAt: string | null
  active: boolean
  createdAt: string
}

export interface AccessCodeItem {
  id: string
  courseId: string
  codeHint: string
  expiresAt: string | null
  active: boolean
  revokedAt: string | null
  createdBy: string
  createdAt: string
}

export const getAccessCodes = async (courseId: string): Promise<AccessCodeItem[]> => {
  const response = await httpClient.get<ApiResponse<AccessCodeItem[]>>(
    `/courses/${courseId}/access-codes`,
  )

  return response.data.data
}

export const createAccessCode = async (
  courseId: string,
  expiresAt: string | null,
): Promise<AccessCode> => {
  const response = await httpClient.post<ApiResponse<AccessCode>>(
    `/courses/${courseId}/access-codes`,
    { expiresAt },
  )

  return response.data.data
}

export const revokeAccessCode = async (courseId: string, codeId: string): Promise<void> => {
  await httpClient.delete(`/courses/${courseId}/access-codes/${codeId}`)
}
