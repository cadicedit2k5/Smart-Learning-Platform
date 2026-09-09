<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  ArrowLeft,
  CalendarClock,
  CheckCircle2,
  ClipboardList,
  Eye,
  FileText,
  Pencil,
  Plus,
  RefreshCw,
  Trash2,
  UserRound,
} from 'lucide-vue-next'

import {
  BaseAlert,
  BaseButton,
  ConfirmDialog,
} from '@/shared/components'

import type {
  Assignment,
  AssignmentSubmission,
} from '@/shared/assignment/types'

import {
  createAssignment,
  deleteAssignment,
  getAssignments,
  getSubmissions,
  gradeSubmission,
  updateAssignment,
  type AssignmentInput,
  type GradeInput,
} from '../api/assignmentApi'

import {
  getUserLookup,
  type UserLookup,
} from '../api/memberApi'

import AssignmentFormModal from './AssignmentFormModal.vue'
import SubmissionReviewModal from './SubmissionReviewModal.vue'
import { useLecturerApiError } from '../composables/useLecturerApiError'
import { formatDateTime } from '@/shared/utils/date'

const props = defineProps<{
  courseId: string
}>()

const { handleApiError } = useLecturerApiError()

type AssignmentView = 'overview' | 'submissions'

const assignments = ref<Assignment[]>([])
const selectedAssignment = ref<Assignment | null>(null)
const assignmentView = ref<AssignmentView>('overview')

const submissions = ref<AssignmentSubmission[]>([])
const submissionsLoaded = ref(false)

const users = ref<Record<string, UserLookup>>({})

const loadingAssignments = ref(true)
const loadingSubmissions = ref(false)

const actionMessage = ref('')
const successMessage = ref('')

const formOpen = ref(false)
const editingAssignment = ref<Assignment | null>(null)
const saving = ref(false)
const formMessage = ref('')

const deleteOpen = ref(false)
const deleting = ref(false)

const reviewOpen = ref(false)
const reviewingSubmission = ref<AssignmentSubmission | null>(null)
const grading = ref(false)

const gradedCount = computed(() =>
  submissions.value.filter((item) => item.status === 'GRADED').length,
)

const ungradedCount = computed(() =>
  submissions.value.length - gradedCount.value,
)

const studentName = (studentId: string) => {
  const user = users.value[studentId]

  return user?.fullName || user?.email || studentId
}

const studentEmail = (studentId: string) =>
  users.value[studentId]?.email ?? ''

const sortAssignments = () => {
  assignments.value.sort(
    (a, b) =>
      new Date(a.dueAt).getTime() -
      new Date(b.dueAt).getTime(),
  )
}

const loadAssignments = async () => {
  loadingAssignments.value = true
  actionMessage.value = ''

  try {
    assignments.value = await getAssignments(props.courseId)
    sortAssignments()
  } catch (error) {
    actionMessage.value = handleApiError(
      error,
      'Không thể tải danh sách bài tập.',
    ).message
  } finally {
    loadingAssignments.value = false
  }
}

const loadUserLookups = async (
  items: AssignmentSubmission[],
) => {
  const ids = [
    ...new Set(
      items.map((item) => item.studentId),
    ),
  ].filter((id) => !users.value[id])

  await Promise.all(
    ids.map(async (id) => {
      try {
        const user = await getUserLookup(id)

        users.value = {
          ...users.value,
          [id]: user,
        }
      } catch {
        // Nếu không lấy được thông tin user,
        // giao diện vẫn hiển thị studentId.
      }
    }),
  )
}

const loadSubmissions = async () => {
  if (!selectedAssignment.value) return

  loadingSubmissions.value = true
  actionMessage.value = ''

  try {
    const result = await getSubmissions(
      props.courseId,
      selectedAssignment.value.id,
    )

    submissions.value = result
    submissionsLoaded.value = true

    await loadUserLookups(result)
  } catch (error) {
    actionMessage.value = handleApiError(
      error,
      'Không thể tải danh sách bài nộp.',
    ).message
  } finally {
    loadingSubmissions.value = false
  }
}

const openAssignment = (assignment: Assignment) => {
  selectedAssignment.value = assignment
  assignmentView.value = 'overview'

  submissions.value = []
  submissionsLoaded.value = false

  actionMessage.value = ''
  successMessage.value = ''
}

const backToAssignments = () => {
  selectedAssignment.value = null
  assignmentView.value = 'overview'

  submissions.value = []
  submissionsLoaded.value = false

  reviewOpen.value = false
  reviewingSubmission.value = null
}

