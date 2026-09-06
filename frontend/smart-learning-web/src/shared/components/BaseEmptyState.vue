<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <section
    class="rounded-panel border border-dashed border-app-border bg-app-surface px-6 py-14 text-center"
  >
    <div
      v-if="$slots.icon"
      class="mx-auto flex h-12 w-12 items-center justify-center rounded-card bg-app-surface-muted text-app-text-muted"
    >
      <slot name="icon" />
    </div>

    <h2
      class="mt-4 font-heading text-xl font-bold text-app-text"
    >
      {{ title }}
    </h2>

    <p
      v-if="description"
      class="mx-auto mt-2 max-w-md text-sm leading-6 text-app-text-muted"
    >
      {{ description }}
    </p>

    <div
      v-if="$slots.actions"
      class="mt-5 flex justify-center"
    >
      <slot name="actions" />
    </div>
  </section>
</template>