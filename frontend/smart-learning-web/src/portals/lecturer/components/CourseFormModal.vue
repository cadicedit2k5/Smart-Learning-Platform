<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ImagePlus, X } from 'lucide-vue-next'

import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import BaseInput from '@/shared/components/BaseInput.vue'
import CourseCover from '@/shared/course/CourseCover.vue'
import type { Course } from '@/shared/course'
import type { TopicContent } from '@/shared/course-content'
import {
  createEmptyRichText,
  parseStoredRichText,
  RichTextEditor,
  richTextToPlainText,
  serializeRichText,
} from '@/shared/rich-text'

import type { CourseInput } from '../api/courseApi'

const props = withDefaults(defineProps<{
  open: boolean
  course?: Course | null
  loading?: boolean
  serverMessage?: string
  serverErrors?: Record<string, string>
}>(), {
  course: null,
  loading: false,
  serverMessage: '',
  serverErrors: () => ({}),
})

const emit = defineEmits<{
  close: []
  submit: [input: CourseInput]
}>()

const form = reactive<CourseInput>({
  title: '',
  description: '',
  level: '',
  visibility: 'INVITE_ONLY',
  image: null,
})

const descriptionContent = ref<TopicContent>(createEmptyRichText())
const imageInput = ref<HTMLInputElement | null>(null)
const imagePreviewUrl = ref('')
const localErrors = reactive<Record<string, string>>({})

const isEditing = computed(() => props.course !== null)
const descriptionLength = computed(() => richTextToPlainText(descriptionContent.value).length)

const clearLocalErrors = () => {
  Object.keys(localErrors).forEach((field) => delete localErrors[field])
}

const revokeImagePreview = () => {
  if (!imagePreviewUrl.value) return
  URL.revokeObjectURL(imagePreviewUrl.value)
  imagePreviewUrl.value = ''
}

const resetImage = () => {
  revokeImagePreview()
  form.image = null

  if (imageInput.value) imageInput.value.value = ''
}

const resetForm = () => {
  form.title = props.course?.title ?? ''
  form.level = props.course?.level ?? ''
  form.visibility = props.course?.visibility ?? 'INVITE_ONLY'
  form.image = null
  descriptionContent.value = parseStoredRichText(props.course?.description)

  clearLocalErrors()
  resetImage()
}

watch(() => [props.open, props.course] as const, ([open]) => {
  if (open) resetForm()
})

const fieldError = (field: string) => localErrors[field] ?? props.serverErrors[field]

const validate = () => {
  clearLocalErrors()

  if (!form.title.trim()) {
    localErrors.title = 'Vui lòng nhập tên khóa học.'
  } else if (form.title.trim().length > 255) {
    localErrors.title = 'Tên khóa học không được vượt quá 255 ký tự.'
  }

  if (descriptionLength.value > 5000) {
    localErrors.description = 'Mô tả không được vượt quá 5.000 ký tự.'
  }

  if (form.level.length > 50) {
    localErrors.level = 'Cấp độ không được vượt quá 50 ký tự.'
  }

  return Object.keys(localErrors).length === 0
}

const selectImage = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0] ?? null

  delete localErrors.image
  if (!file) return

  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    localErrors.image = 'Ảnh chỉ hỗ trợ JPG, PNG hoặc WebP.'
    input.value = ''
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    localErrors.image = 'Ảnh không được vượt quá 5 MB.'
    input.value = ''
    return
  }

  revokeImagePreview()

  form.image = file
  imagePreviewUrl.value = URL.createObjectURL(file)
}

const clearSelectedImage = () => {
  resetImage()
  delete localErrors.image
}

const submit = () => {
  if (!validate()) return

  emit('submit', {
    title: form.title.trim(),
    description: serializeRichText(descriptionContent.value),
    level: form.level.trim(),
    visibility: form.visibility,
    image: form.image,
  })
}

