<script setup lang="ts">
import { computed } from 'vue'

type BadgeTone =
  | 'neutral'
  | 'primary'
  | 'secondary'
  | 'success'
  | 'warning'
  | 'danger'

const props = withDefaults(
  defineProps<{
    tone?: BadgeTone
  }>(),
  {
    tone: 'neutral',
  },
)

const toneClass = computed(() => {
  const classes: Record<BadgeTone, string> = {
    neutral:
      'bg-app-surface-muted text-app-text-muted',
    primary:
      'bg-primary/10 text-primary',
    secondary:
      'bg-secondary-soft text-secondary',
    success:
      'bg-ai-soft text-ai',
    warning:
      'bg-amber-50 text-amber-800',
    danger:
      'bg-danger-soft text-danger',
  }

  return classes[props.tone]
})
</script>

<template>
  <span
    class="inline-flex items-center gap-1.5 rounded-pill px-2.5 py-1 text-xs font-semibold"
    :class="toneClass"
  >
    <slot />
  </span>
</template>