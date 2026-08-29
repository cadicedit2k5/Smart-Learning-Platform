import type { AuthUser, RegisterRequest, UserStatus } from '@/features/auth/types'
import type { UserRole } from '@/features/auth/role'
import {
    httpClient,
    type ApiResponse,
} from '@/shared/api'

const ADMIN_USERS_PATH = '/admin/users'

export interface UsersPage {
    content: AuthUser[]
    pageable: {
        page: number
        size: number
        totalElements: number
        totalPages: number
    }
}

export interface UserListParams {
    page: number
    keyword?: string
    roleCode?: UserRole
    status?: UserStatus
    createdFrom?: string
    createdTo?: string
    createdAtOrder?: 'ASC' | 'DESC'
}

export interface AdminCreateUserRequest extends RegisterRequest {
    roleCode: UserRole
};

export interface AdminUpdateUserRequest {
  email?: string
  password?: string
  fullName?: string
  roleCode?: UserRole
  avatar?: File
}

export const getUsers = async (params: UserListParams): Promise<UsersPage> => {
    const response =
        await httpClient.get<ApiResponse<UsersPage>>(
            ADMIN_USERS_PATH,
            {
                params: {
                    page: params.page ? params.page : 1,
                    keyword: params.keyword || undefined,
                    roleCode: params.roleCode || undefined,
                    status: params.status || undefined,
                    createdFrom: params.createdFrom || undefined,
                    createdTo: params.createdTo || undefined,
                    'orders[createdAt]':
                        params.createdAtOrder || undefined,
                },
            },
        )

    return response.data.data;
};

export const createUser = async (
request: AdminCreateUserRequest,
): Promise<AuthUser> => {
    const formData = new FormData();

    formData.append('email', request.email);
    formData.append('password', request.password);
    formData.append('fullName', request.fullName);
    formData.append('roleCode', request.roleCode);

    if (request.avatar) {
        formData.append('avatar', request.avatar)
    }

    const response =await httpClient.
            post<ApiResponse<AuthUser>>(
                ADMIN_USERS_PATH,
                formData,
            );

    return response.data.data;
}

export const updateUser = async (
  id: string,
  request: AdminUpdateUserRequest,
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

  if (request.roleCode !== undefined) {
    formData.append('roleCode', request.roleCode)
  }

  if (request.avatar !== undefined) {
    formData.append('avatar', request.avatar)
  }

  const response =
    await httpClient.patch<ApiResponse<AuthUser>>(
      `${ADMIN_USERS_PATH}/${id}`,
      formData,
    )

  return response.data.data
}

export const deleteUser = async (
    id: string,
): Promise<void> => {
    await httpClient.delete(
        `${ADMIN_USERS_PATH}/${id}`,
    );
}