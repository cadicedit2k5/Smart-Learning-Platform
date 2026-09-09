<script setup lang="ts">
import { computed, reactive, watch } from 'vue'

import {
  BaseAlert,
  BaseButton,
  BaseInput,
  BaseModal,
} from '@/shared/components'

import type { Assignment } from '@/shared/assignment/types'
import type { AssignmentInput } from '../api/assignmentApi'

const props = withDefaults(
  defineProps<{
    open: boolean
    assignment?: Assignment | null
    loading?: boolean
    serverMessage?: string
  }>(),
  {
    assignment: null,
    loading: false,
    serverMessage: '',
  },
)

const emit = defineEmits<{
  close: []
  submit: [input: AssignmentInput]
}>()

const form = reactive({
  title: '',
  description: '',
  dueAt: '',
  maxScore: '10',
})

const errors = reactive({
  title: '',
  dueAt: '',
  maxScore: '',
})

const isEditing = computed(() => Boolean(props.assignment))

const toLocalDateTime = (value?: string | null) => {
  if (!value) return ''

  const date = new Date(value)

  const local = new Date(
    date.getTime() - date.getTimezoneOffset() * 60_000,
  )

  return local.toISOString().slice(0, 16)
}

const reset = () => {
  form.title = props.assignment?.title ?? ''
  form.description = props.assignment?.description ?? ''
  form.dueAt = toLocalDateTime(props.assignment?.dueAt)
  form.maxScore = String(props.assignment?.maxScore ?? 10)

  errors.title = ''
  errors.dueAt = ''
  errors.maxScore = ''
}

watch(
  () => [props.open, props.assignment],
  () => {
    if (props.open) {
      reset()
    }
  },
  {
    immediate: true,
  },
)

const validate = () => {
  errors.title = ''
  errors.dueAt = ''
  errors.maxScore = ''

  if (!form.title.trim()) {
    errors.title = 'Vui lòng nhập tên bài tập.'
  }

  if (!form.dueAt) {
    errors.dueAt = 'Vui lòng chọn hạn nộp.'
  }

  const score = Number(form.maxScore)

  if (!Number.isFinite(score) || score <= 0) {
    errors.maxScore = 'Điểm tối đa phải lớn hơn 0.'
  }

  return !(
    errors.title ||
    errors.dueAt ||
    errors.maxScore
  )
}

const submit = () => {
  if (!validate()) return

  emit('submit', {
    title: form.title.trim(),
    description: form.description.trim() || undefined,
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
        ? 'Cập nhật yêu cầu, hạn nộp và điểm tối đa.'
        : 'Tạo bài tập mới cho học viên trong khóa học.'
    "
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
        label="Tên bài tập"
        placeholder="Ví dụ: Bài tập REST API"
        maxlength="255"
        required
        :disabled="loading"
        :error="errors.title"
      />

      <label class="block">
        <span class="mb-2 block text-sm font-semibold text-app-text">
          Mô tả bài tập
        </span>

        <textarea
          v-model="form.description"
          rows="7"
          maxlength="10000"
          :disabled="loading"
          placeholder="Nhập yêu cầu và hướng dẫn làm bài..."
          class="w-full resize-y rounded-control border border-app-border bg-app-surface px-3 py-2.5 text-sm text-app-text outline-none transition focus:border-secondary focus:ring-2 focus:ring-secondary/20 disabled:opacity-60"
        />
      </label>

      <div class="grid gap-5 sm:grid-cols-2">
        <BaseInput
          v-model="form.dueAt"
          type="datetime-local"
          label="Hạn nộp"
          required
          :disabled="loading"
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
        <BaseButton
          type="button"
          variant="secondary"
          :disabled="loading"
          @click="emit('close')"
        >
          Hủy
        </BaseButton>

        <BaseButton
          type="submit"
          :loading="loading"
        >
          {{ isEditing ? 'Lưu thay đổi' : 'Tạo bài tập' }}
        </BaseButton>
      </div>
    </form>
  </BaseModal>
</template>