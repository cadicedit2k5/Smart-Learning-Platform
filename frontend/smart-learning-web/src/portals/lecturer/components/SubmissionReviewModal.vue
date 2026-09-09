<script setup lang="ts">
import { ref, watch } from 'vue'
import { CheckCircle2, ClipboardCheck, FileText, Paperclip } from 'lucide-vue-next'

import { BaseButton, BaseModal } from '@/shared/components'
import type { AssignmentSubmission } from '@/shared/assignment/types'
import { formatDateTime } from '@/shared/utils/date'
import type { GradeInput } from '../api/assignmentApi'

const props = defineProps<{
  open: boolean
  submission: AssignmentSubmission | null
  studentName: string
  studentEmail?: string
  maxScore: number
  loading?: boolean
}>()

const emit = defineEmits<{
  close: []
  grade: [input: GradeInput]
}>()

const gradingMode = ref(false)
const score = ref('')
const feedback = ref('')
const error = ref('')

const reset = () => {
  gradingMode.value = false
  score.value = props.submission?.score != null ? String(props.submission.score) : ''
  feedback.value = props.submission?.feedback ?? ''
  error.value = ''
}

watch(() => [props.open, props.submission], reset, { immediate: true })

const startGrading = () => {
  gradingMode.value = true
  error.value = ''
}

const cancelGrading = () => {
  reset()
}

const submitGrade = () => {
  const value = Number(score.value)

  if (!Number.isFinite(value) || value < 0 || value > props.maxScore) {
    error.value = `Điểm phải nằm trong khoảng 0 - ${props.maxScore}.`
    return
  }

  emit('grade', {
    score: value,
    feedback: feedback.value.trim() || undefined,
  })
}
</script>

<template>
  <BaseModal
    :open="open"
    :title="studentName"
    :description="studentEmail || 'Bài nộp của học viên'"
    :loading="loading"
    max-width="max-w-3xl"
    @close="emit('close')"
  >
    <div v-if="submission" class="space-y-6">
      <div class="flex flex-wrap items-center gap-3">
        <span
          class="rounded-pill px-2.5 py-1 text-xs font-semibold"
          :class="submission.status === 'GRADED'
            ? 'bg-ai-soft text-ai'
            : 'bg-secondary-soft text-secondary'"
        >
          {{ submission.status === 'GRADED' ? 'Đã chấm' : 'Chưa chấm' }}
        </span>

        <span v-if="submission.late" class="rounded-pill bg-danger-soft px-2.5 py-1 text-xs font-semibold text-danger">
          Nộp trễ
        </span>

        <span class="text-sm text-app-text-muted">
          Nộp lúc {{ formatDateTime(submission.submittedAt) }}
        </span>
      </div>

      <section>
        <p class="text-xs font-semibold uppercase tracking-wide text-app-text-muted">
          Nội dung bài làm
        </p>

        <div class="mt-3 rounded-card bg-app-surface-muted/60 p-4">
          <p class="whitespace-pre-wrap text-sm leading-7 text-app-text">
            {{ submission.content || 'Học viên không nhập nội dung văn bản.' }}
          </p>
        </div>

        <a
          v-if="submission.attachmentUrl"
          :href="submission.attachmentUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="mt-3 inline-flex items-center gap-2 text-sm font-semibold text-secondary hover:underline"
        >
          <Paperclip :size="16" />
          {{ submission.originalFileName || 'Xem tệp đính kèm' }}
        </a>
      </section>

      <section
        v-if="submission.status === 'GRADED' && !gradingMode"
        class="rounded-card border border-ai/20 bg-ai-soft p-5"
      >
        <div class="flex items-center gap-2 text-ai">
          <CheckCircle2 :size="19" />
          <p class="font-semibold">Đã chấm bài</p>
        </div>

        <p class="mt-3 font-heading text-2xl font-bold text-app-text">
          {{ submission.score }} / {{ maxScore }}
        </p>

        <p v-if="submission.feedback" class="mt-3 whitespace-pre-wrap text-sm leading-6 text-app-text-muted">
          {{ submission.feedback }}
        </p>
      </section>

      <section v-if="gradingMode" class="border-t border-app-border pt-5">
        <div class="flex items-center gap-2">
          <ClipboardCheck :size="18" class="text-secondary" />
          <h3 class="font-heading text-lg font-bold text-app-text">
            {{ submission.status === 'GRADED' ? 'Cập nhật điểm' : 'Chấm bài' }}
          </h3>
        </div>

        <p v-if="error" class="mt-3 text-sm font-medium text-danger">
          {{ error }}
        </p>

        <div class="mt-4 grid gap-4 sm:grid-cols-[12rem_minmax(0,1fr)]">
          <label>
            <span class="mb-2 block text-sm font-semibold text-app-text">Điểm</span>

            <div class="relative">
              <input
                v-model="score"
                type="number"
                min="0"
                :max="maxScore"
                step="0.1"
                class="h-11 w-full rounded-control border border-app-border bg-app-surface px-3 pr-14 text-sm outline-none focus:border-secondary focus:ring-2 focus:ring-secondary/20"
              />

              <span class="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-app-text-muted">
                / {{ maxScore }}
              </span>
            </div>
          </label>

          <label>
            <span class="mb-2 block text-sm font-semibold text-app-text">Nhận xét</span>

            <textarea
              v-model="feedback"
              rows="4"
              maxlength="5000"
              placeholder="Nhập nhận xét cho học viên..."
              class="w-full resize-y rounded-control border border-app-border bg-app-surface px-3 py-2.5 text-sm outline-none focus:border-secondary focus:ring-2 focus:ring-secondary/20"
            />
          </label>
        </div>
      </section>

      <div class="flex justify-end gap-3 border-t border-app-border pt-5">
        <template v-if="gradingMode">
          <BaseButton variant="secondary" :disabled="loading" @click="cancelGrading">
            Hủy
          </BaseButton>

          <BaseButton :loading="loading" @click="submitGrade">
            <template #leading>
              <ClipboardCheck :size="16" />
            </template>
            Lưu điểm
          </BaseButton>
        </template>

        <template v-else>
          <BaseButton variant="secondary" @click="emit('close')">
            Đóng
          </BaseButton>

          <BaseButton @click="startGrading">
            <template #leading>
              <ClipboardCheck :size="16" />
            </template>
            {{ submission.status === 'GRADED' ? 'Chỉnh sửa điểm' : 'Chấm bài' }}
          </BaseButton>
        </template>
      </div>
    </div>
  </BaseModal>
</template>