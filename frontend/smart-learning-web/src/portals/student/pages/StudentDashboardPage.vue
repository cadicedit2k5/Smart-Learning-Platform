<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowRight, BookOpen, Compass, GraduationCap } from 'lucide-vue-next'

import { useAuthStore } from '@/features/auth/stores'
import {
  BaseAlert,
  BaseButton,
  BaseEmptyState,
  BaseStatCard,
} from '@/shared/components'
import CourseCover from '@/shared/course/CourseCover.vue'
import { getMyCourses, type Course } from '@/shared/course'
import { storedRichTextToPlainText } from '@/shared/rich-text'
import { formatDate } from '@/shared/utils'

import { useStudentApiError } from '../composables/useStudentApiError'

const authStore = useAuthStore()
const { handleApiError } = useStudentApiError()

const courses = ref<Course[]>([])
const totalCourses = ref(0)
const loading = ref(true)
const loadError = ref('')

const displayName = computed(() => authStore.user?.fullName || 'Học viên')

const descriptionPreview = (description: string | null) =>
  storedRichTextToPlainText(description) || 'Khóa học chưa có mô tả.'

const loadDashboard = async () => {
  loading.value = true
  loadError.value = ''

  try {
    const response = await getMyCourses({ page: 1 })
    courses.value = response.content.slice(0, 3)
    totalCourses.value = response.pageable.totalElements
  } catch (error) {
    loadError.value = handleApiError(error, 'Không thể tải thông tin trang chủ.').message
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
          <p class="text-sm font-semibold text-secondary-soft">Không gian học tập</p>

          <h1 class="mt-2 font-heading text-3xl font-bold sm:text-4xl">
            Chào {{ displayName }}
          </h1>

          <p class="mt-3 max-w-2xl text-sm leading-6 text-white/75 sm:text-base">
            Tiếp tục các khóa học đang tham gia hoặc khám phá thêm nội dung mới phù hợp với bạn.
          </p>

          <div class="mt-6 flex flex-wrap gap-3">
            <RouterLink :to="{ name: 'student-courses' }">
              <BaseButton>
                Khóa học của tôi
                <template #trailing><ArrowRight :size="17" /></template>
              </BaseButton>
            </RouterLink>

            <RouterLink
              :to="{ name: 'student-public-courses' }"
              class="inline-flex h-11 items-center gap-2 rounded-control border border-white/25 px-4 text-sm font-semibold text-white transition hover:bg-white/10"
            >
              <Compass :size="17" />
              Khám phá khóa học
            </RouterLink>
          </div>
        </div>

        <div class="hidden justify-self-end lg:block">
          <div class="flex h-44 w-44 items-center justify-center rounded-full bg-white/10">
            <div class="flex h-28 w-28 items-center justify-center rounded-full bg-white/10">
              <GraduationCap :size="56" class="text-secondary-soft" />
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

    <div v-if="loading" class="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
      <div v-for="index in 3" :key="index" class="h-32 animate-pulse rounded-panel bg-app-surface-muted" />
    </div>

    <div v-else class="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
      <BaseStatCard label="Khóa học đang tham gia" :value="totalCourses" tone="secondary">
        <template #icon><BookOpen :size="20" /></template>
      </BaseStatCard>

      <RouterLink
        :to="{ name: 'student-courses' }"
        class="group flex items-center justify-between rounded-panel border border-app-border bg-app-surface p-5 shadow-card transition hover:border-secondary/40 hover:shadow-overlay"
      >
        <div>
          <p class="text-sm font-medium text-app-text-muted">Không gian học tập</p>
          <p class="mt-2 font-heading text-lg font-bold text-app-text">Tiếp tục học</p>
          <p class="mt-1 text-sm text-app-text-muted">Mở danh sách khóa học của bạn.</p>
        </div>

        <ArrowRight :size="22" class="text-secondary transition group-hover:translate-x-1" />
      </RouterLink>

      <RouterLink
        :to="{ name: 'student-public-courses' }"
        class="group flex items-center justify-between rounded-panel border border-app-border bg-app-surface p-5 shadow-card transition hover:border-secondary/40 hover:shadow-overlay"
      >
        <div>
          <p class="text-sm font-medium text-app-text-muted">Khám phá</p>
          <p class="mt-2 font-heading text-lg font-bold text-app-text">Tìm khóa học mới</p>
          <p class="mt-1 text-sm text-app-text-muted">Xem các khóa học đang được công khai.</p>
        </div>

        <Compass :size="22" class="text-secondary" />
      </RouterLink>
    </div>

    <section>
      <div class="mb-4 flex items-end justify-between gap-4">
        <div>
          <h2 class="font-heading text-2xl font-bold text-app-text">Khóa học của bạn</h2>
          <p class="mt-1 text-sm text-app-text-muted">
            Truy cập nhanh các khóa học đang tham gia.
          </p>
        </div>

        <RouterLink
          v-if="courses.length"
          :to="{ name: 'student-courses' }"
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
        title="Bạn chưa tham gia khóa học nào"
        description="Khám phá các khóa học công khai và gửi yêu cầu tham gia để bắt đầu học."
      >
        <template #icon><BookOpen :size="24" /></template>

        <template #actions>
          <RouterLink :to="{ name: 'student-public-courses' }">
            <BaseButton>
              <template #leading><Compass :size="17" /></template>
              Khám phá khóa học
            </BaseButton>
          </RouterLink>
        </template>
      </BaseEmptyState>

      <div v-else class="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
        <RouterLink
          v-for="course in courses"
          :key="course.id"
          :to="{ name: 'student-course-detail', params: { courseId: course.id } }"
          class="group overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card transition hover:-translate-y-0.5 hover:border-secondary/50 hover:shadow-overlay"
        >
          <CourseCover :image-url="course.imageUrl" :title="course.title" />

          <div class="p-5">
            <div class="flex items-center justify-between gap-3">
              <span class="rounded-pill bg-ai-soft px-2.5 py-1 text-xs font-semibold text-ai">
                Đang học
              </span>

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