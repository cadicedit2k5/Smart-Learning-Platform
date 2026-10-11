
<script setup lang="ts">
import { ref, watch } from 'vue'
import { BaseButton } from '@/shared/components'
import {
  getCurrentQuiz, getQuizAttempt, getQuizHistory, submitQuiz,
  type StudentQuiz, type QuizAttemptResult, type QuizAttemptSummary,
} from '@/shared/topic-quiz/topicQuizApi'
import { useStudentApiError } from '../composables/useStudentApiError'

const props = defineProps<{ courseId: string; topicId: string }>()
const { handleApiError } = useStudentApiError()

const quiz = ref<StudentQuiz | null>(null)
const history = ref<QuizAttemptSummary[]>([])
const result = ref<QuizAttemptResult | null>(null)
const choices = ref<Record<string, string[]>>({})
const loading = ref(false)
const submitting = ref(false)
const error = ref('')

const load = async () => {
  loading.value = true
  error.value = ''
  result.value = null
  choices.value = {}

  try {
    const [current, attempts] = await Promise.all([
      getCurrentQuiz(props.courseId, props.topicId),
      getQuizHistory(props.courseId, props.topicId),
    ])
    quiz.value = current
    history.value = attempts
  } catch (e) {
    error.value = handleApiError(e, 'Không tải được bài kiểm tra.').message
  } finally {
    loading.value = false
  }
}

const selectOne = (questionId: string, optionId: string) => {
  choices.value[questionId] = [optionId]
}

const toggleMany = (questionId: string, optionId: string) => {
  const selected = choices.value[questionId] ?? []
  choices.value[questionId] = selected.includes(optionId)
    ? selected.filter(id => id !== optionId)
    : [...selected, optionId]
}

const submit = async () => {
  if (!quiz.value || submitting.value) return
  submitting.value = true
  error.value = ''

  try {
    result.value = await submitQuiz(
      props.courseId, props.topicId, quiz.value.id,
      quiz.value.questions.map(q => ({
        questionId: q.id,
        selectedOptionIds: choices.value[q.id] ?? [],
      })),
    )
  } catch (e) {
    error.value = handleApiError(e, 'Không nộp được bài kiểm tra.').message
  } finally {
    submitting.value = false
  }

  if (result.value) {
    try {
      history.value = await getQuizHistory(props.courseId, props.topicId)
    } catch {
      // Kết quả đã nộp vẫn được hiển thị dù tải lại danh sách lịch sử thất bại.
    }
  }
}

const viewAttempt = async (attemptId: string) => {
  error.value = ''
  try {
    result.value = await getQuizAttempt(props.courseId, props.topicId, attemptId)
  } catch (e) {
    error.value = handleApiError(e, 'Không tải được kết quả.').message
  }
}

const formatDate = (value: string) => new Date(value).toLocaleString('vi-VN')
watch(() => [props.courseId, props.topicId], () => void load(), { immediate: true })
</script>

