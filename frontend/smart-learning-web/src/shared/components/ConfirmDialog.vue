<script setup lang="ts">
import {
  AlertTriangle,
} from 'lucide-vue-next'

import {
  BaseButton,
  BaseModal,
} from '@/shared/components'

withDefaults(
  defineProps<{
    open: boolean
    title: string
    description: string
    confirmText?: string
    loading?: boolean
    error?: string
  }>(),
  {
    confirmText: 'Xác nhận',
    loading: false,
    error: '',
  },
)

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
    max-width="max-w-md"
    @close="$emit('close')"
  >
    <div
      class="flex gap-4"
    >
      <div
        class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-danger-soft text-danger"
      >
        <AlertTriangle :size="20" />
      </div>

      <div class="min-w-0">
        <p
          class="text-sm leading-6 text-app-text-muted"
        >
          {{ description }}
        </p>

        <p
          v-if="error"
          role="alert"
          class="mt-4 rounded-control border border-danger/20 bg-danger-soft p-3 text-sm text-danger"
        >
          {{ error }}
        </p>
      </div>
    </div>

    <template #footer>
      <div
        class="flex justify-end gap-3"
      >
        <BaseButton
          variant="secondary"
          :disabled="loading"
          @click="$emit('close')"
        >
          Hủy
        </BaseButton>

        <button
          type="button"
          class="inline-flex h-11 items-center justify-center rounded-control bg-danger px-4 text-sm font-semibold text-white transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="loading"
          @click="$emit('confirm')"
        >
          {{
            loading
              ? 'Đang xử lý...'
              : confirmText
          }}
        </button>
      </div>
    </template>
  </BaseModal>
</template>