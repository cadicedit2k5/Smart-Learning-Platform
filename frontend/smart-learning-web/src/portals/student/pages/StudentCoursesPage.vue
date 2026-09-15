<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowRight, BookOpen, CheckCircle2, Search, X } from 'lucide-vue-next'

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
import type { PaginatedData } from '@/shared/api'

import {
  getCourseProgress,
  type CourseLearningProgress,
} from '../api/learningProgressApi'
import { useStudentApiError } from '../composables/useStudentApiError'

const { handleApiError } = useStudentApiError()

const coursesPage = ref<PaginatedData<Course>>({
  content: [],
  pageable: { page: 1, size: 6, totalElements: 0, totalPages: 0 },
})

const progressByCourse = ref<Record<string, CourseLearningProgress>>({})
const loading = ref(true)
const loadError = ref('')
const keyword = ref('')
const appliedKeyword = ref('')
const currentPage = ref(1)

const totalPages = computed(() => coursesPage.value.pageable.totalPages)
const hasSearch = computed(() => appliedKeyword.value !== '')
const hasKeyword = computed(() => keyword.value.trim() !== '')

const resultText = computed(() => {
  const total = coursesPage.value.pageable.totalElements

  if (hasSearch.value) {
    return `${total} khóa học cho "${appliedKeyword.value}"`
  }

  return `${total} khóa học đang tham gia`
})

const descriptionPreview = (description: string | null) =>
  storedRichTextToPlainText(description) || 'Khóa học chưa có mô tả.'

const courseProgress = (courseId: string) =>
  progressByCourse.value[courseId]

const progressPercentage = (courseId: string) =>
  courseProgress(courseId)?.progressPercentage ?? 0

const isCompleted = (courseId: string) =>
  progressPercentage(courseId) >= 100

const loadProgress = async (courses: Course[]) => {
  const results = await Promise.allSettled(
    courses.map(async (course) => ({
      courseId: course.id,
      progress: await getCourseProgress(course.id),
    })),
  )

  progressByCourse.value = Object.fromEntries(
    results
      .filter(
        (
          result,
        ): result is PromiseFulfilledResult<{
          courseId: string
          progress: CourseLearningProgress
        }> => result.status === 'fulfilled',
      )
      .map((result) => [
        result.value.courseId,
        result.value.progress,
      ]),
  )
}

const loadCourses = async () => {
  loading.value = true
  loadError.value = ''

  try {
    coursesPage.value = await getMyCourses({
      page: currentPage.value,
      keyword: appliedKeyword.value || undefined,
    })

    await loadProgress(coursesPage.value.content)
  } catch (error) {
    loadError.value = handleApiError(
      error,
      'Không thể tải danh sách khóa học.',
    ).message
  } finally {
    loading.value = false
  }
}

const applySearch = () => {
  const value = keyword.value.trim()

  if (value === appliedKeyword.value) return

  appliedKeyword.value = value
  currentPage.value = 1
  void loadCourses()
}

const clearSearch = () => {
  keyword.value = ''

  if (!appliedKeyword.value) return

  appliedKeyword.value = ''
  currentPage.value = 1
  void loadCourses()
}

const goToPage = (page: number) => {
  if (
    page < 1 ||
    page > totalPages.value ||
    page === currentPage.value
  ) {
    return
  }

  currentPage.value = page
  void loadCourses()
}

onMounted(() => void loadCourses())
</script>

