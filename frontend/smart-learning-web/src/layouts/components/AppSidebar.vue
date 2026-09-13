<script setup lang="ts">
import {
  BookOpen,
  LogOut,
  PanelLeftClose,
  PanelLeftOpen,
  X,
} from 'lucide-vue-next'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/features/auth/stores'
import type { NavigationItem } from '@/portals/types'

defineProps<{
  open: boolean
  collapsed: boolean
  navigation: NavigationItem[]
  basePath: string
}>()

const emit = defineEmits<{
  close: []
  toggleCollapse: []
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
      aria-label="Đóng thanh điều hướng"
      class="fixed inset-0 z-40 bg-slate-950/30 backdrop-blur-[1px] lg:hidden"
      @click="emit('close')"
    />
  </Transition>

  <aside
    class="fixed inset-y-0 left-0 z-50 flex flex-col border-r border-app-border/80 bg-app-surface transition-[width,transform] duration-300"
    :class="[
      open ? 'translate-x-0' : '-translate-x-full',
      collapsed ? 'lg:w-sidebar-collapsed' : 'lg:w-sidebar',
      'w-sidebar lg:translate-x-0',
    ]"
  >
    <header
      class="flex h-[74px] shrink-0 items-center border-b border-app-border/60"
      :class="collapsed ? 'lg:justify-center lg:px-2' : 'justify-between px-5'"
    >
      <RouterLink
        :to="basePath"
        class="flex min-w-0 items-center gap-3"
        :class="{ 'lg:hidden': collapsed }"
        @click="emit('close')"
      >
        <div
          class="flex h-10 w-10 shrink-0 items-center justify-center rounded-card bg-primary text-on-primary"
        >
          <BookOpen :size="20" stroke-width="2.2" />
        </div>

        <div class="min-w-0">
          <h1 class="truncate text-sm font-bold text-app-text">
            Smart Learning
          </h1>

          <p class="truncate text-xs text-app-text-muted">
            {{ authStore.user?.role.name }} Portal
          </p>
        </div>
      </RouterLink>

      <button
        type="button"
        aria-label="Đóng thanh điều hướng"
        class="flex h-9 w-9 items-center justify-center rounded-lg text-app-text-muted transition hover:bg-app-surface-muted hover:text-app-text lg:hidden"
        @click="emit('close')"
      >
        <X :size="20" />
      </button>

      <button
        type="button"
        :aria-label="collapsed ? 'Mở rộng thanh điều hướng' : 'Thu gọn thanh điều hướng'"
        :title="collapsed ? 'Mở rộng sidebar' : 'Thu gọn sidebar'"
        class="hidden h-9 w-9 items-center justify-center rounded-lg text-app-text-muted transition hover:bg-app-surface-muted hover:text-app-text lg:flex"
        @click="emit('toggleCollapse')"
      >
        <PanelLeftOpen v-if="collapsed" :size="19" />
        <PanelLeftClose v-else :size="19" />
      </button>
    </header>

    <nav class="flex-1 overflow-y-auto px-3 py-4">
      <ul class="space-y-1.5">
        <li
          v-for="item in navigation"
          :key="item.routeName"
        >
          <RouterLink
            :to="{ name: item.routeName }"
            :title="collapsed ? item.label : undefined"
            class="group flex h-11 items-center rounded-control text-sm font-medium text-app-text-muted transition-colors hover:bg-app-surface-muted hover:text-app-text"
            :class="
              collapsed
                ? 'lg:justify-center lg:px-0'
                : 'gap-3 px-3'
            "
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

            <span :class="{ 'lg:hidden': collapsed }">
              {{ item.label }}
            </span>
          </RouterLink>
        </li>
      </ul>
    </nav>

    <div class="shrink-0 px-3 pb-4">
      <div class="border-t border-app-border pt-3">
        <button
          type="button"
          title="Đăng xuất"
          class="flex h-10 w-full items-center rounded-control text-sm font-medium text-app-text-muted transition hover:bg-danger-soft hover:text-danger"
          :class="
            collapsed
              ? 'lg:justify-center lg:px-0'
              : 'gap-3 px-3'
          "
          @click="handleLogout"
        >
          <LogOut :size="17" :stroke-width="1.8" />

          <span :class="{ 'lg:hidden': collapsed }">
            Đăng xuất
          </span>
        </button>
      </div>
    </div>
  </aside>
</template>