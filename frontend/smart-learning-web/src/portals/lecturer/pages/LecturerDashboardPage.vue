<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  ArrowRight,
  BookOpen,
  CheckCircle2,
  FileEdit,
  Plus,
  Archive,
} from 'lucide-vue-next'

import { useAuthStore } from '@/features/auth/stores'
import {
  BaseAlert,
  BaseBadge,
  BaseButton,
  BaseEmptyState,
  BaseStatCard,
} from '@/shared/components'
import CourseCover from '@/shared/course/CourseCover.vue'
import { getMyCourses, type Course } from '@/shared/course'
import { courseStatusLabel, courseStatusTone } from '@/shared/course/presentation'
import { storedRichTextToPlainText } from '@/shared/rich-text'
import { formatDate } from '@/shared/utils'

import { useLecturerApiError } from '../composables/useLecturerApiError'

const authStore = useAuthStore()
const { handleApiError } = useLecturerApiError()

const courses = ref<Course[]>([])
const totalCourses = ref(0)
const draftCourses = ref(0)
const publishedCourses = ref(0)
const archivedCourses = ref(0)
const loading = ref(true)
const loadError = ref('')

const displayName = computed(() => authStore.user?.fullName || 'Giảng viên')

const descriptionPreview = (description: string | null) =>
  storedRichTextToPlainText(description) || 'Chưa có mô tả cho khóa học này.'

const loadDashboard = async () => {
  loading.value = true
  loadError.value = ''

  try {
    const [all, draft, published, archived] = await Promise.all([
      getMyCourses({ page: 1 }),
      getMyCourses({ page: 1, status: 'DRAFT' }),
      getMyCourses({ page: 1, status: 'PUBLISHED' }),
      getMyCourses({ page: 1, status: 'ARCHIVED' }),
    ])

    courses.value = all.content.slice(0, 3)
    totalCourses.value = all.pageable.totalElements
    draftCourses.value = draft.pageable.totalElements
    publishedCourses.value = published.pageable.totalElements
    archivedCourses.value = archived.pageable.totalElements
  } catch (error) {
    loadError.value = handleApiError(error, 'Không thể tải thông tin trang quản lý.').message
  } finally {
    loading.value = false
  }
}

onMounted(() => void loadDashboard())
</script>

