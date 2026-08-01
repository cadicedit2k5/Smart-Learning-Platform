import type { UserRole } from "@/features/auth/role"
import { portalRegistry } from "@/portals/registry"
import { computed } from "vue"
import { useRoute } from "vue-router"

export const useCurrentPortal = () => {
  const route = useRoute()

  const portal = computed(() => {
    const role = route.meta.portal as UserRole | undefined

    return role ? portalRegistry[role] : undefined
  })

  return {
    portal,
  }
}