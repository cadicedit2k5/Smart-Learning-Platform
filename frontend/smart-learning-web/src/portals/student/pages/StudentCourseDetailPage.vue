<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  ArrowLeft,
  BookOpen,
  Bot,
  CalendarDays,
  FileText,
  GraduationCap,
  Layers3,
  UserRoundCheck,
  MessagesSquare,
} from 'lucide-vue-next'
import { useRoute } from 'vue-router'
import { ClipboardList } from 'lucide-vue-next'
import StudentAnnouncementsPanel from '../components/StudentAnnouncementsPanel.vue'

import StudentAssignmentsPanel
  from '../components/StudentAssignmentsPanel.vue'
import {
  getCourseProgress,
  type CourseLearningProgress,
} from '../api/learningProgressApi'

import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import BaseCard from '@/shared/components/BaseCard.vue'

import {
  getCurrentMembership,
  type CourseMembership,
} from '../api/courseApi'
import StudentAiTutorPanel from '../components/StudentAiTutorPanel.vue'
import StudentDocumentsPanel from '../components/StudentDocumentsPanel.vue'
import StudentCourseContentPanel from '../components/StudentCourseContentPanel.vue'
import { useStudentApiError } from '../composables/useStudentApiError'
import type { Course } from '@/shared/course/types.ts'
import { getCourse } from '@/shared/course/api.ts'
import { BaseTabs } from '@/shared/components/index.ts'
import { formatDate } from '@/shared/utils/date.ts'
import DiscussionPanel from '@/features/discusstion/components/DiscussionPanel.vue'

type DetailTab = 'overview' | 'content' | 'assignments' | 'documents' | 'discussion' | 'ai'

const route = useRoute()
const { handleApiError } = useStudentApiError()
const courseId = computed(() => String(route.params.courseId))
const course = ref<Course | null>(null)
const membership = ref<CourseMembership | null>(null)
const activeTab = ref<DetailTab>('overview')
const loading = ref(true)
const loadError = ref('')
const progress =
  ref<CourseLearningProgress | null>(null)

const tabs: Array<{ id: DetailTab; label: string; icon: typeof BookOpen }> = [
  { id: 'overview', label: 'Tổng quan', icon: BookOpen },
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
    const [courseData, membershipData, progressData] = await Promise.all([
      getCourse(courseId.value),
      getCurrentMembership(courseId.value),
      getCourseProgress(courseId.value),
    ])
    course.value = courseData
    membership.value = membershipData
    progress.value = progressData
  } catch (error) {
    loadError.value = handleApiError(error, 'Không thể tải thông tin khóa học.').message
  } finally {
    loading.value = false
  }
}

onMounted(() => void loadDetail())
</script>

