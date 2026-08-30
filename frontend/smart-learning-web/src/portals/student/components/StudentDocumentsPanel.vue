<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ChevronLeft, ChevronRight, Download, FileText, Info, Search, X } from 'lucide-vue-next'

import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import BaseCard from '@/shared/components/BaseCard.vue'
import BaseInput from '@/shared/components/BaseInput.vue'
import type { PaginatedData } from '@/shared/api'

import {
  downloadDocument,
  getDocument,
  getDocuments,
  type CourseDocument,
} from '../api/documentApi'
import { useStudentApiError } from '../composables/useStudentApiError'
import { formatDate } from '@/shared/utils'

const props = defineProps<{
  courseId: string
}>()

const { handleApiError } = useStudentApiError()
const documentsPage = ref<PaginatedData<CourseDocument>>({
  content: [],
  pageable: { page: 1, size: 10, totalElements: 0, totalPages: 0 },
})
const page = ref(1)
const keyword = ref('')
const loading = ref(true)
const loadError = ref('')
const actionError = ref('')
const selectedDocument = ref<CourseDocument | null>(null)
const loadingDetailId = ref('')
const downloadingId = ref('')

const totalPages = computed(() => Math.max(1, documentsPage.value.pageable.totalPages))

const loadDocuments = async () => {
  loading.value = true
  loadError.value = ''

  try {
    documentsPage.value = await getDocuments(props.courseId, {
      keyword: keyword.value.trim() || undefined,
      page: page.value,
    })
  } catch (error) {
    loadError.value = handleApiError(error, 'Không thể tải danh sách tài liệu.').message
  } finally {
    loading.value = false
  }
}

const applySearch = () => {
  page.value = 1
  void loadDocuments()
}

const clearSearch = () => {
  keyword.value = ''
  applySearch()
}

const goToPage = (nextPage: number) => {
  if (nextPage < 1 || nextPage > totalPages.value || nextPage === page.value) return
  page.value = nextPage
  void loadDocuments()
}

const showDetail = async (item: CourseDocument) => {
  loadingDetailId.value = item.id
  actionError.value = ''

  try {
    selectedDocument.value = await getDocument(props.courseId, item.id)
  } catch (error) {
    actionError.value = handleApiError(error, 'Không thể tải chi tiết tài liệu.').message
  } finally {
    loadingDetailId.value = ''
  }
}

const handleDownload = async (item: CourseDocument) => {
  downloadingId.value = item.id
  actionError.value = ''

  try {
    const blob = await downloadDocument(props.courseId, item.id)
    const url = URL.createObjectURL(blob)
    const link = window.document.createElement('a')
    link.href = url
    link.download = item.version.fileName
    link.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    actionError.value = handleApiError(error, 'Không thể tải tài liệu xuống.').message
  } finally {
    downloadingId.value = ''
  }
}

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

onMounted(() => void loadDocuments())
</script>

