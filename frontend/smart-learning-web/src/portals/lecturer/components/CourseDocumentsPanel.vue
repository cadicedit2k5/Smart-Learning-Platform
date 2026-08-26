<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import {
  AlertTriangle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock3,
  FileText,
  RefreshCw,
  Upload,
  X,
} from 'lucide-vue-next'

import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import BaseInput from '@/shared/components/BaseInput.vue'
import type { PaginatedData } from '@/shared/api'

import {
  getDocuments,
  uploadDocument,
  type CourseDocument,
  type DocumentProcessingStatus,
} from '../api/courseApi'
import { useLecturerApiError } from '../composables/useLecturerApiError'

const props = defineProps<{
  courseId: string
}>()

const MAX_FILE_SIZE = 50 * 1024 * 1024
const POLL_INTERVAL = 5000

const { handleApiError } = useLecturerApiError()

const documentsPage = ref<PaginatedData<CourseDocument>>({
  content: [],
  pageable: {
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  },
})
const requestedPage = ref(1)
const loading = ref(true)
const polling = ref(false)
const loadError = ref('')
const uploadOpen = ref(false)
const uploading = ref(false)
const uploadMessage = ref('')
const uploadErrors = reactive<Record<string, string>>({})
const fileInput = ref<HTMLInputElement | null>(null)
const uploadForm = reactive({
  title: '',
  description: '',
  file: null as File | null,
})

let pollTimer: ReturnType<typeof setTimeout> | undefined

const totalPages = computed(() => {
  return Math.max(1, documentsPage.value.pageable.totalPages)
})

const hasProcessingDocuments = computed(() => {
  return documentsPage.value.content.some((document) => {
    return ['UPLOADED', 'QUEUED', 'PROCESSING'].includes(document.version.processingStatus)
  })
})

const clearPollTimer = () => {
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = undefined
  }
}

const schedulePoll = () => {
  clearPollTimer()

  if (!hasProcessingDocuments.value) {
    return
  }

  pollTimer = setTimeout(() => {
    void loadDocuments(true)
  }, POLL_INTERVAL)
}

const loadDocuments = async (silent = false) => {
  if (silent) {
    polling.value = true
  } else {
    loading.value = true
  }

  loadError.value = ''

  try {
    documentsPage.value = await getDocuments(props.courseId, requestedPage.value)
  } catch (error) {
    loadError.value = handleApiError(error, 'Không thể tải danh sách tài liệu.').message
  } finally {
    loading.value = false
    polling.value = false
    schedulePoll()
  }
}

const goToPage = (page: number) => {
  if (page < 1 || page > totalPages.value || page === requestedPage.value) {
    return
  }

  clearPollTimer()
  requestedPage.value = page
  void loadDocuments()
}

const selectFile = (event: Event) => {
  const input = event.target as HTMLInputElement
  uploadForm.file = input.files?.[0] ?? null
  delete uploadErrors.file

  if (uploadForm.file && uploadForm.file.size > MAX_FILE_SIZE) {
    uploadErrors.file = 'Tệp không được vượt quá 50 MB.'
  }
}

const clearUploadErrors = () => {
  Object.keys(uploadErrors).forEach((field) => {
    delete uploadErrors[field]
  })
  uploadMessage.value = ''
}