const openOverview = () => {
  assignmentView.value = 'overview'
}

const openSubmissions = async () => {
  if (!selectedAssignment.value) return

  assignmentView.value = 'submissions'

  if (!submissionsLoaded.value) {
    await loadSubmissions()
  }
}

const openCreate = () => {
  editingAssignment.value = null
  formMessage.value = ''
  formOpen.value = true
}

const openEdit = () => {
  if (!selectedAssignment.value) return

  editingAssignment.value = selectedAssignment.value
  formMessage.value = ''
  formOpen.value = true
}

const closeForm = () => {
  if (saving.value) return

  formOpen.value = false
  editingAssignment.value = null
  formMessage.value = ''
}

const submitForm = async (
  input: AssignmentInput,
) => {
  saving.value = true
  formMessage.value = ''
  successMessage.value = ''
  actionMessage.value = ''

  try {
    if (editingAssignment.value) {
      const updated = await updateAssignment(
        props.courseId,
        editingAssignment.value.id,
        input,
      )

      const index = assignments.value.findIndex(
        (item) => item.id === updated.id,
      )

      if (index >= 0) {
        assignments.value[index] = updated
      }

      if (
        selectedAssignment.value?.id === updated.id
      ) {
        selectedAssignment.value = updated
      }

      sortAssignments()

      successMessage.value =
        'Đã cập nhật bài tập.'
    } else {
      const created = await createAssignment(
        props.courseId,
        input,
      )

      assignments.value.push(created)
      sortAssignments()

      successMessage.value =
        'Đã tạo bài tập mới.'
    }

    formOpen.value = false
    editingAssignment.value = null
  } catch (error) {
    formMessage.value = handleApiError(
      error,
      'Không thể lưu bài tập.',
    ).message
  } finally {
    saving.value = false
  }
}

const confirmDelete = () => {
  if (!selectedAssignment.value) return

  actionMessage.value = ''
  deleteOpen.value = true
}

const handleDelete = async () => {
  if (!selectedAssignment.value) return

  deleting.value = true
  actionMessage.value = ''

  const deletingId = selectedAssignment.value.id

  try {
    await deleteAssignment(
      props.courseId,
      deletingId,
    )

    assignments.value = assignments.value.filter(
      (item) => item.id !== deletingId,
    )

    deleteOpen.value = false
    successMessage.value = 'Đã xóa bài tập.'

    backToAssignments()
  } catch (error) {
    actionMessage.value = handleApiError(
      error,
      'Không thể xóa bài tập.',
    ).message
  } finally {
    deleting.value = false
  }
}

const openSubmission = (
  submission: AssignmentSubmission,
) => {
  reviewingSubmission.value = submission
  reviewOpen.value = true

  actionMessage.value = ''
}

const closeSubmission = () => {
  if (grading.value) return

  reviewOpen.value = false
  reviewingSubmission.value = null
}

const handleGrade = async (
  input: GradeInput,
) => {
  if (
    !selectedAssignment.value ||
    !reviewingSubmission.value
  ) {
    return
  }

  grading.value = true
  actionMessage.value = ''
  successMessage.value = ''

  try {
    const updated = await gradeSubmission(
      props.courseId,
      selectedAssignment.value.id,
      reviewingSubmission.value.id,
      input,
    )

    const index = submissions.value.findIndex(
      (item) => item.id === updated.id,
    )

    if (index >= 0) {
      submissions.value[index] = updated
    }

    reviewingSubmission.value = updated

    successMessage.value =
      `Đã chấm bài của ${studentName(updated.studentId)}.`
  } catch (error) {
    actionMessage.value = handleApiError(
      error,
      'Không thể chấm bài.',
    ).message
  } finally {
    grading.value = false
  }
}

const resetForCourse = () => {
  selectedAssignment.value = null
  assignmentView.value = 'overview'

  submissions.value = []
  submissionsLoaded.value = false

  users.value = {}

  reviewOpen.value = false
  reviewingSubmission.value = null

  actionMessage.value = ''
  successMessage.value = ''

  void loadAssignments()
}

watch(
  () => props.courseId,
  () => {
    resetForCourse()
  },
)

onMounted(() => {
  void loadAssignments()
})
</script>

