<script setup lang="ts">
defineProps<{
  eyebrow?: string
  title: string
  description?: string
}>()
</script>

<template>
  <header
    class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between"
  >
    <div class="min-w-0">
      <p
        v-if="eyebrow"
        class="text-sm font-semibold text-secondary"
      >
        {{ eyebrow }}
      </p>

      <h1
        class="font-heading text-3xl font-bold tracking-tight text-app-text"
        :class="{ 'mt-1': eyebrow }"
      >
        {{ title }}
      </h1>

      <p
        v-if="description"
        class="mt-2 max-w-2xl text-sm leading-6 text-app-text-muted"
      >
        {{ description }}
      </p>
    </div>

    <div
      v-if="$slots.actions"
      class="flex shrink-0 items-center gap-2"
    >
      <slot name="actions" />
    </div>
  </header>
</template>