const resetUpload = () => {
  uploadForm.title = ''
  uploadForm.description = ''
  uploadForm.file = null
  clearUploadErrors()

  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

const closeUpload = () => {
  if (!uploading.value) {
    uploadOpen.value = false
    resetUpload()
  }
}

const submitUpload = async () => {
  clearUploadErrors()

  if (!uploadForm.title.trim()) {
    uploadErrors.title = 'Vui lòng nhập tiêu đề tài liệu.'
  } else if (uploadForm.title.trim().length > 255) {
    uploadErrors.title = 'Tiêu đề không được vượt quá 255 ký tự.'
  }

  if (uploadForm.description.length > 10000) {
    uploadErrors.description = 'Mô tả không được vượt quá 10.000 ký tự.'
  }

  if (!uploadForm.file) {
    uploadErrors.file = 'Vui lòng chọn tài liệu cần tải lên.'
  } else if (uploadForm.file.size > MAX_FILE_SIZE) {
    uploadErrors.file = 'Tệp không được vượt quá 50 MB.'
  }

  if (Object.keys(uploadErrors).length > 0 || !uploadForm.file) {
    return
  }

  uploading.value = true

  try {
    await uploadDocument(props.courseId, {
      title: uploadForm.title.trim(),
      description: uploadForm.description.trim(),
      file: uploadForm.file,
    })

    uploadOpen.value = false
    resetUpload()
    requestedPage.value = 1
    await loadDocuments()
  } catch (error) {
    const parsed = handleApiError(error, 'Không thể tải tài liệu lên.')
    uploadMessage.value = parsed.message
    Object.assign(uploadErrors, parsed.fieldErrors)
  } finally {
    uploading.value = false
  }
}

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

const formatDate = (value: string) => {
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

const statusLabel: Record<DocumentProcessingStatus, string> = {
  UPLOADED: 'Đã tải lên',
  QUEUED: 'Đang chờ',
  PROCESSING: 'Đang lập chỉ mục',
  INDEXED: 'Sẵn sàng cho AI',
  FAILED: 'Xử lý thất bại',
}

const statusClass: Record<DocumentProcessingStatus, string> = {
  UPLOADED: 'bg-amber-50 text-amber-800',
  QUEUED: 'bg-amber-50 text-amber-800',
  PROCESSING: 'bg-secondary-soft text-secondary',
  INDEXED: 'bg-ai-soft text-ai',
  FAILED: 'bg-danger-soft text-on-danger-soft',
}

onMounted(() => {
  void loadDocuments()
})

onBeforeUnmount(() => {
  clearPollTimer()
})
</script>

<template>
  <section class="space-y-5">
    <header class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h2 class="font-heading text-xl font-bold text-app-text">Tài liệu khóa học</h2>
        <p class="mt-1 text-sm text-app-text-muted">
          Tài liệu đã lập chỉ mục sẽ được dùng làm nguồn cho trợ lý AI.
        </p>
      </div>

      <BaseButton @click="uploadOpen = true">
        <template #leading><Upload :size="18" /></template>
        Tải tài liệu lên
      </BaseButton>
    </header>

    <BaseAlert v-if="loadError">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <span>{{ loadError }}</span>
        <button type="button" class="font-semibold underline" @click="loadDocuments()">
          Thử lại
        </button>
      </div>
    </BaseAlert>

    <div class="overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card">
      <header class="flex items-center justify-between border-b border-app-border px-5 py-4">
        <div>
          <h3 class="font-semibold text-app-text">Danh sách tài liệu</h3>
          <p class="mt-1 text-xs text-app-text-muted">
            {{ documentsPage.pageable.totalElements }} tài liệu
          </p>
        </div>

        <span v-if="polling" class="flex items-center gap-2 text-xs font-medium text-secondary">
          <RefreshCw :size="14" class="animate-spin" />
          Đang cập nhật
        </span>
      </header>

      <div v-if="loading" class="space-y-3 p-5">
        <div
          v-for="index in 4"
          :key="index"
          class="h-20 animate-pulse rounded-control bg-app-surface-muted"
        />
      </div>

      <div v-else-if="documentsPage.content.length === 0" class="px-6 py-16 text-center">
        <FileText :size="40" class="mx-auto text-app-text-muted/50" />
        <h3 class="mt-4 font-semibold text-app-text">Chưa có tài liệu</h3>
        <p class="mt-1 text-sm text-app-text-muted">
          Tải lên PDF, DOCX hoặc tài liệu môn học để bắt đầu lập chỉ mục.
        </p>
      </div>

      <ul v-else class="divide-y divide-app-border">
        <li
          v-for="document in documentsPage.content"
          :key="document.id"
          class="flex gap-4 px-5 py-4"
        >
          <span
            class="flex h-11 w-11 shrink-0 items-center justify-center rounded-control bg-app-surface-muted text-app-text-muted"
          >
            <FileText :size="21" />
          </span>

          <div class="min-w-0 flex-1">
            <div class="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
              <div class="min-w-0">
                <p class="truncate font-semibold text-app-text">{{ document.title }}</p>
                <p class="mt-1 truncate text-sm text-app-text-muted">
                  {{ document.version.fileName }} · {{ formatFileSize(document.version.fileSize) }}
                </p>
              </div>

              <span
                class="inline-flex w-fit shrink-0 items-center gap-1.5 rounded-pill px-2.5 py-1 text-xs font-semibold"
                :class="statusClass[document.version.processingStatus]"
              >
                <CheckCircle2 v-if="document.version.processingStatus === 'INDEXED'" :size="13" />
                <AlertTriangle
                  v-else-if="document.version.processingStatus === 'FAILED'"
                  :size="13"
                />
                <Clock3 v-else :size="13" />
                {{ statusLabel[document.version.processingStatus] }}
              </span>
            </div>

            <p v-if="document.description" class="mt-2 line-clamp-2 text-sm text-app-text-muted">
              {{ document.description }}
            </p>

            <p class="mt-2 text-xs text-app-text-muted">
              Tải lên {{ formatDate(document.createdAt) }}
            </p>
          </div>
        </li>
      </ul>

      <footer
        v-if="documentsPage.pageable.totalElements > 0"
        class="flex items-center justify-between border-t border-app-border px-5 py-4 text-sm"
      >
        <span class="text-app-text-muted">Trang {{ requestedPage }} / {{ totalPages }}</span>

        <div class="flex gap-2">
          <button
            type="button"
            aria-label="Trang trước"
            class="flex h-9 w-9 items-center justify-center rounded-control border border-app-border disabled:opacity-40"
            :disabled="requestedPage <= 1 || loading"
            @click="goToPage(requestedPage - 1)"
          >
            <ChevronLeft :size="18" />
          </button>

          <button
            type="button"
            aria-label="Trang sau"
            class="flex h-9 w-9 items-center justify-center rounded-control border border-app-border disabled:opacity-40"
            :disabled="requestedPage >= totalPages || loading"
            @click="goToPage(requestedPage + 1)"
          >
            <ChevronRight :size="18" />
          </button>
        </div>
      </footer>
    </div>

    <Teleport to="body">
      <div
        v-if="uploadOpen"
        class="fixed inset-0 z-[70] flex items-center justify-center bg-slate-950/50 p-4"
        @mousedown.self="closeUpload"
      >
        <section
          role="dialog"
          aria-modal="true"
          aria-labelledby="upload-title"
          class="max-h-[calc(100vh-2rem)] w-full max-w-xl overflow-y-auto rounded-panel bg-app-surface shadow-overlay"
        >
          <header
            class="flex items-start justify-between border-b border-app-border px-5 py-4 sm:px-6"
          >
            <div>
              <h2 id="upload-title" class="font-heading text-xl font-bold text-app-text">
                Tải tài liệu lên
              </h2>
              <p class="mt-1 text-sm text-app-text-muted">Dung lượng tối đa 50 MB cho mỗi tệp.</p>
            </div>

            <button
              type="button"
              aria-label="Đóng"
              class="flex h-9 w-9 items-center justify-center rounded-control text-app-text-muted hover:bg-app-surface-muted"
              :disabled="uploading"
              @click="closeUpload"
            >
              <X :size="20" />
            </button>
          </header>

          <form class="space-y-5 p-5 sm:p-6" @submit.prevent="submitUpload">
            <BaseAlert v-if="uploadMessage">{{ uploadMessage }}</BaseAlert>

            <BaseInput
              v-model="uploadForm.title"
              label="Tiêu đề tài liệu"
              maxlength="255"
              required
              :disabled="uploading"
              :error="uploadErrors.title"
            />

            <div class="space-y-2">
              <label for="document-description" class="block text-sm font-semibold text-app-text"
                >Mô tả</label
              >
              <textarea
                id="document-description"
                v-model="uploadForm.description"
                rows="3"
                maxlength="10000"
                class="w-full rounded-control border bg-app-surface px-3 py-2.5 text-sm outline-none focus:border-secondary focus:ring-2 focus:ring-secondary/20"
                :class="uploadErrors.description ? 'border-danger' : 'border-app-border'"
                :disabled="uploading"
              />
              <p v-if="uploadErrors.description" role="alert" class="text-sm text-danger">
                {{ uploadErrors.description }}
              </p>
            </div>

            <div class="space-y-2">
              <p class="text-sm font-semibold text-app-text">
                Tệp tài liệu <span class="text-danger">*</span>
              </p>
              <label
                for="document-file"
                class="flex cursor-pointer items-center gap-3 rounded-control border border-dashed bg-app-surface-muted px-4 py-4 transition hover:border-secondary"
                :class="uploadErrors.file ? 'border-danger' : 'border-app-border'"
              >
                <span
                  class="flex h-10 w-10 items-center justify-center rounded-control bg-secondary-soft text-secondary"
                >
                  <Upload :size="19" />
                </span>
                <span class="min-w-0 text-sm">
                  <span class="block truncate font-medium text-app-text">{{
                    uploadForm.file?.name ?? 'Chọn tệp từ thiết bị'
                  }}</span>
                  <span class="text-app-text-muted">Tối đa 50 MB</span>
                </span>
              </label>
              <input
                id="document-file"
                ref="fileInput"
                type="file"
                class="sr-only"
                :disabled="uploading"
                @change="selectFile"
              />
              <p v-if="uploadErrors.file" role="alert" class="text-sm text-danger">
                {{ uploadErrors.file }}
              </p>
            </div>

            <footer
              class="flex flex-col-reverse gap-3 border-t border-app-border pt-5 sm:flex-row sm:justify-end"
            >
              <BaseButton variant="secondary" :disabled="uploading" @click="closeUpload"
                >Hủy</BaseButton
              >
              <BaseButton type="submit" :loading="uploading">Tải lên</BaseButton>
            </footer>
          </form>
        </section>
      </div>
    </Teleport>
  </section>
</template>
