<script setup lang="ts">
import { computed } from 'vue'

type StatTone =
  | 'default'
  | 'secondary'
  | 'success'
  | 'warning'

const props = withDefaults(
  defineProps<{
    label: string
    value: string | number
    tone?: StatTone
  }>(),
  {
    tone: 'default',
  },
)

const valueClass = computed(() => {
  const classes: Record<StatTone, string> = {
    default: 'text-app-text',
    secondary: 'text-secondary',
    success: 'text-ai',
    warning: 'text-amber-700',
  }

  return classes[props.tone]
})
</script>

<template>
  <article
    class="rounded-panel border border-app-border bg-app-surface p-5 shadow-card"
  >
    <div class="flex items-start justify-between gap-3">
      <div>
        <p
          class="text-sm font-medium text-app-text-muted"
        >
          {{ label }}
        </p>

        <p
          class="mt-2 font-heading text-3xl font-bold"
          :class="valueClass"
        >
          {{ value }}
        </p>
      </div>

      <div
        v-if="$slots.icon"
        class="flex h-10 w-10 items-center justify-center rounded-card bg-app-surface-muted text-app-text-muted"
      >
        <slot name="icon" />
      </div>
    </div>
  </article>
</template>