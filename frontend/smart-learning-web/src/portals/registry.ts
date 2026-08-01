import { adminPortal } from './admin'
import { lecturerPortal } from './lecturer'
import { studentPortal } from './student'

export const portals = [
  studentPortal,
  lecturerPortal,
  adminPortal,
]

export const portalRegistry = {
  student: studentPortal,
  lecturer: lecturerPortal,
  admin: adminPortal,
}