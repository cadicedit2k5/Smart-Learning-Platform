<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowRight, BookOpen, CalendarDays, Search } from 'lucide-vue-next'

import {
  BaseAlert,
  BaseButton,
  BaseEmptyState,
  BaseInput,
  BasePageHeader,
  BasePagination,
} from '@/shared/components'
import CourseCover from '@/shared/course/CourseCover.vue'
import { getMyCourses, type Course } from '@/shared/course'
import { storedRichTextToPlainText } from '@/shared/rich-text'
import { formatDate } from '@/shared/utils'
import type { PaginatedData } from '@/shared/api'

import { useStudentApiError } from '../composables/useStudentApiError'

const { handleApiError } = useStudentApiError()

const coursesPage = ref<PaginatedData<Course>>({
  content: [],
  pageable: { page: 1, size: 6, totalElements: 0, totalPages: 0 },
})

const loading = ref(true)
const loadError = ref('')
const keyword = ref('')
const appliedKeyword = ref('')
const currentPage = ref(1)

const totalPages = computed(() => coursesPage.value.pageable.totalPages)
const hasSearch = computed(() => appliedKeyword.value !== '')

const descriptionPreview = (description: string | null) =>
  storedRichTextToPlainText(description) || 'Khóa học chưa có mô tả.'

const loadCourses = async () => {
  loading.value = true
  loadError.value = ''

  try {
    coursesPage.value = await getMyCourses({
      page: currentPage.value,
      keyword: appliedKeyword.value || undefined,
    })
  } catch (error) {
    loadError.value = handleApiError(error, 'Không thể tải danh sách khóa học.').message
  } finally {
    loading.value = false
  }
}

const applySearch = () => {
  appliedKeyword.value = keyword.value.trim()
  currentPage.value = 1
  void loadCourses()
}

const resetSearch = () => {
  keyword.value = ''
  appliedKeyword.value = ''
  currentPage.value = 1
  void loadCourses()
}

const goToPage = (page: number) => {
  if (page < 1 || page > totalPages.value || page === currentPage.value) return
  currentPage.value = page
  void loadCourses()
}

onMounted(() => void loadCourses())
</script>

<template>
  <section class="mx-auto max-w-app space-y-6">
    <BasePageHeader title="Khóa học của tôi" />

    <BaseAlert v-if="loadError">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <span>{{ loadError }}</span>
        <button type="button" class="font-semibold underline" @click="loadCourses">Thử lại</button>
      </div>
    </BaseAlert>

    <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <h2 class="font-heading text-xl font-bold text-app-text">Danh sách khóa học</h2>

      <form class="flex w-full gap-3 sm:max-w-lg" @submit.prevent="applySearch">
        <BaseInput v-model="keyword" class="flex-1" placeholder="Tìm khóa học...">
          <template #leading><Search :size="18" /></template>
        </BaseInput>

        <BaseButton type="submit" variant="secondary">Tìm</BaseButton>
      </form>
    </div>

    <button
      v-if="hasSearch"
      type="button"
      class="text-sm font-semibold text-secondary hover:underline"
      @click="resetSearch"
    >
      Xóa tìm kiếm
    </button>

    <div v-if="loading" class="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
      <div v-for="index in 6" :key="index" class="h-96 animate-pulse rounded-card bg-app-surface-muted" />
    </div>

    <BaseEmptyState
      v-else-if="coursesPage.content.length === 0"
      :title="hasSearch ? 'Không tìm thấy khóa học phù hợp' : 'Bạn chưa tham gia khóa học nào'"
      :description="
        hasSearch
          ? 'Thử thay đổi từ khóa tìm kiếm.'
          : 'Khám phá các khóa học công khai để gửi yêu cầu tham gia.'
      "
    >
      <template #icon><BookOpen :size="24" /></template>
    </BaseEmptyState>

    <div v-else class="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
      <RouterLink
        v-for="course in coursesPage.content"
        :key="course.id"
        :to="{ name: 'student-course-detail', params: { courseId: course.id } }"
        class="group flex overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card transition hover:-translate-y-0.5 hover:border-secondary/50 hover:shadow-overlay"
      >
        <div class="flex min-h-0 w-full flex-col">
          <CourseCover :image-url="course.imageUrl" :title="course.title" />

          <div class="flex flex-1 flex-col p-5">
            <span class="w-fit rounded-pill bg-ai-soft px-2.5 py-1 text-xs font-semibold text-ai">
              Đã tham gia
            </span>

            <h3 class="mt-4 line-clamp-2 font-heading text-xl font-bold text-app-text group-hover:text-secondary">
              {{ course.title }}
            </h3>

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

              <ArrowRight :size="20" class="text-secondary transition group-hover:translate-x-1" />
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
  </section>
</template>