<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  ArrowLeft,
  BookOpen,
  Bot,
  CalendarDays,
  FileText,
  GraduationCap,
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
import { useStudentApiError } from '../composables/useStudentApiError'

type DetailTab = 'overview' | 'documents' | 'ai'

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
      <header class="rounded-panel bg-primary p-6 text-on-primary shadow-card sm:p-8">
        <div class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p class="text-sm font-semibold text-on-primary/75">Khóa học đang tham gia</p>
            <h1 class="mt-2 font-heading text-3xl font-bold tracking-tight">{{ course.title }}</h1>
            <p class="mt-3 max-w-3xl text-sm leading-6 text-on-primary/80">
              {{ course.description || 'Khóa học chưa có mô tả.' }}
            </p>
          </div>
          <span
            class="inline-flex w-fit items-center gap-2 rounded-pill bg-white/15 px-3 py-1.5 text-sm font-semibold"
            ><UserRoundCheck :size="17" />{{
              membership.status === 'ACTIVE' ? 'Thành viên' : membership.status
            }}</span
          >
        </div>
      </header>

      <nav
        aria-label="Nội dung khóa học"
        class="flex gap-1 overflow-x-auto rounded-card border border-app-border bg-app-surface p-1 shadow-card"
      >
        <button
          v-for="tab in tabs"
          :key="tab.id"
          type="button"
          class="inline-flex shrink-0 items-center gap-2 rounded-control px-4 py-2.5 text-sm font-semibold transition"
          :class="
            activeTab === tab.id
              ? 'bg-secondary text-white'
              : 'text-app-text-muted hover:bg-app-surface-muted hover:text-app-text'
          "
          @click="activeTab = tab.id"
        >
          <component :is="tab.icon" :size="17" />{{ tab.label }}
        </button>
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

      <StudentDocumentsPanel v-else-if="activeTab === 'documents'" :course-id="course.id" />
      <StudentAiTutorPanel v-else :course-id="course.id" />
    </template>
  </section>
</template>
