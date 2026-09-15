<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  ArrowRight,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  Clock3,
  Search,
} from 'lucide-vue-next'

import {
  BaseAlert,
  BaseButton,
  BaseEmptyState,
  BaseInput,
  BasePageHeader,
  BasePagination,
} from '@/shared/components'
import CourseCover from '@/shared/course/CourseCover.vue'
import type { PaginatedData } from '@/shared/api'
import { storedRichTextToPlainText } from '@/shared/rich-text'
import { formatDate } from '@/shared/utils'

import {
  getPublicCourses,
  type PublicCourse,
} from '../api/courseApi'
import { useStudentApiError } from '../composables/useStudentApiError'

const { handleApiError } = useStudentApiError()

const coursesPage = ref<PaginatedData<PublicCourse>>({
  content: [],
  pageable: { page: 1, size: 6, totalElements: 0, totalPages: 0 },
})

const loading = ref(true)
const loadError = ref('')
const keyword = ref('')
const appliedKeyword = ref('')

const resultLabel = computed(() => {
  const total = coursesPage.value.pageable.totalElements

  if (!appliedKeyword.value) {
    return `${total} khóa học`
  }

  return `${total} kết quả cho "${appliedKeyword.value}"`
})

const descriptionPreview = (description: string | null) =>
  storedRichTextToPlainText(description) || 'Khóa học chưa có mô tả.'

const courseDestination = (course: PublicCourse) => {
  if (course.currentUserMembershipStatus === 'ACTIVE') {
    return {
      name: 'student-course-detail',
      params: { courseId: course.id },
    }
  }

  return {
    name: 'student-public-course-detail',
    params: { courseId: course.id },
  }
}

const loadCourses = async (page = 1) => {
  loading.value = true
  loadError.value = ''

  try {
    coursesPage.value = await getPublicCourses({
      page,
      keyword: appliedKeyword.value || undefined,
    })
  } catch (error) {
    loadError.value = handleApiError(
      error,
      'Không thể tải danh sách khóa học công khai.',
    ).message
  } finally {
    loading.value = false
  }
}

const applySearch = () => {
  appliedKeyword.value = keyword.value.trim()
  void loadCourses(1)
}

const resetSearch = () => {
  keyword.value = ''
  appliedKeyword.value = ''
  void loadCourses(1)
}

const goToPage = async (page: number) => {
  if (
    page < 1 ||
    page > coursesPage.value.pageable.totalPages ||
    page === coursesPage.value.pageable.page
  ) {
    return
  }

  await loadCourses(page)
}

onMounted(() => void loadCourses())
</script>