<template>
  <section class="space-y-5">
    <!-- Messages -->
    <BaseAlert v-if="actionMessage">
      {{ actionMessage }}
    </BaseAlert>

    <BaseAlert
      v-if="successMessage"
      variant="ai"
    >
      <template #icon>
        <CheckCircle2 :size="18" />
      </template>

      {{ successMessage }}
    </BaseAlert>

    <!-- =====================================================
         ASSIGNMENT LIST PAGE
         ===================================================== -->
    <template v-if="!selectedAssignment">
      <header
        class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between"
      >
        <div>
          <h2
            class="font-heading text-xl font-bold text-app-text"
          >
            Bài tập
          </h2>

          <p
            class="mt-1 text-sm text-app-text-muted"
          >
            Tạo và quản lý bài tập của khóa học.
          </p>
        </div>

        <BaseButton @click="openCreate">
          <template #leading>
            <Plus :size="17" />
          </template>

          Tạo bài tập
        </BaseButton>
      </header>

      <!-- Loading assignments -->
      <div
        v-if="loadingAssignments"
        class="space-y-3"
      >
        <div
          v-for="index in 4"
          :key="index"
          class="h-32 animate-pulse rounded-card bg-app-surface-muted"
        />
      </div>

      <!-- Empty assignments -->
      <div
        v-else-if="assignments.length === 0"
        class="rounded-panel border border-dashed border-app-border bg-app-surface px-6 py-16 text-center"
      >
        <ClipboardList
          :size="42"
          class="mx-auto text-app-text-muted/40"
        />

        <h3
          class="mt-4 font-heading text-xl font-bold text-app-text"
        >
          Chưa có bài tập
        </h3>

        <p
          class="mt-2 text-sm text-app-text-muted"
        >
          Tạo bài tập đầu tiên cho học viên
          trong khóa học.
        </p>

        <BaseButton
          class="mt-5"
          @click="openCreate"
        >
          <template #leading>
            <Plus :size="17" />
          </template>

          Tạo bài tập
        </BaseButton>
      </div>

      <!-- Assignment cards -->
      <div
        v-else
        class="space-y-3"
      >
        <button
          v-for="assignment in assignments"
          :key="assignment.id"
          type="button"
          class="group w-full rounded-card border border-app-border bg-app-surface p-5 text-left shadow-card transition hover:border-secondary/40 hover:shadow-md"
          @click="openAssignment(assignment)"
        >
          <div
            class="flex items-start justify-between gap-5"
          >
            <div class="min-w-0 flex-1">
              <div
                class="flex flex-wrap items-center gap-2"
              >
                <h3
                  class="font-heading text-lg font-bold text-app-text"
                >
                  {{ assignment.title }}
                </h3>

                <span
                  class="rounded-pill px-2.5 py-1 text-xs font-semibold"
                  :class="
                    assignment.expired
                      ? 'bg-danger-soft text-danger'
                      : 'bg-ai-soft text-ai'
                  "
                >
                  {{
                    assignment.expired
                      ? 'Đã hết hạn'
                      : 'Đang mở'
                  }}
                </span>
              </div>

              <div
                class="mt-2 flex flex-wrap gap-x-5 gap-y-2 text-sm text-app-text-muted"
              >
                <span
                  class="inline-flex items-center gap-1.5"
                >
                  <CalendarClock :size="15" />

                  Hạn
                  {{
                    formatDateTime(
                      assignment.dueAt,
                    )
                  }}
                </span>

                <span>
                  {{ assignment.maxScore }}
                  điểm
                </span>
              </div>

              <p
                v-if="assignment.description"
                class="mt-3 line-clamp-2 max-w-4xl text-sm leading-6 text-app-text-muted"
              >
                {{ assignment.description }}
              </p>
            </div>

            <span
              class="shrink-0 pt-1 text-sm font-semibold text-secondary transition group-hover:translate-x-1"
            >
              Xem chi tiết →
            </span>
          </div>
        </button>
      </div>
    </template>

    <!-- =====================================================
         ASSIGNMENT DETAIL PAGE
         ===================================================== -->
    <template v-else>
      <!-- Back -->
      <button
        type="button"
        class="inline-flex items-center gap-2 text-sm font-semibold text-app-text-muted transition hover:text-secondary"
        @click="backToAssignments"
      >
        <ArrowLeft :size="16" />
        Danh sách bài tập
      </button>

      <!-- Assignment header -->
      <header
        class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between"
      >
        <div class="min-w-0">
          <div
            class="flex flex-wrap items-center gap-2"
          >
            <h2
              class="font-heading text-2xl font-bold text-app-text"
            >
              {{ selectedAssignment.title }}
            </h2>

            <span
              class="rounded-pill px-2.5 py-1 text-xs font-semibold"
              :class="
                selectedAssignment.expired
                  ? 'bg-danger-soft text-danger'
                  : 'bg-ai-soft text-ai'
              "
            >
              {{
                selectedAssignment.expired
                  ? 'Đã hết hạn'
                  : 'Đang mở'
              }}
            </span>
          </div>

          <div
            class="mt-2 flex flex-wrap gap-x-4 gap-y-2 text-sm text-app-text-muted"
          >
            <span
              class="inline-flex items-center gap-1.5"
            >
              <CalendarClock :size="15" />

              Hạn
              {{
                formatDateTime(
                  selectedAssignment.dueAt,
                )
              }}
            </span>

            <span>
              {{ selectedAssignment.maxScore }}
              điểm
            </span>
          </div>
        </div>

        <div
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

          <button
            type="button"
            class="inline-flex h-11 items-center gap-2 rounded-control bg-danger-soft px-4 text-sm font-semibold text-danger transition hover:opacity-80"
            @click="confirmDelete"
          >
            <Trash2 :size="16" />
            Xóa
          </button>
        </div>
      </header>

      <!-- Assignment navigation -->
      <nav
        class="border-b border-app-border"
        aria-label="Quản lý bài tập"
      >
        <div class="flex gap-6">
          <button
            type="button"
            class="h-11 border-b-2 text-sm font-semibold transition"
            :class="
              assignmentView === 'overview'
                ? 'border-secondary text-secondary'
                : 'border-transparent text-app-text-muted hover:text-app-text'
            "
            @click="openOverview"
          >
            Tổng quan
          </button>

          <button
            type="button"
            class="h-11 border-b-2 text-sm font-semibold transition"
            :class="
              assignmentView === 'submissions'
                ? 'border-secondary text-secondary'
                : 'border-transparent text-app-text-muted hover:text-app-text'
            "
            @click="openSubmissions"
          >
            Bài nộp

            <span v-if="submissionsLoaded">
              ({{ submissions.length }})
            </span>
          </button>
        </div>
      </nav>

      <!-- ===================================================
           OVERVIEW
           =================================================== -->
      <section
        v-if="assignmentView === 'overview'"
        class="rounded-card border border-app-border bg-app-surface p-6 shadow-card"
      >
        <h3
          class="font-heading text-lg font-bold text-app-text"
        >
          Nội dung bài tập
        </h3>

        <p
          class="mt-4 whitespace-pre-wrap text-sm leading-7 text-app-text-muted"
        >
          {{
            selectedAssignment.description ||
            'Bài tập không có mô tả.'
          }}
        </p>

        <dl
          class="mt-6 grid gap-5 border-t border-app-border pt-5 sm:grid-cols-2"
        >
          <div>
            <dt
              class="text-xs font-semibold uppercase tracking-wide text-app-text-muted"
            >
              Hạn nộp
            </dt>

            <dd
              class="mt-2 font-semibold text-app-text"
            >
              {{
                formatDateTime(
                  selectedAssignment.dueAt,
                )
              }}
            </dd>
          </div>

          <div>
            <dt
              class="text-xs font-semibold uppercase tracking-wide text-app-text-muted"
            >
              Điểm tối đa
            </dt>

            <dd
              class="mt-2 font-semibold text-app-text"
            >
              {{ selectedAssignment.maxScore }}
              điểm
            </dd>
          </div>
        </dl>
      </section>

      <!-- ===================================================
           SUBMISSIONS
           =================================================== -->
      <section
        v-else
        class="overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card"
      >
        <!-- Submission header -->
        <header
          class="flex flex-wrap items-center justify-between gap-4 border-b border-app-border px-5 py-4"
        >
          <div>
            <h3
              class="font-heading text-lg font-bold text-app-text"
            >
              Bài nộp
            </h3>

            <p
              class="mt-1 text-sm text-app-text-muted"
            >
              {{ submissions.length }}
              đã nộp ·
              {{ gradedCount }}
              đã chấm ·
              {{ ungradedCount }}
              chưa chấm
            </p>
          </div>

          <BaseButton
            variant="secondary"
            :disabled="loadingSubmissions"
            @click="loadSubmissions"
          >
            <template #leading>
              <RefreshCw
                :size="15"
                :class="{
                  'animate-spin':
                    loadingSubmissions,
                }"
              />
            </template>

            Tải lại
          </BaseButton>
        </header>

        <!-- Loading submissions -->
        <div
          v-if="loadingSubmissions"
          class="space-y-2 p-5"
        >
          <div
            v-for="index in 4"
            :key="index"
            class="h-16 animate-pulse rounded-control bg-app-surface-muted"
          />
        </div>

        <!-- Empty submissions -->
        <div
          v-else-if="
            submissions.length === 0
          "
          class="px-6 py-14 text-center"
        >
          <FileText
            :size="34"
            class="mx-auto text-app-text-muted/40"
          />

          <p
            class="mt-3 font-semibold text-app-text"
          >
            Chưa có bài nộp
          </p>

          <p
            class="mt-1 text-sm text-app-text-muted"
          >
            Chưa có học viên nào nộp
            bài tập này.
          </p>
        </div>

        <!-- Submission list -->
        <div
          v-else
          class="divide-y divide-app-border"
        >
          <div
            v-for="submission in submissions"
            :key="submission.id"
            class="flex flex-col gap-4 px-5 py-4 transition hover:bg-app-surface-muted/40 md:flex-row md:items-center"
          >
            <!-- Student -->
            <div
              class="flex min-w-0 flex-1 items-center gap-3"
            >
              <span
                class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-secondary-soft text-secondary"
              >
                <UserRound :size="18" />
              </span>

              <div class="min-w-0">
                <p
                  class="truncate font-semibold text-app-text"
                >
                  {{
                    studentName(
                      submission.studentId,
                    )
                  }}
                </p>

                <p
                  v-if="
                    studentEmail(
                      submission.studentId,
                    )
                  "
                  class="truncate text-xs text-app-text-muted"
                >
                  {{
                    studentEmail(
                      submission.studentId,
                    )
                  }}
                </p>
              </div>
            </div>

            <!-- Submitted time -->
            <div
              class="min-w-36 text-sm text-app-text-muted"
            >
              {{
                formatDateTime(
                  submission.submittedAt,
                )
              }}
            </div>

            <!-- Late -->
            <div class="min-w-20">
              <span
                v-if="submission.late"
                class="inline-flex rounded-pill bg-danger-soft px-2.5 py-1 text-xs font-semibold text-danger"
              >
                Nộp trễ
              </span>
            </div>

            <!-- Status -->
            <div class="min-w-24">
              <span
                class="inline-flex rounded-pill px-2.5 py-1 text-xs font-semibold"
                :class="
                  submission.status ===
                  'GRADED'
                    ? 'bg-ai-soft text-ai'
                    : 'bg-secondary-soft text-secondary'
                "
              >
                {{
                  submission.status ===
                  'GRADED'
                    ? 'Đã chấm'
                    : 'Chưa chấm'
                }}
              </span>
            </div>

            <!-- Score -->
            <div
              class="min-w-20 text-sm font-semibold text-app-text"
            >
              {{
                submission.score != null
                  ? `${submission.score}/${selectedAssignment.maxScore}`
                  : '--'
              }}
            </div>

            <!-- Action -->
            <BaseButton
              variant="secondary"
              @click="
                openSubmission(submission)
              "
            >
              <template #leading>
                <Eye :size="15" />
              </template>

              Xem bài
            </BaseButton>
          </div>
        </div>
      </section>
    </template>

    <!-- =====================================================
         SUBMISSION REVIEW MODAL
         ===================================================== -->
    <SubmissionReviewModal
      :open="reviewOpen"
      :submission="reviewingSubmission"
      :student-name="
        reviewingSubmission
          ? studentName(
              reviewingSubmission.studentId,
            )
          : ''
      "
      :student-email="
        reviewingSubmission
          ? studentEmail(
              reviewingSubmission.studentId,
            )
          : ''
      "
      :max-score="
        selectedAssignment?.maxScore ?? 0
      "
      :loading="grading"
      @close="closeSubmission"
      @grade="handleGrade"
    />

    <!-- =====================================================
         CREATE / EDIT ASSIGNMENT
         ===================================================== -->
    <AssignmentFormModal
      :open="formOpen"
      :assignment="editingAssignment"
      :loading="saving"
      :server-message="formMessage"
      @close="closeForm"
      @submit="submitForm"
    />

    <!-- =====================================================
         DELETE ASSIGNMENT
         ===================================================== -->
    <ConfirmDialog
      :open="deleteOpen"
      title="Xóa bài tập?"
      :description="
        selectedAssignment
          ? `Bạn có chắc muốn xóa “${selectedAssignment.title}”?`
          : ''
      "
      confirm-text="Xóa bài tập"
      :loading="deleting"
      @close="
        !deleting &&
        (deleteOpen = false)
      "
      @confirm="handleDelete"
    />
  </section>
</template>