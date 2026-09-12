<script setup lang="ts">
import { BookOpen, LogOut, X } from 'lucide-vue-next'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/features/auth/stores'
import type { NavigationItem } from '@/portals/types'

defineProps<{
  open: boolean
  navigation: NavigationItem[]
  basePath: string
}>()

const emit = defineEmits<{
  close: []
}>()

const router = useRouter()
const authStore = useAuthStore()

const handleLogout = async () => {
  authStore.logout()
  emit('close')
  await router.replace({ name: 'login' })
}
</script>

<template>
  <Transition
    enter-active-class="transition-opacity duration-200"
    enter-from-class="opacity-0"
    enter-to-class="opacity-100"
    leave-active-class="transition-opacity duration-200"
    leave-from-class="opacity-100"
    leave-to-class="opacity-0"
  >
    <button
      v-if="open"
      type="button"
      aria-label="Close sidebar"
      class="fixed inset-0 z-40 bg-slate-950/30 backdrop-blur-[1px] lg:hidden"
      @click="emit('close')"
    />
  </Transition>

  <aside
    class="fixed inset-y-0 left-0 z-50 flex w-sidebar flex-col border-r border-app-border/80 bg-app-surface transition-transform duration-300 lg:translate-x-0"
    :class="open ? 'translate-x-0' : '-translate-x-full'"
  >
    <header class="flex h-[74px] shrink-0 items-center justify-between px-5">
      <RouterLink
        :to="basePath"
        class="flex min-w-0 items-center gap-3"
        @click="emit('close')"
      >
        <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-card bg-primary text-on-primary">
          <BookOpen :size="20" stroke-width="2.2" />
        </div>

        <div class="min-w-0">
          <h1 class="truncate text-sm font-bold text-app-text">Smart Learning</h1>
          <p class="truncate text-xs text-app-text-muted">{{ authStore.user?.role.name }} Portal</p>
        </div>
      </RouterLink>

      <button
        type="button"
        aria-label="Close sidebar"
        class="flex h-9 w-9 items-center justify-center rounded-lg text-slate-500 transition hover:bg-slate-100 lg:hidden"
        @click="emit('close')"
      >
        <X :size="20" />
      </button>
    </header>

    <nav class="flex-1 overflow-y-auto px-3 py-4">
      <ul class="space-y-1.5">
        <li v-for="item in navigation" :key="item.routeName">
          <RouterLink
            :to="{ name: item.routeName }"
            class="group flex h-11 items-center gap-3 rounded-control px-3 text-sm font-medium text-app-text-muted transition-colors hover:bg-app-surface-muted hover:text-app-text"
            active-class="!bg-secondary-soft !font-semibold !text-secondary"
            @click="emit('close')"
          >
            <component
              :is="item.icon"
              v-if="item.icon"
              :size="18"
              stroke-width="1.8"
              class="shrink-0"
            />

            <span>{{ item.label }}</span>
          </RouterLink>
        </li>
      </ul>
    </nav>

    <div class="shrink-0 space-y-3 px-3 pb-4">
      <div class="border-t border-slate-100 pt-3">
        <button
          type="button"
          class="flex h-10 w-full items-center gap-3 rounded-control px-3 text-sm font-medium text-app-text-muted transition hover:bg-danger-soft hover:text-danger"
          @click="handleLogout"
        >
          <LogOut :size="17" :stroke-width="1.8" />
          <span>Logout</span>
        </button>
      </div>
    </div>
  </aside>
</template>