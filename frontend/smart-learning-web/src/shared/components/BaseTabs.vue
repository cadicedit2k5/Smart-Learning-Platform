<script setup lang="ts">
import type { Component } from 'vue'

export interface TabItem {
  id: string
  label: string
  icon?: Component
}

defineProps<{
  modelValue: string
  tabs: TabItem[]
  ariaLabel?: string
}>()

defineEmits<{
  'update:modelValue': [value: string]
}>()
</script>

<template>
  <nav
    class="border-b border-app-border"
    :aria-label="ariaLabel"
  >
    <div
      class="flex gap-7 overflow-x-auto"
    >
      <button
        v-for="tab in tabs"
        :key="tab.id"
        type="button"
        class="relative flex h-12 shrink-0 items-center gap-2 px-1 text-sm font-semibold transition"
        :class="
          modelValue === tab.id
            ? 'text-secondary'
            : 'text-app-text-muted hover:text-app-text'
        "
        @click="$emit('update:modelValue', tab.id)"
      >
        <component
          :is="tab.icon"
          v-if="tab.icon"
          :size="17"
        />

        {{ tab.label }}

        <span
          v-if="modelValue === tab.id"
          class="absolute inset-x-0 bottom-0 h-0.5 rounded-full bg-secondary"
        />
      </button>
    </div>
  </nav>
</template>