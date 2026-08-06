<script setup lang="ts">
import { computed, useId } from 'vue'

defineOptions({
  inheritAttrs: false,
})

const model = defineModel<string>({
  default: '',
})

const props = withDefaults(
  defineProps<{
    id?: string
    label?: string
    type?: string
    placeholder?: string
    autocomplete?: string
    error?: string
    disabled?: boolean
    required?: boolean
  }>(),
  {
    id: undefined,
    label: undefined,
    type: 'text',
    placeholder: undefined,
    autocomplete: undefined,
    error: undefined,
    disabled: false,
    required: false,
  },
)

const generatedId = useId()

const inputId = computed(() => {
  return props.id ?? generatedId
})

const errorId = computed(() => {
  return `${inputId.value}-error`
})
</script>

<template>
  <div class="space-y-2">
    <label
      v-if="props.label"
      :for="inputId"
      class="block text-sm font-semibold text-app-text"
    >
      {{ props.label }}

      <span
        v-if="props.required"
        aria-hidden="true"
        class="text-danger"
      >
        *
      </span>
    </label>

    <div class="relative">
      <div
        v-if="$slots.leading"
        class="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-app-text-muted"
      >
        <slot name="leading" />
      </div>

      <input
        v-bind="$attrs"
        :id="inputId"
        v-model="model"
        :type="props.type"
        :placeholder="props.placeholder"
        :autocomplete="props.autocomplete"
        :disabled="props.disabled"
        :required="props.required"
        :aria-invalid="props.error ? 'true' : undefined"
        :aria-describedby="props.error ? errorId : undefined"
        class="h-11 w-full rounded-control border bg-app-surface px-3 text-sm text-app-text outline-none transition placeholder:text-app-text-muted/60 focus:border-secondary focus:ring-2 focus:ring-secondary/20 disabled:cursor-not-allowed disabled:bg-app-surface-muted disabled:opacity-70"
        :class="[
          props.error
            ? 'border-danger'
            : 'border-app-border',
          {
            'pl-10': $slots.leading,
            'pr-11': $slots.trailing,
          },
        ]"
      />

      <div
        v-if="$slots.trailing"
        class="absolute inset-y-0 right-0 flex items-center pr-2"
      >
        <slot name="trailing" />
      </div>
    </div>

    <p
      v-if="props.error"
      :id="errorId"
      role="alert"
      class="text-sm text-danger"
    >
      {{ props.error }}
    </p>
  </div>
</template>