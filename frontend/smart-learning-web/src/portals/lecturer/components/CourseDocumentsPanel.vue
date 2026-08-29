<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import {
  AlertTriangle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock3,
  Download,
  FileText,
  Pencil,
  RefreshCw,
  Search,
  Trash2,
  Upload,
} from 'lucide-vue-next'

import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import BaseInput from '@/shared/components/BaseInput.vue'
import type { PaginatedData } from '@/shared/api'

import { getChapters, getTopics, type CourseChapter, type CourseTopic } from '../api/contentApi'
import {
  deleteDocument,
  downloadDocument,
  getDocument,
  getDocuments,
  updateDocument,
  uploadDocument,
  type CourseDocument,
  type DocumentProcessingStatus,
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
  pageable: { page: 1, size: 10, totalElements: 0, totalPages: 0 },
})

const documentsPage = ref(emptyPage())
const chapters = ref<CourseChapter[]>([])
const filterTopics = ref<CourseTopic[]>([])
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
const downloadingId = ref('')
const deletingId = ref('')
const filters = reactive({
  keyword: '',
  chapterId: '',
  topicId: '',
})

let pollTimer: ReturnType<typeof setTimeout> | undefined

const totalPages = computed(() => Math.max(1, documentsPage.value.pageable.totalPages))
const hasProcessingDocuments = computed(() =>
  documentsPage.value.content.some((item) =>
    ['UPLOADED', 'QUEUED', 'PROCESSING'].includes(item.version.processingStatus),
  ),
)

const clearPollTimer = () => {
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = undefined
  }
}

const schedulePoll = () => {
  clearPollTimer()
  if (!hasProcessingDocuments.value) return
  pollTimer = setTimeout(() => void loadDocuments(true), 5000)
}

const loadDocuments = async (silent = false) => {
  if (silent) polling.value = true
  else loading.value = true
  loadError.value = ''

  try {
    documentsPage.value = await getDocuments(props.courseId, {
      page: requestedPage.value,
      keyword: filters.keyword.trim() || undefined,
      chapterId: filters.chapterId || undefined,
      topicId: filters.topicId || undefined,
    })
  } catch (error) {
    loadError.value = handleApiError(error, 'Không thể tải danh sách tài liệu.').message
  } finally {
    loading.value = false
    polling.value = false
    schedulePoll()
  }
}

const loadChapters = async () => {
  if (!props.canManage) return
  try {
    chapters.value = await getChapters(props.courseId)
  } catch (error) {
    actionMessage.value = handleApiError(error, 'Không thể tải danh sách chương.').message
  }
}

const changeFilterChapter = async () => {
  filters.topicId = ''
  filterTopics.value = filters.chapterId ? await getTopics(props.courseId, filters.chapterId) : []
}

const applyFilters = () => {
  requestedPage.value = 1
  void loadDocuments()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.chapterId = ''
  filters.topicId = ''
  filterTopics.value = []
  applyFilters()
}

const goToPage = (page: number) => {
  if (page < 1 || page > totalPages.value || page === requestedPage.value) return
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
  try {
    editingDocument.value = await getDocument(props.courseId, item.id)
    formOpen.value = true
  } catch (error) {
    actionMessage.value = handleApiError(error, 'Không thể tải chi tiết tài liệu.').message
  }
}

const submitUpload = async (input: DocumentUploadInput) => {
  formLoading.value = true
  formMessage.value = ''
  formErrors.value = {}
  try {
    await uploadDocument(props.courseId, input)
    formOpen.value = false
    requestedPage.value = 1
    await loadDocuments()
  } catch (error) {
    const parsed = handleApiError(error, 'Không thể tải tài liệu lên.')
    formMessage.value = parsed.message
    formErrors.value = parsed.fieldErrors
  } finally {
    formLoading.value = false
  }
}

