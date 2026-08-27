<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { Upload, X } from 'lucide-vue-next'

import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import BaseInput from '@/shared/components/BaseInput.vue'

import { getTopics, type CourseChapter, type CourseTopic } from '../api/contentApi'
import type { CourseDocument, DocumentUpdateInput, DocumentUploadInput } from '../api/documentApi'

const props = withDefaults(
  defineProps<{
    open: boolean
    courseId: string
    document?: CourseDocument | null
    chapters?: CourseChapter[]
    loading?: boolean
    serverMessage?: string
    serverErrors?: Record<string, string>
  }>(),
  {
    document: null,
    chapters: () => [],
    loading: false,
    serverMessage: '',
    serverErrors: () => ({}),
  },
)

const emit = defineEmits<{
  close: []
  upload: [input: DocumentUploadInput]
  update: [input: DocumentUpdateInput]
}>()

const fileInput = ref<HTMLInputElement | null>(null)
const topics = ref<CourseTopic[]>([])
const localErrors = reactive<Record<string, string>>({})
const form = reactive({
  title: '',
  description: '',
  lifecycleStatus: 'ACTIVE' as 'ACTIVE' | 'ARCHIVED',
  chapterId: '',
  topicId: '',
  file: null as File | null,
})

const clearErrors = () => Object.keys(localErrors).forEach((key) => delete localErrors[key])

const loadTopics = async (chapterId: string) => {
  topics.value = chapterId ? await getTopics(props.courseId, chapterId) : []
}

const reset = async () => {
  form.title = props.document?.title ?? ''
  form.description = props.document?.description ?? ''
  form.lifecycleStatus = props.document?.lifecycleStatus ?? 'ACTIVE'
  form.chapterId = props.document?.chapterId ?? ''
  form.topicId = props.document?.topicId ?? ''
  form.file = null
  clearErrors()
  if (fileInput.value) fileInput.value.value = ''
  try {
    await loadTopics(form.chapterId)
  } catch {
    topics.value = []
  }
}

watch(
  () => [props.open, props.document] as const,
  ([open]) => {
    if (open) void reset()
  },
)

const changeChapter = async () => {
  form.topicId = ''
  try {
    await loadTopics(form.chapterId)
  } catch {
    topics.value = []
  }
}

const selectFile = (event: Event) => {
  form.file = (event.target as HTMLInputElement).files?.[0] ?? null
}

const submit = () => {
  clearErrors()
  if (!form.title.trim()) localErrors.title = 'Vui lòng nhập tiêu đề tài liệu.'
  if (form.title.trim().length > 255) localErrors.title = 'Tiêu đề không được vượt quá 255 ký tự.'
  if (form.description.length > 10000)
    localErrors.description = 'Mô tả không được vượt quá 10.000 ký tự.'
  if (!props.document && !form.file) localErrors.file = 'Vui lòng chọn tài liệu.'
  if (form.file && form.file.size > 50 * 1024 * 1024)
    localErrors.file = 'Tệp không được vượt quá 50 MB.'
  if (Object.keys(localErrors).length > 0) return

  const common = {
    title: form.title.trim(),
    description: form.description.trim(),
    chapterId: form.chapterId || undefined,
    topicId: form.topicId || undefined,
  }

  if (props.document) {
    emit('update', { ...common, lifecycleStatus: form.lifecycleStatus })
  } else if (form.file) {
    emit('upload', { ...common, file: form.file })
  }
}

const close = () => {
  if (!props.loading) emit('close')
}

const fieldError = (field: string) => localErrors[field] ?? props.serverErrors[field]
</script>

