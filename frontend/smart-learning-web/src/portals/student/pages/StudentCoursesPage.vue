<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowRight, BookOpen, CalendarDays, Search } from 'lucide-vue-next'

import {
  BaseAlert,
  BaseEmptyState,
  BaseInput,
  BasePageHeader,
} from '@/shared/components'
import { useStudentApiError } from '../composables/useStudentApiError'
import { getMyCourses, type Course } from '@/shared/course'
import { formatDate } from '@/shared/utils'

const { handleApiError } = useStudentApiError()
const courses = ref<Course[]>([])
const loading = ref(true)
const loadError = ref('')
const keyword = ref('')

const filteredCourses = computed(() => {
  const query = keyword.value.trim().toLocaleLowerCase('vi')
  if (!query) return courses.value
  return courses.value.filter(
    (course) =>
      course.title.toLocaleLowerCase('vi').includes(query) ||
      course.description?.toLocaleLowerCase('vi').includes(query) ||
      course.level?.toLocaleLowerCase('vi').includes(query),
  )
})

const loadCourses = async () => {
  loading.value = true
  loadError.value = ''
  try {
    courses.value = await getMyCourses()
  } catch (error) {
    loadError.value = handleApiError(error, 'Không thể tải danh sách khóa học.').message
  } finally {
    loading.value = false
  }
}

onMounted(() => void loadCourses())
</script>

<template>
  <section class="mx-auto max-w-app space-y-6">
    <BasePageHeader
      eyebrow="Không gian học tập"
      title="Khóa học của tôi"
      description="Tiếp tục học tập và truy cập các khóa học bạn đang tham gia."
    />

    <BaseAlert v-if="loadError">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <span>{{ loadError }}</span
        ><button type="button" class="font-semibold underline" @click="loadCourses">Thử lại</button>
      </div>
    </BaseAlert>

    <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <h2 class="font-heading text-xl font-bold text-app-text">Danh sách khóa học</h2>
      <BaseInput v-model="keyword" class="w-full sm:max-w-sm" placeholder="Tìm khóa học..."
        ><template #leading><Search :size="18" /></template
      ></BaseInput>
    </div>

    <div v-if="loading" class="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
      <div
        v-for="index in 6"
        :key="index"
        class="h-64 animate-pulse rounded-card bg-app-surface-muted"
      />
    </div>
    <BaseEmptyState
      v-else-if="filteredCourses.length === 0"
      :title="
        courses.length
          ? 'Không tìm thấy khóa học phù hợp'
          : 'Bạn chưa tham gia khóa học nào'
      "
      :description="
        courses.length
          ? 'Thử thay đổi từ khóa tìm kiếm.'
          : 'Khám phá các khóa học công khai để gửi yêu cầu tham gia.'
      "
    >
      <template #icon>
        <BookOpen :size="24" />
      </template>
    </BaseEmptyState>
    <div v-else class="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
      <RouterLink
        v-for="course in filteredCourses"
        :key="course.id"
        :to="{ name: 'student-course-detail', params: { courseId: course.id } }"
        class="group flex min-h-64 flex-col overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card transition hover:-translate-y-0.5 hover:border-secondary/50 hover:shadow-overlay"
      >
        <div
          class="flex h-11 w-11 items-center justify-center rounded-card bg-secondary-soft text-secondary"
        >
          <BookOpen :size="21" />
        </div>
        <div class="flex flex-1 flex-col p-5">
          <span class="w-fit rounded-pill bg-ai-soft px-2.5 py-1 text-xs font-semibold text-ai"
            >Đã tham gia</span
          >
          <h3
            class="mt-4 line-clamp-2 font-heading text-xl font-bold text-app-text group-hover:text-secondary"
          >
            {{ course.title }}
          </h3>
          <p class="mt-2 line-clamp-3 text-sm leading-6 text-app-text-muted">
            {{ course.description || 'Khóa học chưa có mô tả.' }}
          </p>
          <div class="mt-auto flex items-end justify-between gap-3 pt-6">
            <div class="space-y-1 text-xs text-app-text-muted">
              <p v-if="course.level" class="font-medium text-app-text">{{ course.level }}</p>
              <p class="flex items-center gap-1.5">
                <CalendarDays :size="14" />Cập nhật {{ formatDate(course.updatedAt) }}
              </p>
            </div>
            <ArrowRight :size="20" class="text-secondary transition group-hover:translate-x-1" />
          </div>
        </div>
      </RouterLink>
    </div>
  </section>
</template>
