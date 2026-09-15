<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  ArrowLeft,
  BookOpen,
  CheckCircle2,
  Circle,
  CircleDot,
  GraduationCap,
  TrendingUp,
  UsersRound,
} from 'lucide-vue-next'

import { BaseAlert, BasePagination, BaseStatCard } from '@/shared/components'
import { formatDateTime } from '@/shared/utils/date'
import { useLecturerApiError } from '../composables/useLecturerApiError'
import {
  getCourseStudentProgress,
  getStudentProgress,
  type LecturerCourseProgress,
  type StudentProgressDetail,
  type StudentProgressSummary,
  type StudentTopicProgress,
} from '../api/learningProgressApi'

const props = defineProps<{
  courseId: string
}>()

const { handleApiError } = useLecturerApiError()

const progress = ref<LecturerCourseProgress | null>(null)
const selectedStudent = ref<StudentProgressSummary | null>(null)
const studentDetail = ref<StudentProgressDetail | null>(null)

const page = ref(1)
const loading = ref(true)
const loadingDetail = ref(false)
const message = ref('')
const detailMessage = ref('')

const students = computed(() => progress.value?.students.content ?? [])
const totalPages = computed(() => progress.value?.students.pageable.totalPages ?? 0)

const notStartedTopics = (student: StudentProgressSummary) => {
  return Math.max(0, student.totalTopics - student.completedTopics - student.inProgressTopics)
}

const studentStatus = (student: StudentProgressSummary) => {
  if (student.totalTopics > 0 && student.progressPercentage === 100) return 'Hoàn thành'
  if (student.completedTopics === 0 && student.inProgressTopics === 0) return 'Chưa bắt đầu'
  return 'Đang học'
}

const studentStatusClass = (student: StudentProgressSummary) => {
  if (student.totalTopics > 0 && student.progressPercentage === 100) {
    return 'bg-ai-soft text-ai'
  }

  if (student.completedTopics === 0 && student.inProgressTopics === 0) {
    return 'bg-app-surface-muted text-app-text-muted'
  }

  return 'bg-secondary-soft text-secondary'
}

const topicStatusLabel = (topic: StudentTopicProgress) => {
  if (topic.status === 'COMPLETED') return 'Hoàn thành'
  if (topic.status === 'IN_PROGRESS') return 'Đang học'
  return 'Chưa bắt đầu'
}

const loadProgress = async () => {
  loading.value = true
  message.value = ''

  try {
    progress.value = await getCourseStudentProgress(props.courseId, page.value)
  } catch (error) {
    message.value = handleApiError(error, 'Không thể tải tiến độ học tập.').message
  } finally {
    loading.value = false
  }
}

const goToPage = (newPage: number) => {
  if (newPage < 1 || newPage > totalPages.value || newPage === page.value) return

  page.value = newPage
  void loadProgress()
}

const openStudent = async (student: StudentProgressSummary) => {
  selectedStudent.value = student
  studentDetail.value = null
  detailMessage.value = ''
  loadingDetail.value = true

  try {
    studentDetail.value = await getStudentProgress(props.courseId, student.studentId)
  } catch (error) {
    detailMessage.value = handleApiError(
      error,
      'Không thể tải chi tiết tiến độ học viên.',
    ).message
  } finally {
    loadingDetail.value = false
  }
}

const backToStudents = () => {
  selectedStudent.value = null
  studentDetail.value = null
  detailMessage.value = ''
}

const retryStudent = () => {
  if (selectedStudent.value) {
    void openStudent(selectedStudent.value)
  }
}

const reset = () => {
  page.value = 1
  progress.value = null
  selectedStudent.value = null
  studentDetail.value = null
  message.value = ''
  detailMessage.value = ''
  void loadProgress()
}

watch(() => props.courseId, reset)

onMounted(() => void loadProgress())
</script>

