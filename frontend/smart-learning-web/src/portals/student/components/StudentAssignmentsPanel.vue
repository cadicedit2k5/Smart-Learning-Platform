<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  ArrowLeft,
  CalendarClock,
  CheckCircle2,
  ClipboardCheck,
  ClipboardList,
  Eye,
  FileText,
  Paperclip,
  Trash2,
  Upload,
} from 'lucide-vue-next'

import {
  BaseAlert,
  BaseButton,
  BaseModal,
  ConfirmDialog,
} from '@/shared/components'

import {
  deleteMySubmission,
  getAssignments,
  getMySubmissions,
  submitAssignment,
} from '../api/assignmentApi'

import type {
  Assignment,
  AssignmentSubmission,
} from '@/shared/assignment/types'

import { useStudentApiError } from '../composables/useStudentApiError'
import { formatDateTime } from '@/shared/utils/date'

const props = defineProps<{
  courseId: string
}>()

const { handleApiError } = useStudentApiError()

const assignments = ref<Assignment[]>([])
const submissions = ref<AssignmentSubmission[]>([])
const selectedAssignment = ref<Assignment | null>(null)

const loading = ref(true)
const submitting = ref(false)
const deleting = ref(false)

const message = ref('')
const successMessage = ref('')

const submissionModalOpen = ref(false)
const submissionMode = ref<'view' | 'edit'>('view')

const content = ref('')
const file = ref<File | null>(null)
const fileInputKey = ref(0)

const deleteOpen = ref(false)

const submissionByAssignment = computed(() => {
  const map = new Map<string, AssignmentSubmission>()

  for (const submission of submissions.value) {
    map.set(submission.assignmentId, submission)
  }

  return map
})

const currentSubmission = computed(() => {
  if (!selectedAssignment.value) return null
  return submissionByAssignment.value.get(selectedAssignment.value.id) ?? null
})

const assignmentStatus = (assignment: Assignment) => {
  const submission = submissionByAssignment.value.get(assignment.id)

  if (submission?.status === 'GRADED') return 'GRADED'
  if (submission) return 'SUBMITTED'
  return 'NOT_SUBMITTED'
}

const load = async () => {
  loading.value = true
  message.value = ''

  try {
    const [assignmentData, submissionData] = await Promise.all([
      getAssignments(props.courseId),
      getMySubmissions(props.courseId),
    ])

    assignments.value = assignmentData.sort(
      (a, b) => new Date(a.dueAt).getTime() - new Date(b.dueAt).getTime(),
    )

    submissions.value = submissionData
  } catch (error) {
    message.value = handleApiError(
      error,
      'Không thể tải danh sách bài tập.',
    ).message
  } finally {
    loading.value = false
  }
}

const openAssignment = (assignment: Assignment) => {
  selectedAssignment.value = assignment
  message.value = ''
  successMessage.value = ''
}

const backToAssignments = () => {
  selectedAssignment.value = null
  closeSubmissionModal()
}

const resetSubmissionForm = () => {
  content.value = ''
  file.value = null
  fileInputKey.value += 1
}

const openCreateSubmission = () => {
  if (!selectedAssignment.value) return

  resetSubmissionForm()
  submissionMode.value = 'edit'
  submissionModalOpen.value = true
}

const openEditSubmission = () => {
  if (!currentSubmission.value) return

  content.value = currentSubmission.value.content ?? ''
  file.value = null
  fileInputKey.value += 1

  submissionMode.value = 'edit'
  submissionModalOpen.value = true
}

const openViewSubmission = () => {
  if (!currentSubmission.value) return

  submissionMode.value = 'view'
  submissionModalOpen.value = true
}

const closeSubmissionModal = () => {
  if (submitting.value) return

  submissionModalOpen.value = false
  resetSubmissionForm()
}

const onFileChange = (event: Event) => {
  const input = event.target as HTMLInputElement
  file.value = input.files?.[0] ?? null
}

