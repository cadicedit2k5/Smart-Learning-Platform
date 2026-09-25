<script setup lang="ts">
import { CheckCircle2, CircleAlert, Info, TriangleAlert, X } from 'lucide-vue-next'

import { useToastStore, type ToastType } from './store'

const toast = useToastStore()

const iconFor = (type: ToastType) => {
  if (type === 'success') return CheckCircle2
  if (type === 'error') return CircleAlert
  if (type === 'warning') return TriangleAlert
  return Info
}

const classesFor = (type: ToastType) => {
  if (type === 'success') return 'border-emerald-200 bg-emerald-50 text-emerald-800'
  if (type === 'error') return 'border-danger/20 bg-danger-soft text-on-danger-soft'
  if (type === 'warning') return 'border-amber-200 bg-amber-50 text-amber-800'
  return 'border-secondary/20 bg-secondary-soft text-app-text'
}
</script>

<template>
  <Teleport to="body">
    <div
      class="pointer-events-none fixed right-4 top-4 z-[100] flex w-[calc(100%-2rem)] max-w-sm flex-col gap-2"
      aria-live="polite"
    >
      <TransitionGroup
        enter-active-class="transition duration-200"
        enter-from-class="translate-x-3 opacity-0"
        leave-active-class="transition duration-200"
        leave-to-class="translate-x-3 opacity-0"
      >
        <div
          v-for="item in toast.items"
          :key="item.id"
          :role="item.type === 'error' ? 'alert' : 'status'"
          class="pointer-events-auto flex items-start gap-3 rounded-control border px-4 py-3 shadow-overlay"
          :class="classesFor(item.type)"
        >
          <component :is="iconFor(item.type)" :size="18" class="mt-0.5 shrink-0" />

          <p class="min-w-0 flex-1 text-sm leading-5">{{ item.message }}</p>

          <button
            type="button"
            class="shrink-0 opacity-60 transition hover:opacity-100"
            aria-label="Đóng thông báo"
            @click="toast.remove(item.id)"
          >
            <X :size="16" />
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>