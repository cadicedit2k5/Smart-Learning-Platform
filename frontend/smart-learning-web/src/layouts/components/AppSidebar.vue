<script setup lang="ts">
import {
  BookOpen,
  LogOut,
  X,
} from 'lucide-vue-next'
import type { NavigationItem } from '@/portals/types';

defineProps<{
  open: boolean
  navigation: NavigationItem[]
}>()

const emit = defineEmits<{
  close: []
}>()
</script>

<template>
  <!-- Mobile overlay -->
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
    class="fixed inset-y-0 left-0 z-50 flex w-[248px] flex-col border-r border-slate-200 bg-white transition-transform duration-300 lg:translate-x-0"
    :class="open ? 'translate-x-0' : '-translate-x-full'"
  >
    <!-- Logo -->
    <header class="flex h-[74px] shrink-0 items-center justify-between px-5">
      <RouterLink
        :to="navigation[0]?.routeName ? { name: navigation[0].routeName } : '/'"
        class="flex min-w-0 items-center gap-3"
        @click="emit('close')"
      >
        <div
          class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-violet-600 text-white shadow-sm shadow-violet-200"
        >
          <BookOpen :size="20" stroke-width="2.2" />
        </div>

        <div class="min-w-0">
          <h1 class="truncate text-[15px] font-bold text-slate-900">EduAI Portal</h1>

          <p class="truncate text-[10px] font-medium text-slate-400">Faculty Dashboard</p>
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

    <!-- Main navigation -->
    <nav class="flex-1 overflow-y-auto px-3 py-4">
      <ul class="space-y-1.5">
        <li v-for="item in navigation" :key="item.routeName">
          <RouterLink :to="{ name: item.routeName }" custom v-slot="{ href, navigate, isActive }">
            <a
              :href="href"
              class="group flex h-11 items-center gap-3 rounded-lg px-3 text-[13px] font-medium transition-colors"
              :class="
                isActive
                  ? 'bg-violet-50 text-violet-700'
                  : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
              "
              @click="
                (event) => {
                  navigate(event)
                  emit('close')
                }
              "
            >
              <component
                :is="item.icon"
                :size="18"
                stroke-width="1.8"
                class="shrink-0"
                :class="isActive ? 'text-violet-600' : 'text-slate-500 group-hover:text-slate-700'"
              />

              <span>{{ item.label }}</span>
            </a>
          </RouterLink>
        </li>
      </ul>
    </nav>

    <!-- Bottom actions -->
    <div class="shrink-0 space-y-3 px-3 pb-4">

      <div class="border-t border-slate-100 pt-3">

        <button
          type="button"
          class="flex h-10 w-full items-center gap-3 rounded-lg px-3 text-[13px] font-medium text-slate-600 transition hover:bg-red-50 hover:text-red-600"
        >
          <LogOut :size="17" stroke-width="1.8" />

          <span>Logout</span>
        </button>
      </div>
    </div>
  </aside>
</template>
