<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  ArrowLeft,
  CalendarClock,
  CheckCircle2,
  ChevronRight,
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
import type {
  Assignment,
  AssignmentSubmission,
} from '@/shared/assignment/types'
import type { TopicContent } from '@/shared/course-content'
import {
  createEmptyRichText,
  hasRichTextContent,
  parseStoredRichText,
  RichTextEditor,
  RichTextViewer,
  serializeRichText,
  storedRichTextToPlainText,
} from '@/shared/rich-text'
import { formatDateTime } from '@/shared/utils/date'

import {
  deleteMySubmission,
  getAssignments,
  getMySubmissions,
  submitAssignment,
} from '../api/assignmentApi'
import { useStudentApiError } from '../composables/useStudentApiError'

const props = defineProps<{ courseId: string }>()

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

const submissionContent = ref<TopicContent>(createEmptyRichText())
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

  return (
    submissionByAssignment.value.get(
      selectedAssignment.value.id,
    ) ?? null
  )
})

const selectedDescription = computed(() =>
  parseStoredRichText(
    selectedAssignment.value?.description,
  ),
)

const hasSelectedDescription = computed(() =>
  hasRichTextContent(selectedDescription.value),
)

const currentSubmissionContent = computed(() =>
  parseStoredRichText(
    currentSubmission.value?.content,
  ),
)

const hasCurrentSubmissionContent = computed(() =>
  hasRichTextContent(currentSubmissionContent.value),
)

const preview = (value: string | null) =>
  storedRichTextToPlainText(value)

const assignmentStatus = (
  assignment: Assignment,
) => {
  const submission =
    submissionByAssignment.value.get(
      assignment.id,
    )

  if (submission?.status === 'GRADED') {
    return 'GRADED'
  }

  if (submission) {
    return 'SUBMITTED'
  }

  return 'NOT_SUBMITTED'
}

