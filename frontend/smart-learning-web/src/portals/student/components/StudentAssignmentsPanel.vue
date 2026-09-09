<script setup lang="ts">
import {
  computed,
  onMounted,
  ref,
} from 'vue'

import {
  CalendarClock,
  CheckCircle2,
  FileText,
  Paperclip,
  Upload,
  Trash2 
} from 'lucide-vue-next'
import {
  BaseAlert,
  BaseButton,
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

import { useStudentApiError }
  from '../composables/useStudentApiError'

import { formatDateTime }
  from '@/shared/utils/date'

const props = defineProps<{
  courseId: string
}>()

const { handleApiError } =
  useStudentApiError()

const assignments = ref<Assignment[]>([])
const submissions =
  ref<AssignmentSubmission[]>([])

const loading = ref(true)
const submitting = ref(false)
const message = ref('')
const deleteOpen = ref(false)
const deleting = ref(false)

const selected =
  ref<Assignment | null>(null)

const content = ref('')
const file = ref<File | null>(null)

const submissionByAssignment =
  computed(() => {
    const map =
      new Map<string, AssignmentSubmission>()

    for (const item of submissions.value) {
      map.set(item.assignmentId, item)
    }

    return map
  })

const currentSubmission = computed(() =>
  selected.value
    ? submissionByAssignment.value.get(
        selected.value.id,
      ) ?? null
    : null,
)

const selectAssignment = (
  assignment: Assignment,
) => {
  selected.value = assignment

  const submission =
    submissionByAssignment.value.get(
      assignment.id,
    )

  content.value =
    submission?.content ?? ''

  file.value = null
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

    assignments.value = assignmentData
    submissions.value = submissionData

    if (
      assignmentData.length &&
      !selected.value
    ) {
      selectAssignment(
        assignmentData[0]!,
      )
    }
  } catch (error) {
    message.value = handleApiError(
      error,
      'Không thể tải bài tập.',
    ).message
  } finally {
    loading.value = false
  }
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
  if (!selected.value) return

  submitting.value = true
  message.value = ''

  try {
    const result =
      await submitAssignment(
        props.courseId,
        selected.value.id,
        content.value,
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

    file.value = null
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
  deleteOpen.value = true
}

const handleDeleteSubmission = async () => {
  if (!selected.value || !currentSubmission.value) return

  deleting.value = true
  message.value = ''

  try {
    await deleteMySubmission(
      props.courseId,
      selected.value.id,
    )

    submissions.value = submissions.value.filter(
      (item) => item.assignmentId !== selected.value?.id,
    )

    content.value = ''
    file.value = null
    deleteOpen.value = false
  } catch (error) {
    message.value = handleApiError(
      error,
      'Không thể xóa bài đã nộp.',
    ).message
  } finally {
    deleting.value = false
  }
}

onMounted(() => void load())
</script>

<template>
  <section class="space-y-5">
    <BaseAlert v-if="message">
      {{ message }}
    </BaseAlert>

    <div
      v-if="loading"
      class="h-96 animate-pulse rounded-panel bg-app-surface-muted"
    />

    <div
      v-else-if="assignments.length === 0"
      class="rounded-panel border border-dashed border-app-border bg-app-surface p-12 text-center"
    >
      <FileText
        :size="36"
        class="mx-auto text-app-text-muted"
      />

      <p class="mt-3 font-semibold">
        Chưa có bài tập
      </p>

      <p
        class="mt-1 text-sm text-app-text-muted"
      >
        Giảng viên chưa giao bài tập cho khóa học.
      </p>
    </div>

    <div
      v-else
      class="grid gap-5 lg:grid-cols-[20rem_minmax(0,1fr)]"
    >
      <aside
        class="overflow-hidden rounded-card border border-app-border bg-app-surface"
      >
        <button
          v-for="assignment in assignments"
          :key="assignment.id"
          type="button"
          class="block w-full border-b border-app-border p-4 text-left last:border-b-0"
          :class="
            selected?.id === assignment.id
              ? 'bg-secondary-soft'
              : 'hover:bg-app-surface-muted'
          "
          @click="selectAssignment(assignment)"
        >
          <p
            class="font-semibold text-app-text"
          >
            {{ assignment.title }}
          </p>

          <div
            class="mt-2 flex items-center gap-1 text-xs text-app-text-muted"
          >
            <CalendarClock :size="13" />

            {{ formatDateTime(assignment.dueAt) }}
          </div>

          <p
            v-if="
              submissionByAssignment.get(
                assignment.id,
              )?.status === 'GRADED'
            "
            class="mt-2 text-xs font-semibold text-ai"
          >
            Đã chấm
          </p>

          <p
            v-else-if="
              submissionByAssignment.has(
                assignment.id,
              )
            "
            class="mt-2 text-xs font-semibold text-secondary"
          >
            Đã nộp
          </p>
        </button>
      </aside>

      <article
        v-if="selected"
        class="rounded-card border border-app-border bg-app-surface p-6 shadow-card"
      >
        <header
          class="border-b border-app-border pb-5"
        >
          <h2
            class="font-heading text-2xl font-bold text-app-text"
          >
            {{ selected.title }}
          </h2>

          <p
            class="mt-2 text-sm text-app-text-muted"
          >
            Hạn nộp:
            {{ formatDateTime(selected.dueAt) }}
            · {{ selected.maxScore }} điểm
          </p>
        </header>

        <p
          class="mt-6 whitespace-pre-wrap text-sm leading-7 text-app-text"
        >
          {{
            selected.description ||
            'Không có mô tả.'
          }}
        </p>

        <!-- Đã chấm -->
        <div
          v-if="
            currentSubmission?.status
              === 'GRADED'
          "
          class="mt-7 rounded-card bg-ai-soft p-5"
        >
          <div
            class="flex items-center gap-2 font-semibold text-ai"
          >
            <CheckCircle2 :size="19" />
            Bài đã được chấm
          </div>

          <p
            class="mt-4 text-lg font-bold"
          >
            {{ currentSubmission.score }}
            /
            {{ selected.maxScore }}
          </p>

          <p
            v-if="currentSubmission.feedback"
            class="mt-3 whitespace-pre-wrap text-sm leading-6"
          >
            {{ currentSubmission.feedback }}
          </p>

          <a
            v-if="
              currentSubmission.attachmentUrl
            "
            :href="
              currentSubmission.attachmentUrl
            "
            target="_blank"
            class="mt-4 inline-flex items-center gap-2 text-sm font-semibold text-secondary"
          >
            <Paperclip :size="15" />
            {{
              currentSubmission.originalFileName
            }}
          </a>
        </div>

        <!-- Submit -->
        <form
          v-else
          class="mt-7 space-y-5"
          @submit.prevent="submit"
        >
          <label class="block">
            <span
              class="mb-2 block text-sm font-semibold text-app-text"
            >
              Nội dung bài làm
            </span>

            <textarea
              v-model="content"
              rows="8"
              maxlength="20000"
              class="w-full rounded-control border border-app-border bg-app-surface p-3 text-sm outline-none focus:border-secondary focus:ring-2 focus:ring-secondary/20"
              placeholder="Nhập nội dung bài làm..."
            />
          </label>

          <label class="block">
            <span
              class="mb-2 block text-sm font-semibold text-app-text"
            >
              Tệp đính kèm
            </span>

            <input
              type="file"
              class="block w-full text-sm"
              @change="onFileChange"
            />
          </label>

          <a
            v-if="
              currentSubmission?.attachmentUrl
            "
            :href="
              currentSubmission.attachmentUrl
            "
            target="_blank"
            class="inline-flex items-center gap-2 text-sm font-semibold text-secondary"
          >
            <Paperclip :size="15" />

            Tệp đã nộp:
            {{
              currentSubmission.originalFileName
            }}
          </a>

          <div class="flex flex-wrap items-center justify-between gap-4">
            <p
                v-if="currentSubmission?.late"
                class="text-sm font-medium text-danger"
            >
                Bài được nộp sau hạn.
            </p>

            <span v-else />

            <div class="flex items-center gap-3">
                <button
                v-if="currentSubmission"
                type="button"
                class="inline-flex h-11 items-center gap-2 rounded-control bg-danger-soft px-4 text-sm font-semibold text-danger transition hover:opacity-80"
                @click="openDeleteSubmission"
                >
                <Trash2 :size="16" />
                Xóa bài đã nộp
                </button>

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
            </div>
        </form>
      </article>
    </div>
    <ConfirmDialog
    :open="deleteOpen"
    title="Xóa bài đã nộp?"
    description="Bài làm hiện tại và tệp đính kèm sẽ bị xóa. Bạn có thể nộp lại bài sau đó nếu cần."
    confirm-text="Xóa bài đã nộp"
    :loading="deleting"
    @close="!deleting && (deleteOpen = false)"
    @confirm="handleDeleteSubmission"
    />
  </section>
</template>