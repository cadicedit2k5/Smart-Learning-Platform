<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  BookOpen,
  Bot,
  CalendarDays,
  CheckCircle2,
  FileText,
  Globe2,
  KeyRound,
  Layers3,
  LockKeyhole,
  Pencil,
  Rocket,
  Trash2,
  UsersRound,
  X,
} from 'lucide-vue-next'

import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'

import {
  deleteCourse,
  getCourse,
  publishCourse,
  updateCourse,
  type Course,
  type CourseInput,
  type CourseStatus,
  type CourseVisibility,
} from '../api/courseApi'
import CourseAccessCodesPanel from '../components/CourseAccessCodesPanel.vue'
import CourseAiPanel from '../components/CourseAiPanel.vue'
import CourseContentPanel from '../components/CourseContentPanel.vue'
import CourseDocumentsPanel from '../components/CourseDocumentsPanel.vue'
import CourseFormModal from '../components/CourseFormModal.vue'
import CourseMembersPanel from '../components/CourseMembersPanel.vue'
import { useLecturerApiError } from '../composables/useLecturerApiError'

type DetailTab = 'overview' | 'members' | 'access' | 'content' | 'documents' | 'ai'

const route = useRoute()
const router = useRouter()
const { handleApiError } = useLecturerApiError()

const courseId = computed(() => String(route.params.courseId))
const course = ref<Course | null>(null)
const loading = ref(true)
const loadError = ref('')
const actionMessage = ref('')
const successMessage = ref('')
const activeTab = ref<DetailTab>('overview')
const editOpen = ref(false)
const saving = ref(false)
const publishing = ref(false)
const deleteOpen = ref(false)
const deleting = ref(false)
const formMessage = ref('')
const formErrors = ref<Record<string, string>>({})

const canManageCourse = computed(() => course.value?.currentUserRole === 'OWNER')

const tabs = computed<
  Array<{
    id: DetailTab
    label: string
    icon: typeof BookOpen
  }>
>(() => [
  { id: 'overview', label: 'Tổng quan', icon: BookOpen },
  ...(canManageCourse.value
    ? [
        { id: 'members' as const, label: 'Thành viên', icon: UsersRound },
        { id: 'access' as const, label: 'Mã tham gia', icon: KeyRound },
        { id: 'content' as const, label: 'Nội dung', icon: Layers3 },
      ]
    : []),
  { id: 'documents', label: 'Tài liệu', icon: FileText },
  { id: 'ai', label: 'Trợ lý AI', icon: Bot },
])

const loadCourse = async () => {
  loading.value = true
  loadError.value = ''

  try {
    course.value = await getCourse(courseId.value)
  } catch (error) {
    loadError.value = handleApiError(error, 'Không thể tải chi tiết khóa học.').message
  } finally {
    loading.value = false
  }
}

const openEdit = () => {
  formMessage.value = ''
  formErrors.value = {}
  editOpen.value = true
}

const submitUpdate = async (input: CourseInput) => {
  if (!course.value) return

  saving.value = true
  formMessage.value = ''
  formErrors.value = {}

  try {
    course.value = await updateCourse(course.value.id, input)
    editOpen.value = false
    successMessage.value = 'Đã cập nhật thông tin khóa học.'
  } catch (error) {
    const parsed = handleApiError(error, 'Không thể cập nhật khóa học.')
    formMessage.value = parsed.message
    formErrors.value = parsed.fieldErrors
  } finally {
    saving.value = false
  }
}

const handlePublish = async () => {
  if (!course.value) return

  publishing.value = true
  actionMessage.value = ''
  successMessage.value = ''

  try {
    course.value = await publishCourse(course.value.id)
    successMessage.value = 'Khóa học đã được xuất bản thành công.'
  } catch (error) {
    actionMessage.value = handleApiError(error, 'Không thể xuất bản khóa học.').message
  } finally {
    publishing.value = false
  }
}

const handleDelete = async () => {
  if (!course.value) return

  deleting.value = true
  actionMessage.value = ''

  try {
    await deleteCourse(course.value.id)
    await router.replace({ name: 'lecturer-courses' })
  } catch (error) {
    actionMessage.value = handleApiError(error, 'Không thể xóa khóa học.').message
    deleteOpen.value = false
  } finally {
    deleting.value = false
  }
}

