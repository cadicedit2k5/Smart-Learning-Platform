<script setup lang="ts">
import { reactive, ref, watch } from 'vue'

import {
  BaseAlert,
  BaseButton,
  BaseInput,
  BaseModal,
} from '@/shared/components'
import type { CourseAnnouncement } from '@/shared/announcement/types'
import type { TopicContent } from '@/shared/course-content'
import {
  createEmptyRichText,
  hasRichTextContent,
  parseStoredRichText,
  RichTextEditor,
  serializeRichText,
} from '@/shared/rich-text'

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

const form = reactive({ title: '' })
const content = ref<TopicContent>(createEmptyRichText())
const errors = reactive({ title: '', content: '' })

const reset = () => {
  form.title = props.announcement?.title ?? ''
  content.value = parseStoredRichText(props.announcement?.content)
  errors.title = ''
  errors.content = ''
}

watch(
  () => [props.open, props.announcement],
  () => {
    if (props.open) reset()
  },
  { immediate: true },
)

const submit = () => {
  errors.title = ''
  errors.content = ''

  if (!form.title.trim()) {
    errors.title = 'Vui lòng nhập tiêu đề thông báo.'
  }

  if (!hasRichTextContent(content.value)) {
    errors.content = 'Vui lòng nhập nội dung thông báo.'
  }

  if (errors.title || errors.content) return

  emit('submit', {
    title: form.title.trim(),
    content: serializeRichText(content.value),
  })
}
</script>

<template>
  <BaseModal
    :open="open"
    :title="announcement ? 'Chỉnh sửa thông báo' : 'Tạo thông báo'"
    :description="
      announcement
        ? 'Cập nhật nội dung thông báo dành cho học viên.'
        : 'Đăng thông tin mới đến các học viên trong khóa học.'
    "
    :loading="loading"
    max-width="max-w-3xl"
    @close="emit('close')"
  >
    <form class="space-y-5" @submit.prevent="submit">
      <BaseAlert v-if="serverMessage">
        {{ serverMessage }}
      </BaseAlert>

      <BaseInput
        v-model="form.title"
        label="Tiêu đề"
        placeholder="Ví dụ: Thay đổi lịch học tuần này"
        maxlength="255"
        required
        :disabled="loading"
        :error="errors.title"
      />

      <div>
        <div class="mb-2 flex items-center justify-between gap-3">
          <label class="text-sm font-semibold text-app-text">
            Nội dung <span class="text-danger">*</span>
          </label>

          <span class="text-xs text-app-text-muted">
            Hỗ trợ tiêu đề, danh sách, code và trích dẫn
          </span>
        </div>

        <RichTextEditor
          v-model="content"
          :disabled="loading"
        />

        <p v-if="errors.content" class="mt-2 text-sm text-danger">
          {{ errors.content }}
        </p>
      </div>

      <div class="flex justify-end gap-3 border-t border-app-border pt-5">
        <BaseButton
          type="button"
          variant="secondary"
          :disabled="loading"
          @click="emit('close')"
        >
          Hủy
        </BaseButton>

        <BaseButton type="submit" :loading="loading">
          {{ announcement ? 'Lưu thay đổi' : 'Đăng thông báo' }}
        </BaseButton>
      </div>
    </form>
  </BaseModal>
</template>