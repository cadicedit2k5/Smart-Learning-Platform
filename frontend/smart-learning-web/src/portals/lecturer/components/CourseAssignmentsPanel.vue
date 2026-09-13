<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  ArrowLeft,
  CalendarClock,
  CheckCircle2,
  ChevronRight,
  ClipboardList,
  FileText,
  Pencil,
  Plus,
  RefreshCw,
  Trash2,
  UserRound,
  UsersRound,
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
  hasRichTextContent,
  parseStoredRichText,
  RichTextViewer,
  storedRichTextToPlainText,
} from '@/shared/rich-text'
import { formatDateTime } from '@/shared/utils/date'

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

const props = defineProps<{ courseId: string }>()

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

const selectedDescription = computed(() =>
  parseStoredRichText(selectedAssignment.value?.description),
)

const hasSelectedDescription = computed(() =>
  hasRichTextContent(selectedDescription.value),
)

const preview = (value: string | null) =>
  storedRichTextToPlainText(value)

const studentName = (studentId: string) => {
  const user = users.value[studentId]
  return user?.fullName || user?.email || studentId
}

const studentEmail = (studentId: string) =>
  users.value[studentId]?.email ?? ''

const sortAssignments = () => {
  assignments.value.sort(
    (a, b) => new Date(a.dueAt).getTime() - new Date(b.dueAt).getTime(),
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

const loadUserLookups = async (items: AssignmentSubmission[]) => {
  const ids = [...new Set(items.map((item) => item.studentId))]
    .filter((id) => !users.value[id])

  await Promise.all(
    ids.map(async (id) => {
      try {
        const user = await getUserLookup(id)
        users.value = { ...users.value, [id]: user }
      } catch {
        // Giữ studentId nếu user lookup thất bại.
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

const submitForm = async (input: AssignmentInput) => {
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

      const index = assignments.value.findIndex((item) => item.id === updated.id)
      if (index >= 0) assignments.value[index] = updated
      if (selectedAssignment.value?.id === updated.id) selectedAssignment.value = updated

      sortAssignments()
      successMessage.value = 'Đã cập nhật bài tập.'
    } else {
      const created = await createAssignment(props.courseId, input)
      assignments.value.push(created)
      sortAssignments()
      successMessage.value = 'Đã tạo bài tập mới.'
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
    await deleteAssignment(props.courseId, deletingId)
    assignments.value = assignments.value.filter((item) => item.id !== deletingId)
    deleteOpen.value = false
    backToAssignments()
    successMessage.value = 'Đã xóa bài tập.'
  } catch (error) {
    actionMessage.value = handleApiError(
      error,
      'Không thể xóa bài tập.',
    ).message
  } finally {
    deleting.value = false
  }
}

const openSubmission = (submission: AssignmentSubmission) => {
  reviewingSubmission.value = submission
  reviewOpen.value = true
  actionMessage.value = ''
}

const closeSubmission = () => {
  if (grading.value) return
  reviewOpen.value = false
  reviewingSubmission.value = null
}

const handleGrade = async (input: GradeInput) => {
  if (!selectedAssignment.value || !reviewingSubmission.value) return

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

    const index = submissions.value.findIndex((item) => item.id === updated.id)
    if (index >= 0) submissions.value[index] = updated

    reviewingSubmission.value = updated
    successMessage.value = `Đã chấm bài của ${studentName(updated.studentId)}.`
  } catch (error) {
    actionMessage.value = handleApiError(error, 'Không thể chấm bài.').message
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

watch(() => props.courseId, resetForCourse)
onMounted(() => void loadAssignments())
</script>

<template>
  <section class="space-y-6">
    <BaseAlert v-if="actionMessage">{{ actionMessage }}</BaseAlert>

    <BaseAlert v-if="successMessage" variant="success">
      <template #icon><CheckCircle2 :size="18" /></template>
      {{ successMessage }}
    </BaseAlert>

    <template v-if="!selectedAssignment">
      <header class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 class="mt-1 font-heading text-2xl font-bold text-app-text">
            Bài tập
          </h2>

          <p class="mt-1 text-sm text-app-text-muted">
            Tạo hoạt động đánh giá, theo dõi bài nộp và chấm điểm học viên.
          </p>
        </div>

        <BaseButton @click="openCreate">
          <template #leading><Plus :size="17" /></template>
          Tạo bài tập
        </BaseButton>
      </header>

      <div v-if="loadingAssignments" class="space-y-3">
        <div
          v-for="index in 4"
          :key="index"
          class="h-32 animate-pulse rounded-[1.1rem] bg-app-surface-muted"
        />
      </div>

      <div
        v-else-if="assignments.length === 0"
        class="rounded-[1.25rem] border border-dashed border-app-border bg-app-surface px-6 py-14 text-center"
      >
        <div class="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-secondary-soft text-secondary">
          <ClipboardList :size="25" />
        </div>

        <h3 class="mt-4 font-heading text-lg font-bold text-app-text">
          Chưa có bài tập
        </h3>

        <p class="mt-1 text-sm text-app-text-muted">
          Tạo hoạt động đánh giá đầu tiên cho khóa học.
        </p>

        <BaseButton class="mt-5" @click="openCreate">
          <template #leading><Plus :size="17" /></template>
          Tạo bài tập
        </BaseButton>
      </div>

      <div v-else class="space-y-3">
        <button
          v-for="assignment in assignments"
          :key="assignment.id"
          type="button"
          class="group grid w-full gap-4 rounded-[1.1rem] border border-app-border bg-app-surface p-5 text-left shadow-card transition hover:border-secondary/30 hover:shadow-md sm:grid-cols-[auto_minmax(0,1fr)_auto]"
          @click="openAssignment(assignment)"
        >
          <div
            class="flex h-11 w-11 items-center justify-center rounded-xl"
            :class="
              assignment.expired
                ? 'bg-danger-soft text-danger'
                : 'bg-secondary-soft text-secondary'
            "
          >
            <ClipboardList :size="19" />
          </div>

          <div class="min-w-0">
            <div class="flex flex-wrap items-center gap-2">
              <h3 class="font-heading text-base font-bold text-app-text">
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
                {{ assignment.expired ? 'Đã hết hạn' : 'Đang mở' }}
              </span>
            </div>

            <p
              v-if="preview(assignment.description)"
              class="mt-2 line-clamp-2 max-w-4xl text-sm leading-6 text-app-text-muted"
            >
              {{ preview(assignment.description) }}
            </p>

            <div class="mt-3 flex flex-wrap gap-x-5 gap-y-1 text-xs text-app-text-muted">
              <span class="inline-flex items-center gap-1.5">
                <CalendarClock :size="14" />
                {{ formatDateTime(assignment.dueAt) }}
              </span>

              <span>{{ assignment.maxScore }} điểm</span>
            </div>
          </div>

          <ChevronRight
            :size="19"
            class="hidden self-center text-app-text-muted transition group-hover:translate-x-1 group-hover:text-secondary sm:block"
          />
        </button>
      </div>
    </template>

    <template v-else>
      <button
        type="button"
        class="inline-flex items-center gap-2 text-sm font-semibold text-app-text-muted transition hover:text-secondary"
        @click="backToAssignments"
      >
        <ArrowLeft :size="16" />
        Danh sách bài tập
      </button>

      <section class="overflow-hidden rounded-[1.3rem] border border-app-border bg-app-surface shadow-card">
        <header class="border-b border-app-border bg-gradient-to-br from-white to-secondary-soft/30 px-6 py-6 sm:px-7">
          <div class="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
            <div class="min-w-0">
              <div class="flex flex-wrap items-center gap-2">
                <span
                  class="rounded-pill px-2.5 py-1 text-xs font-semibold"
                  :class="
                    selectedAssignment.expired
                      ? 'bg-danger-soft text-danger'
                      : 'bg-ai-soft text-ai'
                  "
                >
                  {{ selectedAssignment.expired ? 'Đã hết hạn' : 'Đang mở' }}
                </span>
              </div>

              <h2 class="mt-3 font-heading text-2xl font-bold leading-tight text-app-text sm:text-3xl">
                {{ selectedAssignment.title }}
              </h2>

              <div class="mt-3 flex flex-wrap gap-x-5 gap-y-2 text-sm text-app-text-muted">
                <span class="inline-flex items-center gap-1.5">
                  <CalendarClock :size="15" />
                  Hạn {{ formatDateTime(selectedAssignment.dueAt) }}
                </span>

                <span>{{ selectedAssignment.maxScore }} điểm</span>
              </div>
            </div>

            <div class="flex shrink-0 gap-2">
              <BaseButton variant="secondary" @click="openEdit">
                <template #leading><Pencil :size="16" /></template>
                Chỉnh sửa
              </BaseButton>

              <button
                type="button"
                title="Xóa bài tập"
                class="flex h-11 w-11 items-center justify-center rounded-control border border-app-border text-app-text-muted transition hover:border-danger/30 hover:bg-danger-soft hover:text-danger"
                @click="confirmDelete"
              >
                <Trash2 :size="17" />
              </button>
            </div>
          </div>
        </header>

        <nav class="flex border-b border-app-border px-6" aria-label="Chi tiết bài tập">
          <button
            type="button"
            class="h-12 border-b-2 px-1 text-sm font-semibold"
            :class="
              assignmentView === 'overview'
                ? 'border-secondary text-secondary'
                : 'border-transparent text-app-text-muted hover:text-app-text'
            "
            @click="openOverview"
          >
            Nội dung bài tập
          </button>

          <button
            type="button"
            class="ml-6 flex h-12 items-center gap-2 border-b-2 px-1 text-sm font-semibold"
            :class="
              assignmentView === 'submissions'
                ? 'border-secondary text-secondary'
                : 'border-transparent text-app-text-muted hover:text-app-text'
            "
            @click="openSubmissions"
          >
            Bài nộp
            <span
              v-if="submissionsLoaded"
              class="rounded-pill bg-app-surface-muted px-2 py-0.5 text-xs"
            >
              {{ submissions.length }}
            </span>
          </button>
        </nav>

        <div v-if="assignmentView === 'overview'" class="grid gap-0 lg:grid-cols-[minmax(0,1fr)_18rem]">
          <article class="p-6 sm:p-7">
            <h3 class="font-heading text-lg font-bold text-app-text">
              Yêu cầu và hướng dẫn
            </h3>

            <div class="mt-5">
              <RichTextViewer
                v-if="hasSelectedDescription"
                :content="selectedDescription"
              />

              <p v-else class="text-sm text-app-text-muted">
                Bài tập chưa có nội dung hướng dẫn.
              </p>
            </div>
          </article>

          <aside class="border-t border-app-border bg-app-surface-muted/45 p-6 lg:border-l lg:border-t-0">
            <p class="text-xs font-bold uppercase tracking-[0.12em] text-app-text-muted">
              Thiết lập
            </p>

            <dl class="mt-5 space-y-5">
              <div>
                <dt class="text-xs text-app-text-muted">Hạn nộp</dt>
                <dd class="mt-1 text-sm font-semibold text-app-text">
                  {{ formatDateTime(selectedAssignment.dueAt) }}
                </dd>
              </div>

              <div>
                <dt class="text-xs text-app-text-muted">Điểm tối đa</dt>
                <dd class="mt-1 text-sm font-semibold text-app-text">
                  {{ selectedAssignment.maxScore }} điểm
                </dd>
              </div>

              <div>
                <dt class="text-xs text-app-text-muted">Trạng thái</dt>
                <dd
                  class="mt-1 text-sm font-semibold"
                  :class="selectedAssignment.expired ? 'text-danger' : 'text-ai'"
                >
                  {{ selectedAssignment.expired ? 'Đã hết hạn' : 'Đang nhận bài' }}
                </dd>
              </div>
            </dl>
          </aside>
        </div>

        <div v-else class="p-6 sm:p-7">
          <div class="flex flex-col gap-4 border-b border-app-border pb-5 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h3 class="font-heading text-lg font-bold text-app-text">
                Bài nộp của học viên
              </h3>

              <p class="mt-1 text-sm text-app-text-muted">
                {{ submissions.length }} bài nộp ·
                {{ ungradedCount }} chưa chấm ·
                {{ gradedCount }} đã chấm
              </p>
            </div>

            <BaseButton
              variant="secondary"
              :loading="loadingSubmissions"
              @click="loadSubmissions"
            >
              <template #leading><RefreshCw :size="16" /></template>
              Làm mới
            </BaseButton>
          </div>

          <div v-if="loadingSubmissions" class="mt-5 space-y-3">
            <div
              v-for="index in 4"
              :key="index"
              class="h-20 animate-pulse rounded-card bg-app-surface-muted"
            />
          </div>

          <div
            v-else-if="submissions.length === 0"
            class="py-12 text-center"
          >
            <UsersRound :size="34" class="mx-auto text-app-text-muted/35" />

            <h4 class="mt-3 font-heading font-bold text-app-text">
              Chưa có bài nộp
            </h4>

            <p class="mt-1 text-sm text-app-text-muted">
              Bài nộp của học viên sẽ xuất hiện tại đây.
            </p>
          </div>

          <div v-else class="mt-5 divide-y divide-app-border rounded-card border border-app-border">
            <button
              v-for="submission in submissions"
              :key="submission.id"
              type="button"
              class="flex w-full items-center gap-4 px-4 py-4 text-left transition hover:bg-app-surface-muted/50"
              @click="openSubmission(submission)"
            >
              <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-app-surface-muted text-app-text-muted">
                <UserRound :size="18" />
              </div>

              <div class="min-w-0 flex-1">
                <p class="truncate text-sm font-semibold text-app-text">
                  {{ studentName(submission.studentId) }}
                </p>

                <p class="mt-0.5 truncate text-xs text-app-text-muted">
                  {{ studentEmail(submission.studentId) || formatDateTime(submission.submittedAt) }}
                </p>
              </div>

              <span
                v-if="submission.late"
                class="hidden rounded-pill bg-danger-soft px-2.5 py-1 text-xs font-semibold text-danger sm:inline"
              >
                Nộp trễ
              </span>

              <div class="shrink-0 text-right">
                <p
                  class="text-xs font-semibold"
                  :class="submission.status === 'GRADED' ? 'text-ai' : 'text-secondary'"
                >
                  {{ submission.status === 'GRADED' ? 'Đã chấm' : 'Chờ chấm' }}
                </p>

                <p
                  v-if="submission.status === 'GRADED'"
                  class="mt-1 text-sm font-bold text-app-text"
                >
                  {{ submission.score }} / {{ selectedAssignment.maxScore }}
                </p>
              </div>

              <ChevronRight :size="17" class="shrink-0 text-app-text-muted" />
            </button>
          </div>
        </div>
      </section>

      <AssignmentFormModal
        :open="formOpen"
        :assignment="editingAssignment"
        :loading="saving"
        :server-message="formMessage"
        @close="closeForm"
        @submit="submitForm"
      />

      <ConfirmDialog
        :open="deleteOpen"
        title="Xóa bài tập?"
        :description="`Bạn có chắc muốn xóa “${selectedAssignment.title}”?`"
        confirm-text="Xóa bài tập"
        :loading="deleting"
        @close="deleteOpen = false"
        @confirm="handleDelete"
      />

      <SubmissionReviewModal
        :open="reviewOpen"
        :submission="reviewingSubmission"
        :student-name="reviewingSubmission ? studentName(reviewingSubmission.studentId) : ''"
        :student-email="reviewingSubmission ? studentEmail(reviewingSubmission.studentId) : ''"
        :max-score="selectedAssignment.maxScore"
        :loading="grading"
        @close="closeSubmission"
        @grade="handleGrade"
      />
    </template>
  </section>
</template>