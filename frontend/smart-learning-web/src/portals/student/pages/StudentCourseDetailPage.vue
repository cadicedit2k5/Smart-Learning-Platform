<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  ArrowLeft,
  Bell,
  BookOpen,
  Bot,
  CheckCircle2,
  ClipboardList,
  FileText,
  Layers3,
  MessagesSquare,
  Play,
} from 'lucide-vue-next'

import {
  BaseAlert,
  BaseButton,
  BaseTabs,
} from '@/shared/components'
import CourseCover from '@/shared/course/CourseCover.vue'
import { getCourse, type Course } from '@/shared/course'
import {
  hasRichTextContent,
  parseStoredRichText,
  RichTextViewer,
} from '@/shared/rich-text'
import DiscussionPanel from '@/features/discusstion/components/DiscussionPanel.vue'

import {
  getCourseProgress,
  type CourseLearningProgress,
} from '../api/learningProgressApi'
import StudentAiTutorPanel from '../components/StudentAiTutorPanel.vue'
import StudentAnnouncementsPanel from '../components/StudentAnnouncementsPanel.vue'
import StudentAssignmentsPanel from '../components/StudentAssignmentsPanel.vue'
import StudentCourseContentPanel from '../components/StudentCourseContentPanel.vue'
import StudentDocumentsPanel from '../components/StudentDocumentsPanel.vue'
import { useStudentApiError } from '../composables/useStudentApiError'

type DetailTab =
  | 'overview'
  | 'announcements'
  | 'content'
  | 'assignments'
  | 'documents'
  | 'discussion'
  | 'ai'

const route = useRoute()
const { handleApiError } = useStudentApiError()

const courseId = computed(() => String(route.params.courseId))
const course = ref<Course | null>(null)
const progress = ref<CourseLearningProgress | null>(null)
const activeTab = ref<DetailTab>('overview')
const loading = ref(true)
const loadError = ref('')

const descriptionContent = computed(() =>
  parseStoredRichText(course.value?.description),
)

const hasDescription = computed(() =>
  hasRichTextContent(descriptionContent.value),
)

const progressCompleted = computed(() =>
  (progress.value?.progressPercentage ?? 0) >= 100,
)

const tabs: Array<{
  id: DetailTab
  label: string
  icon: typeof BookOpen
}> = [
  { id: 'overview', label: 'Tổng quan', icon: BookOpen },
  { id: 'announcements', label: 'Thông báo', icon: Bell },
  { id: 'content', label: 'Bài học', icon: Layers3 },
  { id: 'assignments', label: 'Bài tập', icon: ClipboardList },
  { id: 'documents', label: 'Tài liệu', icon: FileText },
  { id: 'discussion', label: 'Thảo luận', icon: MessagesSquare },
  { id: 'ai', label: 'AI tutor', icon: Bot },
]

const loadDetail = async () => {
  loading.value = true
  loadError.value = ''

  try {
    const [courseData, progressData] = await Promise.all([
      getCourse(courseId.value),
      getCourseProgress(courseId.value),
    ])

    course.value = courseData
    progress.value = progressData
  } catch (error) {
    loadError.value = handleApiError(
      error,
      'Không thể tải thông tin khóa học.',
    ).message
  } finally {
    loading.value = false
  }
}

watch(courseId, () => {
  activeTab.value = 'overview'
  void loadDetail()
})

onMounted(() => void loadDetail())
</script>