const submit = async () => {
  if (!selectedAssignment.value) return

  if (!content.value.trim() && !file.value && !currentSubmission.value?.attachmentUrl) {
    message.value = 'Vui lòng nhập nội dung bài làm hoặc chọn tệp đính kèm.'
    return
  }

  const isUpdating = currentSubmission.value !== null

  submitting.value = true
  message.value = ''
  successMessage.value = ''

  try {
    const result = await submitAssignment(
      props.courseId,
      selectedAssignment.value.id,
      content.value.trim(),
      file.value,
    )

    const index = submissions.value.findIndex(
      (item) => item.assignmentId === result.assignmentId,
    )

    if (index >= 0) {
      submissions.value[index] = result
    } else {
      submissions.value.push(result)
    }

    submissionModalOpen.value = false
    resetSubmissionForm()
    successMessage.value = isUpdating ? 'Đã cập nhật bài nộp.' : 'Đã nộp bài thành công.'
  } catch (error) {
    message.value = handleApiError(error, 'Không thể nộp bài.').message
  } finally {
    submitting.value = false
  }
}

const openDeleteSubmission = () => {
  if (!currentSubmission.value) return

  submissionModalOpen.value = false
  deleteOpen.value = true
}

const handleDeleteSubmission = async () => {
  if (!selectedAssignment.value || !currentSubmission.value) return

  deleting.value = true
  message.value = ''
  successMessage.value = ''

  try {
    await deleteMySubmission(props.courseId, selectedAssignment.value.id)

    submissions.value = submissions.value.filter(
      (item) => item.assignmentId !== selectedAssignment.value?.id,
    )

    deleteOpen.value = false
    resetSubmissionForm()
    successMessage.value = 'Đã xóa bài nộp.'
  } catch (error) {
    message.value = handleApiError(
      error,
      'Không thể xóa bài đã nộp.',
    ).message
  } finally {
    deleting.value = false
  }
}

const resetForCourse = () => {
  selectedAssignment.value = null
  submissions.value = []
  assignments.value = []
  submissionModalOpen.value = false
  message.value = ''
  successMessage.value = ''
  void load()
}

watch(() => props.courseId, resetForCourse)

onMounted(() => void load())
</script>

