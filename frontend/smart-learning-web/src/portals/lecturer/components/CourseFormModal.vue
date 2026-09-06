<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { X } from 'lucide-vue-next'

import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import BaseInput from '@/shared/components/BaseInput.vue'

import type { CourseInput } from '../api/courseApi'
import type { Course } from '@/shared/course'

const props = withDefaults(
  defineProps<{
    open: boolean
    course?: Course | null
    loading?: boolean
    serverMessage?: string
    serverErrors?: Record<string, string>
  }>(),
  {
    course: null,
    loading: false,
    serverMessage: '',
    serverErrors: () => ({}),
  },
)

const emit = defineEmits<{
  close: []
  submit: [input: CourseInput]
}>()

const form = reactive<CourseInput>({
  title: '',
  description: '',
  level: '',
  visibility: 'PRIVATE',
})

const localErrors = reactive<Record<string, string>>({})

const isEditing = computed(() => props.course !== null)

const clearLocalErrors = () => {
  Object.keys(localErrors).forEach((field) => {
    delete localErrors[field]
  })
}

const resetForm = () => {
  form.title = props.course?.title ?? ''
  form.description = props.course?.description ?? ''
  form.level = props.course?.level ?? ''
  form.visibility = props.course?.visibility ?? 'PRIVATE'
  clearLocalErrors()
}

watch(
  () => [props.open, props.course],
  ([open]) => {
    if (open) {
      resetForm()
    }
  },
)

const fieldError = (field: string) => {
  return localErrors[field] ?? props.serverErrors[field]
}

const validate = () => {
  clearLocalErrors()

  if (!form.title.trim()) {
    localErrors.title = 'Vui lòng nhập tên khóa học.'
  } else if (form.title.trim().length > 255) {
    localErrors.title = 'Tên khóa học không được vượt quá 255 ký tự.'
  }

  if (form.description.length > 5000) {
    localErrors.description = 'Mô tả không được vượt quá 5.000 ký tự.'
  }

  if (form.level.length > 50) {
    localErrors.level = 'Cấp độ không được vượt quá 50 ký tự.'
  }

  return Object.keys(localErrors).length === 0
}

const submit = () => {
  if (!validate()) {
    return
  }

  emit('submit', {
    title: form.title.trim(),
    description: form.description.trim(),
    level: form.level.trim(),
    visibility: form.visibility,
  })
}

const close = () => {
  if (!props.loading) {
    emit('close')
  }
}
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-200"
      enter-from-class="opacity-0"
      leave-active-class="transition duration-150"
      leave-to-class="opacity-0"
    >
      <div
        v-if="open"
        class="fixed inset-0 z-[70] flex items-center justify-center bg-slate-950/50 p-4"
        @mousedown.self="close"
      >
        <section
          role="dialog"
          aria-modal="true"
          aria-labelledby="course-form-title"
          class="max-h-[calc(100vh-2rem)] w-full max-w-2xl overflow-y-auto rounded-panel bg-app-surface shadow-overlay"
        >
          <header
            class="flex items-start justify-between gap-4 border-b border-app-border px-5 py-4 sm:px-6"
          >
            <div>
              <h2 id="course-form-title" class="font-heading text-xl font-bold text-app-text">
                {{ isEditing ? 'Cập nhật khóa học' : 'Tạo khóa học mới' }}
              </h2>

              <p class="mt-1 text-sm text-app-text-muted">
                {{
                  isEditing
                    ? 'Điều chỉnh thông tin hiển thị và quyền truy cập khóa học.'
                    : 'Bắt đầu ở trạng thái bản nháp và xuất bản khi nội dung đã sẵn sàng.'
                }}
              </p>
            </div>

            <button
              type="button"
              aria-label="Đóng"
              class="flex h-9 w-9 shrink-0 items-center justify-center rounded-control text-app-text-muted hover:bg-app-surface-muted"
              :disabled="loading"
              @click="close"
            >
              <X :size="20" />
            </button>
          </header>

          <form class="space-y-5 p-5 sm:p-6" @submit.prevent="submit">
            <BaseAlert v-if="serverMessage">
              {{ serverMessage }}
            </BaseAlert>

            <BaseInput
              v-model="form.title"
              label="Tên khóa học"
              placeholder="Ví dụ: Nhập môn trí tuệ nhân tạo"
              maxlength="255"
              required
              :disabled="loading"
              :error="fieldError('title')"
            />

            <div class="space-y-2">
              <label for="course-description" class="block text-sm font-semibold text-app-text">
                Mô tả
              </label>

              <textarea
                id="course-description"
                v-model="form.description"
                rows="5"
                maxlength="5000"
                placeholder="Mục tiêu, nội dung chính và đối tượng phù hợp..."
                class="w-full resize-y rounded-control border bg-app-surface px-3 py-2.5 text-sm outline-none transition focus:border-secondary focus:ring-2 focus:ring-secondary/20 disabled:bg-app-surface-muted"
                :class="fieldError('description') ? 'border-danger' : 'border-app-border'"
                :disabled="loading"
                :aria-invalid="fieldError('description') ? 'true' : undefined"
              />

              <div class="flex justify-between gap-3 text-xs">
                <p v-if="fieldError('description')" role="alert" class="text-danger">
                  {{ fieldError('description') }}
                </p>
                <span class="ml-auto text-app-text-muted">{{ form.description.length }}/5.000</span>
              </div>
            </div>

            <div class="grid gap-5 sm:grid-cols-2">
              <BaseInput
                v-model="form.level"
                label="Cấp độ"
                placeholder="Cơ bản, Trung cấp..."
                maxlength="50"
                :disabled="loading"
                :error="fieldError('level')"
              />

              <div class="space-y-2">
                <label for="course-visibility" class="block text-sm font-semibold text-app-text">
                  Quyền truy cập
                </label>

                <select
                  id="course-visibility"
                  v-model="form.visibility"
                  class="h-11 w-full rounded-control border border-app-border bg-app-surface px-3 text-sm outline-none focus:border-secondary focus:ring-2 focus:ring-secondary/20 disabled:bg-app-surface-muted"
                  :disabled="loading"
                >
                  <option value="PRIVATE">Riêng tư</option>
                  <option value="INVITE_ONLY">Chỉ người được mời</option>
                  <option value="PUBLIC">Công khai</option>
                </select>

                <p v-if="fieldError('visibility')" role="alert" class="text-sm text-danger">
                  {{ fieldError('visibility') }}
                </p>
              </div>
            </div>

            <footer
              class="flex flex-col-reverse gap-3 border-t border-app-border pt-5 sm:flex-row sm:justify-end"
            >
              <BaseButton variant="secondary" :disabled="loading" @click="close"> Hủy </BaseButton>

              <BaseButton type="submit" :loading="loading">
                {{ isEditing ? 'Lưu thay đổi' : 'Tạo khóa học' }}
              </BaseButton>
            </footer>
          </form>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>
