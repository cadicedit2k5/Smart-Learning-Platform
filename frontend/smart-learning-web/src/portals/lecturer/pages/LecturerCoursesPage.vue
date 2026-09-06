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
  BaseStatCard,
} from '@/shared/components'

import {
  createCourse,
  type CourseInput,
} from '../api/courseApi'
import CourseFormModal from '../components/CourseFormModal.vue'
import { useLecturerApiError } from '../composables/useLecturerApiError'
import type { Course, CourseStatus } from '@/shared/course/types.ts'
import { getMyCourses } from '@/shared/course/api.ts'
import { courseStatusLabel, courseStatusTone, courseVisibilityLabel } from '@/shared/course/presentation.ts'
import { formatDate } from '@/shared/utils/date.ts'

const { handleApiError } = useLecturerApiError()

const courses = ref<Course[]>([])
const loading = ref(true)
const loadError = ref('')
const keyword = ref('')
const statusFilter = ref<CourseStatus | ''>('')
const formOpen = ref(false)
const saving = ref(false)
const formMessage = ref('')
const formErrors = ref<Record<string, string>>({})

const filteredCourses = computed(() => {
  const query = keyword.value.trim().toLocaleLowerCase('vi')

  return courses.value.filter((course) => {
    const matchesStatus = !statusFilter.value || course.status === statusFilter.value
    const matchesKeyword =
      !query ||
      course.title.toLocaleLowerCase('vi').includes(query) ||
      course.description?.toLocaleLowerCase('vi').includes(query) ||
      course.level?.toLocaleLowerCase('vi').includes(query)

    return matchesStatus && Boolean(matchesKeyword)
  })
})

const publishedCount = computed(() => {
  return courses.value.filter((course) => course.status === 'PUBLISHED').length
})

const draftCount = computed(() => {
  return courses.value.filter((course) => course.status === 'DRAFT').length
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
    const created = await createCourse(input)
    courses.value.unshift(created)
    formOpen.value = false
  } catch (error) {
    const parsed = handleApiError(error, 'Không thể tạo khóa học.')
    formMessage.value = parsed.message
    formErrors.value = parsed.fieldErrors
  } finally {
    saving.value = false
  }
}


onMounted(() => {
  void loadCourses()
})
</script>

<template>
  <section class="mx-auto max-w-app space-y-6">
    <BasePageHeader
      title="Khóa học của tôi"
    >
      <template #actions>
        <BaseButton @click="openCreate">
          <template #leading>
            <Plus :size="18" />
          </template>

          Tạo khóa học
        </BaseButton>
      </template>
    </BasePageHeader>

    <div class="grid gap-4 sm:grid-cols-3">
      <BaseStatCard
        label="Tổng khóa học"
        :value="courses.length"
      />

      <BaseStatCard
        label="Đã xuất bản"
        :value="publishedCount"
        tone="success"
      />

      <BaseStatCard
        label="Đang soạn"
        :value="draftCount"
        tone="warning"
      />
    </div>

    <BaseAlert v-if="loadError">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <span>{{ loadError }}</span>
        <button type="button" class="font-semibold underline" @click="loadCourses">Thử lại</button>
      </div>
    </BaseAlert>

    <section class="rounded-card border border-app-border bg-app-surface p-4 shadow-card sm:p-5">
      <div class="grid gap-3 sm:grid-cols-[minmax(0,1fr)_13rem]">
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
      </div>
    </section>

    <div v-if="loading" class="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
      <div
        v-for="index in 6"
        :key="index"
        class="h-72 animate-pulse rounded-card bg-app-surface-muted"
      />
    </div>

    <BaseEmptyState
      v-else-if="filteredCourses.length === 0"
      :title="
        courses.length === 0
          ? 'Chưa có khóa học nào'
          : 'Không tìm thấy khóa học phù hợp'
      "
      :description="
        courses.length === 0
          ? 'Tạo khóa học đầu tiên để bắt đầu xây dựng nội dung và mời học viên.'
          : 'Thử thay đổi từ khóa hoặc bộ lọc trạng thái.'
      "
    >
      <template #icon>
        <BookOpen :size="24" />
      </template>

      <template
        v-if="courses.length === 0"
        #actions
      >
        <BaseButton @click="openCreate">
          <template #leading>
            <Plus :size="18" />
          </template>

          Tạo khóa học đầu tiên
        </BaseButton>
      </template>
    </BaseEmptyState>

    <div v-else class="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
      <RouterLink
        v-for="course in filteredCourses"
        :key="course.id"
        :to="{ name: 'lecturer-course-detail', params: { courseId: course.id } }"
        class="group flex min-h-72 flex-col overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card transition hover:-translate-y-0.5 hover:border-secondary/50 hover:shadow-overlay"
      >
        <div class="h-2 bg-primary" :class="course.status === 'PUBLISHED' ? 'bg-ai' : ''" />
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

          <h2
            class="mt-5 line-clamp-2 font-heading text-xl font-bold text-app-text group-hover:text-secondary"
          >
            {{ course.title }}
          </h2>
          <p class="mt-2 line-clamp-3 text-sm leading-6 text-app-text-muted">
            {{ course.description || 'Chưa có mô tả cho khóa học này.' }}
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
      </RouterLink>
    </div>

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
