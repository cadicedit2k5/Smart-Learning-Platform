<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import {
  AlertTriangle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock3,
  Download,
  Eye,
  FileText,
  Pencil,
  RefreshCw,
  Search,
  Trash2,
  Upload,
} from 'lucide-vue-next'

import {
  BaseAlert,
  BaseButton,
  BaseInput,
  DocumentViewer,
} from '@/shared/components'
import type { PaginatedData } from '@/shared/api'
import type {
  CourseDocument,
  DocumentProcessingStatus,
} from '@/shared/document/types'
import {
  documentStatusLabel,
  formatFileSize,
} from '@/shared/document/presentation'
import { formatDateTime } from '@/shared/utils/date'

import {
  deleteDocument,
  downloadDocument,
  getDocument,
  getDocuments,
  updateDocument,
  uploadDocument,
  type DocumentUpdateInput,
  type DocumentUploadInput,
} from '../api/documentApi'
import { useLecturerApiError } from '../composables/useLecturerApiError'
import DocumentFormModal from './DocumentFormModal.vue'

const props = withDefaults(
  defineProps<{
    courseId: string
    canManage?: boolean
  }>(),
  {
    canManage: false,
  },
)

const { handleApiError } = useLecturerApiError()

const emptyPage = (): PaginatedData<CourseDocument> => ({
  content: [],
  pageable: {
    page: 1,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  },
})

const documentsPage = ref(emptyPage())

const requestedPage = ref(1)

const loading = ref(true)
const polling = ref(false)

const loadError = ref('')
const actionMessage = ref('')

const formOpen = ref(false)
const formLoading = ref(false)
const formMessage = ref('')
const formErrors = ref<Record<string, string>>({})
const editingDocument = ref<CourseDocument | null>(null)

const previewDocument = ref<CourseDocument | null>(null)
const previewBlob = ref<Blob | null>(null)
const previewingId = ref('')

const downloadingId = ref('')
const deletingId = ref('')

const filters = reactive({ keyword: '' })

let pollTimer: ReturnType<typeof setTimeout> | undefined

const totalPages = computed(() =>
  Math.max(1, documentsPage.value.pageable.totalPages),
)

const hasProcessingDocuments = computed(() =>
  documentsPage.value.content.some((item) =>
    ['UPLOADED', 'QUEUED'].includes(item.version.processingStatus),
  ),
)

const clearPollTimer = () => {
  if (!pollTimer) return

  clearTimeout(pollTimer)
  pollTimer = undefined
}

const schedulePoll = () => {
  clearPollTimer()

  if (!hasProcessingDocuments.value) return

  pollTimer = setTimeout(
    () => void loadDocuments(true),
    5000,
  )
}

const loadDocuments = async (silent = false) => {
  if (silent) polling.value = true
  else loading.value = true

  loadError.value = ''

  try {
    documentsPage.value = await getDocuments(props.courseId, {
      page: requestedPage.value,
      keyword: filters.keyword.trim() || undefined,
    })
  } catch (error) {
    loadError.value = handleApiError(error, 'Không thể tải danh sách tài liệu.').message
  } finally {
    loading.value = false
    polling.value = false
    schedulePoll()
  }
}

const applyFilters = () => {
  requestedPage.value = 1
  void loadDocuments()
}

const resetFilters = () => {
  filters.keyword = ''
  applyFilters()
}

const goToPage = (page: number) => {
  if (
    page < 1 ||
    page > totalPages.value ||
    page === requestedPage.value
  ) {
    return
  }

  requestedPage.value = page
  void loadDocuments()
}

const openUpload = () => {
  editingDocument.value = null
  formMessage.value = ''
  formErrors.value = {}
  formOpen.value = true
}

const openEdit = async (item: CourseDocument) => {
  formMessage.value = ''
  formErrors.value = {}
  actionMessage.value = ''

  try {
    editingDocument.value = await getDocument(
      props.courseId,
      item.id,
    )

    formOpen.value = true
  } catch (error) {
    actionMessage.value = handleApiError(
      error,
      'Không thể tải chi tiết tài liệu.',
    ).message
  }
}

