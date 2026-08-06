export const USER_ROLES = {
  STUDENT: 'STUDENT',
  LECTURER: 'LECTURER',
  ADMIN: 'ADMIN',
} as const

export type UserRole = (typeof USER_ROLES)[keyof typeof USER_ROLES]