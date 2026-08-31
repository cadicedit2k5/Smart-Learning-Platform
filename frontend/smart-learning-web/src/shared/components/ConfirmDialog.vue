<script setup lang="ts">
import BaseButton from './BaseButton.vue'
import BaseModal from './BaseModal.vue'

defineProps<{
  open: boolean
  title: string
  description: string
  confirmText?: string
  loading?: boolean
  error?: string
}>()

defineEmits<{
  close: []
  confirm: []
}>()
</script>

<template>
  <BaseModal
    :open="open"
    :title="title"
    :loading="loading"
    @close="$emit('close')"
  >
    <p class="text-sm leading-6 text-app-text-muted">
      {{ description }}
    </p>

    <p
      v-if="error"
      class="mt-4 rounded-control bg-danger-soft p-3 text-sm text-danger"
    >
      {{ error }}
    </p>

    <template #footer>
      <div class="flex justify-end gap-3">
        <BaseButton
          variant="secondary"
          :disabled="loading"
          @click="$emit('close')"
        >
          Hủy
        </BaseButton>

        <button
          type="button"
          class="inline-flex h-11 items-center justify-center rounded-control bg-danger px-4 text-sm font-semibold text-white disabled:opacity-60"
          :disabled="loading"
          @click="$emit('confirm')"
        >
          {{ loading ? 'Đang xử lý...' : (confirmText ?? 'Xác nhận') }}
        </button>
      </div>
    </template>
  </BaseModal>
</template>