const load = async () => {
  loading.value = true
  message.value = ''

  try {
    const [
      assignmentData,
      submissionData,
    ] = await Promise.all([
      getAssignments(props.courseId),
      getMySubmissions(props.courseId),
    ])

    assignments.value = assignmentData.sort(
      (a, b) =>
        new Date(a.dueAt).getTime() -
        new Date(b.dueAt).getTime(),
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

const openAssignment = (
  assignment: Assignment,
) => {
  selectedAssignment.value = assignment
  message.value = ''
  successMessage.value = ''
}

const resetSubmissionForm = () => {
  submissionContent.value =
    createEmptyRichText()

  file.value = null
  fileInputKey.value += 1
}

const closeSubmissionModal = () => {
  if (submitting.value) return

  submissionModalOpen.value = false
  resetSubmissionForm()
}

const backToAssignments = () => {
  selectedAssignment.value = null
  closeSubmissionModal()
}

const openCreateSubmission = () => {
  if (!selectedAssignment.value) return

  resetSubmissionForm()
  submissionMode.value = 'edit'
  submissionModalOpen.value = true
}

const openEditSubmission = () => {
  if (!currentSubmission.value) return

  submissionContent.value =
    parseStoredRichText(
      currentSubmission.value.content,
    )

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

const onFileChange = (
  event: Event,
) => {
  const input =
    event.target as HTMLInputElement

  file.value =
    input.files?.[0] ?? null
}

const submit = async () => {
  if (!selectedAssignment.value) {
    return
  }

  const serializedContent =
    serializeRichText(
      submissionContent.value,
    )

  if (
    !serializedContent &&
    !file.value &&
    !currentSubmission.value
      ?.attachmentUrl
  ) {
    message.value =
      'Vui lòng nhập nội dung bài làm hoặc chọn tệp đính kèm.'

    return
  }

  const isUpdating =
    currentSubmission.value !== null

  submitting.value = true
  message.value = ''
  successMessage.value = ''

  try {
    const result =
      await submitAssignment(
        props.courseId,
        selectedAssignment.value.id,
        serializedContent,
        file.value,
      )

    const index =
      submissions.value.findIndex(
        (item) =>
          item.assignmentId ===
          result.assignmentId,
      )

    if (index >= 0) {
      submissions.value[index] = result
    } else {
      submissions.value.push(result)
    }

    submissionModalOpen.value = false
    resetSubmissionForm()

    successMessage.value =
      isUpdating
        ? 'Đã cập nhật bài nộp.'
        : 'Đã nộp bài thành công.'
  } catch (error) {
    message.value = handleApiError(
      error,
      'Không thể nộp bài.',
    ).message
  } finally {
    submitting.value = false
  }
}

const openDeleteSubmission = () => {
  if (!currentSubmission.value) return

  submissionModalOpen.value = false
  deleteOpen.value = true
}

const handleDeleteSubmission =
  async () => {
    if (
      !selectedAssignment.value ||
      !currentSubmission.value
    ) {
      return
    }

    deleting.value = true
    message.value = ''
    successMessage.value = ''

    try {
      await deleteMySubmission(
        props.courseId,
        selectedAssignment.value.id,
      )

      submissions.value =
        submissions.value.filter(
          (item) =>
            item.assignmentId !==
            selectedAssignment.value?.id,
        )

      deleteOpen.value = false
      resetSubmissionForm()

      successMessage.value =
        'Đã xóa bài nộp.'
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

watch(
  () => props.courseId,
  resetForCourse,
)

onMounted(() => void load())
</script>

<template>
  <section class="space-y-6">
    <BaseAlert v-if="message">
      {{ message }}
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

    <template v-if="!selectedAssignment">
      <header>
        <p
          class="text-xs font-bold uppercase tracking-[0.14em] text-secondary"
        >
          Course work
        </p>

        <h2
          class="mt-1 font-heading text-2xl font-bold text-app-text"
        >
          Bài tập
        </h2>

        <p
          class="mt-1 text-sm text-app-text-muted"
        >
          Theo dõi yêu cầu, hạn nộp và
          kết quả bài tập.
        </p>
      </header>

      <div
        v-if="loading"
        class="space-y-3"
      >
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
        <div
          class="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-secondary-soft text-secondary"
        >
          <ClipboardList :size="25" />
        </div>

        <h3
          class="mt-4 font-heading text-lg font-bold text-app-text"
        >
          Chưa có bài tập
        </h3>

        <p
          class="mt-1 text-sm text-app-text-muted"
        >
          Giảng viên chưa giao bài tập
          cho khóa học.
        </p>
      </div>

      <div
        v-else
        class="space-y-3"
      >
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
              assignmentStatus(assignment) ===
              'GRADED'
                ? 'bg-ai-soft text-ai'
                : 'bg-secondary-soft text-secondary'
            "
          >
            <ClipboardList :size="19" />
          </div>

          <div class="min-w-0">
            <div
              class="flex flex-wrap items-center gap-2"
            >
              <h3
                class="font-heading font-bold text-app-text"
              >
                {{ assignment.title }}
              </h3>

              <span
                class="rounded-pill px-2.5 py-1 text-xs font-semibold"
                :class="{
                  'bg-ai-soft text-ai':
                    assignmentStatus(
                      assignment,
                    ) === 'GRADED',

                  'bg-secondary-soft text-secondary':
                    assignmentStatus(
                      assignment,
                    ) === 'SUBMITTED',

                  'bg-danger-soft text-danger':
                    assignmentStatus(
                      assignment,
                    ) ===
                      'NOT_SUBMITTED' &&
                    assignment.expired,

                  'bg-app-surface-muted text-app-text-muted':
                    assignmentStatus(
                      assignment,
                    ) ===
                      'NOT_SUBMITTED' &&
                    !assignment.expired,
                }"
              >
                {{
                  assignmentStatus(
                    assignment,
                  ) === 'GRADED'
                    ? 'Đã chấm'
                    : assignmentStatus(
                          assignment,
                        ) ===
                        'SUBMITTED'
                      ? 'Đã nộp'
                      : assignment.expired
                        ? 'Quá hạn'
                        : 'Chưa nộp'
                }}
              </span>
            </div>

            <p
              v-if="
                preview(
                  assignment.description,
                )
              "
              class="mt-2 line-clamp-2 max-w-4xl text-sm leading-6 text-app-text-muted"
            >
              {{
                preview(
                  assignment.description,
                )
              }}
            </p>

            <div
              class="mt-3 flex flex-wrap gap-x-5 gap-y-1 text-xs text-app-text-muted"
            >
              <span
                class="inline-flex items-center gap-1.5"
              >
                <CalendarClock :size="14" />

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

      <section
        class="overflow-hidden rounded-[1.3rem] border border-app-border bg-app-surface shadow-card"
      >
        <header
          class="border-b border-app-border bg-gradient-to-br from-white to-secondary-soft/30 px-6 py-6 sm:px-7"
        >
          <div
            class="flex flex-wrap items-center gap-2"
          >
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
                  ? 'Đã quá hạn'
                  : 'Đang mở'
              }}
            </span>

            <span
              v-if="
                assignmentStatus(
                  selectedAssignment,
                ) === 'GRADED'
              "
              class="rounded-pill bg-ai-soft px-2.5 py-1 text-xs font-semibold text-ai"
            >
              Đã chấm điểm
            </span>
          </div>

          <h2
            class="mt-3 font-heading text-2xl font-bold leading-tight text-app-text sm:text-3xl"
          >
            {{ selectedAssignment.title }}
          </h2>

          <div
            class="mt-3 flex flex-wrap gap-x-5 gap-y-2 text-sm text-app-text-muted"
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
              {{
                selectedAssignment.maxScore
              }}
              điểm
            </span>
          </div>
        </header>

        <div
          class="grid lg:grid-cols-[minmax(0,1fr)_19rem]"
        >
          <article class="p-6 sm:p-7">
            <h3
              class="font-heading text-lg font-bold text-app-text"
            >
              Nội dung bài tập
            </h3>

            <div class="mt-5">
              <RichTextViewer
                v-if="
                  hasSelectedDescription
                "
                :content="
                  selectedDescription
                "
              />

              <p
                v-else
                class="text-sm text-app-text-muted"
              >
                Bài tập không có nội dung
                hướng dẫn.
              </p>
            </div>
          </article>

          <aside
            class="border-t border-app-border bg-app-surface-muted/40 p-6 lg:border-l lg:border-t-0"
          >
            <template
              v-if="!currentSubmission"
            >
              <FileText
                :size="21"
                class="text-app-text-muted"
              />

              <h3
                class="mt-4 font-heading font-bold text-app-text"
              >
                Bài làm của bạn
              </h3>

              <p
                class="mt-2 text-sm leading-6 text-app-text-muted"
              >
                Bạn chưa nộp bài tập này.
              </p>

              <BaseButton
                class="mt-5"
                block
                @click="
                  openCreateSubmission
                "
              >
                <template #leading>
                  <Upload :size="16" />
                </template>

                {{
                  selectedAssignment.expired
                    ? 'Nộp bài trễ'
                    : 'Làm bài'
                }}
              </BaseButton>
            </template>

            <template v-else>
              <ClipboardCheck
                :size="21"
                :class="
                  currentSubmission.status ===
                  'GRADED'
                    ? 'text-ai'
                    : 'text-secondary'
                "
              />

              <h3
                class="mt-4 font-heading font-bold text-app-text"
              >
                {{
                  currentSubmission.status ===
                  'GRADED'
                    ? 'Đã chấm điểm'
                    : 'Bài đã nộp'
                }}
              </h3>

              <p
                class="mt-2 text-sm text-app-text-muted"
              >
                Nộp
                {{
                  formatDateTime(
                    currentSubmission.submittedAt,
                  )
                }}
              </p>

              <p
                v-if="
                  currentSubmission.status ===
                  'GRADED'
                "
                class="mt-4 font-heading text-2xl font-bold text-app-text"
              >
                {{
                  currentSubmission.score
                }}
                /
                {{
                  selectedAssignment.maxScore
                }}
              </p>

              <BaseButton
                class="mt-5"
                block
                variant="secondary"
                @click="
                  openViewSubmission
                "
              >
                <template #leading>
                  <Eye :size="16" />
                </template>

                Xem bài đã nộp
              </BaseButton>
            </template>
          </aside>
        </div>
      </section>
    </template>

    <BaseModal
      :open="submissionModalOpen"
      :title="
        submissionMode === 'view'
          ? 'Bài đã nộp'
          : currentSubmission
            ? 'Chỉnh sửa bài nộp'
            : 'Nộp bài tập'
      "
      :description="
        submissionMode === 'view'
          ? 'Nội dung bài làm và tệp đính kèm của bạn.'
          : 'Soạn nội dung bài làm và đính kèm tệp nếu cần.'
      "
      :loading="submitting"
      max-width="max-w-3xl"
      @close="closeSubmissionModal"
    >
      <div
        v-if="
          currentSubmission &&
          submissionMode === 'view'
        "
        class="space-y-6"
      >
        <div
          class="flex flex-wrap items-center gap-2"
        >
          <span
            class="rounded-pill px-2.5 py-1 text-xs font-semibold"
            :class="
              currentSubmission.status ===
              'GRADED'
                ? 'bg-ai-soft text-ai'
                : 'bg-secondary-soft text-secondary'
            "
          >
            {{
              currentSubmission.status ===
              'GRADED'
                ? 'Đã chấm'
                : 'Chờ chấm'
            }}
          </span>

          <span
            v-if="currentSubmission.late"
            class="rounded-pill bg-danger-soft px-2.5 py-1 text-xs font-semibold text-danger"
          >
            Nộp trễ
          </span>

          <span
            class="text-xs text-app-text-muted"
          >
            {{
              formatDateTime(
                currentSubmission.submittedAt,
              )
            }}
          </span>
        </div>

        <section>
          <p
            class="text-xs font-bold uppercase tracking-[0.12em] text-app-text-muted"
          >
            Nội dung bài làm
          </p>

          <div
            class="mt-3 rounded-[1rem] border border-app-border bg-app-surface p-5"
          >
            <RichTextViewer
              v-if="
                hasCurrentSubmissionContent
              "
              :content="
                currentSubmissionContent
              "
            />

            <p
              v-else
              class="text-sm text-app-text-muted"
            >
              Không có nội dung văn bản.
            </p>
          </div>
        </section>

        <a
          v-if="
            currentSubmission.attachmentUrl
          "
          :href="
            currentSubmission.attachmentUrl
          "
          target="_blank"
          rel="noopener noreferrer"
          class="flex items-center gap-3 rounded-[1rem] border border-app-border bg-app-surface-muted/40 p-4 transition hover:border-secondary/30 hover:bg-secondary-soft/30"
        >
          <div
            class="flex h-10 w-10 items-center justify-center rounded-xl bg-secondary-soft text-secondary"
          >
            <Paperclip :size="17" />
          </div>

          <div class="min-w-0 flex-1">
            <p
              class="truncate text-sm font-semibold text-app-text"
            >
              {{
                currentSubmission.originalFileName ||
                'Tệp đính kèm'
              }}
            </p>

            <p
              class="mt-0.5 text-xs text-app-text-muted"
            >
              Mở tệp đính kèm
            </p>
          </div>
        </a>

        <section
          v-if="
            currentSubmission.status ===
            'GRADED'
          "
          class="rounded-[1rem] border border-ai/20 bg-ai-soft p-5"
        >
          <div
            class="flex items-center gap-2 text-ai"
          >
            <CheckCircle2 :size="18" />

            <span
              class="text-sm font-semibold"
            >
              Kết quả
            </span>
          </div>

          <p
            class="mt-3 font-heading text-2xl font-bold text-app-text"
          >
            {{
              currentSubmission.score
            }}
            /
            {{
              selectedAssignment?.maxScore
            }}
          </p>

          <div
            v-if="
              currentSubmission.feedback
            "
            class="mt-4 border-t border-ai/15 pt-4"
          >
            <p
              class="text-xs font-semibold text-ai"
            >
              Nhận xét của giảng viên
            </p>

            <p
              class="mt-2 whitespace-pre-wrap text-sm leading-6 text-app-text-muted"
            >
              {{
                currentSubmission.feedback
              }}
            </p>
          </div>
        </section>

        <div
          class="flex justify-end gap-3 border-t border-app-border pt-5"
        >
          <template
            v-if="
              currentSubmission.status !==
              'GRADED'
            "
          >
            <button
              type="button"
              class="inline-flex h-11 items-center gap-2 rounded-control px-4 text-sm font-semibold text-danger transition hover:bg-danger-soft"
              @click="
                openDeleteSubmission
              "
            >
              <Trash2 :size="16" />
              Xóa bài
            </button>

            <BaseButton
              variant="secondary"
              @click="
                openEditSubmission
              "
            >
              Chỉnh sửa
            </BaseButton>
          </template>

          <BaseButton
            @click="
              closeSubmissionModal
            "
          >
            Đóng
          </BaseButton>
        </div>
      </div>

      <form
        v-else
        class="space-y-6"
        @submit.prevent="submit"
      >
        <div>
          <div
            class="mb-2 flex items-center justify-between gap-3"
          >
            <label
              class="text-sm font-semibold text-app-text"
            >
              Nội dung bài làm
            </label>

            <span
              class="text-xs text-app-text-muted"
            >
              Hỗ trợ định dạng văn bản
            </span>
          </div>

          <RichTextEditor
            v-model="submissionContent"
            :disabled="submitting"
          />
        </div>

        <div>
          <label
            class="text-sm font-semibold text-app-text"
          >
            Tệp đính kèm
          </label>

          <div
            class="mt-2 rounded-[1rem] border border-dashed border-app-border bg-app-surface-muted/30 p-4"
          >
            <input
              :key="fileInputKey"
              type="file"
              :disabled="submitting"
              class="block w-full text-sm text-app-text file:mr-4 file:rounded-control file:border-0 file:bg-secondary-soft file:px-4 file:py-2 file:text-sm file:font-semibold file:text-secondary"
              @change="onFileChange"
            />

            <p
              v-if="
                currentSubmission
                  ?.originalFileName
              "
              class="mt-3 text-xs text-app-text-muted"
            >
              Tệp hiện tại:
              <span
                class="font-medium text-app-text"
              >
                {{
                  currentSubmission.originalFileName
                }}
              </span>
            </p>

            <p
              class="mt-2 text-xs text-app-text-muted"
            >
              Có thể nộp nội dung,
              tệp đính kèm hoặc cả hai.
            </p>
          </div>
        </div>

        <div
          class="flex justify-end gap-3 border-t border-app-border pt-5"
        >
          <BaseButton
            type="button"
            variant="secondary"
            :disabled="submitting"
            @click="
              closeSubmissionModal
            "
          >
            Hủy
          </BaseButton>

          <BaseButton
            type="submit"
            :loading="submitting"
          >
            <template #leading>
              <Upload :size="16" />
            </template>

            {{
              currentSubmission
                ? 'Cập nhật bài nộp'
                : 'Nộp bài'
            }}
          </BaseButton>
        </div>
      </form>
    </BaseModal>

    <ConfirmDialog
      :open="deleteOpen"
      title="Xóa bài đã nộp?"
      description="Bạn có chắc muốn xóa bài nộp hiện tại?"
      confirm-text="Xóa bài"
      :loading="deleting"
      @close="deleteOpen = false"
      @confirm="
        handleDeleteSubmission
      "
    />
  </section>
</template>