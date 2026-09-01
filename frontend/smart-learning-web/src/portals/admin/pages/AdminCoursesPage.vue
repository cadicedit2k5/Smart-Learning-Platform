<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import type { Course, CourseStatus, CourseVisibility } from '@/shared/course'

import { parseApiError } from '@/shared/api'

import {
  BaseAlert,
  BasePageHeader,
  BasePagination,
  ConfirmDialog,
} from '@/shared/components'

import {
  deleteCourse,
  getCourses,
  updateCourse,
  type AdminUpdateCourseRequest,
  type CoursesPage,
} from '../api/courseApi'
import CourseTable from '../components/course/CourseTable.vue'
import CourseFormModal from '../components/course/CourseFormModal.vue'
import CourseFilters from '../components/course/CourseFilters.vue'

const coursesPage = ref<CoursesPage>({
  content: [],
  pageable: {
    page: 0,
    size: 5,
    totalElements: 0,
    totalPages: 0,
  },
})

const currentPage = ref(1)

const loading = ref(false)
const saving = ref(false)
const deleting = ref(false)

const errorMessage = ref('')
const successMessage = ref('')
const deleteError = ref('')

const formOpen = ref(false)

const editingCourse = ref<Course | null>(null)

const deleteTarget = ref<Course | null>(null)

const serverErrors = ref<Record<string, string>>({})

const serverMessage = ref('')

const filters = reactive({
  keyword: '',

  status: '' as CourseStatus | '',

  visibility: '' as CourseVisibility | '',

  updatedAtOrder: 'DESC' as 'ASC' | 'DESC',
})

const totalPages = computed(() =>
    coursesPage.value.pageable.totalPages,
)

let latestLoadId = 0

const loadCourses = async () => {
  const loadId = ++latestLoadId

  loading.value = true
  errorMessage.value = ''

  try {
    const result = await getCourses({
        page: currentPage.value,
        keyword: filters.keyword.trim() || undefined,
        status: filters.status || undefined,
        visibility: filters.visibility || undefined,
        updatedAtOrder: filters.updatedAtOrder,
      })

    if (loadId === latestLoadId) {
      coursesPage.value = result
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
  void loadCourses()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.status = ''
  filters.visibility = ''
  filters.updatedAtOrder = 'DESC'

  currentPage.value = 1

  void loadCourses()
}

const goToPage = (page: number,) => {
  if ( page < 1 ||  page > totalPages.value ||  page === currentPage.value) {
    return
  }

  currentPage.value = page

  void loadCourses()
}

const openEdit = (course: Course) => {
  editingCourse.value = course

  serverErrors.value = {}
  serverMessage.value = ''
  successMessage.value = ''

  formOpen.value = true
}

const handleUpdate = async (
  id: string,
  request: AdminUpdateCourseRequest) => {
  if (Object.keys(request).length === 0) {
    formOpen.value = false
    return;
  }

  saving.value = true

  serverErrors.value = {}
  serverMessage.value = ''
  successMessage.value = ''

  try {
    await updateCourse(id, request)

    formOpen.value = false

    successMessage.value = 'Cập nhật môn học thành công.'

    await loadCourses()
  } catch (error) {
    const parsed = parseApiError(error)

    serverErrors.value =
      Object.fromEntries(parsed.errors.filter(item => item.field)
          .map(item => [
              item.field as string,
              item.message,
            ],
          ),
      )

    if (parsed.statusCode === 404) {
      serverMessage.value ='Không tìm thấy môn học.'
      return
    }

    if (parsed.statusCode === 403) {
      serverMessage.value = 'Bạn không có quyền quản lý môn học.'
      return
    }

    serverMessage.value = parsed.message
  } finally {
    saving.value = false
  }
}

const openDelete = (course: Course) => {
  deleteTarget.value = course
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
    await deleteCourse( target.id)

    deleteTarget.value = null

    successMessage.value = `Đã xóa môn học "${target.title}".`

    if (coursesPage.value .content.length === 1 && currentPage.value > 1) {
      currentPage.value -= 1
    }

    await loadCourses()
  } catch (error) {
    const parsed = parseApiError(error)

    deleteError.value = parsed.statusCode === 404 ? 'Môn học không tồn tại hoặc đã bị xóa.' : parsed.message
  } finally {
    deleting.value = false
  }
}

onMounted(loadCourses)
</script>

<template>
  <section
    class="mx-auto max-w-[90rem] space-y-6"
  >
    <BasePageHeader
      eyebrow="Quản trị hệ thống"
      title="Quản lý môn học"
      description="Theo dõi, chỉnh sửa và quản lý các môn học trong hệ thống."
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

    <CourseFilters
      v-model="filters"
      :loading="loading"
      @apply="applyFilters"
      @reset="resetFilters"
    />

    <CourseTable
      :courses="
        coursesPage.content
      "
      :loading="loading"
      :total="
        coursesPage.pageable
          .totalElements
      "
      @edit="openEdit"
      @delete="openDelete"
    />

    <BasePagination
      v-if="
        coursesPage.pageable
          .totalElements > 0
      "
      :page="currentPage"
      :total-pages="totalPages"
      :total-elements="
        coursesPage.pageable
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

    <CourseFormModal
      :open="formOpen"
      :course="editingCourse"
      :loading="saving"
      :server-errors="
        serverErrors
      "
      :server-message="
        serverMessage
      "
      @close="
        formOpen = false
      "
      @update="handleUpdate"
    />

    <ConfirmDialog
      :open="
        deleteTarget !== null
      "
      title="Xóa môn học?"
      :description="
        deleteTarget
          ? `Môn học '${deleteTarget.title}' sẽ bị xóa khỏi hệ thống.`
          : ''
      "
      confirm-text="Xóa môn học"
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