const submitUpload = async (
  input: DocumentUploadInput,
) => {
  formLoading.value = true
  formMessage.value = ''
  formErrors.value = {}
  actionMessage.value = ''

  try {
    await uploadDocument(props.courseId, input)

    formOpen.value = false
    requestedPage.value = 1

    await loadDocuments()
  } catch (error) {
    const parsed = handleApiError(
      error,
      'Không thể tải tài liệu lên.',
    )

    formMessage.value = parsed.message
    formErrors.value = parsed.fieldErrors
  } finally {
    formLoading.value = false
  }
}

const submitUpdate = async (
  input: DocumentUpdateInput,
) => {
  if (!editingDocument.value) return

  formLoading.value = true
  formMessage.value = ''
  formErrors.value = {}
  actionMessage.value = ''

  try {
    await updateDocument(
      props.courseId,
      editingDocument.value.id,
      input,
    )

    formOpen.value = false
    editingDocument.value = null

    await loadDocuments()
  } catch (error) {
    const parsed = handleApiError(
      error,
      'Không thể cập nhật tài liệu.',
    )

    formMessage.value = parsed.message
    formErrors.value = parsed.fieldErrors
  } finally {
    formLoading.value = false
  }
}

const openDocument = async (
  item: CourseDocument,
) => {
  previewingId.value = item.id
  actionMessage.value = ''

  try {
    const blob = await downloadDocument(
      props.courseId,
      item.id,
    )

    previewBlob.value = blob
    previewDocument.value = item
  } catch (error) {
    actionMessage.value = handleApiError(
      error,
      'Không thể mở tài liệu.',
    ).message
  } finally {
    previewingId.value = ''
  }
}

const closeDocument = () => {
  previewDocument.value = null
  previewBlob.value = null
}

const handleDownload = async (
  item: CourseDocument,
) => {
  downloadingId.value = item.id
  actionMessage.value = ''

  try {
    const blob = await downloadDocument(
      props.courseId,
      item.id,
    )

    const objectUrl = URL.createObjectURL(blob)
    const anchor = window.document.createElement('a')

    anchor.href = objectUrl
    anchor.download = item.version.fileName || item.title
    anchor.click()

    URL.revokeObjectURL(objectUrl)
  } catch (error) {
    actionMessage.value = handleApiError(
      error,
      'Không thể tải tài liệu.',
    ).message
  } finally {
    downloadingId.value = ''
  }
}

const handleDelete = async (
  item: CourseDocument,
) => {
  if (
    !window.confirm(
      `Xóa tài liệu “${item.title}”?`,
    )
  ) {
    return
  }

  deletingId.value = item.id
  actionMessage.value = ''

  try {
    await deleteDocument(
      props.courseId,
      item.id,
    )

    if (
      documentsPage.value.content.length === 1 &&
      requestedPage.value > 1
    ) {
      requestedPage.value -= 1
    }

    await loadDocuments()
  } catch (error) {
    actionMessage.value = handleApiError(
      error,
      'Không thể xóa tài liệu.',
    ).message
  } finally {
    deletingId.value = ''
  }
}

const statusClass: Record<
  DocumentProcessingStatus,
  string
> = {
  UPLOADED: 'bg-amber-50 text-amber-800',
  QUEUED: 'bg-amber-50 text-amber-800',
  INDEXED: 'bg-ai-soft text-ai',
  FAILED: 'bg-danger-soft text-on-danger-soft',
}

watch(() => props.courseId, () => {
  clearPollTimer()
  closeDocument()
  requestedPage.value = 1
  documentsPage.value = emptyPage()
  filters.keyword = ''
  actionMessage.value = ''
  loadError.value = ''
  void loadDocuments()
})

onMounted(() => {
  void Promise.all([
    loadDocuments(),
  ])
})

onBeforeUnmount(() => {
  clearPollTimer()
  closeDocument()
})
</script>

