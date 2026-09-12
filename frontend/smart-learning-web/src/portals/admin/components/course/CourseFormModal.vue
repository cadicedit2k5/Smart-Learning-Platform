<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'

import type { Course, CourseVisibility } from '@/shared/course'
import type { TopicContent } from '@/shared/course-content'
import { BaseAlert, BaseButton, BaseModal } from '@/shared/components'
import {
  parseStoredRichText,
  RichTextEditor,
  richTextToPlainText,
  serializeRichText,
} from '@/shared/rich-text'

import type { AdminUpdateCourseRequest } from '../../api/courseApi'

const props = defineProps<{
  open: boolean
  course: Course | null
  loading: boolean
  serverErrors: Record<string, string>
  serverMessage: string
}>()

const emit = defineEmits<{
  close: []
  update: [id: string, request: AdminUpdateCourseRequest]
}>()

const form = reactive({
  title: '',
  level: '',
  visibility: 'INVITE_ONLY' as CourseVisibility,
})

const descriptionContent = ref<TopicContent | null>(null)
const descriptionLength = computed(() => richTextToPlainText(descriptionContent.value).length)

watch(
  () => [props.open, props.course] as const,
  () => {
    if (!props.open || !props.course) return

    form.title = props.course.title
    form.level = props.course.level || ''
    form.visibility = props.course.visibility
    descriptionContent.value = parseStoredRichText(props.course.description)
  },
  { immediate: true },
)

const submit = () => {
  if (!props.course) return

  const request: AdminUpdateCourseRequest = {}
  const title = form.title.trim()
  const level = form.level.trim()
  const description = serializeRichText(descriptionContent.value)
  const originalDescription = serializeRichText(parseStoredRichText(props.course.description))

  if (title !== props.course.title) request.title = title
  if (description !== originalDescription) request.description = description
  if (level !== (props.course.level || '')) request.level = level
  if (form.visibility !== props.course.visibility) request.visibility = form.visibility

  emit('update', props.course.id, request)
}
</script>

<template>
  <BaseModal :open="open" title="Chỉnh sửa môn học" @close="$emit('close')">
    <form class="space-y-5" @submit.prevent="submit">
      <BaseAlert v-if="serverMessage" variant="error">
        {{ serverMessage }}
      </BaseAlert>

      <div class="space-y-1.5">
        <label class="text-sm font-medium text-app-text">Tên môn học</label>

        <input
          v-model="form.title"
          type="text"
          maxlength="255"
          class="h-11 w-full rounded-control border border-app-border bg-app-surface px-3 text-sm outline-none focus:border-secondary"
        />

        <p v-if="serverErrors.title" class="text-xs text-danger">
          {{ serverErrors.title }}
        </p>
      </div>

      <div class="space-y-2">
        <div class="flex items-end justify-between gap-3">
          <label class="text-sm font-medium text-app-text">Giới thiệu khóa học</label>

          <span class="text-xs text-app-text-muted">
            {{ descriptionLength }}/5.000
          </span>
        </div>

        <RichTextEditor v-model="descriptionContent" :disabled="loading" />

        <p v-if="serverErrors.description" class="text-xs text-danger">
          {{ serverErrors.description }}
        </p>
      </div>

      <div class="space-y-1.5">
        <label class="text-sm font-medium text-app-text">Cấp độ</label>

        <input
          v-model="form.level"
          type="text"
          maxlength="50"
          class="h-11 w-full rounded-control border border-app-border bg-app-surface px-3 text-sm outline-none focus:border-secondary"
        />

        <p v-if="serverErrors.level" class="text-xs text-danger">
          {{ serverErrors.level }}
        </p>
      </div>

      <div class="space-y-1.5">
        <label class="text-sm font-medium text-app-text">Phạm vi truy cập</label>

        <select
          v-model="form.visibility"
          class="h-11 w-full rounded-control border border-app-border bg-app-surface px-3 text-sm"
        >
          <option value="PUBLIC">Công khai</option>
          <option value="INVITE_ONLY">Chỉ lời mời</option>
        </select>
      </div>

      <div class="flex justify-end gap-2 border-t border-app-border pt-4">
        <BaseButton
          type="button"
          variant="secondary"
          :disabled="loading"
          @click="$emit('close')"
        >
          Hủy
        </BaseButton>

        <BaseButton type="submit" :disabled="loading || descriptionLength > 5000">
          {{ loading ? 'Đang lưu...' : 'Lưu thay đổi' }}
        </BaseButton>
      </div>
    </form>
  </BaseModal>
</template>