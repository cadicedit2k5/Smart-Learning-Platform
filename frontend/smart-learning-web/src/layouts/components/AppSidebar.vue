<script setup lang="ts">
import { markRaw } from 'vue'
import {
  BarChart3,
  BookOpen,
  BrainCircuit,
  GraduationCap,
  LayoutDashboard,
  LogOut,
  Plus,
  Settings,
  X,
} from 'lucide-vue-next'
import type { NavigationItem } from '@/portals/type';

defineProps<{
  open: boolean
  navigation: NavigationItem[]
}>()

const emit = defineEmits<{
  close: []
}>()

const secondaryNavigation = [
  {
    label: 'Settings',
    path: '/settings',
    icon: markRaw(Settings),
  },
]
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
        v-for="item in navigation"
        :key="item.routeName"
        :to="{ name: item.routeName }"
      >
        <component
          :is="item.icon"
          v-if="item.icon"
        />

        <span>{{ item.label }}</span>
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
      <!-- <RouterLink
        to="/lecturer/courses/create"
        class="flex h-11 w-full items-center justify-center gap-2 rounded-lg bg-[#082544] px-4 text-[12px] font-semibold text-white shadow-sm transition hover:bg-[#0d3158] active:scale-[0.99]"
        @click="emit('close')"
      >
        <Plus :size="16" stroke-width="2.5" />
        <span>Create New Course</span>
      </RouterLink> -->

      <div class="border-t border-slate-100 pt-3">
        <!-- <RouterLink
          v-for="item in secondaryNavigation"
          :key="item.path"
          :to="{ name: item.path ? item.path : '' }"
          class="flex h-10 items-center gap-3 rounded-lg px-3 text-[13px] font-medium text-slate-600 transition hover:bg-slate-50 hover:text-slate-900"
          @click="emit('close')"
        >
          <component :is="item.icon" :size="17" stroke-width="1.8" class="text-slate-500" />

          <span>{{ item.label }}</span>
        </RouterLink> -->

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
