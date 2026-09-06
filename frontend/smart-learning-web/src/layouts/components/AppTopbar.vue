<script setup lang="ts">
import { computed, ref } from 'vue'
import { Bell, CircleHelp, Grid3X3, Menu, Search, UserRound } from 'lucide-vue-next'
import type { TopbarConfig } from '@/portals/types';
import { useAuthStore } from '@/features/auth/stores';

const props = defineProps<{
  config: TopbarConfig,
}>()

const emit = defineEmits<{
  toggleSidebar: []
}>()

const authStore = useAuthStore();

const initials = computed(() => {
  const fullName = authStore.user?.fullName?.trim()

  if (!fullName) {
    return 'U'
  }

  return fullName
    .split(/\s+/)
    .slice(-2)
    .map((word) => word[0])
    .join('')
    .toUpperCase()
})
</script>

<template>
  <header
    class="fixed left-0 right-0 top-0 z-30 h-16 border-b border-app-border bg-app-surface/95 backdrop-blur lg:left-sidebar"
  >
    <div class="flex h-full items-center gap-3 px-4 sm:px-6">
      <button
        type="button"
        aria-label="Open sidebar"
        class="flex h-10 w-10 shrink-0 items-center justify-center rounded-control text-app-text-muted transition hover:bg-app-surface-muted lg:hidden"
        @click="emit('toggleSidebar')"
      >
        <Menu :size="21" />
      </button>

      <div class="ml-auto flex items-center gap-3">
        <RouterLink
          v-if="config.primaryAction"
          :to="{ name: config.primaryAction.routeName }"
          class="flex h-10 items-center gap-2 rounded-control bg-primary px-4 text-sm font-semibold text-on-primary transition hover:bg-primary-hover"
        >
          <component
            :is="config.primaryAction.icon"
            v-if="config.primaryAction.icon"
            :size="16"
          />

          <span class="hidden sm:inline">
            {{ config.primaryAction.label }}
          </span>
        </RouterLink>

        <div class="flex items-center gap-2">
          <div
            class="flex h-9 w-9 items-center justify-center rounded-full bg-secondary-soft text-xs font-bold text-secondary"
          >
            {{ initials }}
          </div>

          <div class="hidden sm:block">
            <p class="text-sm font-semibold text-app-text">
              {{ authStore.user?.fullName }}
            </p>

            <p class="text-xs text-app-text-muted">
              {{ authStore.user?.role.name }}
            </p>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>