<template>
  <section class="mx-auto max-w-app space-y-7">
    <BasePageHeader
      title="Khám phá khóa học"
    />

    <section
      class="rounded-[1.25rem] border border-app-border bg-app-surface p-4 shadow-card sm:p-5"
    >
      <form
        class="flex flex-col gap-3 sm:flex-row"
        @submit.prevent="applySearch"
      >
        <div class="min-w-0 flex-1">
          <BaseInput
            v-model="keyword"
            placeholder="Bạn muốn học gì?"
          >
            <template #leading>
              <Search :size="18" />
            </template>
          </BaseInput>
        </div>

        <BaseButton
          type="submit"
          class="sm:min-w-24"
        >
          Tìm kiếm
        </BaseButton>
      </form>

      <div
        v-if="appliedKeyword"
        class="mt-3 flex items-center justify-between gap-3"
      >
        <p class="text-sm text-app-text-muted">
          {{ resultLabel }}
        </p>

        <button
          type="button"
          class="text-sm font-semibold text-secondary transition hover:text-secondary-hover"
          @click="resetSearch"
        >
          Xóa tìm kiếm
        </button>
      </div>
    </section>

    <BaseAlert v-if="loadError">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <span>{{ loadError }}</span>

        <BaseButton
          variant="secondary"
          @click="loadCourses(coursesPage.pageable.page)"
        >
          Thử lại
        </BaseButton>
      </div>
    </BaseAlert>



    <div
      v-if="loading"
      class="grid gap-6 md:grid-cols-2 xl:grid-cols-3"
    >
      <div
        v-for="index in 6"
        :key="index"
        class="overflow-hidden rounded-[1.25rem] border border-app-border bg-app-surface"
      >
        <div class="aspect-[16/9] animate-pulse bg-app-surface-muted" />

        <div class="space-y-3 p-5">
          <div class="h-4 w-24 animate-pulse rounded bg-app-surface-muted" />
          <div class="h-6 w-4/5 animate-pulse rounded bg-app-surface-muted" />
          <div class="h-4 w-full animate-pulse rounded bg-app-surface-muted" />
          <div class="h-4 w-2/3 animate-pulse rounded bg-app-surface-muted" />
        </div>
      </div>
    </div>

    <BaseEmptyState
      v-else-if="coursesPage.content.length === 0"
      :title="
        appliedKeyword
          ? 'Không tìm thấy khóa học phù hợp'
          : 'Chưa có khóa học công khai'
      "
      :description="
        appliedKeyword
          ? 'Thử tìm kiếm bằng một từ khóa khác.'
          : 'Hiện chưa có khóa học nào được công khai.'
      "
    >
      <template #icon>
        <BookOpen :size="24" />
      </template>
    </BaseEmptyState>

    <div
      v-else
      class="grid gap-6 md:grid-cols-2 xl:grid-cols-3"
    >
      <RouterLink
        v-for="course in coursesPage.content"
        :key="course.id"
        :to="courseDestination(course)"
        class="group relative flex min-w-0 flex-col overflow-hidden rounded-[1.25rem] bg-app-surface ring-1 ring-app-border transition duration-300 hover:-translate-y-1 hover:shadow-overlay hover:ring-secondary/25"
      >
        <div class="relative overflow-hidden">
          <CourseCover
            :image-url="course.imageUrl"
            :title="course.title"
            class="transition duration-500 group-hover:scale-[1.025]"
          />

          <div
            class="pointer-events-none absolute inset-x-0 bottom-0 h-20 bg-gradient-to-t from-slate-950/35 to-transparent"
          />

          <span
            v-if="course.level"
            class="absolute bottom-3 left-3 rounded-pill border border-white/20 bg-slate-950/55 px-2.5 py-1 text-xs font-semibold text-white backdrop-blur-md"
          >
            {{ course.level }}
          </span>

          <span
            v-if="course.currentUserMembershipStatus === 'ACTIVE'"
            class="absolute right-3 top-3 inline-flex items-center gap-1.5 rounded-pill bg-white/95 px-2.5 py-1 text-xs font-semibold text-ai shadow-sm"
          >
            <CheckCircle2 :size="13" />
            Đã tham gia
          </span>

          <span
            v-else-if="course.currentUserMembershipStatus === 'PENDING'"
            class="absolute right-3 top-3 inline-flex items-center gap-1.5 rounded-pill bg-white/95 px-2.5 py-1 text-xs font-semibold text-amber-700 shadow-sm"
          >
            <Clock3 :size="13" />
            Chờ duyệt
          </span>
        </div>

        <div class="flex flex-1 flex-col p-5">
          <div
            class="flex items-center gap-2 text-xs font-medium text-app-text-muted"
          >
            <span>Khóa học</span>

            <span class="h-1 w-1 rounded-full bg-slate-300" />
          </div>

          <h2
            class="mt-3 line-clamp-2 font-heading text-xl font-bold leading-7 text-app-text transition-colors group-hover:text-secondary"
          >
            {{ course.title }}
          </h2>

          <p
            class="mt-2 mb-2 line-clamp-2 text-sm leading-6 text-app-text-muted"
          >
            {{ descriptionPreview(course.description) }}
          </p>

          <div
            class="mt-auto flex items-center justify-between border-t border-app-border pt-5"
          >
            <span
              class="text-sm font-semibold text-secondary"
            >
              {{
                course.currentUserMembershipStatus === 'ACTIVE'
                  ? 'Tiếp tục học'
                  : 'Xem khóa học'
              }}
            </span>

            <div
              class="flex h-9 w-9 items-center justify-center rounded-full bg-app-surface-muted text-app-text-muted transition duration-300 group-hover:translate-x-1 group-hover:bg-secondary group-hover:text-white"
            >
              <ArrowRight :size="17" />
            </div>
          </div>
        </div>
      </RouterLink>
    </div>

    <BasePagination
      v-if="!loading && coursesPage.pageable.totalElements > 0"
      :page="coursesPage.pageable.page"
      :total-pages="coursesPage.pageable.totalPages"
      :total-elements="coursesPage.pageable.totalElements"
      @previous="goToPage(coursesPage.pageable.page - 1)"
      @next="goToPage(coursesPage.pageable.page + 1)"
    />
  </section>
</template>