<script setup lang="ts">
import {
  computed,
  onMounted,
  reactive,
  ref,
} from 'vue'

import {
  CalendarClock,
  CheckCircle2,
  ClipboardCheck,
  ClipboardList,
  FileText,
  Pencil,
  Plus,
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
} from '../api/assignmentApi'

import {
  getUserLookup,
  type UserLookup,
} from '../api/memberApi'

import AssignmentFormModal
  from './AssignmentFormModal.vue'

import { useLecturerApiError }
  from '../composables/useLecturerApiError'

import { formatDateTime }
  from '@/shared/utils/date'

const props = defineProps<{
  courseId: string
}>()

const { handleApiError } =
  useLecturerApiError()

const assignments =
  ref<Assignment[]>([])

const selectedAssignmentId =
  ref('')

const submissions =
  ref<AssignmentSubmission[]>([])

const users =
  ref<Record<string, UserLookup>>({})

const loadingAssignments =
  ref(true)

const loadingSubmissions =
  ref(false)

const actionMessage = ref('')
const successMessage = ref('')

const formOpen = ref(false)
const editingAssignment =
  ref<Assignment | null>(null)

const saving = ref(false)
const formMessage = ref('')

const deleteOpen = ref(false)
const deleting = ref(false)

const gradingSubmissionId =
  ref('')

interface GradeForm {
  score: string
  feedback: string
}

const gradeForms =
  reactive<Record<string, GradeForm>>({})

const selectedAssignment =
  computed(() =>
    assignments.value.find(
      (item) =>
        item.id ===
        selectedAssignmentId.value,
    ) ?? null,
  )

const submittedCount =
  computed(
    () => submissions.value.length,
  )

const gradedCount =
  computed(
    () =>
      submissions.value.filter(
        (item) =>
          item.status === 'GRADED',
      ).length,
  )

const studentName = (
  studentId: string,
) => {
  const user =
    users.value[studentId]

  return (
    user?.fullName ||
    user?.email ||
    studentId
  )
}

const studentEmail = (
  studentId: string,
) =>
  users.value[studentId]?.email ?? ''

const loadAssignments =
  async () => {
    loadingAssignments.value = true
    actionMessage.value = ''

    try {
      assignments.value =
        await getAssignments(
          props.courseId,
        )

      if (
        assignments.value.length === 0
      ) {
        selectedAssignmentId.value = ''
        submissions.value = []
        return
      }

      const stillExists =
        assignments.value.some(
          (item) =>
            item.id ===
            selectedAssignmentId.value,
        )

      if (!stillExists) {
        selectedAssignmentId.value =
          assignments.value[0]!.id
      }

      await loadSubmissions()
    } catch (error) {
      actionMessage.value =
        handleApiError(
          error,
          'Không thể tải danh sách bài tập.',
        ).message
    } finally {
      loadingAssignments.value = false
    }
  }

const loadUserLookups =
  async (
    items: AssignmentSubmission[],
  ) => {
    const ids = [
      ...new Set(
        items.map(
          (item) => item.studentId,
        ),
      ),
    ].filter(
      (id) => !users.value[id],
    )

    await Promise.all(
      ids.map(async (id) => {
        try {
          const user =
            await getUserLookup(id)

          users.value = {
            ...users.value,
            [id]: user,
          }
        } catch {
          // Nếu lookup user lỗi thì vẫn
          // hiển thị studentId.
        }
      }),
    )
  }

const initGradeForms = (
  items: AssignmentSubmission[],
) => {
  for (const submission of items) {
    gradeForms[submission.id] = {
      score:
        submission.score != null
          ? String(submission.score)
          : '',

      feedback:
        submission.feedback ?? '',
    }
  }
}

const loadSubmissions =
  async () => {
    if (!selectedAssignment.value) {
      submissions.value = []
      return
    }

    loadingSubmissions.value = true
    actionMessage.value = ''

    try {
      const result =
        await getSubmissions(
          props.courseId,
          selectedAssignment.value.id,
        )
      initGradeForms(result)
      submissions.value = result


      await loadUserLookups(result)
    } catch (error) {
      actionMessage.value =
        handleApiError(
          error,
          'Không thể tải danh sách bài nộp.',
        ).message
    } finally {
      loadingSubmissions.value = false
    }
  }

