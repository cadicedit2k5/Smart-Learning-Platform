import { USER_ROLES, type UserRole } from '@/features/auth/role'
import { adminPortal } from './admin'
import { lecturerPortal } from './lecturer'
import { studentPortal } from './student'
import type { PortalDefinition } from './types'

export const portals = [
  studentPortal,
  lecturerPortal,
  adminPortal,
]

export const portalRegistry: Record<UserRole,PortalDefinition> = {
  [USER_ROLES.STUDENT]: studentPortal,
  [USER_ROLES.LECTURER]: lecturerPortal,
  [USER_ROLES.ADMIN]: adminPortal,
}