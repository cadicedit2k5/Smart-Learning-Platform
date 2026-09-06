<script setup lang="ts">
import {
  computed,
  onMounted,
  reactive,
  ref,
} from 'vue'

import type {
  CourseDocument,
  DocumentProcessingStatus,
} from '@/shared/document'

import type {
  PaginatedData,
} from '@/shared/api'

import {
  parseApiError,
} from '@/shared/api'

import {
  BaseAlert,
  BasePageHeader,
  BasePagination,
  ConfirmDialog,
  DocumentViewer,
} from '@/shared/components'

import {
  deleteDocument,
  downloadDocument,
  getDocuments,
} from '../api/documentApi.ts'
import DocumentFilters from '../components/document/DocumentFilters.vue'
import DocumentTable from '../components/document/DocumentTable.vue'

const emptyPage =
  (): PaginatedData<CourseDocument> => ({
    content: [],
    pageable: {
      page: 1,
      size: 5,
      totalElements: 0,
      totalPages: 0,
    },
  })

const documentsPage = ref(emptyPage())

const currentPage = ref(1)

const loading = ref(false)
const deleting = ref(false)

const errorMessage = ref('')
const successMessage = ref('')
const deleteError = ref('')

const deleteTarget = ref<CourseDocument | null>(null)

const previewDocument = ref<CourseDocument | null>(null)
const previewBlob = ref<Blob | null>(null)
const previewingId = ref('')

const filters = reactive({
  keyword: '',
  courseKeyword: '',
  processingStatus: '' as | DocumentProcessingStatus | '',
  createdAtOrder: 'DESC' as | 'ASC' | 'DESC',
})

const totalPages = computed(() =>
      documentsPage.value.pageable.totalPages)

let latestLoadId = 0

const loadDocuments = async () => {
  const loadId = ++latestLoadId

  loading.value = true
  errorMessage.value = ''

  try {
    const result =  await getDocuments({
        page: currentPage.value,
        keyword: filters.keyword.trim() || undefined,
        courseKeyword: filters.courseKeyword.trim() || undefined,
        processingStatus: filters.processingStatus || undefined,
        createdAtOrder: filters.createdAtOrder,
      })

    if (loadId === latestLoadId) {
      documentsPage.value = result
    }
  } catch (error) {
    if (loadId === latestLoadId) {
      errorMessage.value = parseApiError(error).message
    }
  } finally {
    if (loadId === latestLoadId) {
      loading.value = false
    }
  }
}

const applyFilters = () => {
  currentPage.value = 1
  void loadDocuments()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.courseKeyword = ''
  filters.processingStatus = ''
  filters.createdAtOrder = 'DESC'

  currentPage.value = 1

  void loadDocuments()
}

const goToPage = (page: number) => {
  if (page < 1 ||page > totalPages.value || page === currentPage.value) {
    return
  }

  currentPage.value = page;

  void loadDocuments()
}

const openDocument = async (document: CourseDocument,) => {
  if (previewingId.value) {
    return
  }

  previewingId.value = document.id
  errorMessage.value = ''

  try {
    previewBlob.value = await downloadDocument(document.id)

    previewDocument.value = document
  } catch (error) {
    errorMessage.value = parseApiError(
        error).message
  } finally {
    previewingId.value = ''
  }
}

const closeDocument = () => {
  previewDocument.value = null
  previewBlob.value = null
}

const openDelete = (document: CourseDocument,) => {
  deleteTarget.value = document

  deleteError.value = ''
  successMessage.value = ''
}

const handleDelete = async () => {
  if (!deleteTarget.value || deleting.value) {
    return
  }

  const target = deleteTarget.value

  deleting.value = true
  deleteError.value = ''

  try {
    await deleteDocument(target.id)

    deleteTarget.value = null

    successMessage.value = `Đã xóa tài liệu "${target.title}".`

    if (documentsPage.value.content.length === 1 &&currentPage.value > 1) {
      currentPage.value -= 1
    }

    await loadDocuments()
  } catch (error) {
    const parsed = parseApiError(error)

    if (parsed.statusCode === 409) {
      deleteError.value = 'Tài liệu đang được xử lý, chưa thể xóa.'
    } else if (
      parsed.statusCode === 404
    ) {
      deleteError.value = 'Tài liệu không tồn tại hoặc đã bị xóa.'
    } else {
      deleteError.value = parsed.message
    }
  } finally {
    deleting.value = false
  }
}

onMounted(loadDocuments)
</script>

<template>

   <DocumentViewer
    v-if="previewDocument &&  previewBlob"
    :blob="previewBlob"
    :title=" previewDocument.title"
    :file-name="previewDocument.version.fileName"
    :mime-type=" previewDocument.version.mimeType"
    @close="closeDocument"
  />

  <section
    class="mx-auto max-w-[90rem] space-y-6"
  >
    <BasePageHeader
      eyebrow="Quản trị hệ thống"
      title="Quản lý tài liệu"
      description="Theo dõi trạng thái xử lý và quản lý tài liệu trong toàn hệ thống."
    />

    <BaseAlert
      v-if="errorMessage"
      variant="error"
    >
      {{ errorMessage }}
    </BaseAlert>

    <BaseAlert
      v-if="successMessage"
      variant="success"
    >
      {{ successMessage }}
    </BaseAlert>

    <DocumentFilters
      v-model="filters"
      :loading="loading"
      @apply="applyFilters"
      @reset="resetFilters"
    />

    <DocumentTable
      :documents="documentsPage.content"
      :loading="loading"
      :total="documentsPage.pageable.totalElements"
      :previewing-id="previewingId"
      @view="openDocument"
      @delete="openDelete"
    />

    <BasePagination
      v-if="
        documentsPage.pageable
          .totalElements > 0
      "
      :page="currentPage"
      :total-pages="totalPages"
      :total-elements="
        documentsPage.pageable
          .totalElements
      "
      @previous="
        goToPage(
          currentPage - 1,
        )
      "
      @next="
        goToPage(
          currentPage + 1,
        )
      "
    />

    <ConfirmDialog
      :open="
        deleteTarget !== null
      "
      title="Xóa tài liệu?"
      :description="
        deleteTarget
          ? `Tài liệu '${deleteTarget.title}' sẽ bị xóa khỏi hệ thống và dữ liệu lập chỉ mục liên quan cũng sẽ được yêu cầu xóa.`
          : ''
      "
      confirm-text="Xóa tài liệu"
      :loading="deleting"
      :error="deleteError"
      @close="
        !deleting &&
          (deleteTarget = null)
      "
      @confirm="handleDelete"
    />
  </section>
</template>