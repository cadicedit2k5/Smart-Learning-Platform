<script setup lang="ts">
import { computed } from 'vue'

type ButtonVariant =
  | 'primary'
  | 'secondary'
  | 'ai'
  | 'ghost'

const props = withDefaults(
  defineProps<{
    type?: 'button' | 'submit' | 'reset'
    variant?: ButtonVariant
    loading?: boolean
    disabled?: boolean
    block?: boolean
  }>(),
  {
    type: 'button',
    variant: 'primary',
    loading: false,
    disabled: false,
    block: false,
  },
)

const variantClasses: Record<ButtonVariant, string> = {
  primary:
    'bg-primary text-on-primary hover:bg-primary-hover',

  secondary:
    'border border-app-border bg-app-surface text-app-text hover:bg-app-surface-muted',

  ai:
    'bg-ai text-on-ai hover:bg-ai-hover',

  ghost:
    'bg-transparent text-app-text hover:bg-app-surface-muted',
}

const buttonClasses = computed(() => {
  return variantClasses[props.variant]
})
</script>

<template>
  <button
    :type="props.type"
    :disabled="props.disabled || props.loading"
    :aria-busy="props.loading"
    class="inline-flex h-11 items-center justify-center gap-2 rounded-control px-4 text-sm font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-60"
    :class="[
      buttonClasses,
      {
        'w-full': props.block,
      },
    ]"
  >
    <span
      v-if="props.loading"
      aria-hidden="true"
      class="h-4 w-4 animate-spin rounded-full border-2 border-current border-r-transparent"
    />

    <slot
      v-else
      name="leading"
    />

    <span>
      <slot />
    </span>

    <slot
      v-if="!props.loading"
      name="trailing"
    />
  </button>
</template>