<script setup lang="ts">
import { reactive, watch } from 'vue'

import {
  BaseAlert,
  BaseButton,
  BaseInput,
  BaseModal,
} from '@/shared/components'

import type { CourseAnnouncement } from '@/shared/announcement/types'
import type { AnnouncementInput } from '../api/announcementApi'

const props = withDefaults(
  defineProps<{
    open: boolean
    announcement?: CourseAnnouncement | null
    loading?: boolean
    serverMessage?: string
  }>(),
  {
    announcement: null,
    loading: false,
    serverMessage: '',
  },
)

const emit = defineEmits<{
  close: []
  submit: [input: AnnouncementInput]
}>()

const form = reactive({
  title: '',
  content: '',
})

const errors = reactive({
  title: '',
  content: '',
})

watch(
  () => [props.open, props.announcement],
  () => {
    if (!props.open) return

    form.title = props.announcement?.title ?? ''
    form.content = props.announcement?.content ?? ''
    errors.title = ''
    errors.content = ''
  },
  { immediate: true },
)

const submit = () => {
  errors.title = ''
  errors.content = ''

  if (!form.title.trim()) {
    errors.title = 'Vui lòng nhập tiêu đề thông báo.'
  }

  if (!form.content.trim()) {
    errors.content = 'Vui lòng nhập nội dung thông báo.'
  }

  if (errors.title || errors.content) return

  emit('submit', {
    title: form.title.trim(),
    content: form.content.trim(),
  })
}
</script>

<template>
  <BaseModal
    :open="open"
    :title="announcement ? 'Chỉnh sửa thông báo' : 'Tạo thông báo'"
    :loading="loading"
    max-width="max-w-2xl"
    @close="emit('close')"
  >
    <form class="space-y-5" @submit.prevent="submit">
      <BaseAlert v-if="serverMessage">
        {{ serverMessage }}
      </BaseAlert>

      <BaseInput
        v-model="form.title"
        label="Tiêu đề"
        maxlength="255"
        required
        :disabled="loading"
        :error="errors.title"
      />

      <label class="block">
        <span class="mb-2 block text-sm font-semibold text-app-text">
          Nội dung <span class="text-danger">*</span>
        </span>

        <textarea
          v-model="form.content"
          rows="7"
          maxlength="10000"
          :disabled="loading"
          class="w-full resize-y rounded-control border border-app-border bg-app-surface px-3 py-2.5 text-sm text-app-text outline-none transition focus:border-secondary focus:ring-2 focus:ring-secondary/20"
          placeholder="Nhập nội dung thông báo..."
        />

        <p v-if="errors.content" class="mt-2 text-sm text-danger">
          {{ errors.content }}
        </p>
      </label>

      <div class="flex justify-end gap-3 border-t border-app-border pt-5">
        <BaseButton variant="secondary" :disabled="loading" @click="emit('close')">
          Hủy
        </BaseButton>

        <BaseButton type="submit" :loading="loading">
          {{ announcement ? 'Lưu thay đổi' : 'Đăng thông báo' }}
        </BaseButton>
      </div>
    </form>
  </BaseModal>
</template>