<template>
  <DocumentViewer
    v-if="previewDocument && previewBlob"
    :blob="previewBlob"
    :title="previewDocument.title"
    :file-name="previewDocument.version.fileName"
    :mime-type="previewDocument.version.mimeType"
    @close="closeDocument"
  />

  <section class="space-y-5">
    <header
      class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between"
    >
      <div>
        <h2
          class="font-heading text-xl font-bold text-app-text"
        >
          Tài liệu khóa học
        </h2>

        <p
          class="mt-1 text-sm text-app-text-muted"
        >
          Tài liệu đã lập chỉ mục sẽ được dùng làm nguồn
          cho trợ lý AI.
        </p>
      </div>

      <BaseButton
        v-if="canManage"
        @click="openUpload"
      >
        <template #leading>
          <Upload :size="18" />
        </template>

        Tải tài liệu lên
      </BaseButton>
    </header>

    <BaseAlert v-if="actionMessage">
      {{ actionMessage }}
    </BaseAlert>

    <BaseAlert v-if="loadError">
      <div
        class="flex items-center justify-between gap-3"
      >
        <span>
          {{ loadError }}
        </span>

        <button
          type="button"
          class="font-semibold underline"
          @click="loadDocuments()"
        >
          Thử lại
        </button>
      </div>
    </BaseAlert>

    <form class="flex flex-col gap-3 rounded-card border border-app-border bg-app-surface p-4 shadow-card sm:flex-row" @submit.prevent="applyFilters">
      <BaseInput v-model="filters.keyword" class="flex-1" placeholder="Tìm tài liệu">
        <template #leading><Search :size="17" /></template>
      </BaseInput>

      <div class="flex gap-2">
        <BaseButton type="submit">Lọc</BaseButton>
        <BaseButton type="button" variant="secondary" @click="resetFilters">Đặt lại</BaseButton>
      </div>
    </form>

    <div
      class="overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card"
    >
      <header
        class="flex items-center justify-between border-b border-app-border px-5 py-4"
      >
        <div>
          <h3
            class="font-semibold text-app-text"
          >
            Danh sách tài liệu
          </h3>

          <p
            class="mt-1 text-xs text-app-text-muted"
          >
            {{ documentsPage.pageable.totalElements }}
            tài liệu
          </p>
        </div>

        <span
          v-if="polling"
          class="flex items-center gap-2 text-xs font-medium text-secondary"
        >
          <RefreshCw
            :size="14"
            class="animate-spin"
          />

          Đang cập nhật
        </span>
      </header>

      <div
        v-if="loading"
        class="space-y-3 p-5"
      >
        <div
          v-for="index in 4"
          :key="index"
          class="h-20 animate-pulse rounded-control bg-app-surface-muted"
        />
      </div>

      <div
        v-else-if="documentsPage.content.length === 0"
        class="px-6 py-16 text-center"
      >
        <FileText
          :size="40"
          class="mx-auto text-app-text-muted/50"
        />

        <h3
          class="mt-4 font-semibold text-app-text"
        >
          Chưa có tài liệu
        </h3>
      </div>

      <ul
        v-else
        class="divide-y divide-app-border"
      >
        <li
          v-for="item in documentsPage.content"
          :key="item.id"
          class="flex gap-4 px-5 py-4"
        >
          <span
            class="flex h-11 w-11 shrink-0 items-center justify-center rounded-control bg-app-surface-muted text-app-text-muted"
          >
            <FileText :size="21" />
          </span>

          <div class="min-w-0 flex-1">
            <div
              class="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between"
            >
              <div class="min-w-0">
                <p
                  class="truncate font-semibold text-app-text"
                >
                  {{ item.title }}
                </p>

                <p
                  class="mt-1 truncate text-sm text-app-text-muted"
                >
                  {{ item.version.fileName }}
                  ·
                  {{ formatFileSize(item.version.fileSize) }}
                </p>
              </div>

              <span
                class="inline-flex w-fit items-center gap-1.5 rounded-pill px-2.5 py-1 text-xs font-semibold"
                :class="
                  statusClass[
                    item.version.processingStatus
                  ]
                "
              >
                <CheckCircle2
                  v-if="
                    item.version.processingStatus ===
                    'INDEXED'
                  "
                  :size="13"
                />

                <AlertTriangle
                  v-else-if="
                    item.version.processingStatus ===
                    'FAILED'
                  "
                  :size="13"
                />

                <Clock3
                  v-else
                  :size="13"
                />

                {{
                  documentStatusLabel[
                    item.version.processingStatus
                  ]
                }}
              </span>
            </div>

            <p
              v-if="item.description"
              class="mt-2 line-clamp-2 text-sm text-app-text-muted"
            >
              {{ item.description }}
            </p>

            <p
              class="mt-2 text-xs text-app-text-muted"
            >
              Tải lên {{ formatDateTime(item.createdAt) }}
            </p>
          </div>

          <div
            class="flex shrink-0 items-start gap-1"
          >
            <button
              type="button"
              class="rounded-control p-2 text-ai transition hover:bg-ai-soft disabled:opacity-50"
              :disabled="previewingId === item.id"
              aria-label="Xem tài liệu"
              title="Xem tài liệu"
              @click="openDocument(item)"
            >
              <span
                v-if="previewingId === item.id"
                class="block h-4 w-4 animate-spin rounded-full border-2 border-current border-r-transparent"
              />

              <Eye
                v-else
                :size="17"
              />
            </button>

            <button
              type="button"
              class="rounded-control p-2 text-secondary transition hover:bg-secondary-soft disabled:opacity-50"
              :disabled="downloadingId === item.id"
              aria-label="Tải xuống"
              title="Tải xuống"
              @click="handleDownload(item)"
            >
              <span
                v-if="downloadingId === item.id"
                class="block h-4 w-4 animate-spin rounded-full border-2 border-current border-r-transparent"
              />

              <Download
                v-else
                :size="17"
              />
            </button>

            <button
              v-if="canManage"
              type="button"
              class="rounded-control p-2 text-app-text-muted transition hover:bg-app-surface-muted"
              aria-label="Sửa tài liệu"
              title="Chỉnh sửa"
              @click="openEdit(item)"
            >
              <Pencil :size="17" />
            </button>

            <button
              v-if="canManage"
              type="button"
              class="rounded-control p-2 text-danger transition hover:bg-danger-soft disabled:opacity-50"
              :disabled="deletingId === item.id"
              aria-label="Xóa tài liệu"
              title="Xóa"
              @click="handleDelete(item)"
            >
              <span
                v-if="deletingId === item.id"
                class="block h-4 w-4 animate-spin rounded-full border-2 border-current border-r-transparent"
              />

              <Trash2
                v-else
                :size="17"
              />
            </button>
          </div>
        </li>
      </ul>

      <footer
        v-if="documentsPage.pageable.totalElements > 0"
        class="flex items-center justify-between border-t border-app-border px-5 py-4 text-sm"
      >
        <span
          class="text-app-text-muted"
        >
          Trang {{ requestedPage }} / {{ totalPages }}
        </span>

        <div class="flex gap-2">
          <button
            type="button"
            class="rounded-control border border-app-border p-2 disabled:opacity-40"
            :disabled="
              requestedPage <= 1 || loading
            "
            aria-label="Trang trước"
            @click="
              goToPage(requestedPage - 1)
            "
          >
            <ChevronLeft :size="18" />
          </button>

          <button
            type="button"
            class="rounded-control border border-app-border p-2 disabled:opacity-40"
            :disabled="
              requestedPage >= totalPages ||
              loading
            "
            aria-label="Trang sau"
            @click="
              goToPage(requestedPage + 1)
            "
          >
            <ChevronRight :size="18" />
          </button>
        </div>
      </footer>
    </div>

    <DocumentFormModal
      :open="formOpen"
      :document="editingDocument"
      :loading="formLoading"
      :server-message="formMessage"
      :server-errors="formErrors"
      @close="formOpen = false"
      @upload="submitUpload"
      @update="submitUpdate"
    />
  </section>
</template>