<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="fixed inset-0 z-[80] flex items-center justify-center bg-slate-950/50 p-4"
      @mousedown.self="close"
    >
      <section
        role="dialog"
        aria-modal="true"
        class="max-h-[calc(100vh-2rem)] w-full max-w-2xl overflow-y-auto rounded-panel bg-app-surface shadow-overlay"
      >
        <header
          class="flex items-start justify-between border-b border-app-border px-5 py-4 sm:px-6"
        >
          <div>
            <h2 class="font-heading text-xl font-bold text-app-text">
              {{ document ? 'Cập nhật tài liệu' : 'Tải tài liệu lên' }}
            </h2>
            <p class="mt-1 text-sm text-app-text-muted">
              {{ document ? 'Cập nhật thông tin và vị trí tài liệu.' : 'Dung lượng tối đa 50 MB.' }}
            </p>
          </div>
          <button
            type="button"
            class="rounded-control p-2 text-app-text-muted hover:bg-app-surface-muted"
            :disabled="loading"
            aria-label="Đóng"
            @click="close"
          >
            <X :size="20" />
          </button>
        </header>

        <form class="space-y-5 p-5 sm:p-6" @submit.prevent="submit">
          <BaseAlert v-if="serverMessage">{{ serverMessage }}</BaseAlert>
          <BaseInput
            v-model="form.title"
            label="Tiêu đề"
            maxlength="255"
            required
            :disabled="loading"
            :error="fieldError('title')"
          />
          <div class="space-y-2">
            <label for="document-form-description" class="text-sm font-semibold text-app-text"
              >Mô tả</label
            >
            <textarea
              id="document-form-description"
              v-model="form.description"
              rows="3"
              maxlength="10000"
              class="w-full rounded-control border bg-app-surface px-3 py-2.5 text-sm"
              :class="fieldError('description') ? 'border-danger' : 'border-app-border'"
              :disabled="loading"
            />
            <p v-if="fieldError('description')" class="text-sm text-danger">
              {{ fieldError('description') }}
            </p>
          </div>

          <div class="grid gap-4 sm:grid-cols-2">
            <div class="space-y-2">
              <label for="document-chapter" class="text-sm font-semibold text-app-text"
                >Chương</label
              >
              <select
                id="document-chapter"
                v-model="form.chapterId"
                class="h-11 w-full rounded-control border border-app-border bg-app-surface px-3 text-sm"
                :disabled="loading"
                @change="changeChapter"
              >
                <option value="">Không chọn</option>
                <option v-for="chapter in chapters" :key="chapter.id" :value="chapter.id">
                  {{ chapter.title }}
                </option>
              </select>
            </div>
            <div class="space-y-2">
              <label for="document-topic" class="text-sm font-semibold text-app-text">Chủ đề</label>
              <select
                id="document-topic"
                v-model="form.topicId"
                class="h-11 w-full rounded-control border border-app-border bg-app-surface px-3 text-sm"
                :disabled="loading || !form.chapterId"
              >
                <option value="">Không chọn</option>
                <option v-for="topic in topics" :key="topic.id" :value="topic.id">
                  {{ topic.title }}
                </option>
              </select>
            </div>
          </div>

          <div v-if="document" class="space-y-2">
            <label for="document-lifecycle" class="text-sm font-semibold text-app-text"
              >Trạng thái</label
            >
            <select
              id="document-lifecycle"
              v-model="form.lifecycleStatus"
              class="h-11 w-full rounded-control border border-app-border bg-app-surface px-3 text-sm"
              :disabled="loading"
            >
              <option value="ACTIVE">Đang hoạt động</option>
              <option value="ARCHIVED">Đã lưu trữ</option>
            </select>
          </div>

          <div v-else class="space-y-2">
            <p class="text-sm font-semibold text-app-text">
              Tệp tài liệu <span class="text-danger">*</span>
            </p>
            <label
              for="document-form-file"
              class="flex cursor-pointer items-center gap-3 rounded-control border border-dashed border-app-border bg-app-surface-muted p-4"
            >
              <Upload :size="20" class="text-secondary" />
              <span class="truncate text-sm text-app-text">{{
                form.file?.name ?? 'Chọn tệp từ thiết bị'
              }}</span>
            </label>
            <input
              id="document-form-file"
              ref="fileInput"
              type="file"
              class="sr-only"
              :disabled="loading"
              @change="selectFile"
            />
            <p v-if="fieldError('file')" class="text-sm text-danger">{{ fieldError('file') }}</p>
          </div>

          <footer class="flex justify-end gap-3 border-t border-app-border pt-5">
            <BaseButton variant="secondary" :disabled="loading" @click="close">Hủy</BaseButton>
            <BaseButton type="submit" :loading="loading">{{
              document ? 'Lưu thay đổi' : 'Tải lên'
            }}</BaseButton>
          </footer>
        </form>
      </section>
    </div>
  </Teleport>
</template>
