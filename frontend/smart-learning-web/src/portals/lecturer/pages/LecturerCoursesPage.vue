<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  ArrowRight,
  BookOpen,
  CalendarDays,
  Globe2,
  LockKeyhole,
  Plus,
  Search,
  UsersRound,
} from 'lucide-vue-next'

import {
  BaseAlert,
  BaseBadge,
  BaseButton,
  BaseEmptyState,
  BaseInput,
  BasePageHeader,
  BasePagination,
} from '@/shared/components'
import CourseCover from '@/shared/course/CourseCover.vue'
import type { Course, CourseStatus } from '@/shared/course'
import { getMyCourses } from '@/shared/course'
import {
  courseStatusLabel,
  courseStatusTone,
  courseVisibilityLabel,
} from '@/shared/course/presentation'
import { storedRichTextToPlainText } from '@/shared/rich-text'
import { formatDate } from '@/shared/utils'
import type { PaginatedData } from '@/shared/api'

import { createCourse, type CourseInput } from '../api/courseApi'
import CourseFormModal from '../components/CourseFormModal.vue'
import { useLecturerApiError } from '../composables/useLecturerApiError'

const { handleApiError } = useLecturerApiError()

const coursesPage = ref<PaginatedData<Course>>({
  content: [],
  pageable: { page: 1, size: 6, totalElements: 0, totalPages: 0 },
})

const currentPage = ref(1)
const loading = ref(true)
const loadError = ref('')
const keyword = ref('')
const statusFilter = ref<CourseStatus | ''>('')
const appliedKeyword = ref('')
const appliedStatus = ref<CourseStatus | ''>('')
const formOpen = ref(false)
const saving = ref(false)
const formMessage = ref('')
const formErrors = ref<Record<string, string>>({})

const totalPages = computed(() => coursesPage.value.pageable.totalPages)
const hasActiveFilters = computed(() => appliedKeyword.value !== '' || appliedStatus.value !== '')

const descriptionPreview = (description: string | null) =>
  storedRichTextToPlainText(description) || 'Chưa có mô tả cho khóa học này.'

const loadCourses = async () => {
  loading.value = true
  loadError.value = ''

  try {
    coursesPage.value = await getMyCourses({
      page: currentPage.value,
      keyword: appliedKeyword.value || undefined,
      status: appliedStatus.value || undefined,
    })
  } catch (error) {
    loadError.value = handleApiError(error, 'Không thể tải danh sách khóa học.').message
  } finally {
    loading.value = false
  }
}

const applyFilters = () => {
  appliedKeyword.value = keyword.value.trim()
  appliedStatus.value = statusFilter.value
  currentPage.value = 1
  void loadCourses()
}

const resetFilters = () => {
  keyword.value = ''
  statusFilter.value = ''
  appliedKeyword.value = ''
  appliedStatus.value = ''
  currentPage.value = 1
  void loadCourses()
}

const goToPage = (page: number) => {
  if (page < 1 || page > totalPages.value || page === currentPage.value) return
  currentPage.value = page
  void loadCourses()
}

const openCreate = () => {
  formMessage.value = ''
  formErrors.value = {}
  formOpen.value = true
}

const submitCreate = async (input: CourseInput) => {
  saving.value = true
  formMessage.value = ''
  formErrors.value = {}

  try {
    await createCourse(input)
    formOpen.value = false
    currentPage.value = 1
    await loadCourses()
  } catch (error) {
    const parsed = handleApiError(error, 'Không thể tạo khóa học.')
    formMessage.value = parsed.message
    formErrors.value = parsed.fieldErrors
  } finally {
    saving.value = false
  }
}

onMounted(() => void loadCourses())
</script>