<template>
  <section class="mx-auto max-w-app space-y-7">
    <BasePageHeader
      title="Khóa học của tôi"
      description="Tiếp tục học và theo dõi tiến độ các khóa học bạn đang tham gia."
    />

    <BaseAlert v-if="loadError">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <span>{{ loadError }}</span>

        <button
          type="button"
          class="font-semibold underline"
          @click="loadCourses"
        >
          Thử lại
        </button>
      </div>
    </BaseAlert>

    <section
      class="flex flex-col gap-4 rounded-[1.25rem] border border-app-border bg-app-surface p-5 shadow-card lg:flex-row lg:items-center"
    >
      <div class="min-w-0 lg:w-72 lg:shrink-0">
        <h2 class="font-heading text-lg font-bold text-app-text">
          {{ hasSearch ? 'Kết quả tìm kiếm' : 'Danh sách khóa học' }}
        </h2>

        <p class="mt-1 text-sm text-app-text-muted">
          {{ resultText }}
        </p>
      </div>

      <form
        class="flex min-w-0 flex-1 gap-2 lg:ml-8"
        @submit.prevent="applySearch"
      >
        <div class="min-w-0 flex-1">
          <BaseInput
            v-model="keyword"
            placeholder="Tìm theo tên khóa học..."
            autocomplete="off"
          >
            <template #leading>
              <Search :size="17" />
            </template>

            <template #trailing>
              <button
                v-if="hasKeyword"
                type="button"
                aria-label="Xóa tìm kiếm"
                class="flex h-7 w-7 items-center justify-center rounded-lg text-app-text-muted transition hover:bg-app-surface-muted hover:text-app-text"
                @click="clearSearch"
              >
                <X :size="15" />
              </button>
            </template>
          </BaseInput>
        </div>

        <BaseButton
          type="submit"
          class="shrink-0"
          :disabled="keyword.trim() === appliedKeyword"
        >
          Tìm
        </BaseButton>
      </form>
    </section>

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

        <div class="space-y-4 p-5">
          <div class="h-5 w-24 animate-pulse rounded bg-app-surface-muted" />
          <div class="h-6 w-4/5 animate-pulse rounded bg-app-surface-muted" />
          <div class="h-4 w-full animate-pulse rounded bg-app-surface-muted" />
          <div class="h-2 w-full animate-pulse rounded bg-app-surface-muted" />
        </div>
      </div>
    </div>

    <BaseEmptyState
      v-else-if="coursesPage.content.length === 0"
      :title="
        hasSearch
          ? 'Không tìm thấy khóa học phù hợp'
          : 'Bạn chưa tham gia khóa học nào'
      "
      :description="
        hasSearch
          ? `Không có khóa học nào phù hợp với &quot;${appliedKeyword}&quot;. Hãy thử từ khóa khác.`
          : 'Khám phá các khóa học công khai để gửi yêu cầu tham gia.'
      "
    >
      <template #icon>
        <BookOpen :size="24" />
      </template>

      <template
        v-if="hasSearch"
        #actions
      >
        <BaseButton
          variant="secondary"
          @click="clearSearch"
        >
          Xóa tìm kiếm
        </BaseButton>
      </template>
    </BaseEmptyState>

    <div
      v-else
      class="grid gap-6 md:grid-cols-2 xl:grid-cols-3"
    >
      <RouterLink
        v-for="course in coursesPage.content"
        :key="course.id"
        :to="{
          name: 'student-course-detail',
          params: { courseId: course.id },
        }"
        class="group flex min-w-0 flex-col overflow-hidden rounded-[1.25rem] bg-app-surface ring-1 ring-app-border transition duration-300 hover:-translate-y-1 hover:shadow-overlay hover:ring-secondary/25"
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
            v-if="isCompleted(course.id)"
            class="absolute right-3 top-3 inline-flex items-center gap-1.5 rounded-pill bg-white/95 px-2.5 py-1 text-xs font-semibold text-ai shadow-sm"
          >
            <CheckCircle2 :size="13" />
            Hoàn thành
          </span>
        </div>

        <div class="flex flex-1 flex-col p-5">
          <p
            class="text-xs font-semibold"
            :class="
              isCompleted(course.id)
                ? 'text-ai'
                : 'text-secondary'
            "
          >
            {{
              isCompleted(course.id)
                ? 'Đã hoàn thành'
                : progressPercentage(course.id) > 0
                  ? 'Đang học'
                  : 'Chưa bắt đầu'
            }}
          </p>

          <h3
            class="mt-2 line-clamp-2 font-heading text-xl font-bold leading-7 text-app-text transition-colors group-hover:text-secondary"
          >
            {{ course.title }}
          </h3>

          <p
            class="mt-2 line-clamp-2 text-sm leading-6 text-app-text-muted"
          >
            {{ descriptionPreview(course.description) }}
          </p>

          <div class="mt-5">
            <div class="flex items-center justify-between gap-3">
              <span class="text-xs font-semibold text-app-text">
                Tiến độ
              </span>

              <span
                class="text-sm font-bold"
                :class="
                  isCompleted(course.id)
                    ? 'text-ai'
                    : 'text-secondary'
                "
              >
                {{ progressPercentage(course.id) }}%
              </span>
            </div>

            <div
              class="mt-2 h-2 overflow-hidden rounded-pill bg-app-surface-muted"
            >
              <div
                class="h-full rounded-pill transition-all duration-500"
                :class="
                  isCompleted(course.id)
                    ? 'bg-ai'
                    : 'bg-secondary'
                "
                :style="{
                  width: `${progressPercentage(course.id)}%`,
                }"
              />
            </div>

            <div
              class="mt-2 flex items-center justify-between text-xs text-app-text-muted"
            >
              <span v-if="courseProgress(course.id)">
                {{ courseProgress(course.id)?.completedTopics }}
                /
                {{ courseProgress(course.id)?.totalTopics }}
                bài học
              </span>

              <span v-else>
                Chưa có dữ liệu tiến độ
              </span>
            </div>
          </div>

          <div
            class="mt-auto flex items-center justify-between border-t border-app-border pt-5"
          >
            <span class="text-sm font-semibold text-secondary">
              {{
                isCompleted(course.id)
                  ? 'Xem lại khóa học'
                  : progressPercentage(course.id) > 0
                    ? 'Tiếp tục học'
                    : 'Bắt đầu học'
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
      :page="currentPage"
      :total-pages="totalPages"
      :total-elements="coursesPage.pageable.totalElements"
      @previous="goToPage(currentPage - 1)"
      @next="goToPage(currentPage + 1)"
    />
  </section>
</template>