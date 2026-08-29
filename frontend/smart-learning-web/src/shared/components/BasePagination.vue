<script setup lang="ts">
import BaseButton from './BaseButton.vue'

defineProps<{
  page: number
  totalPages: number
  totalElements?: number
}>()

defineEmits<{
  previous: []
  next: []
}>()
</script>

<template>
  <div
    class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"
  >
    <p class="text-sm text-app-text-muted">
      Trang

      <span class="font-semibold text-app-text">
        {{ page }}
      </span>

      / {{ totalPages }}

      <template v-if="totalElements !== undefined">
        · {{ totalElements }} kết quả
      </template>
    </p>

    <div class="flex gap-2">
      <BaseButton
        variant="secondary"
        :disabled="page <= 1"
        @click="$emit('previous')"
      >
        Trước
      </BaseButton>

      <BaseButton
        variant="secondary"
        :disabled="page >= totalPages"
        @click="$emit('next')"
      >
        Sau
      </BaseButton>
    </div>
  </div>
</template>