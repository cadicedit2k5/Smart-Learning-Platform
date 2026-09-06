<script setup lang="ts">
import { computed } from 'vue'

type AlertVariant =
  | 'error'
  | 'info'
  | 'success'
  | 'ai'

const props = withDefaults(
  defineProps<{
    variant?: AlertVariant
    title?: string
  }>(),
  {
    variant: 'error',
    title: undefined,
  },
)

const variantClasses: Record<AlertVariant, string> = {
  error:
    'border-danger/20 bg-danger-soft text-on-danger-soft',

  info:
    'border-secondary/20 bg-secondary-soft text-app-text',
    
   success:
    'border-emerald-200 bg-emerald-50 text-emerald-800',

  ai:
    'border-ai/20 bg-ai-soft text-ai',
}

const alertClasses = computed(() => {
  return variantClasses[props.variant]
})

const alertRole = computed(() => {
  return props.variant === 'error'
    ? 'alert'
    : 'status'
})
</script>

<template>
  <div
    :role="alertRole"
    class="flex gap-3 rounded-control border px-4 py-3 text-sm"
    :class="alertClasses"
  >
    <div
      v-if="$slots.icon"
      class="mt-0.5 shrink-0"
    >
      <slot name="icon" />
    </div>

    <div class="min-w-0">
      <p
        v-if="props.title"
        class="font-semibold"
      >
        {{ props.title }}
      </p>

      <div :class="{ 'mt-1': props.title }">
        <slot />
      </div>
    </div>
  </div>
</template>