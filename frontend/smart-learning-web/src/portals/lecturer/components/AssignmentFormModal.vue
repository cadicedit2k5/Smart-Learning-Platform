<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'

import { BaseButton, BaseInput, BaseModal } from '@/shared/components'
import type { Assignment } from '@/shared/assignment/types'
import type { TopicContent } from '@/shared/course-content'
import {
  createEmptyRichText,
  parseStoredRichText,
  RichTextEditor,
  serializeRichText,
} from '@/shared/rich-text'

import type { AssignmentInput } from '../api/assignmentApi'

const props = withDefaults(
  defineProps<{
    open: boolean
    assignment?: Assignment | null
    loading?: boolean
    deadlineLocked?: boolean
  }>(),
  {
    assignment: null,
    loading: false,
    deadlineLocked: false,
  },
)

const emit = defineEmits<{
  close: []
  submit: [input: AssignmentInput]
}>()

const form = reactive({
  title: '',
  dueAt: '',
  maxScore: '10',
})

const description = ref<TopicContent>(createEmptyRichText())

const errors = reactive({
  title: '',
  dueAt: '',
  maxScore: '',
})

const isEditing = computed(() => Boolean(props.assignment))

const toLocalDateTime = (value?: string | null) => {
  if (!value) return ''

  const date = new Date(value)
  return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 16)
}

const reset = () => {
  form.title = props.assignment?.title ?? ''
  description.value = parseStoredRichText(props.assignment?.description)
  form.dueAt = toLocalDateTime(props.assignment?.dueAt)
  form.maxScore = String(props.assignment?.maxScore ?? 10)

  errors.title = ''
  errors.dueAt = ''
  errors.maxScore = ''
}

watch(
  () => [props.open, props.assignment],
  () => {
    if (props.open) reset()
  },
  { immediate: true },
)

const validate = () => {
  errors.title = ''
  errors.dueAt = ''
  errors.maxScore = ''

  if (!form.title.trim()) errors.title = 'Vui lòng nhập tên bài tập.'
  if (!form.dueAt) errors.dueAt = 'Vui lòng chọn hạn nộp.'

  const score = Number(form.maxScore)

  if (!Number.isFinite(score) || score <= 0) {
    errors.maxScore = 'Điểm tối đa phải lớn hơn 0.'
  }

  return !(errors.title || errors.dueAt || errors.maxScore)
}

const submit = () => {
  if (!validate()) return

  const serializedDescription = serializeRichText(description.value)

  emit('submit', {
    title: form.title.trim(),
    description: serializedDescription || undefined,
    dueAt: new Date(form.dueAt).toISOString(),
    maxScore: Number(form.maxScore),
  })
}
</script>

<template>
  <BaseModal
    :open="open"
    :title="isEditing ? 'Chỉnh sửa bài tập' : 'Tạo bài tập'"
    :description="
      isEditing
        ? 'Cập nhật yêu cầu và điểm của bài tập.'
        : 'Tạo một hoạt động đánh giá mới cho khóa học.'
    "
    :loading="loading"
    max-width="max-w-3xl"
    @close="emit('close')"
  >
    <form class="space-y-5" @submit.prevent="submit">
      <BaseInput
        v-model="form.title"
        label="Tên bài tập"
        placeholder="Ví dụ: Xây dựng REST API"
        maxlength="255"
        required
        :disabled="loading"
        :error="errors.title"
      />

      <div>
        <div class="mb-2 flex items-center justify-between gap-3">
          <label class="text-sm font-semibold text-app-text">
            Nội dung và hướng dẫn
          </label>

          <span class="text-xs text-app-text-muted">
            Có thể định dạng nội dung bằng Tiptap
          </span>
        </div>

        <RichTextEditor v-model="description" :disabled="loading" />
      </div>

      <div class="grid gap-5 sm:grid-cols-2">
        <BaseInput
          v-model="form.dueAt"
          type="datetime-local"
          :label="deadlineLocked ? 'Hạn nộp (sử dụng chức năng Gia hạn để thay đổi)' : 'Hạn nộp'"
          required
          :disabled="loading || deadlineLocked"
          :error="errors.dueAt"
        />

        <BaseInput
          v-model="form.maxScore"
          type="number"
          label="Điểm tối đa"
          min="0.1"
          step="0.1"
          required
          :disabled="loading"
          :error="errors.maxScore"
        />
      </div>

      <div class="flex justify-end gap-3 border-t border-app-border pt-5">
        <BaseButton type="button" variant="secondary" :disabled="loading" @click="emit('close')">
          Hủy
        </BaseButton>

        <BaseButton type="submit" :loading="loading">
          {{ isEditing ? 'Lưu thay đổi' : 'Tạo bài tập' }}
        </BaseButton>
      </div>
    </form>
  </BaseModal>
</template>