<template>
  <section class="mt-10 space-y-5 border-t border-app-border pt-8">
    <div>
      <h2 class="font-heading text-xl font-bold">Kiểm tra nhanh</h2>
      <p class="mt-1 text-sm text-app-text-muted">
        Tự đánh giá kiến thức sau khi học. Có thể làm lại nhiều lần.
      </p>
    </div>

    <p v-if="loading" class="text-sm text-app-text-muted">Đang tải...</p>
    <p v-if="error" role="alert" class="text-sm text-danger">{{ error }}</p>

    <template v-if="!loading">
      <p v-if="!quiz && !result" class="text-sm text-app-text-muted">
        Chủ đề này chưa có bài kiểm tra đang được công bố.
      </p>

      <form v-if="quiz && !result" class="space-y-5" @submit.prevent="submit">
        <div>
          <h3 class="font-heading text-lg font-semibold">{{ quiz.title }}</h3>
          <p v-if="quiz.description" class="mt-1 text-sm text-app-text-muted">{{ quiz.description }}</p>
          <p class="mt-2 text-xs text-app-text-muted">Phiên bản {{ quiz.version }}</p>
        </div>

        <fieldset v-for="(question, index) in quiz.questions" :key="question.id"
          class="space-y-3 rounded-card border border-app-border p-4">
          <legend class="px-1 font-semibold">Câu {{ index + 1 }}: {{ question.prompt }}</legend>
          <p class="text-xs text-app-text-muted">
            {{ question.type === 'SINGLE_CHOICE' ? 'Chọn một đáp án' : 'Có thể chọn nhiều đáp án' }}
          </p>

          <label v-for="option in question.options" :key="option.id"
            class="flex cursor-pointer items-start gap-3 rounded-control p-2 hover:bg-app-surface-muted">
            <input v-if="question.type === 'SINGLE_CHOICE'" type="radio"
              :name="question.id" :value="option.id"
              :checked="(choices[question.id] ?? []).includes(option.id)"
              @change="selectOne(question.id, option.id)" />
            <input v-else type="checkbox"
              :checked="(choices[question.id] ?? []).includes(option.id)"
              @change="toggleMany(question.id, option.id)" />
            <span class="text-sm">{{ option.content }}</span>
          </label>
        </fieldset>

        <BaseButton type="submit" :loading="submitting">Nộp bài</BaseButton>
      </form>

      <div v-if="result" class="space-y-4">
        <div class="rounded-card border border-app-border bg-app-surface-muted p-5">
          <p class="font-heading text-2xl font-bold text-secondary">
            {{ result.score }} / {{ result.maxScore }} điểm
          </p>
          <p class="mt-1 text-sm text-app-text-muted">
            Đề v{{ result.version }} — {{ formatDate(result.submittedAt) }}
          </p>
        </div>

        <article v-for="(answer, index) in result.answers" :key="answer.questionId"
          class="space-y-3 rounded-card border border-app-border p-4">
          <div class="flex items-center justify-between gap-3">
            <h3 class="font-semibold">Câu {{ index + 1 }}: {{ answer.prompt }}</h3>
            <span class="text-sm font-semibold" :class="answer.awardedScore ? 'text-ai' : 'text-danger'">
              {{ answer.awardedScore }} / 1
            </span>
          </div>

          <div v-for="option in answer.options" :key="option.id"
            class="rounded-control px-3 py-2 text-sm"
            :class="answer.correctOptionIds.includes(option.id) ? 'bg-ai-soft text-ai' : 'bg-app-surface-muted'">
            {{ option.content }}
            <span v-if="answer.selectedOptionIds.includes(option.id)"> — Bạn đã chọn</span>
            <span v-if="answer.correctOptionIds.includes(option.id)"> — Đáp án đúng</span>
          </div>

          <p v-if="answer.explanation" class="text-sm text-app-text-muted">
            Giải thích: {{ answer.explanation }}
          </p>
        </article>

        <BaseButton v-if="quiz" type="button" variant="secondary" @click="load">
          Làm lại
        </BaseButton>
      </div>

      <div v-if="history.length" class="space-y-3 border-t border-app-border pt-5">
        <h3 class="font-semibold">Lịch sử làm bài</h3>

        <div v-for="attempt in history" :key="attempt.id"
          class="flex flex-wrap items-center justify-between gap-3 rounded-card border border-app-border p-3">
          <div class="text-sm">
            <p class="font-medium">
              {{ attempt.score }}/{{ attempt.maxScore }} điểm — Đề v{{ attempt.version }}
            </p>
            <p class="text-app-text-muted">{{ formatDate(attempt.submittedAt) }}</p>
          </div>
          <BaseButton type="button" variant="secondary" @click="viewAttempt(attempt.id)">
            Xem kết quả
          </BaseButton>
        </div>
      </div>
    </template>
  </section>
</template>
