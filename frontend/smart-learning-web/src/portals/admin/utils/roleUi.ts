import type {UserRole} from '@/features/auth/role'

export type BadgeTone =
  | 'neutral'
  | 'primary'
  | 'secondary'
  | 'success'
  | 'warning'
  | 'danger'

export const roleLabel = (
  role: UserRole,
): string => {
  const labels: Record<UserRole, string> = {
    ADMIN: 'Quản trị viên',
    LECTURER: 'Giảng viên',
    STUDENT: 'Học viên',
  }

  return labels[role]
}

export const roleTone = (role: UserRole): BadgeTone => {
  const tones: Record<UserRole, BadgeTone> = {
    ADMIN: 'secondary',
    LECTURER: 'warning',
    STUDENT: 'primary',
  }

  return tones[role]
}

export const isAdminRole = ( role: UserRole) => {
  return role === 'ADMIN'
}