<template>
  <section class="mx-auto max-w-app space-y-6">
    <BasePageHeader title="Khóa học của tôi">
      <template #actions>
        <BaseButton @click="openCreate">
          <template #leading><Plus :size="18" /></template>
          Tạo khóa học
        </BaseButton>
      </template>
    </BasePageHeader>

    <BaseAlert v-if="loadError">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <span>{{ loadError }}</span>
        <button type="button" class="font-semibold underline" @click="loadCourses">Thử lại</button>
      </div>
    </BaseAlert>

    <form class="grid gap-3 sm:grid-cols-[minmax(0,1fr)_13rem_auto]" @submit.prevent="applyFilters">
      <BaseInput v-model="keyword" placeholder="Tìm theo tên, mô tả hoặc cấp độ...">
        <template #leading><Search :size="18" /></template>
      </BaseInput>

      <select
        v-model="statusFilter"
        aria-label="Lọc theo trạng thái"
        class="h-11 rounded-control border border-app-border bg-app-surface px-3 text-sm outline-none focus:border-secondary focus:ring-2 focus:ring-secondary/20"
      >
        <option value="">Tất cả trạng thái</option>
        <option value="DRAFT">Bản nháp</option>
        <option value="PUBLISHED">Đã xuất bản</option>
        <option value="ARCHIVED">Đã lưu trữ</option>
      </select>

      <BaseButton type="submit" variant="secondary">Lọc</BaseButton>
    </form>

    <button
      v-if="hasActiveFilters"
      type="button"
      class="text-sm font-semibold text-secondary hover:underline"
      @click="resetFilters"
    >
      Xóa bộ lọc
    </button>

    <div v-if="loading" class="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
      <div v-for="index in 6" :key="index" class="h-96 animate-pulse rounded-card bg-app-surface-muted" />
    </div>

    <BaseEmptyState
      v-else-if="coursesPage.content.length === 0"
      :title="hasActiveFilters ? 'Không tìm thấy khóa học phù hợp' : 'Chưa có khóa học nào'"
      :description="
        hasActiveFilters
          ? 'Thử thay đổi từ khóa hoặc trạng thái.'
          : 'Tạo khóa học đầu tiên để bắt đầu xây dựng nội dung và mời học viên.'
      "
    >
      <template #icon><BookOpen :size="24" /></template>

      <template v-if="!hasActiveFilters" #actions>
        <BaseButton @click="openCreate">
          <template #leading><Plus :size="18" /></template>
          Tạo khóa học đầu tiên
        </BaseButton>
      </template>
    </BaseEmptyState>

    <div v-else class="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
      <RouterLink
        v-for="course in coursesPage.content"
        :key="course.id"
        :to="{ name: 'lecturer-course-detail', params: { courseId: course.id } }"
        class="group flex overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card transition hover:-translate-y-0.5 hover:border-secondary/50 hover:shadow-overlay"
      >
        <div class="flex min-h-0 w-full flex-col">
          <CourseCover :image-url="course.imageUrl" :title="course.title" />

          <div class="flex flex-1 flex-col p-5">
            <div class="flex items-center justify-between gap-3">
              <BaseBadge :tone="courseStatusTone[course.status]">
                {{ courseStatusLabel[course.status] }}
              </BaseBadge>

              <span class="flex items-center gap-1.5 text-xs text-app-text-muted">
                <Globe2 v-if="course.visibility === 'PUBLIC'" :size="14" />
                <UsersRound v-else-if="course.visibility === 'INVITE_ONLY'" :size="14" />
                <LockKeyhole v-else :size="14" />
                {{ courseVisibilityLabel[course.visibility] }}
              </span>
            </div>

            <h2 class="mt-4 line-clamp-2 font-heading text-xl font-bold text-app-text group-hover:text-secondary">
              {{ course.title }}
            </h2>

            <p class="mt-2 line-clamp-3 text-sm leading-6 text-app-text-muted">
              {{ descriptionPreview(course.description) }}
            </p>

            <div class="mt-auto flex items-end justify-between gap-3 pt-6">
              <div class="space-y-1 text-xs text-app-text-muted">
                <p v-if="course.level" class="font-medium text-app-text">{{ course.level }}</p>

                <p class="flex items-center gap-1.5">
                  <CalendarDays :size="14" />
                  Cập nhật {{ formatDate(course.updatedAt) }}
                </p>
              </div>

              <ArrowRight
                :size="20"
                class="shrink-0 text-secondary transition group-hover:translate-x-1"
              />
            </div>
          </div>
        </div>
      </RouterLink>
    </div>

    <BasePagination
      v-if="!loading && coursesPage.pageable.totalElements > 0"
      :page="currentPage"
      :total-pages="totalPages"
      :total-elements="coursesPage.pageable.totalElements"
      @previous="goToPage(currentPage - 1)"
      @next="goToPage(currentPage + 1)"
    />

    <CourseFormModal
      :open="formOpen"
      :loading="saving"
      :server-message="formMessage"
      :server-errors="formErrors"
      @close="formOpen = false"
      @submit="submitCreate"
    />
  </section>
</template>