const formatDate = (value: string | null) => {
  if (!value) return '—'

  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'medium',
    timeStyle: 'short',
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

watch(courseId, () => {
  activeTab.value = 'overview'
  void loadCourse()
})

onMounted(() => {
  void loadCourse()
})
</script>

<template>
  <section class="mx-auto max-w-app space-y-6">
    <RouterLink
      :to="{ name: 'lecturer-courses' }"
      class="inline-flex items-center gap-2 text-sm font-semibold text-app-text-muted hover:text-secondary"
    >
      <ArrowLeft :size="17" />
      Quay lại khóa học của tôi
    </RouterLink>

    <div v-if="loading" class="space-y-5">
      <div class="h-48 animate-pulse rounded-panel bg-app-surface-muted" />
      <div class="h-96 animate-pulse rounded-panel bg-app-surface-muted" />
    </div>

    <BaseAlert v-else-if="loadError">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <span>{{ loadError }}</span>
        <button type="button" class="font-semibold underline" @click="loadCourse">Thử lại</button>
      </div>
    </BaseAlert>

    <template v-else-if="course">
      <header class="overflow-hidden rounded-panel bg-primary text-white shadow-card">
        <div class="h-2 bg-secondary" :class="course.status === 'PUBLISHED' ? 'bg-ai-soft' : ''" />
        <div class="flex flex-col gap-6 p-6 lg:flex-row lg:items-end lg:justify-between lg:p-8">
          <div class="min-w-0">
            <div class="flex flex-wrap items-center gap-2">
              <span
                class="rounded-pill px-2.5 py-1 text-xs font-semibold"
                :class="statusClass[course.status]"
              >
                {{ statusLabel[course.status] }}
              </span>
              <span
                class="flex items-center gap-1.5 rounded-pill bg-white/10 px-2.5 py-1 text-xs font-medium"
              >
                <Globe2 v-if="course.visibility === 'PUBLIC'" :size="13" />
                <UsersRound v-else-if="course.visibility === 'INVITE_ONLY'" :size="13" />
                <LockKeyhole v-else :size="13" />
                {{ visibilityLabel[course.visibility] }}
              </span>
              <span
                v-if="course.level"
                class="rounded-pill bg-white/10 px-2.5 py-1 text-xs font-medium"
              >
                {{ course.level }}
              </span>
            </div>

            <h1 class="mt-4 max-w-4xl font-heading text-3xl font-bold tracking-tight sm:text-4xl">
              {{ course.title }}
            </h1>
            <p class="mt-3 flex items-center gap-2 text-sm text-white/70">
              <CalendarDays :size="16" />
              Cập nhật {{ formatDate(course.updatedAt) }}
            </p>
          </div>

          <div v-if="canManageCourse" class="flex flex-wrap gap-3">
            <BaseButton variant="secondary" @click="openEdit">
              <template #leading><Pencil :size="17" /></template>
              Chỉnh sửa
            </BaseButton>
            <BaseButton
              v-if="course.status === 'DRAFT'"
              variant="ai"
              :loading="publishing"
              @click="handlePublish"
            >
              <template #leading><Rocket :size="17" /></template>
              Xuất bản
            </BaseButton>
          </div>
        </div>
      </header>

      <BaseAlert v-if="actionMessage">{{ actionMessage }}</BaseAlert>
      <BaseAlert v-if="successMessage" variant="ai">
        <template #icon><CheckCircle2 :size="18" /></template>
        {{ successMessage }}
      </BaseAlert>

      <nav
        class="flex gap-1 overflow-x-auto rounded-card border border-app-border bg-app-surface p-1.5 shadow-card"
        aria-label="Nội dung khóa học"
      >
        <button
          v-for="tab in tabs"
          :key="tab.id"
          type="button"
          class="inline-flex h-10 shrink-0 items-center gap-2 rounded-control px-4 text-sm font-semibold transition"
          :class="
            activeTab === tab.id
              ? 'bg-secondary-soft text-secondary'
              : 'text-app-text-muted hover:bg-app-surface-muted'
          "
          @click="activeTab = tab.id"
        >
          <component :is="tab.icon" :size="17" />
          {{ tab.label }}
        </button>
      </nav>

      <div v-if="activeTab === 'overview'" class="space-y-5">
        <article
          class="rounded-card border border-app-border bg-app-surface p-5 shadow-card sm:p-6"
        >
          <h2 class="font-heading text-xl font-bold text-app-text">Giới thiệu khóa học</h2>
          <p class="mt-3 whitespace-pre-wrap text-sm leading-7 text-app-text-muted">
            {{ course.description || 'Chưa có mô tả cho khóa học này.' }}
          </p>

          <dl class="mt-6 grid gap-4 border-t border-app-border pt-5 text-sm sm:grid-cols-3">
            <div>
              <dt class="text-app-text-muted">Ngày tạo</dt>
              <dd class="mt-1 font-semibold text-app-text">{{ formatDate(course.createdAt) }}</dd>
            </div>
            <div>
              <dt class="text-app-text-muted">Ngày xuất bản</dt>
              <dd class="mt-1 font-semibold text-app-text">{{ formatDate(course.publishedAt) }}</dd>
            </div>
            <div>
              <dt class="text-app-text-muted">Mã khóa học</dt>
              <dd class="mt-1 break-all font-mono text-xs font-semibold text-app-text">
                {{ course.id }}
              </dd>
            </div>
          </dl>
        </article>

        <article
          v-if="canManageCourse"
          class="rounded-card border border-danger/25 bg-app-surface p-5 shadow-card sm:p-6"
        >
          <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 class="font-heading text-lg font-bold text-app-text">Xóa khóa học</h2>
              <p class="mt-1 text-sm text-app-text-muted">
                Thao tác này sẽ xóa khóa học khỏi danh sách của bạn.
              </p>
            </div>
            <button
              type="button"
              class="inline-flex h-11 items-center justify-center gap-2 rounded-control border border-danger/30 px-4 text-sm font-semibold text-danger hover:bg-danger-soft"
              @click="deleteOpen = true"
            >
              <Trash2 :size="17" />
              Xóa khóa học
            </button>
          </div>
        </article>
      </div>

      <CourseMembersPanel v-else-if="activeTab === 'members'" :course-id="course.id" />
      <CourseAccessCodesPanel v-else-if="activeTab === 'access'" :course-id="course.id" />
      <CourseContentPanel v-else-if="activeTab === 'content'" :course-id="course.id" />
      <CourseDocumentsPanel
        v-else-if="activeTab === 'documents'"
        :course-id="course.id"
        :can-manage="canManageCourse"
      />
      <CourseAiPanel v-else-if="activeTab === 'ai'" :course-id="course.id" />

      <CourseFormModal
        :open="editOpen"
        :course="course"
        :loading="saving"
        :server-message="formMessage"
        :server-errors="formErrors"
        @close="editOpen = false"
        @submit="submitUpdate"
      />

      <Teleport to="body">
        <div
          v-if="deleteOpen"
          class="fixed inset-0 z-[80] flex items-center justify-center bg-slate-950/50 p-4"
          @mousedown.self="!deleting && (deleteOpen = false)"
        >
          <section
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="delete-course-title"
            class="w-full max-w-md rounded-panel bg-app-surface p-6 shadow-overlay"
          >
            <div class="flex items-start justify-between gap-4">
              <div>
                <h2 id="delete-course-title" class="font-heading text-xl font-bold text-app-text">
                  Xóa khóa học?
                </h2>
                <p class="mt-2 text-sm leading-6 text-app-text-muted">
                  Bạn sắp xóa <strong class="text-app-text">{{ course.title }}</strong
                  >. Hãy chắc chắn trước khi tiếp tục.
                </p>
              </div>
              <button
                type="button"
                aria-label="Đóng"
                class="rounded-control p-2 text-app-text-muted hover:bg-app-surface-muted"
                :disabled="deleting"
                @click="deleteOpen = false"
              >
                <X :size="19" />
              </button>
            </div>

            <div class="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
              <BaseButton variant="secondary" :disabled="deleting" @click="deleteOpen = false"
                >Hủy</BaseButton
              >
              <button
                type="button"
                class="inline-flex h-11 items-center justify-center gap-2 rounded-control bg-danger px-4 text-sm font-semibold text-white disabled:opacity-60"
                :disabled="deleting"
                @click="handleDelete"
              >
                <span
                  v-if="deleting"
                  class="h-4 w-4 animate-spin rounded-full border-2 border-current border-r-transparent"
                />
                <Trash2 v-else :size="17" />
                Xóa khóa học
              </button>
            </div>
          </section>
        </div>
      </Teleport>
    </template>
  </section>
</template>
