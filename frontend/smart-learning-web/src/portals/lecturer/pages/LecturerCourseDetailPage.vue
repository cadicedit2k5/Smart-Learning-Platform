<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  Bell,
  BookOpen,
  Bot,
  CalendarDays,
  ChartNoAxesColumnIncreasing,
  CheckCircle2,
  ClipboardList,
  Eye,
  FileText,
  GraduationCap,
  Layers3,
  MessagesSquare,
  Pencil,
  Rocket,
  Settings2,
  Trash2,
  UserRoundCheck,
  UsersRound,
  X,
} from 'lucide-vue-next'

import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import CourseCover from '@/shared/course/CourseCover.vue'
import { getCourse, type Course, type CourseFeatureConfig } from '@/shared/course'
import { courseStatusLabel, courseVisibilityLabel } from '@/shared/course/presentation'
import { hasRichTextContent, parseStoredRichText, RichTextViewer } from '@/shared/rich-text'
import { formatDateTime } from '@/shared/utils/date'
import DiscussionPanel from '@/features/discusstion/components/DiscussionPanel.vue'

import {
  deleteCourse,
  publishCourse,
  updateCourse,
  updateCourseFeatureConfig,
  type CourseInput,
} from '../api/courseApi'
import CourseAiPanel from '../components/CourseAiPanel.vue'
import CourseAnnouncementsPanel from '../components/CourseAnnouncementsPanel.vue'
import CourseAssignmentsPanel from '../components/CourseAssignmentsPanel.vue'
import CourseContentPanel from '../components/CourseContentPanel.vue'
import CourseDocumentsPanel from '../components/CourseDocumentsPanel.vue'
import CourseFormModal from '../components/CourseFormModal.vue'
import CourseJoinRequestPanel from '../components/CourseJoinRequestPanel.vue'
import CourseLearningProgressPanel from '../components/CourseLearningProgressPanel.vue'
import CourseMembersPanel from '../components/CourseMembersPanel.vue'
import { useLecturerApiError } from '../composables/useLecturerApiError'
import CourseFeatureConfigModal from '../components/CourseFeatureConfigModal.vue'

type DetailTab =
  | 'overview'
  | 'announcements'
  | 'members'
  | 'requests'
  | 'content'
  | 'progress'
  | 'assignments'
  | 'documents'
  | 'discussion'
  | 'ai'

const route = useRoute()
const router = useRouter()
const { handleApiError } = useLecturerApiError()

const courseId = computed(() => String(route.params.courseId))
const course = ref<Course | null>(null)
const activeTab = ref<DetailTab>('overview')
const loading = ref(true)
const loadError = ref('')
const actionMessage = ref('')
const successMessage = ref('')

const editOpen = ref(false)
const saving = ref(false)
const publishing = ref(false)
const deleteOpen = ref(false)
const deleting = ref(false)
const featureSettingsOpen = ref(false)
const savingFeatureConfig = ref(false)
const formMessage = ref('')
const formErrors = ref<Record<string, string>>({})

const canManageCourse = computed(() => course.value?.currentUserRole === 'OWNER')

const canReviewJoinRequests = computed(() =>
  canManageCourse.value &&
  course.value?.status === 'PUBLISHED' &&
  course.value?.visibility === 'PUBLIC',
)

const descriptionContent = computed(() => parseStoredRichText(course.value?.description))
const hasDescription = computed(() => hasRichTextContent(descriptionContent.value))

const tabs = computed<Array<{
  id: DetailTab
  label: string
  icon: typeof BookOpen
}>>(() => {
  const config = course.value?.featureConfig

  return [
    { id: 'overview', label: 'Tổng quan', icon: BookOpen },

    ...(config?.announcements
      ? [{ id: 'announcements' as const, label: 'Thông báo', icon: Bell }]
      : []),

    ...(canManageCourse.value
      ? [
          { id: 'members' as const, label: 'Thành viên', icon: UsersRound },

          ...(canReviewJoinRequests.value
            ? [{ id: 'requests' as const, label: 'Yêu cầu tham gia', icon: UserRoundCheck }]
            : []),

          ...(config?.content
            ? [
                { id: 'content' as const, label: 'Nội dung', icon: Layers3 },
                { id: 'progress' as const, label: 'Tiến độ', icon: ChartNoAxesColumnIncreasing },
              ]
            : []),
        ]
      : []),

    ...(config?.assignments
      ? [{ id: 'assignments' as const, label: 'Bài tập', icon: ClipboardList }]
      : []),

    ...(config?.documents
      ? [{ id: 'documents' as const, label: 'Tài liệu', icon: FileText }]
      : []),

    ...(config?.discussion
      ? [{ id: 'discussion' as const, label: 'Thảo luận', icon: MessagesSquare }]
      : []),

    ...(config?.aiTutor
      ? [{ id: 'ai' as const, label: 'Trợ lý AI', icon: Bot }]
      : []),
  ]
})

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
  successMessage.value = ''

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