<template>
  <section class="space-y-5">
    <BaseAlert v-if="message">
      {{ message }}
    </BaseAlert>

    <BaseAlert v-if="successMessage" variant="ai">
      <template #icon>
        <CheckCircle2 :size="18" />
      </template>
      {{ successMessage }}
    </BaseAlert>

    <!-- =====================================================
         ASSIGNMENT LIST
         ===================================================== -->
    <template v-if="!selectedAssignment">
      <header>
        <h2 class="font-heading text-xl font-bold text-app-text">
          Bài tập
        </h2>

        <p class="mt-1 text-sm text-app-text-muted">
          Theo dõi bài tập, hạn nộp và kết quả của bạn.
        </p>
      </header>

      <div v-if="loading" class="space-y-3">
        <div
          v-for="index in 4"
          :key="index"
          class="h-32 animate-pulse rounded-card bg-app-surface-muted"
        />
      </div>

      <div
        v-else-if="assignments.length === 0"
        class="rounded-panel border border-dashed border-app-border bg-app-surface px-6 py-14 text-center"
      >
        <ClipboardList :size="40" class="mx-auto text-app-text-muted/40" />

        <h3 class="mt-4 font-heading text-lg font-bold text-app-text">
          Chưa có bài tập
        </h3>

        <p class="mt-2 text-sm text-app-text-muted">
          Giảng viên chưa giao bài tập cho khóa học.
        </p>
      </div>

      <div v-else class="space-y-3">
        <button
          v-for="assignment in assignments"
          :key="assignment.id"
          type="button"
          class="group w-full rounded-card border border-app-border bg-app-surface p-5 text-left shadow-card transition hover:border-secondary/40 hover:shadow-md"
          @click="openAssignment(assignment)"
        >
          <div class="flex items-start justify-between gap-5">
            <div class="min-w-0 flex-1">
              <div class="flex flex-wrap items-center gap-2">
                <h3 class="font-heading text-lg font-bold text-app-text">
                  {{ assignment.title }}
                </h3>

                <span
                  v-if="assignmentStatus(assignment) === 'GRADED'"
                  class="rounded-pill bg-ai-soft px-2.5 py-1 text-xs font-semibold text-ai"
                >
                  Đã chấm
                </span>

                <span
                  v-else-if="assignmentStatus(assignment) === 'SUBMITTED'"
                  class="rounded-pill bg-secondary-soft px-2.5 py-1 text-xs font-semibold text-secondary"
                >
                  Đã nộp
                </span>

                <span
                  v-else
                  class="rounded-pill px-2.5 py-1 text-xs font-semibold"
                  :class="
                    assignment.expired
                      ? 'bg-danger-soft text-danger'
                      : 'bg-app-surface-muted text-app-text-muted'
                  "
                >
                  {{ assignment.expired ? 'Quá hạn' : 'Chưa nộp' }}
                </span>
              </div>

              <div class="mt-2 flex flex-wrap gap-x-5 gap-y-2 text-sm text-app-text-muted">
                <span class="inline-flex items-center gap-1.5">
                  <CalendarClock :size="15" />
                  Hạn {{ formatDateTime(assignment.dueAt) }}
                </span>

                <span>{{ assignment.maxScore }} điểm</span>
              </div>

              <p
                v-if="assignment.description"
                class="mt-3 line-clamp-2 max-w-4xl text-sm leading-6 text-app-text-muted"
              >
                {{ assignment.description }}
              </p>
            </div>

            <span class="shrink-0 pt-1 text-sm font-semibold text-secondary transition group-hover:translate-x-1">
              Xem chi tiết →
            </span>
          </div>
        </button>
      </div>
    </template>

    <!-- =====================================================
         ASSIGNMENT DETAIL
         ===================================================== -->
    <template v-else>
      <button
        type="button"
        class="inline-flex items-center gap-2 text-sm font-semibold text-app-text-muted transition hover:text-secondary"
        @click="backToAssignments"
      >
        <ArrowLeft :size="16" />
        Danh sách bài tập
      </button>

      <header class="rounded-card border border-app-border bg-app-surface p-6 shadow-card">
        <div class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div class="min-w-0">
            <div class="flex flex-wrap items-center gap-2">
              <h2 class="font-heading text-2xl font-bold text-app-text">
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
                {{ selectedAssignment.expired ? 'Đã quá hạn' : 'Đang mở' }}
              </span>
            </div>

            <div class="mt-2 flex flex-wrap gap-x-5 gap-y-2 text-sm text-app-text-muted">
              <span class="inline-flex items-center gap-1.5">
                <CalendarClock :size="15" />
                Hạn {{ formatDateTime(selectedAssignment.dueAt) }}
              </span>

              <span>{{ selectedAssignment.maxScore }} điểm</span>
            </div>
          </div>
        </div>

        <div class="mt-6 border-t border-app-border pt-5">
          <h3 class="font-heading text-lg font-bold text-app-text">
            Nội dung bài tập
          </h3>

          <p class="mt-3 whitespace-pre-wrap text-sm leading-7 text-app-text-muted">
            {{ selectedAssignment.description || 'Bài tập không có mô tả.' }}
          </p>
        </div>
      </header>

      <!-- ===================================================
           NO SUBMISSION
           =================================================== -->
      <section
        v-if="!currentSubmission"
        class="rounded-card border border-app-border bg-app-surface p-6 shadow-card"
      >
        <div class="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <div class="flex items-center gap-2">
              <FileText :size="19" class="text-app-text-muted" />

              <h3 class="font-heading text-lg font-bold text-app-text">
                Bài làm của bạn
              </h3>
            </div>

            <p class="mt-2 text-sm text-app-text-muted">
              Bạn chưa nộp bài tập này.
            </p>

            <p
              v-if="selectedAssignment.expired"
              class="mt-2 text-sm font-medium text-danger"
            >
              Bài tập đã quá hạn. Bài nộp mới sẽ được ghi nhận là nộp trễ.
            </p>
          </div>

          <BaseButton @click="openCreateSubmission">
            <template #leading>
              <Upload :size="16" />
            </template>

            {{ selectedAssignment.expired ? 'Nộp bài trễ' : 'Làm bài' }}
          </BaseButton>
        </div>
      </section>

      <!-- ===================================================
           SUBMITTED
           =================================================== -->
      <section
        v-else-if="currentSubmission.status === 'SUBMITTED'"
        class="rounded-card border border-app-border bg-app-surface p-6 shadow-card"
      >
        <div class="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <ClipboardCheck :size="19" class="text-secondary" />

              <h3 class="font-heading text-lg font-bold text-app-text">
                Bài đã nộp
              </h3>

              <span class="rounded-pill bg-secondary-soft px-2.5 py-1 text-xs font-semibold text-secondary">
                Chờ chấm
              </span>

              <span
                v-if="currentSubmission.late"
                class="rounded-pill bg-danger-soft px-2.5 py-1 text-xs font-semibold text-danger"
              >
                Nộp trễ
              </span>
            </div>

            <p class="mt-2 text-sm text-app-text-muted">
              Nộp lúc {{ formatDateTime(currentSubmission.submittedAt) }}
            </p>

            <p class="mt-1 text-sm text-app-text-muted">
              Bạn có thể chỉnh sửa bài làm trước khi giảng viên chấm.
            </p>
          </div>

          <div class="flex flex-wrap gap-2">
            <BaseButton variant="secondary" @click="openViewSubmission">
              <template #leading>
                <Eye :size="16" />
              </template>

              Xem bài đã nộp
            </BaseButton>

            <BaseButton @click="openEditSubmission">
              Chỉnh sửa bài nộp
            </BaseButton>
          </div>
        </div>
      </section>

      <!-- ===================================================
           GRADED
           =================================================== -->
      <section
        v-else
        class="overflow-hidden rounded-card border border-ai/20 bg-app-surface shadow-card"
      >
        <div class="border-b border-ai/20 bg-ai-soft px-6 py-5">
          <div class="flex items-center gap-2 text-ai">
            <CheckCircle2 :size="20" />

            <h3 class="font-heading text-lg font-bold">
              Bài tập đã được chấm
            </h3>
          </div>
        </div>

        <div class="p-6">
          <div class="grid gap-5 sm:grid-cols-[12rem_minmax(0,1fr)]">
            <div>
              <p class="text-xs font-semibold uppercase tracking-wide text-app-text-muted">
                Điểm
              </p>

              <p class="mt-2 font-heading text-3xl font-bold text-app-text">
                {{ currentSubmission.score }}
                <span class="text-lg font-semibold text-app-text-muted">
                  / {{ selectedAssignment.maxScore }}
                </span>
              </p>
            </div>

            <div>
              <p class="text-xs font-semibold uppercase tracking-wide text-app-text-muted">
                Nhận xét của giảng viên
              </p>

              <p
                v-if="currentSubmission.feedback"
                class="mt-2 whitespace-pre-wrap text-sm leading-7 text-app-text"
              >
                {{ currentSubmission.feedback }}
              </p>

              <p v-else class="mt-2 text-sm text-app-text-muted">
                Giảng viên không để lại nhận xét.
              </p>
            </div>
          </div>

          <div class="mt-6 flex justify-end border-t border-app-border pt-5">
            <BaseButton variant="secondary" @click="openViewSubmission">
              <template #leading>
                <Eye :size="16" />
              </template>

              Xem bài đã nộp
            </BaseButton>
          </div>
        </div>
      </section>
    </template>

    <!-- =====================================================
         SUBMISSION MODAL
         ===================================================== -->
    <BaseModal
      :open="submissionModalOpen"
      :title="submissionMode === 'edit'
        ? currentSubmission
          ? 'Chỉnh sửa bài nộp'
          : 'Nộp bài tập'
        : 'Bài đã nộp'"
      :description="selectedAssignment?.title"
      :loading="submitting"
      max-width="max-w-3xl"
      @close="closeSubmissionModal"
    >
      <!-- View submission -->
      <div
        v-if="submissionMode === 'view' && currentSubmission"
        class="space-y-6"
      >
        <div class="flex flex-wrap items-center gap-2">
          <span
            class="rounded-pill px-2.5 py-1 text-xs font-semibold"
            :class="
              currentSubmission.status === 'GRADED'
                ? 'bg-ai-soft text-ai'
                : 'bg-secondary-soft text-secondary'
            "
          >
            {{ currentSubmission.status === 'GRADED' ? 'Đã chấm' : 'Đã nộp' }}
          </span>

          <span
            v-if="currentSubmission.late"
            class="rounded-pill bg-danger-soft px-2.5 py-1 text-xs font-semibold text-danger"
          >
            Nộp trễ
          </span>

          <span class="text-sm text-app-text-muted">
            {{ formatDateTime(currentSubmission.submittedAt) }}
          </span>
        </div>

        <section>
          <p class="text-xs font-semibold uppercase tracking-wide text-app-text-muted">
            Nội dung bài làm
          </p>

          <div class="mt-3 rounded-card bg-app-surface-muted/60 p-4">
            <p class="whitespace-pre-wrap text-sm leading-7 text-app-text">
              {{ currentSubmission.content || 'Không có nội dung văn bản.' }}
            </p>
          </div>
        </section>

        <section v-if="currentSubmission.attachmentUrl">
          <p class="text-xs font-semibold uppercase tracking-wide text-app-text-muted">
            Tệp đính kèm
          </p>

          <a
            :href="currentSubmission.attachmentUrl"
            target="_blank"
            rel="noopener noreferrer"
            class="mt-3 inline-flex items-center gap-2 rounded-control border border-app-border px-4 py-3 text-sm font-semibold text-secondary transition hover:bg-app-surface-muted"
          >
            <Paperclip :size="16" />
            {{ currentSubmission.originalFileName || 'Xem tệp đã nộp' }}
          </a>
        </section>

        <div
          v-if="currentSubmission.status !== 'GRADED'"
          class="flex flex-wrap justify-end gap-3 border-t border-app-border pt-5"
        >
          <button
            type="button"
            class="inline-flex h-11 items-center gap-2 rounded-control bg-danger-soft px-4 text-sm font-semibold text-danger transition hover:opacity-80"
            @click="openDeleteSubmission"
          >
            <Trash2 :size="16" />
            Xóa bài nộp
          </button>

          <BaseButton @click="openEditSubmission">
            Chỉnh sửa bài nộp
          </BaseButton>
        </div>
      </div>

      <!-- Edit / create submission -->
      <form v-else class="space-y-5" @submit.prevent="submit">
        <label class="block">
          <span class="mb-2 block text-sm font-semibold text-app-text">
            Nội dung bài làm
          </span>

          <textarea
            v-model="content"
            rows="9"
            maxlength="20000"
            placeholder="Nhập nội dung bài làm..."
            class="w-full resize-y rounded-control border border-app-border bg-app-surface px-3 py-3 text-sm leading-6 outline-none focus:border-secondary focus:ring-2 focus:ring-secondary/20"
          />
        </label>

        <label class="block">
          <span class="mb-2 block text-sm font-semibold text-app-text">
            Tệp đính kèm
          </span>

          <div class="rounded-card border border-dashed border-app-border bg-app-surface-muted/40 p-4">
            <input
              :key="fileInputKey"
              type="file"
              class="block w-full text-sm text-app-text-muted"
              @change="onFileChange"
            />

            <p v-if="file" class="mt-2 text-sm font-medium text-app-text">
              Đã chọn: {{ file.name }}
            </p>
          </div>
        </label>

        <div
          v-if="currentSubmission?.attachmentUrl"
          class="rounded-card border border-app-border bg-app-surface-muted/40 p-4"
        >
          <p class="text-xs font-semibold uppercase tracking-wide text-app-text-muted">
            Tệp hiện tại
          </p>

          <a
            :href="currentSubmission.attachmentUrl"
            target="_blank"
            rel="noopener noreferrer"
            class="mt-2 inline-flex items-center gap-2 text-sm font-semibold text-secondary hover:underline"
          >
            <Paperclip :size="15" />
            {{ currentSubmission.originalFileName || 'Xem tệp đã nộp' }}
          </a>

          <p class="mt-2 text-xs text-app-text-muted">
            Nếu không chọn tệp mới, tệp hiện tại sẽ được giữ nguyên.
          </p>
        </div>

        <p
          v-if="selectedAssignment?.expired"
          class="rounded-control bg-danger-soft px-4 py-3 text-sm font-medium text-danger"
        >
          Bài tập đã quá hạn. Bài nộp sẽ được ghi nhận là nộp trễ.
        </p>

        <div class="flex justify-end gap-3 border-t border-app-border pt-5">
          <BaseButton
            type="button"
            variant="secondary"
            :disabled="submitting"
            @click="closeSubmissionModal"
          >
            Hủy
          </BaseButton>

          <BaseButton type="submit" :loading="submitting">
            <template #leading>
              <Upload :size="16" />
            </template>

            {{ currentSubmission ? 'Lưu thay đổi' : 'Nộp bài' }}
          </BaseButton>
        </div>
      </form>
    </BaseModal>

    <!-- =====================================================
         DELETE SUBMISSION
         ===================================================== -->
    <ConfirmDialog
      :open="deleteOpen"
      title="Xóa bài đã nộp?"
      description="Bài làm hiện tại và tệp đính kèm sẽ bị xóa. Bạn có thể nộp lại nếu bài chưa được chấm."
      confirm-text="Xóa bài nộp"
      :loading="deleting"
      @close="!deleting && (deleteOpen = false)"
      @confirm="handleDeleteSubmission"
    />
  </section>
</template>