<template>
  <section class="mx-auto max-w-app space-y-8">
    <section class="overflow-hidden rounded-panel bg-primary text-white shadow-card">
      <div class="grid items-center gap-8 px-6 py-8 sm:px-8 lg:grid-cols-[minmax(0,1fr)_18rem] lg:py-10">
        <div>
          <p class="text-sm font-semibold text-secondary-soft">Không gian giảng viên</p>

          <h1 class="mt-2 font-heading text-3xl font-bold sm:text-4xl">
            Chào {{ displayName }}
          </h1>

          <p class="mt-3 max-w-2xl text-sm leading-6 text-white/75 sm:text-base">
            Quản lý khóa học, xây dựng nội dung và theo dõi hoạt động học tập trong cùng một không gian.
          </p>

          <div class="mt-6 flex flex-wrap gap-3">
            <RouterLink :to="{ name: 'lecturer-courses' }">
              <BaseButton>
                Quản lý khóa học
                <template #trailing><ArrowRight :size="17" /></template>
              </BaseButton>
            </RouterLink>

            <RouterLink
              :to="{ name: 'lecturer-courses' }"
              class="inline-flex h-11 items-center gap-2 rounded-control border border-white/25 px-4 text-sm font-semibold text-white transition hover:bg-white/10"
            >
              <Plus :size="17" />
              Tạo khóa học
            </RouterLink>
          </div>
        </div>

        <div class="hidden justify-self-end lg:block">
          <div class="flex h-44 w-44 items-center justify-center rounded-full bg-white/10">
            <div class="flex h-28 w-28 items-center justify-center rounded-full bg-white/10">
              <BookOpen :size="54" class="text-secondary-soft" />
            </div>
          </div>
        </div>
      </div>
    </section>

    <BaseAlert v-if="loadError">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <span>{{ loadError }}</span>
        <button type="button" class="font-semibold underline" @click="loadDashboard">Thử lại</button>
      </div>
    </BaseAlert>

    <div v-if="loading" class="grid gap-5 sm:grid-cols-2 xl:grid-cols-4">
      <div v-for="index in 4" :key="index" class="h-32 animate-pulse rounded-panel bg-app-surface-muted" />
    </div>

    <div v-else class="grid gap-5 sm:grid-cols-2 xl:grid-cols-4">
      <BaseStatCard label="Tổng khóa học" :value="totalCourses">
        <template #icon><BookOpen :size="20" /></template>
      </BaseStatCard>

      <BaseStatCard label="Bản nháp" :value="draftCourses" tone="warning">
        <template #icon><FileEdit :size="20" /></template>
      </BaseStatCard>

      <BaseStatCard label="Đã xuất bản" :value="publishedCourses" tone="success">
        <template #icon><CheckCircle2 :size="20" /></template>
      </BaseStatCard>

      <BaseStatCard label="Đã lưu trữ" :value="archivedCourses">
        <template #icon><Archive :size="20" /></template>
      </BaseStatCard>
    </div>

    <section>
      <div class="mb-4 flex items-end justify-between gap-4">
        <div>
          <h2 class="font-heading text-2xl font-bold text-app-text">Khóa học của bạn</h2>

          <p class="mt-1 text-sm text-app-text-muted">
            Truy cập nhanh các khóa học đang quản lý.
          </p>
        </div>

        <RouterLink
          v-if="courses.length"
          :to="{ name: 'lecturer-courses' }"
          class="shrink-0 text-sm font-semibold text-secondary hover:underline"
        >
          Xem tất cả
        </RouterLink>
      </div>

      <div v-if="loading" class="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
        <div v-for="index in 3" :key="index" class="h-80 animate-pulse rounded-card bg-app-surface-muted" />
      </div>

      <BaseEmptyState
        v-else-if="courses.length === 0"
        title="Chưa có khóa học nào"
        description="Tạo khóa học đầu tiên để bắt đầu xây dựng nội dung và quản lý học viên."
      >
        <template #icon><BookOpen :size="24" /></template>

        <template #actions>
          <RouterLink :to="{ name: 'lecturer-courses' }">
            <BaseButton>
              <template #leading><Plus :size="17" /></template>
              Tạo khóa học đầu tiên
            </BaseButton>
          </RouterLink>
        </template>
      </BaseEmptyState>

      <div v-else class="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
        <RouterLink
          v-for="course in courses"
          :key="course.id"
          :to="{ name: 'lecturer-course-detail', params: { courseId: course.id } }"
          class="group overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card transition hover:-translate-y-0.5 hover:border-secondary/50 hover:shadow-overlay"
        >
          <CourseCover :image-url="course.imageUrl" :title="course.title" />

          <div class="p-5">
            <div class="flex items-center justify-between gap-3">
              <BaseBadge :tone="courseStatusTone[course.status]">
                {{ courseStatusLabel[course.status] }}
              </BaseBadge>

              <span v-if="course.level" class="text-xs font-medium text-app-text-muted">
                {{ course.level }}
              </span>
            </div>

            <h3 class="mt-4 line-clamp-2 font-heading text-lg font-bold text-app-text group-hover:text-secondary">
              {{ course.title }}
            </h3>

            <p class="mt-2 line-clamp-2 text-sm leading-6 text-app-text-muted">
              {{ descriptionPreview(course.description) }}
            </p>

            <div class="mt-5 flex items-center justify-between gap-3 border-t border-app-border pt-4">
              <span class="text-xs text-app-text-muted">
                Cập nhật {{ formatDate(course.updatedAt) }}
              </span>

              <ArrowRight :size="18" class="text-secondary transition group-hover:translate-x-1" />
            </div>
          </div>
        </RouterLink>
      </div>
    </section>
  </section>
</template>