const handleFeatureConfigSave = async (config: CourseFeatureConfig) => {
  if (!course.value) return

  savingFeatureConfig.value = true
  actionMessage.value = ''
  successMessage.value = ''

  try {
    course.value = await updateCourseFeatureConfig(course.value.id, config)

    if (!tabs.value.some(tab => tab.id === activeTab.value)) {
      activeTab.value = 'overview'
    }

    featureSettingsOpen.value = false
    successMessage.value = 'Đã cập nhật tính năng của khóa học.'
  } catch (error) {
    actionMessage.value = handleApiError(error, 'Không thể cập nhật tính năng khóa học.').message
  } finally {
    savingFeatureConfig.value = false
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

watch(courseId, () => {
  activeTab.value = 'overview'
  void loadCourse()
})

onMounted(() => void loadCourse())
</script>

<template>
  <section class="mx-auto max-w-app space-y-5">
    <RouterLink
      :to="{ name: 'lecturer-courses' }"
      class="inline-flex items-center gap-2 text-sm font-semibold text-app-text-muted transition hover:text-secondary"
    >
      <ArrowLeft :size="17" />
      Khóa học của tôi
    </RouterLink>

    <div v-if="loading" class="space-y-4">
      <div class="h-12 animate-pulse rounded-card bg-app-surface-muted" />
      <div class="h-80 animate-pulse rounded-panel bg-app-surface-muted" />
    </div>

    <BaseAlert v-else-if="loadError">
      <div class="flex items-center justify-between gap-3">
        <span>{{ loadError }}</span>

        <button type="button" class="font-semibold underline" @click="loadCourse">
          Thử lại
        </button>
      </div>
    </BaseAlert>

    <template v-else-if="course">
      <BaseAlert v-if="actionMessage">
        {{ actionMessage }}
      </BaseAlert>

      <BaseAlert v-if="successMessage" variant="success">
        <template #icon>
          <CheckCircle2 :size="18" />
        </template>

        {{ successMessage }}
      </BaseAlert>

      <div class="flex items-end justify-between gap-4 border-b border-app-border">
        <nav class="min-w-0 flex-1 overflow-x-auto" aria-label="Nội dung khóa học">
          <div class="flex min-w-max gap-6">
            <button
              v-for="tab in tabs"
              :key="tab.id"
              type="button"
              class="flex h-12 shrink-0 items-center gap-2 border-b-2 px-1 text-sm font-semibold transition"
              :class="
                activeTab === tab.id
                  ? 'border-secondary text-secondary'
                  : 'border-transparent text-app-text-muted hover:text-app-text'
              "
              @click="activeTab = tab.id"
            >
              <component :is="tab.icon" :size="17" />
              {{ tab.label }}
            </button>
          </div>
        </nav>

        <button
          v-if="canManageCourse"
          type="button"
          class="mb-2 flex h-9 shrink-0 items-center gap-2 rounded-control border border-app-border bg-app-surface px-3 text-sm font-semibold text-app-text-muted transition hover:border-secondary/40 hover:bg-secondary-soft hover:text-secondary"
          @click="featureSettingsOpen = true"
        >
          <Settings2 :size="17" />
          <span class="hidden sm:inline">Cài đặt tính năng</span>
        </button>
      </div>

      <div v-if="activeTab === 'overview'" class="space-y-6">
        <section class="overflow-hidden rounded-[1.5rem] border border-app-border bg-app-surface shadow-card">
          <div class="grid items-stretch lg:grid-cols-[minmax(0,1fr)_24rem]">
            <div class="flex min-w-0 flex-col p-6 sm:p-8 lg:p-9">
              <div>
                <h1 class="max-w-3xl font-heading text-3xl font-bold leading-tight tracking-tight text-app-text sm:text-4xl">
                  {{ course.title }}
                </h1>

                <div class="mt-6 max-w-3xl">
                  <RichTextViewer v-if="hasDescription" :content="descriptionContent" />

                  <p v-else class="text-sm leading-7 text-app-text-muted">
                    Chưa có mô tả cho khóa học này.
                  </p>
                </div>
              </div>

              <div v-if="canManageCourse" class="mt-7 flex flex-wrap gap-2 border-t border-app-border pt-6">
                <BaseButton variant="secondary" @click="openEdit">
                  <template #leading>
                    <Pencil :size="16" />
                  </template>

                  Chỉnh sửa
                </BaseButton>

                <BaseButton
                  v-if="course.status === 'DRAFT'"
                  :loading="publishing"
                  @click="handlePublish"
                >
                  <template #leading>
                    <Rocket :size="16" />
                  </template>

                  Xuất bản
                </BaseButton>

                <button
                  type="button"
                  class="inline-flex h-11 items-center gap-2 rounded-control border border-danger/30 px-4 text-sm font-semibold text-danger transition hover:bg-danger-soft"
                  @click="deleteOpen = true"
                >
                  <Trash2 :size="16" />
                  Xóa
                </button>
              </div>
            </div>

            <div class="border-t border-app-border bg-app-surface-muted/40 p-4 lg:border-l lg:border-t-0">
              <div class="overflow-hidden rounded-[1.15rem] bg-app-surface shadow-card">
                <CourseCover :image-url="course.imageUrl" :title="course.title" />
              </div>
            </div>
          </div>
        </section>

        <section>
          <h2 class="font-heading text-xl font-bold text-app-text">
            Thông tin khóa học
          </h2>

          <div class="mt-4 grid overflow-hidden rounded-[1.25rem] border border-app-border bg-app-surface shadow-card sm:grid-cols-2 xl:grid-cols-4">
            <div class="border-b border-app-border p-5 sm:border-r xl:border-b-0">
              <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-secondary-soft text-secondary">
                <CheckCircle2 :size="17" />
              </div>

              <p class="mt-4 text-xs font-medium text-app-text-muted">
                Trạng thái
              </p>

              <p class="mt-1 text-sm font-semibold text-app-text">
                {{ courseStatusLabel[course.status] }}
              </p>
            </div>

            <div class="border-b border-app-border p-5 xl:border-b-0 xl:border-r">
              <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-secondary-soft text-secondary">
                <Eye :size="17" />
              </div>

              <p class="mt-4 text-xs font-medium text-app-text-muted">
                Quyền truy cập
              </p>

              <p class="mt-1 text-sm font-semibold text-app-text">
                {{ courseVisibilityLabel[course.visibility] }}
              </p>
            </div>

            <div class="border-b border-app-border p-5 sm:border-r sm:border-b-0">
              <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-secondary-soft text-secondary">
                <GraduationCap :size="17" />
              </div>

              <p class="mt-4 text-xs font-medium text-app-text-muted">
                Cấp độ
              </p>

              <p class="mt-1 text-sm font-semibold text-app-text">
                {{ course.level || 'Chưa cập nhật' }}
              </p>
            </div>

            <div class="p-5">
              <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-secondary-soft text-secondary">
                <CalendarDays :size="17" />
              </div>

              <p class="mt-4 text-xs font-medium text-app-text-muted">
                {{ course.status === 'PUBLISHED' ? 'Ngày xuất bản' : 'Cập nhật gần nhất' }}
              </p>

              <p class="mt-1 text-sm font-semibold text-app-text">
                {{
                  course.status === 'PUBLISHED' && course.publishedAt
                    ? formatDateTime(course.publishedAt)
                    : formatDateTime(course.updatedAt)
                }}
              </p>
            </div>
          </div>
        </section>
      </div>

      <CourseAnnouncementsPanel
        v-else-if="activeTab === 'announcements'"
        :course-id="course.id"
      />

      <CourseMembersPanel
        v-else-if="activeTab === 'members'"
        :course-id="course.id"
        :visibility="course.visibility"
        :status="course.status"
      />

      <CourseJoinRequestPanel
        v-else-if="activeTab === 'requests'"
        :course-id="course.id"
      />

      <CourseContentPanel
        v-else-if="activeTab === 'content'"
        :course-id="course.id"
      />

      <CourseLearningProgressPanel
        v-else-if="activeTab === 'progress'"
        :course-id="course.id"
      />

      <CourseAssignmentsPanel
        v-else-if="activeTab === 'assignments'"
        :course-id="course.id"
      />

      <CourseDocumentsPanel
        v-else-if="activeTab === 'documents'"
        :course-id="course.id"
        :can-manage="canManageCourse"
      />

      <DiscussionPanel
        v-else-if="activeTab === 'discussion'"
        :course-id="course.id"
      />

      <CourseAiPanel
        v-else-if="activeTab === 'ai'"
        :course-id="course.id"
      />

      <CourseFormModal
        :open="editOpen"
        :course="course"
        :loading="saving"
        :server-message="formMessage"
        :server-errors="formErrors"
        @close="editOpen = false"
        @submit="submitUpdate"
      />

      <CourseFeatureConfigModal
        :open="featureSettingsOpen"
        :config="course.featureConfig"
        :loading="savingFeatureConfig"
        @close="featureSettingsOpen = false"
        @save="handleFeatureConfigSave"
      />

      <Teleport to="body">
        <div
          v-if="deleteOpen"
          class="fixed inset-0 z-[80] flex items-center justify-center bg-slate-950/40 p-4"
          @mousedown.self="!deleting && (deleteOpen = false)"
        >
          <section
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="delete-course-title"
            class="w-full max-w-md rounded-card border border-app-border bg-app-surface p-6 shadow-overlay"
          >
            <div class="flex items-start justify-between gap-4">
              <div>
                <h2 id="delete-course-title" class="font-heading text-xl font-bold text-app-text">
                  Xóa khóa học?
                </h2>

                <p class="mt-2 text-sm leading-6 text-app-text-muted">
                  Bạn sắp xóa
                  <strong class="text-app-text">{{ course.title }}</strong>.
                  Hãy chắc chắn trước khi tiếp tục.
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

            <div class="mt-6 flex justify-end gap-3">
              <BaseButton variant="secondary" :disabled="deleting" @click="deleteOpen = false">
                Hủy
              </BaseButton>

              <button
                type="button"
                class="inline-flex h-11 items-center gap-2 rounded-control bg-danger px-4 text-sm font-semibold text-white disabled:opacity-60"
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