<template>
  <section class="mx-auto max-w-app space-y-6">
    <RouterLink
      :to="{ name: 'student-courses' }"
      class="inline-flex items-center gap-2 text-sm font-semibold text-app-text-muted transition hover:text-secondary"
    >
      <ArrowLeft :size="17" />
      Khóa học của tôi
    </RouterLink>

    <div v-if="loading" class="space-y-5">
      <div class="h-12 animate-pulse rounded-card bg-app-surface-muted" />
      <div class="h-80 animate-pulse rounded-panel bg-app-surface-muted" />
      <div class="h-52 animate-pulse rounded-panel bg-app-surface-muted" />
    </div>

    <BaseAlert v-else-if="loadError">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <span>{{ loadError }}</span>

        <BaseButton variant="secondary" @click="loadDetail">
          Thử lại
        </BaseButton>
      </div>
    </BaseAlert>

    <template v-else-if="course">
      <BaseTabs
        v-model="activeTab"
        :tabs="tabs"
        aria-label="Nội dung khóa học"
      />

      <div
        v-if="activeTab === 'overview'"
        class="space-y-6"
      >
        <section
          class="overflow-hidden rounded-[1.5rem] border border-app-border bg-app-surface shadow-card"
        >
          <div
            class="grid items-stretch lg:grid-cols-[minmax(0,1fr)_24rem]"
          >
            <div
              class="flex min-w-0 flex-col justify-center p-6 sm:p-8 lg:p-9"
            >
              <span
                v-if="course.level"
                class="w-fit rounded-pill bg-secondary-soft px-3 py-1 text-xs font-semibold text-secondary"
              >
                {{ course.level }}
              </span>

              <h1
                class="mt-4 max-w-3xl font-heading text-3xl font-bold leading-tight tracking-tight text-app-text sm:text-4xl"
              >
                {{ course.title }}
              </h1>

              <div class="mt-6 max-w-3xl">
                <RichTextViewer
                  v-if="hasDescription"
                  :content="descriptionContent"
                />

                <p
                  v-else
                  class="text-sm leading-7 text-app-text-muted"
                >
                  Khóa học chưa có mô tả.
                </p>
              </div>
            </div>

            <div
              class="border-t border-app-border bg-app-surface-muted/40 p-4 lg:border-l lg:border-t-0"
            >
              <div
                class="overflow-hidden rounded-[1.15rem] bg-app-surface shadow-card"
              >
                <CourseCover
                  :image-url="course.imageUrl"
                  :title="course.title"
                />
              </div>
            </div>
          </div>
        </section>

        <section
          v-if="progress"
          class="rounded-[1.35rem] border border-app-border bg-app-surface p-6 shadow-card sm:p-7"
        >
          <div
            class="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between"
          >
            <div>
              <h2
                class="font-heading text-xl font-bold text-app-text"
              >
                Tiến độ học tập
              </h2>

              <p class="mt-1 text-sm text-app-text-muted">
                {{ progress.completedTopics }}
                /
                {{ progress.totalTopics }}
                bài học đã hoàn thành
              </p>
            </div>

            <span
              class="font-heading text-3xl font-bold"
              :class="
                progressCompleted
                  ? 'text-ai'
                  : 'text-secondary'
              "
            >
              {{ progress.progressPercentage }}%
            </span>
          </div>

          <div
            class="mt-5 h-2.5 overflow-hidden rounded-pill bg-app-surface-muted"
          >
            <div
              class="h-full rounded-pill transition-all duration-500"
              :class="
                progressCompleted
                  ? 'bg-ai'
                  : 'bg-secondary'
              "
              :style="{
                width: `${progress.progressPercentage}%`,
              }"
            />
          </div>

          <div
            class="mt-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between"
          >
            <p class="text-sm leading-6 text-app-text-muted">
              <template v-if="progressCompleted">
                Bạn đã hoàn thành khóa học.
              </template>

              <template v-else-if="progress.lastTopicId">
                Tiếp tục từ bài học gần nhất.
              </template>

              <template v-else>
                Bắt đầu bài học đầu tiên của khóa học.
              </template>
            </p>

            <BaseButton
              class="shrink-0"
              @click="activeTab = 'content'"
            >
              <template #leading>
                <CheckCircle2
                  v-if="progressCompleted"
                  :size="17"
                />

                <Play
                  v-else
                  :size="17"
                />
              </template>

              {{
                progressCompleted
                  ? 'Xem lại bài học'
                  : progress.lastTopicId
                    ? 'Tiếp tục học'
                    : 'Bắt đầu học'
              }}
            </BaseButton>
          </div>
        </section>
      </div>

      <StudentAnnouncementsPanel
        v-else-if="activeTab === 'announcements'"
        :course-id="course.id"
      />

      <StudentCourseContentPanel
        v-else-if="activeTab === 'content'"
        :course-id="course.id"
      />

      <StudentAssignmentsPanel
        v-else-if="activeTab === 'assignments'"
        :course-id="course.id"
      />

      <StudentDocumentsPanel
        v-else-if="activeTab === 'documents'"
        :course-id="course.id"
      />

      <DiscussionPanel
        v-else-if="activeTab === 'discussion'"
        :course-id="course.id"
      />

      <StudentAiTutorPanel
        v-else-if="activeTab === 'ai'"
        :course-id="course.id"
      />
    </template>
  </section>
</template>