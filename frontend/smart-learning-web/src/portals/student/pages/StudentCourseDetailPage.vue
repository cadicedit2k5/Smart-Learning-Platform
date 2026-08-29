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
} from 'lucide-vue-next'
import { useRoute } from 'vue-router'

import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import BaseCard from '@/shared/components/BaseCard.vue'

import {
  getCourse,
  getCurrentMembership,
  type Course,
  type CourseMembership,
} from '../api/courseApi'
import StudentAiTutorPanel from '../components/StudentAiTutorPanel.vue'
import StudentDocumentsPanel from '../components/StudentDocumentsPanel.vue'
import StudentCourseContentPanel from '../components/StudentCourseContentPanel.vue'
import { useStudentApiError } from '../composables/useStudentApiError'

type DetailTab = 'overview' | 'content' | 'documents' | 'ai'

const route = useRoute()
const { handleApiError } = useStudentApiError()
const courseId = computed(() => String(route.params.courseId))
const course = ref<Course | null>(null)
const membership = ref<CourseMembership | null>(null)
const activeTab = ref<DetailTab>('overview')
const loading = ref(true)
const loadError = ref('')

const tabs: Array<{ id: DetailTab; label: string; icon: typeof BookOpen }> = [
  { id: 'overview', label: 'Tổng quan', icon: BookOpen },
  { id: 'content', label: 'Bài học', icon: Layers3 },
  { id: 'documents', label: 'Tài liệu', icon: FileText },
  { id: 'ai', label: 'AI tutor', icon: Bot },
]

const loadDetail = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const [courseData, membershipData] = await Promise.all([
      getCourse(courseId.value),
      getCurrentMembership(courseId.value),
    ])
    course.value = courseData
    membership.value = membershipData
  } catch (error) {
    loadError.value = handleApiError(error, 'Không thể tải thông tin khóa học.').message
  } finally {
    loading.value = false
  }
}

const formatDate = (value: string) =>
  new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium' }).format(new Date(value))

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

            <p
              class="mt-3 max-w-3xl text-sm leading-7 text-app-text-muted sm:text-base"
            >
              {{
                course.description ||
                'Khóa học chưa có mô tả.'
              }}
            </p>
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

      <nav
        aria-label="Nội dung khóa học"
        class="border-b border-app-border"
      >
        <div
          class="flex gap-7 overflow-x-auto"
        >
          <button
            v-for="tab in tabs"
            :key="tab.id"
            type="button"
            class="relative flex h-12 shrink-0 items-center gap-2 text-sm font-semibold transition"
            :class="
              activeTab === tab.id
                ? 'text-secondary'
                : 'text-app-text-muted hover:text-app-text'
            "
            @click="activeTab = tab.id"
          >
            <component
              :is="tab.icon"
              :size="17"
            />

            {{ tab.label }}

            <span
              v-if="activeTab === tab.id"
              class="absolute inset-x-0 bottom-0 h-0.5 rounded-full bg-secondary"
            />
          </button>
        </div>
      </nav>

      <div v-if="activeTab === 'overview'" class="grid gap-5 lg:grid-cols-[minmax(0,1fr)_20rem]">
        <BaseCard>
          <template #header
            ><h2 class="font-heading text-xl font-bold text-app-text">
              Thông tin khóa học
            </h2></template
          >
          <dl class="grid gap-5 sm:grid-cols-2">
            <div>
              <dt class="flex items-center gap-2 text-sm text-app-text-muted">
                <GraduationCap :size="17" />Cấp độ
              </dt>
              <dd class="mt-2 font-semibold text-app-text">
                {{ course.level || 'Chưa cập nhật' }}
              </dd>
            </div>
            <div>
              <dt class="flex items-center gap-2 text-sm text-app-text-muted">
                <CalendarDays :size="17" />Cập nhật gần nhất
              </dt>
              <dd class="mt-2 font-semibold text-app-text">{{ formatDate(course.updatedAt) }}</dd>
            </div>
          </dl>
        </BaseCard>
        <BaseCard>
          <template #header
            ><h2 class="font-heading text-lg font-bold text-app-text">Membership</h2></template
          >
          <dl class="space-y-4 text-sm">
            <div>
              <dt class="text-app-text-muted">Vai trò</dt>
              <dd class="mt-1 font-semibold text-app-text">{{ membership.role }}</dd>
            </div>
            <div>
              <dt class="text-app-text-muted">Trạng thái</dt>
              <dd class="mt-1 font-semibold text-app-text">{{ membership.status }}</dd>
            </div>
            <div v-if="membership.joinedAt">
              <dt class="text-app-text-muted">Tham gia ngày</dt>
              <dd class="mt-1 font-semibold text-app-text">
                {{ formatDate(membership.joinedAt) }}
              </dd>
            </div>
          </dl>
        </BaseCard>
      </div>
      <StudentCourseContentPanel v-else-if="activeTab === 'content'":course-id="course.id"/>
      <StudentDocumentsPanel v-else-if="activeTab === 'documents'" :course-id="course.id" />
      <StudentAiTutorPanel v-else :course-id="course.id" />
    </template>
  </section>
</template>