<template>
  <section class="space-y-5">
    <header>
      <h2 class="font-heading text-xl font-bold text-app-text">Tài liệu khóa học</h2>
      <p class="mt-1 text-sm text-app-text-muted">Xem và tải các tài liệu được chia sẻ.</p>
    </header>

    <form class="flex flex-col gap-3 sm:flex-row" @submit.prevent="applySearch">
      <BaseInput v-model="keyword" class="flex-1" placeholder="Tìm tài liệu...">
        <template #leading><Search :size="18" /></template>
      </BaseInput>
      <div class="flex gap-2">
        <BaseButton type="submit">Tìm kiếm</BaseButton>
        <BaseButton v-if="keyword" variant="secondary" @click="clearSearch">Xóa lọc</BaseButton>
      </div>
    </form>

    <BaseAlert v-if="loadError">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <span>{{ loadError }}</span>
        <button type="button" class="font-semibold underline" @click="loadDocuments">
          Thử lại
        </button>
      </div>
    </BaseAlert>
    <BaseAlert v-if="actionError">{{ actionError }}</BaseAlert>

    <div v-if="loading" class="space-y-3">
      <div
        v-for="index in 4"
        :key="index"
        class="h-24 animate-pulse rounded-card bg-app-surface-muted"
      />
    </div>

    <BaseCard v-else-if="documentsPage.content.length === 0">
      <div class="py-10 text-center">
        <FileText :size="42" class="mx-auto text-app-text-muted/40" />
        <h3 class="mt-4 font-semibold text-app-text">Chưa có tài liệu</h3>
        <p class="mt-1 text-sm text-app-text-muted">
          {{
            keyword ? 'Không tìm thấy tài liệu phù hợp.' : 'Khóa học chưa có tài liệu để hiển thị.'
          }}
        </p>
      </div>
    </BaseCard>

    <div
      v-else
      class="overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card"
    >
      <ul class="divide-y divide-app-border">
        <li v-for="item in documentsPage.content" :key="item.id" class="flex gap-4 p-5">
          <span
            class="flex h-11 w-11 shrink-0 items-center justify-center rounded-control bg-secondary-soft text-secondary"
          >
            <FileText :size="21" />
          </span>
          <div class="min-w-0 flex-1">
            <h3 class="truncate font-semibold text-app-text">{{ item.title }}</h3>
            <p class="mt-1 truncate text-sm text-app-text-muted">
              {{ item.version.fileName }} · {{ formatFileSize(item.version.fileSize) }}
            </p>
            <p v-if="item.description" class="mt-2 line-clamp-2 text-sm text-app-text-muted">
              {{ item.description }}
            </p>
            <p class="mt-2 text-xs text-app-text-muted">
              Cập nhật {{ formatDate(item.updatedAt) }}
            </p>
          </div>
          <div class="flex shrink-0 items-start gap-1">
            <button
              type="button"
              class="rounded-control p-2 text-app-text-muted hover:bg-app-surface-muted disabled:opacity-50"
              :disabled="loadingDetailId === item.id"
              aria-label="Xem chi tiết tài liệu"
              @click="showDetail(item)"
            >
              <Info :size="18" />
            </button>
            <button
              type="button"
              class="rounded-control p-2 text-secondary hover:bg-secondary-soft disabled:opacity-50"
              :disabled="downloadingId === item.id"
              aria-label="Tải tài liệu"
              @click="handleDownload(item)"
            >
              <Download :size="18" />
            </button>
          </div>
        </li>
      </ul>

      <footer
        class="flex items-center justify-between border-t border-app-border px-5 py-4 text-sm"
      >
        <span class="text-app-text-muted">Trang {{ page }} / {{ totalPages }}</span>
        <div class="flex gap-2">
          <button
            type="button"
            class="rounded-control border border-app-border p-2 disabled:opacity-40"
            :disabled="page <= 1 || loading"
            aria-label="Trang trước"
            @click="goToPage(page - 1)"
          >
            <ChevronLeft :size="18" />
          </button>
          <button
            type="button"
            class="rounded-control border border-app-border p-2 disabled:opacity-40"
            :disabled="page >= totalPages || loading"
            aria-label="Trang sau"
            @click="goToPage(page + 1)"
          >
            <ChevronRight :size="18" />
          </button>
        </div>
      </footer>
    </div>

    <div
      v-if="selectedDocument"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      @click.self="selectedDocument = null"
    >
      <BaseCard class="w-full max-w-lg">
        <template #header>
          <div class="flex items-start justify-between gap-4">
            <div>
              <h3 class="font-heading text-lg font-bold text-app-text">
                {{ selectedDocument.title }}
              </h3>
              <p class="mt-1 text-sm text-app-text-muted">
                {{ selectedDocument.version.fileName }}
              </p>
            </div>
            <button
              type="button"
              class="rounded-control p-2 text-app-text-muted hover:bg-app-surface-muted"
              aria-label="Đóng"
              @click="selectedDocument = null"
            >
              <X :size="19" />
            </button>
          </div>
        </template>
        <dl class="grid gap-4 text-sm sm:grid-cols-2">
          <div>
            <dt class="text-app-text-muted">Kích thước</dt>
            <dd class="mt-1 font-medium text-app-text">
              {{ formatFileSize(selectedDocument.version.fileSize) }}
            </dd>
          </div>
          <div>
            <dt class="text-app-text-muted">Định dạng</dt>
            <dd class="mt-1 font-medium text-app-text">{{ selectedDocument.version.mimeType }}</dd>
          </div>
          <div>
            <dt class="text-app-text-muted">Phiên bản</dt>
            <dd class="mt-1 font-medium text-app-text">
              {{ selectedDocument.version.versionNumber }}
            </dd>
          </div>
          <div>
            <dt class="text-app-text-muted">Cập nhật</dt>
            <dd class="mt-1 font-medium text-app-text">
              {{ formatDate(selectedDocument.updatedAt) }}
            </dd>
          </div>
        </dl>
        <p v-if="selectedDocument.description" class="mt-5 text-sm leading-6 text-app-text-muted">
          {{ selectedDocument.description }}
        </p>
        <BaseButton
          class="mt-6"
          :loading="downloadingId === selectedDocument.id"
          @click="handleDownload(selectedDocument)"
        >
          <template #leading><Download :size="18" /></template>Tải xuống
        </BaseButton>
      </BaseCard>
    </div>
  </section>
</template>
