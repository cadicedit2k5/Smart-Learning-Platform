<script setup lang="ts">
import {
  reactive,
  watch,
} from 'vue'

import type {
  Course,
  CourseVisibility,
} from '@/shared/course'

import {
  BaseAlert,
  BaseButton,
  BaseModal,
} from '@/shared/components'

import type {
  AdminUpdateCourseRequest,
} from '../../api/courseApi'

const props = defineProps<{
  open: boolean
  course: Course | null
  loading: boolean
  serverErrors: Record<string, string>
  serverMessage: string
}>()

const emit = defineEmits<{
  close: []
  update: [
    id: string,
    request: AdminUpdateCourseRequest,
  ]
}>()

const form = reactive({
  title: '',
  description: '',
  level: '',
  visibility: 'PRIVATE' as CourseVisibility,
})

watch(
  () => [
    props.open,
    props.course,
  ],
  () => {
    if (!props.open || !props.course) {
      return
    }

    form.title = props.course.title
    form.description = props.course.description || ''
    form.level = props.course.level || ''
    form.visibility = props.course.visibility
  },
  {
    immediate: true,
  },
)

const submit = () => {
  if (!props.course) {
    return
  }

  const request: AdminUpdateCourseRequest = {}

  const title = form.title.trim()
  const description = form.description.trim()
  const level = form.level.trim()

  if (title !== props.course.title) {
    request.title = title
  }

  if (description !==(props.course.description || '')) {
    request.description = description
  }

  if (level !==(props.course.level || '')
  ) {
    request.level = level
  }

  if (form.visibility !== props.course.visibility) {
    request.visibility = form.visibility
  }

  emit(
    'update',
    props.course.id,
    request,
  )
}
</script>

<template>
  <BaseModal
    :open="open"
    title="Chỉnh sửa môn học"
    @close="$emit('close')"
  >
    <form
      class="space-y-5"
      @submit.prevent="submit"
    >
      <BaseAlert
        v-if="serverMessage"
        variant="error"
      >
        {{ serverMessage }}
      </BaseAlert>

      <!-- Title -->
      <div class="space-y-1.5">
        <label
          class="text-sm font-medium text-app-text"
        >
          Tên môn học
        </label>

        <input
          v-model="form.title"
          type="text"
          maxlength="255"
          class="h-11 w-full rounded-control border border-app-border bg-app-surface px-3 text-sm outline-none focus:border-secondary"
        />

        <p
          v-if="serverErrors.title"
          class="text-xs text-danger"
        >
          {{ serverErrors.title }}
        </p>
      </div>

      <!-- Description -->
      <div class="space-y-1.5">
        <label
          class="text-sm font-medium text-app-text"
        >
          Mô tả
        </label>

        <textarea
          v-model="form.description"
          rows="5"
          maxlength="5000"
          class="w-full rounded-control border border-app-border bg-app-surface p-3 text-sm outline-none focus:border-secondary"
        />

        <p
          v-if="serverErrors.description"
          class="text-xs text-danger"
        >
          {{ serverErrors.description }}
        </p>
      </div>

      <!-- Level -->
      <div class="space-y-1.5">
        <label
          class="text-sm font-medium text-app-text"
        >
          Cấp độ
        </label>

        <input
          v-model="form.level"
          type="text"
          maxlength="50"
          class="h-11 w-full rounded-control border border-app-border bg-app-surface px-3 text-sm outline-none focus:border-secondary"
        />

        <p
          v-if="serverErrors.level"
          class="text-xs text-danger"
        >
          {{ serverErrors.level }}
        </p>
      </div>

      <!-- Visibility -->
      <div class="space-y-1.5">
        <label
          class="text-sm font-medium text-app-text"
        >
          Phạm vi truy cập
        </label>

        <select
          v-model="form.visibility"
          class="h-11 w-full rounded-control border border-app-border bg-app-surface px-3 text-sm"
        >
          <option value="PUBLIC">
            Công khai
          </option>

          <option value="PRIVATE">
            Riêng tư
          </option>

          <option value="INVITE_ONLY">
            Chỉ lời mời
          </option>
        </select>
      </div>

      <div
        class="flex justify-end gap-2 border-t border-app-border pt-4"
      >
        <BaseButton
          type="button"
          variant="secondary"
          :disabled="loading"
          @click="$emit('close')"
        >
          Hủy
        </BaseButton>

        <BaseButton
          type="submit"
          :disabled="loading"
        >
          {{
            loading
              ? 'Đang lưu...'
              : 'Lưu thay đổi'
          }}
        </BaseButton>
      </div>
    </form>
  </BaseModal>
</template>