<template>
  <section class="mx-auto max-w-app space-y-6">
    <RouterLink
      :to="{ name: 'student-courses' }"
      class="inline-flex items-center gap-2 text-sm font-semibold text-secondary hover:underline"
      ><ArrowLeft :size="17" />Quay lại khóa học</RouterLink
    >

    <div v-if="loading" class="space-y-5">
      <div class="h-48 animate-pulse rounded-panel bg-app-surface-muted" />
      <div class="h-96 animate-pulse rounded-panel bg-app-surface-muted" />
    </div>
    <BaseAlert v-else-if="loadError">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <span>{{ loadError }}</span
        ><BaseButton variant="secondary" @click="loadDetail">Thử lại</BaseButton>
      </div>
    </BaseAlert>

    <template v-else-if="course && membership">
      <header
        class="relative overflow-hidden rounded-panel border border-app-border bg-app-surface p-6 shadow-card sm:p-8"
      >
        <div
          class="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-secondary via-primary to-ai"
        />

        <div
          class="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between"
        >
          <div class="max-w-4xl">
            <div
              class="flex flex-wrap items-center gap-2"
            >
              <span
                class="rounded-pill bg-secondary-soft px-3 py-1 text-xs font-semibold text-secondary"
              >
                Khóa học đang tham gia
              </span>

              <span
                v-if="course.level"
                class="rounded-pill bg-app-surface-muted px-3 py-1 text-xs font-medium text-app-text-muted"
              >
                {{ course.level }}
              </span>
            </div>

            <h1
              class="mt-5 font-heading text-3xl font-bold tracking-tight text-app-text sm:text-4xl"
            >
              {{ course.title }}
            </h1>
          </div>

          <div
            class="flex items-center gap-3 rounded-card bg-ai-soft px-4 py-3 text-ai"
          >
            <UserRoundCheck :size="20" />

            <div>
              <p class="text-xs opacity-75">
                Trạng thái
              </p>

              <p class="text-sm font-bold">
                Thành viên
              </p>
            </div>
          </div>
        </div>
      </header>

      <BaseTabs
        v-model="activeTab"
        :tabs="tabs"
        aria-label="Nội dung khóa học"
      />

      <div
        v-if="activeTab === 'overview'"
        class="space-y-5"
      >
        <!-- Description -->
        <BaseCard>
          <template #header>
            <h2
              class="font-heading text-xl font-bold text-app-text"
            >
              Giới thiệu khóa học
            </h2>
          </template>

          <p
            class="whitespace-pre-wrap text-sm leading-7 text-app-text-muted sm:text-base"
          >
            {{
              course.description ||
              'Khóa học chưa có mô tả.'
            }}
          </p>
        </BaseCard>

        <!-- Progress -->
        <BaseCard v-if="progress">
          <template #header>
            <div
              class="flex flex-wrap items-center justify-between gap-3"
            >
              <div>
                <h2
                  class="font-heading text-xl font-bold text-app-text"
                >
                  Tiến độ học tập
                </h2>

                <p
                  class="mt-1 text-sm text-app-text-muted"
                >
                  {{
                    progress.completedTopics
                  }}/{{ progress.totalTopics }}
                  bài học đã hoàn thành
                </p>
              </div>

              <span
                class="font-heading text-2xl font-bold text-secondary"
              >
                {{ progress.progressPercentage }}%
              </span>
            </div>
          </template>

          <div>
            <div
              class="h-3 overflow-hidden rounded-pill bg-app-surface-muted"
            >
              <div
                class="h-full rounded-pill bg-secondary transition-all"
                :style="{
                  width: `${progress.progressPercentage}%`,
                }"
              />
            </div>

            <div
              class="mt-5 flex flex-wrap items-center justify-between gap-3"
            >
              <p
                class="text-sm text-app-text-muted"
              >
                <template
                  v-if="progress.progressPercentage === 100"
                >
                  Bạn đã hoàn thành khóa học.
                </template>

                <template v-else-if="progress.lastTopicId">
                  Tiếp tục bài học gần nhất của bạn.
                </template>

                <template v-else>
                  Bắt đầu bài học đầu tiên để ghi nhận
                  tiến độ.
                </template>
              </p>

              <BaseButton
                @click="activeTab = 'content'"
              >
                {{
                  progress.lastTopicId
                    ? 'Tiếp tục học'
                    : 'Bắt đầu học'
                }}
              </BaseButton>
            </div>
          </div>
        </BaseCard>

        <!-- Announcements -->
        <StudentAnnouncementsPanel :course-id="course.id" />
        <div
          class="grid gap-5 lg:grid-cols-[minmax(0,1fr)_20rem]"
        >
          <!-- Course info -->
          <BaseCard>
            <template #header>
              <h2
                class="font-heading text-xl font-bold text-app-text"
              >
                Thông tin khóa học
              </h2>
            </template>

            <dl class="grid gap-5 sm:grid-cols-2">
              <div>
                <dt
                  class="flex items-center gap-2 text-sm text-app-text-muted"
                >
                  <GraduationCap :size="17" />
                  Cấp độ
                </dt>

                <dd
                  class="mt-2 font-semibold text-app-text"
                >
                  {{ course.level || 'Chưa cập nhật' }}
                </dd>
              </div>

              <div>
                <dt
                  class="flex items-center gap-2 text-sm text-app-text-muted"
                >
                  <CalendarDays :size="17" />
                  Cập nhật gần nhất
                </dt>

                <dd
                  class="mt-2 font-semibold text-app-text"
                >
                  {{ formatDate(course.updatedAt) }}
                </dd>
              </div>
            </dl>
          </BaseCard>

          <!-- Membership -->
          <BaseCard>
            <template #header>
              <h2
                class="font-heading text-lg font-bold text-app-text"
              >
                Thành viên
              </h2>
            </template>

            <dl class="space-y-4 text-sm">
              <div>
                <dt class="text-app-text-muted">
                  Vai trò
                </dt>

                <dd
                  class="mt-1 font-semibold text-app-text"
                >
                  Học viên
                </dd>
              </div>

              <div>
                <dt class="text-app-text-muted">
                  Trạng thái
                </dt>

                <dd
                  class="mt-1 font-semibold text-app-text"
                >
                  Đang tham gia
                </dd>
              </div>

              <div v-if="membership.joinedAt">
                <dt class="text-app-text-muted">
                  Tham gia ngày
                </dt>

                <dd
                  class="mt-1 font-semibold text-app-text"
                >
                  {{ formatDate(membership.joinedAt) }}
                </dd>
              </div>
            </dl>
          </BaseCard>
        </div>
      </div>
      <StudentCourseContentPanel v-else-if="activeTab === 'content'" :course-id="course.id"/>
      <StudentAssignmentsPanel
        v-else-if="activeTab === 'assignments'"
        :course-id="course.id"
      />
      <StudentDocumentsPanel v-else-if="activeTab === 'documents'" :course-id="course.id" />
      <DiscussionPanel
        v-else-if="activeTab === 'discussion'"
        :course-id="course.id"
      />
      <StudentAiTutorPanel v-else-if="activeTab === 'ai'" :course-id="course.id" />
    </template>
  </section>
</template>