const close = () => {
  if (!props.loading) emit('close')
}

onBeforeUnmount(revokeImagePreview)
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
          class="max-h-[calc(100vh-2rem)] w-full max-w-3xl overflow-y-auto rounded-panel bg-app-surface shadow-overlay"
        >
          <header class="flex items-start justify-between gap-4 border-b border-app-border px-5 py-4 sm:px-6">
            <div>
              <h2 id="course-form-title" class="font-heading text-xl font-bold text-app-text">
                {{ isEditing ? 'Cập nhật khóa học' : 'Tạo khóa học mới' }}
              </h2>

              <p class="mt-1 text-sm text-app-text-muted">
                {{
                  isEditing
                    ? 'Cập nhật thông tin hiển thị của khóa học.'
                    : 'Tạo thông tin cơ bản trước khi xây dựng nội dung học.'
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

          <form class="space-y-6 p-5 sm:p-6" @submit.prevent="submit">
            <BaseAlert v-if="serverMessage">
              {{ serverMessage }}
            </BaseAlert>

            <div class="space-y-3">
              <div>
                <label class="block text-sm font-semibold text-app-text">Ảnh khóa học</label>
                <p class="mt-1 text-xs text-app-text-muted">
                  JPG, PNG hoặc WebP. Tối đa 5 MB. Nên sử dụng ảnh tỷ lệ 16:9.
                </p>
              </div>

              <div class="max-w-xl overflow-hidden rounded-card border border-app-border">
                <CourseCover
                  :image-url="imagePreviewUrl || course?.imageUrl"
                  :title="form.title || 'Ảnh khóa học'"
                />
              </div>

              <div class="flex flex-wrap gap-2">
                <label
                  class="inline-flex h-10 cursor-pointer items-center gap-2 rounded-control bg-secondary px-4 text-sm font-semibold text-white transition hover:opacity-90"
                  :class="{ 'pointer-events-none opacity-60': loading }"
                >
                  <ImagePlus :size="17" />
                  {{ form.image ? 'Chọn ảnh khác' : course?.imageUrl ? 'Thay ảnh' : 'Chọn ảnh' }}

                  <input
                    ref="imageInput"
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    class="sr-only"
                    :disabled="loading"
                    @change="selectImage"
                  />
                </label>

                <BaseButton
                  v-if="form.image"
                  type="button"
                  variant="secondary"
                  :disabled="loading"
                  @click="clearSelectedImage"
                >
                  Bỏ ảnh đã chọn
                </BaseButton>
              </div>

              <p v-if="fieldError('image')" role="alert" class="text-sm text-danger">
                {{ fieldError('image') }}
              </p>
            </div>

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
              <div class="flex items-end justify-between gap-3">
                <div>
                  <label class="block text-sm font-semibold text-app-text">Giới thiệu khóa học</label>
                  <p class="mt-1 text-xs text-app-text-muted">
                    Trình bày mục tiêu, nội dung chính và kiến thức người học sẽ nhận được.
                  </p>
                </div>

                <span class="shrink-0 text-xs text-app-text-muted">
                  {{ descriptionLength }}/5.000
                </span>
              </div>

              <RichTextEditor v-model="descriptionContent" :disabled="loading" />

              <p v-if="fieldError('description')" role="alert" class="text-sm text-danger">
                {{ fieldError('description') }}
              </p>
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
                  <option value="INVITE_ONLY">Chỉ người được mời</option>
                  <option value="PUBLIC">Công khai</option>
                </select>

                <p v-if="fieldError('visibility')" role="alert" class="text-sm text-danger">
                  {{ fieldError('visibility') }}
                </p>
              </div>
            </div>

            <footer class="flex flex-col-reverse gap-3 border-t border-app-border pt-5 sm:flex-row sm:justify-end">
              <BaseButton type="button" variant="secondary" :disabled="loading" @click="close">
                Hủy
              </BaseButton>

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