import type { AuthRole, Permission} from '@/features/auth/types'

import {httpClient, type ApiResponse } from '@/shared/api'

const ADMIN_ROLES_PATH = '/admin/roles'
const ADMIN_PERMISSIONS_PATH = '/admin/permissions'

export interface AdminUpdateRoleRequest {
  name: string
  permissionIds: number[]
}

export const getRoles =
  async (): Promise<AuthRole[]> => {
    const response = await httpClient.get<ApiResponse<AuthRole[]>>(ADMIN_ROLES_PATH)

    return response.data.data;
  }

export const getRole =async (id: number,): Promise<AuthRole> => {
    const response = await httpClient.get<ApiResponse<AuthRole>>(`${ADMIN_ROLES_PATH}/${id}`,)

    return response.data.data;
  }

export const getPermissions =
  async (): Promise<Permission[]> => {
    const response =
      await httpClient.get<ApiResponse<Permission[]>>(ADMIN_PERMISSIONS_PATH)

    return response.data.data;
  }

export const updateRole = async (id: number, request: AdminUpdateRoleRequest): Promise<AuthRole> => {
  const response =
    await httpClient.patch<ApiResponse<AuthRole>>(
      `${ADMIN_ROLES_PATH}/${id}`,
      request)

  return response.data.data;
}