const selectAssignment =
  async (assignment: Assignment) => {
    if (
      selectedAssignmentId.value ===
      assignment.id
    ) {
      return
    }

    selectedAssignmentId.value =
      assignment.id

    successMessage.value = ''

    await loadSubmissions()
  }

const openCreate = () => {
  editingAssignment.value = null
  formMessage.value = ''
  formOpen.value = true
}

const openEdit = () => {
  if (!selectedAssignment.value) return

  editingAssignment.value =
    selectedAssignment.value

  formMessage.value = ''
  formOpen.value = true
}

const closeForm = () => {
  if (saving.value) return

  formOpen.value = false
  editingAssignment.value = null
  formMessage.value = ''
}

const submitForm =
  async (
    input: AssignmentInput,
  ) => {
    saving.value = true
    formMessage.value = ''
    successMessage.value = ''

    try {
      if (editingAssignment.value) {
        const updated =
          await updateAssignment(
            props.courseId,
            editingAssignment.value.id,
            input,
          )

        const index =
          assignments.value.findIndex(
            (item) =>
              item.id === updated.id,
          )

        if (index >= 0) {
          assignments.value[index] =
            updated
        }

        successMessage.value =
          'Đã cập nhật bài tập.'
      } else {
        const created =
          await createAssignment(
            props.courseId,
            input,
          )

        assignments.value.push(
          created,
        )

        assignments.value.sort(
          (a, b) =>
            new Date(a.dueAt).getTime() -
            new Date(b.dueAt).getTime(),
        )

        selectedAssignmentId.value =
          created.id

        submissions.value = []

        successMessage.value =
          'Đã tạo bài tập mới.'
      }

      formOpen.value = false
      editingAssignment.value = null
    } catch (error) {
      formMessage.value =
        handleApiError(
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

const handleDelete =
  async () => {
    if (!selectedAssignment.value) return

    deleting.value = true
    actionMessage.value = ''

    const deletingId =
      selectedAssignment.value.id

    try {
      await deleteAssignment(
        props.courseId,
        deletingId,
      )

      assignments.value =
        assignments.value.filter(
          (item) =>
            item.id !== deletingId,
        )

      deleteOpen.value = false

      successMessage.value =
        'Đã xóa bài tập.'

      if (
        assignments.value.length > 0
      ) {
        selectedAssignmentId.value =
          assignments.value[0]!.id

        await loadSubmissions()
      } else {
        selectedAssignmentId.value = ''
        submissions.value = []
      }
    } catch (error) {
      actionMessage.value =
        handleApiError(
          error,
          'Không thể xóa bài tập.',
        ).message
    } finally {
      deleting.value = false
    }
  }

const handleGrade =
  async (
    submission: AssignmentSubmission,
  ) => {
    if (!selectedAssignment.value) return

    const form =
      gradeForms[submission.id]

    if (!form) return

    const score =
      Number(form.score)

    if (
      !Number.isFinite(score) ||
      score < 0 ||
      score >
        selectedAssignment.value.maxScore
    ) {
      actionMessage.value =
        `Điểm phải nằm trong khoảng 0 - ${selectedAssignment.value.maxScore}.`

      return
    }

    gradingSubmissionId.value =
      submission.id

    actionMessage.value = ''
    successMessage.value = ''

    try {
      const updated =
        await gradeSubmission(
          props.courseId,
          selectedAssignment.value.id,
          submission.id,
          {
            score,
            feedback:
              form.feedback.trim() ||
              undefined,
          },
        )

      const index =
        submissions.value.findIndex(
          (item) =>
            item.id === updated.id,
        )

      if (index >= 0) {
        submissions.value[index] =
          updated
      }

      gradeForms[updated.id] = {
        score:
          String(updated.score ?? ''),

        feedback:
          updated.feedback ?? '',
      }

      successMessage.value =
        `Đã chấm bài của ${studentName(updated.studentId)}.`
    } catch (error) {
      actionMessage.value =
        handleApiError(
          error,
          'Không thể chấm bài.',
        ).message
    } finally {
      gradingSubmissionId.value = ''
    }
  }

onMounted(() => {
  void loadAssignments()
})
</script>

<template>
  <section class="space-y-5">
    <BaseAlert
      v-if="actionMessage"
    >
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

    <!-- Header -->
    <header
      class="
        flex flex-col gap-4
        sm:flex-row sm:items-center
        sm:justify-between
      "
    >
      <div>
        <h2
          class="
            font-heading text-xl
            font-bold text-app-text
          "
        >
          Bài tập
        </h2>

        <p
          class="
            mt-1 text-sm
            text-app-text-muted
          "
        >
          Giao bài, theo dõi bài nộp và
          đánh giá kết quả học tập.
        </p>
      </div>

      <BaseButton
        @click="openCreate"
      >
        <template #leading>
          <Plus :size="17" />
        </template>

        Tạo bài tập
      </BaseButton>
    </header>

    <!-- Loading -->
    <div
      v-if="loadingAssignments"
      class="
        grid gap-5
        lg:grid-cols-[20rem_minmax(0,1fr)]
      "
    >
      <div
        class="
          h-[30rem] animate-pulse
          rounded-card
          bg-app-surface-muted
        "
      />

      <div
        class="
          h-[30rem] animate-pulse
          rounded-card
          bg-app-surface-muted
        "
      />
    </div>

    <!-- Empty -->
    <div
      v-else-if="
        assignments.length === 0
      "
      class="
        rounded-panel border
        border-dashed
        border-app-border
        bg-app-surface
        px-6 py-16 text-center
      "
    >
      <ClipboardList
        :size="42"
        class="
          mx-auto
          text-app-text-muted/40
        "
      />

      <h3
        class="
          mt-4 font-heading
          text-xl font-bold
          text-app-text
        "
      >
        Chưa có bài tập
      </h3>

      <p
        class="
          mt-2 text-sm
          text-app-text-muted
        "
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

    <!-- Content -->
    <div
      v-else
      class="
        grid gap-5
        xl:grid-cols-[20rem_minmax(0,1fr)]
      "
    >
      <!-- Assignment list -->
      <aside
        class="
          overflow-hidden rounded-card
          border border-app-border
          bg-app-surface
          shadow-card
        "
      >
        <header
          class="
            border-b border-app-border
            px-5 py-4
          "
        >
          <p
            class="
              font-semibold
              text-app-text
            "
          >
            Danh sách bài tập
          </p>

          <p
            class="
              mt-1 text-xs
              text-app-text-muted
            "
          >
            {{ assignments.length }}
            bài tập
          </p>
        </header>

        <div
          class="
            max-h-[42rem]
            overflow-y-auto
          "
        >
          <button
            v-for="
              assignment in assignments
            "
            :key="assignment.id"
            type="button"
            class="
              block w-full
              border-b
              border-app-border
              px-5 py-4
              text-left transition
              last:border-b-0
            "
            :class="
              selectedAssignmentId ===
              assignment.id
                ? 'bg-secondary-soft'
                : 'hover:bg-app-surface-muted'
            "
            @click="
              selectAssignment(
                assignment,
              )
            "
          >
            <div
              class="
                flex items-start
                gap-3
              "
            >
              <div
                class="
                  mt-0.5 flex h-9 w-9
                  shrink-0 items-center
                  justify-center
                  rounded-card
                  bg-secondary-soft
                  text-secondary
                "
              >
                <ClipboardCheck
                  :size="17"
                />
              </div>

              <div
                class="
                  min-w-0 flex-1
                "
              >
                <p
                  class="
                    line-clamp-2
                    text-sm font-semibold
                    text-app-text
                  "
                >
                  {{ assignment.title }}
                </p>

                <p
                  class="
                    mt-2 flex
                    items-center gap-1
                    text-xs
                    text-app-text-muted
                  "
                >
                  <CalendarClock
                    :size="13"
                  />

                  {{
                    formatDateTime(
                      assignment.dueAt,
                    )
                  }}
                </p>

                <div
                  class="
                    mt-2 flex
                    items-center gap-2
                  "
                >
                  <span
                    class="
                      rounded-pill
                      px-2 py-0.5
                      text-[11px]
                      font-semibold
                    "
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

                  <span
                    class="
                      text-[11px]
                      text-app-text-muted
                    "
                  >
                    {{
                      assignment.maxScore
                    }}
                    điểm
                  </span>
                </div>
              </div>
            </div>
          </button>
        </div>
      </aside>

      <!-- Assignment detail -->
      <main
        v-if="selectedAssignment"
        class="space-y-5"
      >
        <article
          class="
            rounded-card border
            border-app-border
            bg-app-surface
            p-5 shadow-card
          "
        >
          <div
            class="
              flex flex-col gap-4
              sm:flex-row
              sm:items-start
              sm:justify-between
            "
          >
            <div>
              <h3
                class="
                  font-heading
                  text-2xl font-bold
                  text-app-text
                "
              >
                {{
                  selectedAssignment.title
                }}
              </h3>

              <div
                class="
                  mt-3 flex
                  flex-wrap gap-x-5
                  gap-y-2
                  text-sm
                  text-app-text-muted
                "
              >
                <span
                  class="
                    inline-flex
                    items-center gap-1.5
                  "
                >
                  <CalendarClock
                    :size="15"
                  />

                  Hạn:
                  {{
                    formatDateTime(
                      selectedAssignment.dueAt,
                    )
                  }}
                </span>

                <span>
                  Điểm tối đa:
                  <strong
                    class="text-app-text"
                  >
                    {{
                      selectedAssignment.maxScore
                    }}
                  </strong>
                </span>
              </div>
            </div>

            <div
              class="
                flex shrink-0 gap-2
              "
            >
              <BaseButton
                variant="secondary"
                @click="openEdit"
              >
                <template #leading>
                  <Pencil :size="16" />
                </template>

                Sửa
              </BaseButton>

              <button
                type="button"
                class="
                  flex h-11
                  items-center
                  justify-center gap-2
                  rounded-control
                  bg-danger-soft px-4
                  text-sm font-semibold
                  text-danger
                  transition
                  hover:opacity-80
                "
                @click="confirmDelete"
              >
                <Trash2 :size="16" />

                Xóa
              </button>
            </div>
          </div>

          <p
            class="
              mt-5 whitespace-pre-wrap
              text-sm leading-7
              text-app-text-muted
            "
          >
            {{
              selectedAssignment.description ||
              'Bài tập không có mô tả.'
            }}
          </p>
        </article>

        <!-- Submission summary -->
        <section
          class="
            rounded-card border
            border-app-border
            bg-app-surface
            shadow-card
          "
        >
          <header
            class="
              flex flex-wrap
              items-center
              justify-between gap-4
              border-b
              border-app-border
              px-5 py-4
            "
          >
            <div>
              <h3
                class="
                  font-heading
                  text-lg font-bold
                  text-app-text
                "
              >
                Bài nộp của học viên
              </h3>

              <p
                class="
                  mt-1 text-sm
                  text-app-text-muted
                "
              >
                {{ submittedCount }}
                bài đã nộp ·
                {{ gradedCount }}
                bài đã chấm
              </p>
            </div>

            <ClipboardCheck
              :size="21"
              class="text-secondary"
            />
          </header>

          <div
            v-if="loadingSubmissions"
            class="space-y-3 p-5"
          >
            <div
              v-for="index in 3"
              :key="index"
              class="
                h-40 animate-pulse
                rounded-control
                bg-app-surface-muted
              "
            />
          </div>

          <div
            v-else-if="
              submissions.length === 0
            "
            class="
              px-6 py-14
              text-center
            "
          >
            <FileText
              :size="34"
              class="
                mx-auto
                text-app-text-muted/40
              "
            />

            <p
              class="
                mt-3 font-semibold
                text-app-text
              "
            >
              Chưa có bài nộp
            </p>

            <p
              class="
                mt-1 text-sm
                text-app-text-muted
              "
            >
              Chưa có học viên nào
              nộp bài tập này.
            </p>
          </div>

          <div
            v-else
            class="
              divide-y
              divide-app-border
            "
          >
            <article
              v-for="
                submission in submissions
              "
              :key="submission.id"
              class="p-5"
            >
              <!-- Student -->
              <div
                class="
                  flex flex-wrap
                  items-start
                  justify-between
                  gap-4
                "
              >
                <div
                  class="
                    flex items-center
                    gap-3
                  "
                >
                  <span
                    class="
                      flex h-10 w-10
                      items-center
                      justify-center
                      rounded-full
                      bg-secondary-soft
                      text-secondary
                    "
                  >
                    <UserRound
                      :size="18"
                    />
                  </span>

                  <div>
                    <p
                      class="
                        font-semibold
                        text-app-text
                      "
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
                      class="
                        text-xs
                        text-app-text-muted
                      "
                    >
                      {{
                        studentEmail(
                          submission.studentId,
                        )
                      }}
                    </p>
                  </div>
                </div>

                <div
                  class="
                    flex flex-wrap
                    items-center gap-2
                  "
                >
                  <span
                    v-if="submission.late"
                    class="
                      rounded-pill
                      bg-danger-soft
                      px-2.5 py-1
                      text-xs
                      font-semibold
                      text-danger
                    "
                  >
                    Nộp trễ
                  </span>

                  <span
                    class="
                      rounded-pill
                      px-2.5 py-1
                      text-xs
                      font-semibold
                    "
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
                        : 'Đã nộp'
                    }}
                  </span>
                </div>
              </div>

              <!-- Submission -->
              <div
                class="
                  mt-5 rounded-control
                  bg-app-surface-muted/50
                  p-4
                "
              >
                <p
                  class="
                    text-xs
                    font-semibold uppercase
                    tracking-wide
                    text-app-text-muted
                  "
                >
                  Nội dung bài làm
                </p>

                <p
                  class="
                    mt-2 whitespace-pre-wrap
                    text-sm leading-6
                    text-app-text
                  "
                >
                  {{
                    submission.content ||
                    'Học viên chỉ nộp tệp đính kèm.'
                  }}
                </p>

                <a
                  v-if="
                    submission.attachmentUrl
                  "
                  :href="
                    submission.attachmentUrl
                  "
                  target="_blank"
                  rel="noopener noreferrer"
                  class="
                    mt-4 inline-flex
                    items-center gap-2
                    text-sm font-semibold
                    text-secondary
                    hover:underline
                  "
                >
                  <FileText
                    :size="15"
                  />

                  {{
                    submission.originalFileName ||
                    'Xem tệp bài làm'
                  }}
                </a>

                <p
                  class="
                    mt-3 text-xs
                    text-app-text-muted
                  "
                >
                  Nộp lúc
                  {{
                    formatDateTime(
                      submission.submittedAt,
                    )
                  }}
                </p>
              </div>

              <!-- Grade -->
              <div
                class="
                  mt-5 grid gap-4
                  md:grid-cols-[10rem_minmax(0,1fr)_auto]
                  md:items-end
                "
              >
                <label>
                  <span
                    class="
                      mb-2 block
                      text-sm font-semibold
                      text-app-text
                    "
                  >
                    Điểm
                  </span>

                  <div
                    class="relative"
                  >
                    <input
                      v-model="
                        gradeForms[
                          submission.id
                        ].score
                      "
                      type="number"
                      min="0"
                      :max="
                        selectedAssignment.maxScore
                      "
                      step="0.1"
                      class="
                        h-11 w-full
                        rounded-control
                        border
                        border-app-border
                        bg-app-surface
                        px-3 pr-12
                        text-sm
                        outline-none
                        focus:border-secondary
                        focus:ring-2
                        focus:ring-secondary/20
                      "
                    />

                    <span
                      class="
                        absolute
                        right-3 top-1/2
                        -translate-y-1/2
                        text-xs
                        text-app-text-muted
                      "
                    >
                      /
                      {{
                        selectedAssignment.maxScore
                      }}
                    </span>
                  </div>
                </label>

                <label>
                  <span
                    class="
                      mb-2 block
                      text-sm font-semibold
                      text-app-text
                    "
                  >
                    Nhận xét
                  </span>

                  <textarea
                    v-model="gradeForms[submission.id].feedback"
                    rows="2"
                    maxlength="5000"
                    placeholder="Nhập nhận xét cho học viên..."
                    class="
                      w-full resize-y
                      rounded-control
                      border
                      border-app-border
                      bg-app-surface
                      px-3 py-2.5
                      text-sm
                      outline-none
                      focus:border-secondary
                      focus:ring-2
                      focus:ring-secondary/20
                    "
                  />
                </label>

                <BaseButton
                  :loading="
                    gradingSubmissionId ===
                    submission.id
                  "
                  @click="
                    handleGrade(submission)
                  "
                >
                  <template #leading>
                    <ClipboardCheck
                      :size="16"
                    />
                  </template>

                  {{
                    submission.status ===
                    'GRADED'
                      ? 'Cập nhật điểm'
                      : 'Chấm bài'
                  }}
                </BaseButton>
              </div>
            </article>
          </div>
        </section>
      </main>
    </div>

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