<template>
  <section class="space-y-5">
    <template v-if="!selectedStudent">
      <header>
        <h2 class="font-heading text-xl font-bold text-app-text">
          Tiến độ học tập
        </h2>

        <p class="mt-1 text-sm text-app-text-muted">
          Theo dõi mức độ hoàn thành nội dung của học viên trong khóa học.
        </p>
      </header>

      <BaseAlert v-if="message">
        {{ message }}
      </BaseAlert>

      <div v-if="loading" class="space-y-4">
        <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <div
            v-for="index in 4"
            :key="index"
            class="h-28 animate-pulse rounded-panel bg-app-surface-muted"
          />
        </div>

        <div class="h-72 animate-pulse rounded-card bg-app-surface-muted" />
      </div>

      <template v-else-if="progress">
        <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <BaseStatCard label="Học viên" :value="progress.totalStudents">
            <template #icon>
              <UsersRound :size="19" />
            </template>
          </BaseStatCard>

          <BaseStatCard label="Bài học" :value="progress.totalTopics" tone="secondary">
            <template #icon>
              <BookOpen :size="19" />
            </template>
          </BaseStatCard>

          <BaseStatCard
            label="Tiến độ trung bình"
            :value="`${progress.averageProgressPercentage}%`"
            tone="secondary"
          >
            <template #icon>
              <TrendingUp :size="19" />
            </template>
          </BaseStatCard>

          <BaseStatCard
            label="Đã hoàn thành"
            :value="progress.completedStudents"
            tone="success"
          >
            <template #icon>
              <GraduationCap :size="19" />
            </template>
          </BaseStatCard>
        </div>

        <div
          v-if="progress.totalStudents === 0"
          class="rounded-panel border border-dashed border-app-border bg-app-surface px-6 py-14 text-center"
        >
          <UsersRound :size="40" class="mx-auto text-app-text-muted/40" />

          <h3 class="mt-4 font-heading text-lg font-bold text-app-text">
            Chưa có học viên
          </h3>

          <p class="mt-2 text-sm text-app-text-muted">
            Khóa học chưa có học viên đang hoạt động để theo dõi tiến độ.
          </p>
        </div>

        <div
          v-else
          class="overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card"
        >
          <button
            v-for="student in students"
            :key="student.studentId"
            type="button"
            class="group block w-full border-b border-app-border px-5 py-5 text-left transition last:border-b-0 hover:bg-app-surface-muted/40"
            @click="openStudent(student)"
          >
            <div class="flex flex-col gap-4 lg:flex-row lg:items-center">
              <div class="flex min-w-0 flex-1 items-center gap-3">
                <span
                  class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-secondary-soft font-bold text-secondary"
                >
                  {{ student.fullName.trim().charAt(0).toUpperCase() }}
                </span>

                <div class="min-w-0">
                  <p class="truncate font-semibold text-app-text">
                    {{ student.fullName }}
                  </p>

                  <p v-if="student.email" class="truncate text-xs text-app-text-muted">
                    {{ student.email }}
                  </p>
                </div>
              </div>

              <div class="w-full lg:w-72">
                <div class="mb-2 flex items-center justify-between gap-3 text-xs">
                  <span class="text-app-text-muted">
                    {{ student.completedTopics }} / {{ student.totalTopics }} bài hoàn thành
                  </span>

                  <span class="font-bold text-app-text">
                    {{ student.progressPercentage }}%
                  </span>
                </div>

                <div class="h-2 overflow-hidden rounded-full bg-app-surface-muted">
                  <div
                    class="h-full rounded-full transition-all"
                    :class="student.progressPercentage === 100 ? 'bg-ai' : 'bg-secondary'"
                    :style="{ width: `${student.progressPercentage}%` }"
                  />
                </div>
              </div>

              <div class="flex min-w-36 items-center justify-between gap-3 lg:justify-end">
                <span
                  class="rounded-pill px-2.5 py-1 text-xs font-semibold"
                  :class="studentStatusClass(student)"
                >
                  {{ studentStatus(student) }}
                </span>

                <span
                  class="text-sm font-semibold text-secondary transition group-hover:translate-x-1"
                >
                  Xem chi tiết →
                </span>
              </div>
            </div>

            <div
              v-if="student.inProgressTopics > 0 || notStartedTopics(student) > 0"
              class="mt-3 flex flex-wrap gap-x-4 gap-y-1 pl-14 text-xs text-app-text-muted"
            >
              <span v-if="student.inProgressTopics > 0">
                {{ student.inProgressTopics }} đang học
              </span>

              <span v-if="notStartedTopics(student) > 0">
                {{ notStartedTopics(student) }} chưa bắt đầu
              </span>
            </div>
          </button>

          <div
            v-if="progress.students.pageable.totalElements > 0"
            class="border-t border-app-border p-4"
          >
            <BasePagination
              :page="page"
              :total-pages="totalPages"
              :total-elements="progress.students.pageable.totalElements"
              @previous="goToPage(page - 1)"
              @next="goToPage(page + 1)"
            />
          </div>
        </div>
      </template>
    </template>

    <template v-else>
      <button
        type="button"
        class="inline-flex items-center gap-2 text-sm font-semibold text-app-text-muted transition hover:text-secondary"
        @click="backToStudents"
      >
        <ArrowLeft :size="16" />
        Danh sách học viên
      </button>

      <BaseAlert v-if="detailMessage">
        <div class="flex items-center justify-between gap-3">
          <span>{{ detailMessage }}</span>

          <button
            type="button"
            class="font-semibold underline"
            @click="retryStudent"
          >
            Thử lại
          </button>
        </div>
      </BaseAlert>

      <div v-if="loadingDetail" class="space-y-4">
        <div class="h-36 animate-pulse rounded-card bg-app-surface-muted" />

        <div
          v-for="index in 3"
          :key="index"
          class="h-48 animate-pulse rounded-card bg-app-surface-muted"
        />
      </div>

      <template v-else-if="studentDetail">
        <section class="rounded-card border border-app-border bg-app-surface p-6 shadow-card">
          <div class="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
            <div class="flex min-w-0 items-center gap-4">
              <span
                class="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-secondary-soft text-xl font-bold text-secondary"
              >
                {{ studentDetail.fullName.trim().charAt(0).toUpperCase() }}
              </span>

              <div class="min-w-0">
                <h2 class="truncate font-heading text-2xl font-bold text-app-text">
                  {{ studentDetail.fullName }}
                </h2>

                <p v-if="studentDetail.email" class="mt-1 text-sm text-app-text-muted">
                  {{ studentDetail.email }}
                </p>
              </div>
            </div>

            <div class="sm:text-right">
              <p class="font-heading text-3xl font-bold text-secondary">
                {{ studentDetail.progressPercentage }}%
              </p>

              <p class="mt-1 text-sm text-app-text-muted">
                {{ studentDetail.completedTopics }} / {{ studentDetail.totalTopics }} bài hoàn thành
              </p>
            </div>
          </div>

          <div class="mt-5 h-2.5 overflow-hidden rounded-full bg-app-surface-muted">
            <div
              class="h-full rounded-full"
              :class="studentDetail.progressPercentage === 100 ? 'bg-ai' : 'bg-secondary'"
              :style="{ width: `${studentDetail.progressPercentage}%` }"
            />
          </div>

          <div class="mt-4 flex flex-wrap gap-4 text-sm text-app-text-muted">
            <span>
              <strong class="text-app-text">{{ studentDetail.completedTopics }}</strong>
              hoàn thành
            </span>

            <span>
              <strong class="text-app-text">{{ studentDetail.inProgressTopics }}</strong>
              đang học
            </span>

            <span>
              <strong class="text-app-text">
                {{
                  Math.max(
                    0,
                    studentDetail.totalTopics -
                      studentDetail.completedTopics -
                      studentDetail.inProgressTopics,
                  )
                }}
              </strong>
              chưa bắt đầu
            </span>
          </div>
        </section>

        <div
          v-if="studentDetail.chapters.length === 0"
          class="rounded-card border border-dashed border-app-border bg-app-surface px-6 py-12 text-center"
        >
          <BookOpen :size="36" class="mx-auto text-app-text-muted/40" />

          <p class="mt-3 font-semibold text-app-text">
            Khóa học chưa có nội dung
          </p>
        </div>

        <div v-else class="space-y-4">
          <article
            v-for="chapter in studentDetail.chapters"
            :key="chapter.chapterId"
            class="overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card"
          >
            <header class="border-b border-app-border px-5 py-4">
              <div class="flex flex-wrap items-center justify-between gap-4">
                <div>
                  <p class="text-xs font-semibold uppercase tracking-wide text-app-text-muted">
                    Chương {{ chapter.orderIndex + 1 }}
                  </p>

                  <h3 class="mt-1 font-heading text-lg font-bold text-app-text">
                    {{ chapter.title }}
                  </h3>
                </div>

                <div class="text-right">
                  <p class="font-semibold text-app-text">
                    {{ chapter.completedTopics }} / {{ chapter.totalTopics }}
                  </p>

                  <p class="text-xs text-app-text-muted">
                    {{ chapter.progressPercentage }}% hoàn thành
                  </p>
                </div>
              </div>

              <div
                v-if="chapter.totalTopics > 0"
                class="mt-3 h-1.5 overflow-hidden rounded-full bg-app-surface-muted"
              >
                <div
                  class="h-full rounded-full"
                  :class="chapter.progressPercentage === 100 ? 'bg-ai' : 'bg-secondary'"
                  :style="{ width: `${chapter.progressPercentage}%` }"
                />
              </div>
            </header>

            <div
              v-if="chapter.topics.length === 0"
              class="px-5 py-6 text-sm text-app-text-muted"
            >
              Chương này chưa có bài học.
            </div>

            <div v-else class="divide-y divide-app-border">
              <div
                v-for="topic in chapter.topics"
                :key="topic.topicId"
                class="flex flex-col gap-3 px-5 py-4 sm:flex-row sm:items-center sm:justify-between"
              >
                <div class="flex min-w-0 items-start gap-3">
                  <CheckCircle2
                    v-if="topic.status === 'COMPLETED'"
                    :size="19"
                    class="mt-0.5 shrink-0 text-ai"
                  />

                  <CircleDot
                    v-else-if="topic.status === 'IN_PROGRESS'"
                    :size="19"
                    class="mt-0.5 shrink-0 text-secondary"
                  />

                  <Circle
                    v-else
                    :size="19"
                    class="mt-0.5 shrink-0 text-app-text-muted/50"
                  />

                  <div class="min-w-0">
                    <p class="font-semibold text-app-text">
                      {{ topic.title }}
                    </p>

                    <p
                      v-if="topic.lastAccessedAt"
                      class="mt-1 text-xs text-app-text-muted"
                    >
                      Truy cập gần nhất {{ formatDateTime(topic.lastAccessedAt) }}
                    </p>

                    <p v-else class="mt-1 text-xs text-app-text-muted">
                      Học viên chưa truy cập bài học này.
                    </p>
                  </div>
                </div>

                <span
                  class="w-fit rounded-pill px-2.5 py-1 text-xs font-semibold"
                  :class="{
                    'bg-ai-soft text-ai': topic.status === 'COMPLETED',
                    'bg-secondary-soft text-secondary': topic.status === 'IN_PROGRESS',
                    'bg-app-surface-muted text-app-text-muted': topic.status === 'NOT_STARTED',
                  }"
                >
                  {{ topicStatusLabel(topic) }}
                </span>
              </div>
            </div>
          </article>
        </div>
      </template>
    </template>
  </section>
</template>