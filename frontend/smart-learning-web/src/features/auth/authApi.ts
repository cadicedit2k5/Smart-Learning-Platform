import { httpClient } from '@/shared/api'

import type { ApiResponse } from '@/shared/api'
import type { AuthUser, LoginRequest, LoginResponse, RegisterRequest, UpdateCurrentUserRequest } from './types'


export const register = async (
  request: RegisterRequest,
): Promise<AuthUser> => {
  const formData = new FormData();

  formData.append('email', request.email);
  formData.append('password', request.password);
  formData.append('fullName', request.fullName);

  if (request.avatar) {
    formData.append('avatar', request.avatar);
  }

  const response = await httpClient.post<ApiResponse<AuthUser>>
  ('/auth/register', formData);

  return response.data.data;
}

export const login = async (
  request: LoginRequest,
): Promise<LoginResponse> => {
  const response = await httpClient.post<ApiResponse<LoginResponse>>
  ('/auth/login', request);

  return response.data.data;
}

export const getCurrentUser = async (): Promise<AuthUser> => {
    const response = await httpClient.get<ApiResponse<AuthUser>>('/auth/me');
    return response.data.data;
}

export const updateCurrentUser = async (
  request: UpdateCurrentUserRequest,
): Promise<AuthUser> => {
  const formData = new FormData()

  if (request.email !== undefined) {
    formData.append('email', request.email)
  }

  if (request.password !== undefined) {
    formData.append('password', request.password)
  }

  if (request.fullName !== undefined) {
    formData.append('fullName', request.fullName)
  }

  if (request.avatar !== undefined) {
    formData.append('avatar', request.avatar)
  }

  const response =
    await httpClient.patch<ApiResponse<AuthUser>>(
      '/auth/me',
      formData,
    )

  return response.data.data
}