const submitUpdate = async (input: DocumentUpdateInput) => {
  if (!editingDocument.value) return
  formLoading.value = true
  formMessage.value = ''
  formErrors.value = {}
  try {
    await updateDocument(props.courseId, editingDocument.value.id, input)
    formOpen.value = false
    editingDocument.value = null
    await loadDocuments()
  } catch (error) {
    const parsed = handleApiError(error, 'Không thể cập nhật tài liệu.')
    formMessage.value = parsed.message
    formErrors.value = parsed.fieldErrors
  } finally {
    formLoading.value = false
  }
}

const handleDownload = async (item: CourseDocument) => {
  downloadingId.value = item.id
  actionMessage.value = ''
  try {
    const blob = await downloadDocument(props.courseId, item.id)
    const objectUrl = URL.createObjectURL(blob)
    const anchor = window.document.createElement('a')
    anchor.href = objectUrl
    anchor.download = item.version.fileName || item.title
    anchor.click()
    URL.revokeObjectURL(objectUrl)
  } catch (error) {
    actionMessage.value = handleApiError(error, 'Không thể tải tài liệu.').message
  } finally {
    downloadingId.value = ''
  }
}

const handleDelete = async (item: CourseDocument) => {
  if (!window.confirm('Xóa tài liệu “' + item.title + '”?')) return
  deletingId.value = item.id
  actionMessage.value = ''
  try {
    await deleteDocument(props.courseId, item.id)
    if (documentsPage.value.content.length === 1 && requestedPage.value > 1) {
      requestedPage.value -= 1
    }
    await loadDocuments()
  } catch (error) {
    actionMessage.value = handleApiError(error, 'Không thể xóa tài liệu.').message
  } finally {
    deletingId.value = ''
  }
}

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const formatDate = (value: string) =>
  new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(
    new Date(value),
  )

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

watch(
  () => props.courseId,
  () => {
    requestedPage.value = 1
    documentsPage.value = emptyPage()
    void Promise.all([loadDocuments(), loadChapters()])
  },
)

