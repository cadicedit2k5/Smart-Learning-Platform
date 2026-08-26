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

import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import BaseInput from '@/shared/components/BaseInput.vue'

import {
  createCourse,
  getMyCourses,
  type Course,
  type CourseInput,
  type CourseStatus,
  type CourseVisibility,
} from '../api/courseApi'
import CourseFormModal from '../components/CourseFormModal.vue'
import { useLecturerApiError } from '../composables/useLecturerApiError'

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

const formatDate = (value: string) => {
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'medium',
  }).format(new Date(value))
}

const statusLabel: Record<CourseStatus, string> = {
  DRAFT: 'Bản nháp',
  PUBLISHED: 'Đã xuất bản',
  ARCHIVED: 'Đã lưu trữ',
}

const statusClass: Record<CourseStatus, string> = {
  DRAFT: 'bg-amber-50 text-amber-800',
  PUBLISHED: 'bg-ai-soft text-ai',
  ARCHIVED: 'bg-app-surface-muted text-app-text-muted',
}

const visibilityLabel: Record<CourseVisibility, string> = {
  PUBLIC: 'Công khai',
  PRIVATE: 'Riêng tư',
  INVITE_ONLY: 'Chỉ người được mời',
}

onMounted(() => {
  void loadCourses()
})
</script>

<template>
  <section class="mx-auto max-w-app space-y-6">
    <header class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
      <div>
        <p class="text-sm font-semibold text-secondary">Không gian giảng dạy</p>
        <h1 class="mt-1 font-heading text-3xl font-bold tracking-tight text-app-text">
          Khóa học của tôi
        </h1>
        <p class="mt-2 max-w-2xl text-sm leading-6 text-app-text-muted">
          Xây dựng nội dung, quản lý quyền truy cập và khai thác trợ lý AI cho từng khóa học.
        </p>
      </div>

      <BaseButton @click="openCreate">
        <template #leading><Plus :size="18" /></template>
        Tạo khóa học
      </BaseButton>
    </header>

    <div class="grid gap-4 sm:grid-cols-3">
      <article class="rounded-card border border-app-border bg-app-surface p-5 shadow-card">
        <p class="text-sm font-medium text-app-text-muted">Tổng khóa học</p>
        <p class="mt-2 font-heading text-3xl font-bold text-app-text">{{ courses.length }}</p>
      </article>
      <article class="rounded-card border border-app-border bg-app-surface p-5 shadow-card">
        <p class="text-sm font-medium text-app-text-muted">Đã xuất bản</p>
        <p class="mt-2 font-heading text-3xl font-bold text-ai">{{ publishedCount }}</p>
      </article>
      <article class="rounded-card border border-app-border bg-app-surface p-5 shadow-card">
        <p class="text-sm font-medium text-app-text-muted">Đang soạn</p>
        <p class="mt-2 font-heading text-3xl font-bold text-amber-700">{{ draftCount }}</p>
      </article>
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

    <section
      v-else-if="filteredCourses.length === 0"
      class="rounded-card border border-dashed border-app-border bg-app-surface px-6 py-16 text-center"
    >
      <BookOpen :size="44" class="mx-auto text-app-text-muted/40" />
      <h2 class="mt-4 font-heading text-xl font-bold text-app-text">
        {{ courses.length === 0 ? 'Chưa có khóa học nào' : 'Không tìm thấy khóa học phù hợp' }}
      </h2>
      <p class="mx-auto mt-2 max-w-md text-sm text-app-text-muted">
        {{
          courses.length === 0
            ? 'Tạo khóa học đầu tiên để bắt đầu xây dựng nội dung và mời học viên.'
            : 'Thử thay đổi từ khóa hoặc bộ lọc trạng thái.'
        }}
      </p>
      <BaseButton v-if="courses.length === 0" class="mt-5" @click="openCreate">
        <template #leading><Plus :size="18" /></template>
        Tạo khóa học đầu tiên
      </BaseButton>
    </section>

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
            <span
              class="rounded-pill px-2.5 py-1 text-xs font-semibold"
              :class="statusClass[course.status]"
            >
              {{ statusLabel[course.status] }}
            </span>
            <span class="flex items-center gap-1.5 text-xs text-app-text-muted">
              <Globe2 v-if="course.visibility === 'PUBLIC'" :size="14" />
              <UsersRound v-else-if="course.visibility === 'INVITE_ONLY'" :size="14" />
              <LockKeyhole v-else :size="14" />
              {{ visibilityLabel[course.visibility] }}
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
