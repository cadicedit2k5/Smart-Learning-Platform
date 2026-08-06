import type { UserRole } from "@/features/auth/role"
import type { RouteLocationRaw } from "vue-router"
import { portalRegistry } from "./registry"

export const getPortalHomeRoute = (
  role: UserRole,
): RouteLocationRaw => {
  return portalRegistry[role].basePath
}