onMounted(() => void Promise.all([loadDocuments(), loadChapters()]))
onBeforeUnmount(clearPollTimer)
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
      <BaseButton v-if="canManage" @click="openUpload">
        <template #leading><Upload :size="18" /></template>
        Tải tài liệu lên
      </BaseButton>
    </header>

    <BaseAlert v-if="actionMessage">{{ actionMessage }}</BaseAlert>
    <BaseAlert v-if="loadError">
      <div class="flex items-center justify-between gap-3">
        <span>{{ loadError }}</span>
        <button type="button" class="font-semibold underline" @click="loadDocuments()">
          Thử lại
        </button>
      </div>
    </BaseAlert>

    <form
      class="grid gap-3 rounded-card border border-app-border bg-app-surface p-4 shadow-card md:grid-cols-[minmax(0,1fr)_11rem_12rem_12rem_auto]"
      @submit.prevent="applyFilters"
    >
      <BaseInput v-model="filters.keyword" placeholder="Tìm tài liệu">
        <template #leading><Search :size="17" /></template>
      </BaseInput>
      <select
        v-if="canManage"
        v-model="filters.chapterId"
        class="h-11 rounded-control border border-app-border bg-app-surface px-3 text-sm"
        @change="changeFilterChapter"
      >
        <option value="">Tất cả chương</option>
        <option v-for="chapter in chapters" :key="chapter.id" :value="chapter.id">
          {{ chapter.title }}
        </option>
      </select>
      <select
        v-if="canManage"
        v-model="filters.topicId"
        class="h-11 rounded-control border border-app-border bg-app-surface px-3 text-sm"
        :disabled="!filters.chapterId"
      >
        <option value="">Tất cả chủ đề</option>
        <option v-for="topic in filterTopics" :key="topic.id" :value="topic.id">
          {{ topic.title }}
        </option>
      </select>
      <div class="flex gap-2">
        <BaseButton type="submit">Lọc</BaseButton>
        <BaseButton variant="secondary" @click="resetFilters">Đặt lại</BaseButton>
      </div>
    </form>

    <div class="overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card">
      <header class="flex items-center justify-between border-b border-app-border px-5 py-4">
        <div>
          <h3 class="font-semibold text-app-text">Danh sách tài liệu</h3>
          <p class="mt-1 text-xs text-app-text-muted">
            {{ documentsPage.pageable.totalElements }} tài liệu
          </p>
        </div>
        <span v-if="polling" class="flex items-center gap-2 text-xs font-medium text-secondary">
          <RefreshCw :size="14" class="animate-spin" /> Đang cập nhật
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
      </div>
      <ul v-else class="divide-y divide-app-border">
        <li v-for="item in documentsPage.content" :key="item.id" class="flex gap-4 px-5 py-4">
          <span
            class="flex h-11 w-11 shrink-0 items-center justify-center rounded-control bg-app-surface-muted text-app-text-muted"
            ><FileText :size="21"
          /></span>
          <div class="min-w-0 flex-1">
            <div class="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
              <div class="min-w-0">
                <p class="truncate font-semibold text-app-text">{{ item.title }}</p>
                <p class="mt-1 truncate text-sm text-app-text-muted">
                  {{ item.version.fileName }} · {{ formatFileSize(item.version.fileSize) }}
                </p>
              </div>
              <span
                class="inline-flex w-fit items-center gap-1.5 rounded-pill px-2.5 py-1 text-xs font-semibold"
                :class="statusClass[item.version.processingStatus]"
              >
                <CheckCircle2 v-if="item.version.processingStatus === 'INDEXED'" :size="13" />
                <AlertTriangle v-else-if="item.version.processingStatus === 'FAILED'" :size="13" />
                <Clock3 v-else :size="13" />
                {{ statusLabel[item.version.processingStatus] }}
              </span>
            </div>
            <p v-if="item.description" class="mt-2 line-clamp-2 text-sm text-app-text-muted">
              {{ item.description }}
            </p>
            <p class="mt-2 text-xs text-app-text-muted">Tải lên {{ formatDate(item.createdAt) }}</p>
          </div>
          <div class="flex shrink-0 items-start gap-1">
            <button
              type="button"
              class="rounded-control p-2 text-secondary hover:bg-secondary-soft disabled:opacity-50"
              :disabled="downloadingId === item.id"
              aria-label="Tải xuống"
              @click="handleDownload(item)"
            >
              <Download :size="17" />
            </button>
            <button
              v-if="canManage"
              type="button"
              class="rounded-control p-2 text-app-text-muted hover:bg-app-surface-muted"
              aria-label="Sửa tài liệu"
              @click="openEdit(item)"
            >
              <Pencil :size="17" />
            </button>
            <button
              v-if="canManage"
              type="button"
              class="rounded-control p-2 text-danger hover:bg-danger-soft disabled:opacity-50"
              :disabled="deletingId === item.id"
              aria-label="Xóa tài liệu"
              @click="handleDelete(item)"
            >
              <Trash2 :size="17" />
            </button>
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
            class="rounded-control border border-app-border p-2 disabled:opacity-40"
            :disabled="requestedPage <= 1 || loading"
            aria-label="Trang trước"
            @click="goToPage(requestedPage - 1)"
          >
            <ChevronLeft :size="18" />
          </button>
          <button
            type="button"
            class="rounded-control border border-app-border p-2 disabled:opacity-40"
            :disabled="requestedPage >= totalPages || loading"
            aria-label="Trang sau"
            @click="goToPage(requestedPage + 1)"
          >
            <ChevronRight :size="18" />
          </button>
        </div>
      </footer>
    </div>

    <DocumentFormModal
      :open="formOpen"
      :course-id="courseId"
      :document="editingDocument"
      :chapters="chapters"
      :loading="formLoading"
      :server-message="formMessage"
      :server-errors="formErrors"
      @close="formOpen = false"
      @upload="submitUpload"
      @update="submitUpdate"
    />
  </section>
</template>
