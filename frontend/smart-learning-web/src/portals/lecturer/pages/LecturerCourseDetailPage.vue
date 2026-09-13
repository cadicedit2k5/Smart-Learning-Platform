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
  Trash2,
  UserRoundCheck,
  UsersRound,
  X,
} from 'lucide-vue-next'

import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import CourseCover from '@/shared/course/CourseCover.vue'
import { getCourse, type Course } from '@/shared/course'
import {
  courseStatusLabel,
  courseVisibilityLabel,
} from '@/shared/course/presentation'
import {
  hasRichTextContent,
  parseStoredRichText,
  RichTextViewer,
} from '@/shared/rich-text'
import { formatDateTime } from '@/shared/utils/date'
import DiscussionPanel from '@/features/discusstion/components/DiscussionPanel.vue'

import {
  deleteCourse,
  publishCourse,
  updateCourse,
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

const canManageCourse = computed(() =>
  course.value?.currentUserRole === 'OWNER',
)

const canReviewJoinRequests = computed(() =>
  canManageCourse.value &&
  course.value?.status === 'PUBLISHED' &&
  course.value?.visibility === 'PUBLIC',
)

const descriptionContent = computed(() =>
  parseStoredRichText(course.value?.description),
)

const hasDescription = computed(() =>
  hasRichTextContent(descriptionContent.value),
)

const tabs = computed<
  Array<{
    id: DetailTab
    label: string
    icon: typeof BookOpen
  }>
>(() => [
  {
    id: 'overview',
    label: 'Tổng quan',
    icon: BookOpen,
  },
  {
    id: 'announcements',
    label: 'Thông báo',
    icon: Bell,
  },

  ...(canManageCourse.value
    ? [
        {
          id: 'members' as const,
          label: 'Thành viên',
          icon: UsersRound,
        },
        ...(canReviewJoinRequests.value
          ? [
              {
                id: 'requests' as const,
                label: 'Yêu cầu tham gia',
                icon: UserRoundCheck,
              },
            ]
          : []),
        {
          id: 'content' as const,
          label: 'Nội dung',
          icon: Layers3,
        },
        {
          id: 'progress' as const,
          label: 'Tiến độ',
          icon: ChartNoAxesColumnIncreasing,
        },
      ]
    : []),

  {
    id: 'assignments',
    label: 'Bài tập',
    icon: ClipboardList,
  },
  {
    id: 'documents',
    label: 'Tài liệu',
    icon: FileText,
  },
  {
    id: 'discussion',
    label: 'Thảo luận',
    icon: MessagesSquare,
  },
  {
    id: 'ai',
    label: 'Trợ lý AI',
    icon: Bot,
  },
])

const loadCourse = async () => {
  loading.value = true
  loadError.value = ''

  try {
    course.value = await getCourse(courseId.value)
  } catch (error) {
    loadError.value = handleApiError(
      error,
      'Không thể tải chi tiết khóa học.',
    ).message
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
    const parsed = handleApiError(
      error,
      'Không thể cập nhật khóa học.',
    )

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
    successMessage.value =
      'Khóa học đã được xuất bản thành công.'
  } catch (error) {
    actionMessage.value = handleApiError(
      error,
      'Không thể xuất bản khóa học.',
    ).message
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

    await router.replace({
      name: 'lecturer-courses',
    })
  } catch (error) {
    actionMessage.value = handleApiError(
      error,
      'Không thể xóa khóa học.',
    ).message

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

    <div
      v-if="loading"
      class="space-y-4"
    >
      <div
        class="h-12 animate-pulse rounded-card bg-app-surface-muted"
      />

      <div
        class="h-[28rem] animate-pulse rounded-[1.5rem] bg-app-surface-muted"
      />
    </div>

    <BaseAlert v-else-if="loadError">
      <div
        class="flex items-center justify-between gap-3"
      >
        <span>{{ loadError }}</span>

        <button
          type="button"
          class="font-semibold underline"
          @click="loadCourse"
        >
          Thử lại
        </button>
      </div>
    </BaseAlert>

    <template v-else-if="course">
      <BaseAlert v-if="actionMessage">
        {{ actionMessage }}
      </BaseAlert>

      <BaseAlert
        v-if="successMessage"
        variant="success"
      >
        <template #icon>
          <CheckCircle2 :size="18" />
        </template>

        {{ successMessage }}
      </BaseAlert>

      <nav
        class="border-b border-app-border"
        aria-label="Nội dung khóa học"
      >
        <div class="flex gap-6 overflow-x-auto">
          <button
            v-for="tab in tabs"
            :key="tab.id"
            type="button"
            class="flex h-12 shrink-0 items-center gap-2 border-b-2 px-1 text-sm font-semibold transition-colors"
            :class="
              activeTab === tab.id
                ? 'border-secondary text-secondary'
                : 'border-transparent text-app-text-muted hover:text-app-text'
            "
            @click="activeTab = tab.id"
          >
            <component
              :is="tab.icon"
              :size="17"
            />

            {{ tab.label }}
          </button>
        </div>
      </nav>

      <div
        v-if="activeTab === 'overview'"
        class="space-y-6"
      >
        <section
          class="relative overflow-hidden rounded-[1.5rem] border border-app-border bg-gradient-to-br from-white via-white to-secondary-soft/40 shadow-card"
        >
          <div
            class="pointer-events-none absolute -right-24 -top-24 h-72 w-72 rounded-full bg-secondary/10 blur-3xl"
          />

          <div
            class="relative grid gap-0 lg:grid-cols-[21rem_minmax(0,1fr)]"
          >
            <div
              class="border-b border-app-border/70 bg-app-surface/70 p-4 backdrop-blur lg:border-b-0 lg:border-r"
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

            <div class="flex min-w-0 flex-col p-6 sm:p-8">
              <div
                class="flex flex-col gap-5 xl:flex-row xl:items-start xl:justify-between"
              >
                <div class="min-w-0">
                  <div
                    class="flex flex-wrap items-center gap-2"
                  >
                    <span
                      class="inline-flex items-center gap-1.5 rounded-pill bg-secondary-soft px-3 py-1.5 text-xs font-semibold text-secondary"
                    >
                      <span
                        class="h-1.5 w-1.5 rounded-full bg-secondary"
                      />

                      {{ courseStatusLabel[course.status] }}
                    </span>

                    <span
                      class="rounded-pill border border-app-border bg-white/80 px-3 py-1.5 text-xs font-medium text-app-text-muted"
                    >
                      {{ courseVisibilityLabel[course.visibility] }}
                    </span>
                  </div>

                  <h1
                    class="mt-5 max-w-3xl font-heading text-3xl font-bold leading-tight tracking-tight text-app-text sm:text-4xl"
                  >
                    {{ course.title }}
                  </h1>
                </div>

                <div
                  v-if="canManageCourse"
                  class="flex shrink-0 flex-wrap gap-2"
                >
                  <BaseButton
                    variant="secondary"
                    @click="openEdit"
                  >
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
                    aria-label="Xóa khóa học"
                    title="Xóa khóa học"
                    class="flex h-11 w-11 items-center justify-center rounded-control border border-app-border bg-white text-app-text-muted transition hover:border-danger/30 hover:bg-danger-soft hover:text-danger"
                    @click="deleteOpen = true"
                  >
                    <Trash2 :size="17" />
                  </button>
                </div>
              </div>

              <div class="mt-6 max-w-4xl">
                <RichTextViewer
                  v-if="hasDescription"
                  :content="descriptionContent"
                />

                <p
                  v-else
                  class="text-sm leading-7 text-app-text-muted"
                >
                  Chưa có mô tả cho khóa học này.
                </p>
              </div>

              <div
                class="mt-auto grid gap-4 border-t border-app-border/80 pt-6 sm:grid-cols-3"
              >
                <div class="flex items-center gap-3">
                  <div
                    class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-white text-secondary shadow-sm ring-1 ring-app-border"
                  >
                    <GraduationCap :size="16" />
                  </div>

                  <div class="min-w-0">
                    <p
                      class="text-xs text-app-text-muted"
                    >
                      Cấp độ
                    </p>

                    <p
                      class="mt-0.5 truncate text-sm font-semibold text-app-text"
                    >
                      {{ course.level || 'Chưa cập nhật' }}
                    </p>
                  </div>
                </div>

                <div class="flex items-center gap-3">
                  <div
                    class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-white text-secondary shadow-sm ring-1 ring-app-border"
                  >
                    <Eye :size="16" />
                  </div>

                  <div class="min-w-0">
                    <p
                      class="text-xs text-app-text-muted"
                    >
                      Truy cập
                    </p>

                    <p
                      class="mt-0.5 truncate text-sm font-semibold text-app-text"
                    >
                      {{ courseVisibilityLabel[course.visibility] }}
                    </p>
                  </div>
                </div>

                <div class="flex items-center gap-3">
                  <div
                    class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-white text-secondary shadow-sm ring-1 ring-app-border"
                  >
                    <CalendarDays :size="16" />
                  </div>

                  <div class="min-w-0">
                    <p
                      class="text-xs text-app-text-muted"
                    >
                      {{
                        course.status === 'PUBLISHED'
                          ? 'Xuất bản'
                          : 'Cập nhật gần nhất'
                      }}
                    </p>

                    <p
                      class="mt-0.5 truncate text-sm font-semibold text-app-text"
                    >
                      {{
                        course.status === 'PUBLISHED' &&
                        course.publishedAt
                          ? formatDateTime(course.publishedAt)
                          : formatDateTime(course.updatedAt)
                      }}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section
          v-if="course.status === 'DRAFT' && canManageCourse"
          class="flex flex-col gap-4 rounded-[1.25rem] border border-secondary/15 bg-secondary-soft/50 p-5 sm:flex-row sm:items-center sm:justify-between"
        >
          <div class="flex gap-3">
            <div
              class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white text-secondary shadow-sm"
            >
              <Rocket :size="18" />
            </div>

            <div>
              <h2
                class="font-heading text-base font-bold text-app-text"
              >
                Khóa học đang ở bản nháp
              </h2>

              <p
                class="mt-1 text-sm leading-6 text-app-text-muted"
              >
                Hoàn thiện nội dung và xuất bản khi khóa học đã sẵn sàng cho học viên.
              </p>
            </div>
          </div>

          <BaseButton
            class="shrink-0"
            :loading="publishing"
            @click="handlePublish"
          >
            <template #leading>
              <Rocket :size="16" />
            </template>

            Xuất bản khóa học
          </BaseButton>
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

      <Teleport to="body">
        <div
          v-if="deleteOpen"
          class="fixed inset-0 z-[80] flex items-center justify-center bg-slate-950/40 p-4"
          @mousedown.self="
            !deleting && (deleteOpen = false)
          "
        >
          <section
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="delete-course-title"
            class="w-full max-w-md rounded-card border border-app-border bg-app-surface p-6 shadow-overlay"
          >
            <div
              class="flex items-start justify-between gap-4"
            >
              <div>
                <h2
                  id="delete-course-title"
                  class="font-heading text-xl font-bold text-app-text"
                >
                  Xóa khóa học?
                </h2>

                <p
                  class="mt-2 text-sm leading-6 text-app-text-muted"
                >
                  Bạn sắp xóa
                  <strong class="text-app-text">
                    {{ course.title }}
                  </strong>.
                  Hãy chắc chắn trước khi tiếp tục.
                </p>
              </div>

              <button
                type="button"
                aria-label="Đóng"
                class="rounded-control p-2 text-app-text-muted transition hover:bg-app-surface-muted"
                :disabled="deleting"
                @click="deleteOpen = false"
              >
                <X :size="19" />
              </button>
            </div>

            <div
              class="mt-6 flex justify-end gap-3"
            >
              <BaseButton
                variant="secondary"
                :disabled="deleting"
                @click="deleteOpen = false"
              >
                Hủy
              </BaseButton>

              <button
                type="button"
                class="inline-flex h-11 items-center gap-2 rounded-control bg-danger px-4 text-sm font-semibold text-white transition hover:opacity-90 disabled:opacity-60"
                :disabled="deleting"
                @click="handleDelete"
              >
                <span
                  v-if="deleting"
                  class="h-4 w-4 animate-spin rounded-full border-2 border-current border-r-transparent"
                />

                <Trash2
                  v-else
                  :size="17"
                />

                Xóa khóa học
              </button>
            </div>
          </section>
        </div>
      </Teleport>
    </template>
  </section>
</template>