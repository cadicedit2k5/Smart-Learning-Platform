
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { BaseButton } from '@/shared/components'
import {
  createQuizDraft, createQuizVersion, getQuizVersions, publishQuiz,
  updateQuizDetails, updateQuizQuestions,
  type EditorQuiz, type QuizQuestionInput,
} from '@/shared/topic-quiz/topicQuizApi'
import { useLecturerApiError } from '../composables/useLecturerApiError'

const props = defineProps<{ courseId: string; topicId: string; topicTitle: string }>()
const emit = defineEmits<{ close: [] }>()
const { handleApiError } = useLecturerApiError()

type FormQuestion = QuizQuestionInput & { id?: string }

const versions = ref<EditorQuiz[]>([])
const selectedId = ref<string | null>(null)
const title = ref('')
const description = ref('')
const questions = ref<FormQuestion[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const success = ref('')

const selectedQuiz = computed(() => versions.value.find(q => q.id === selectedId.value) ?? null)
const canEditQuestions = computed(() =>
  !selectedQuiz.value ||
  (selectedQuiz.value.status !== 'ARCHIVED' && !selectedQuiz.value.hasAttempts),
)

const emptyQuestion = (): FormQuestion => ({
  type: 'SINGLE_CHOICE',
  content: '',
  explanation: null,
  options: [{ content: '', correct: false }, { content: '', correct: false }],
})

const newDraft = () => {
  selectedId.value = null
  title.value = ''
  description.value = ''
  questions.value = [emptyQuestion()]
}

const selectQuiz = (quiz: EditorQuiz) => {
  selectedId.value = quiz.id
  title.value = quiz.title
  description.value = quiz.description ?? ''
  questions.value = quiz.questions.map(q => ({
    id: q.id,
    type: q.type,
    content: q.content,
    explanation: q.explanation,
    options: q.options.map(o => ({ content: o.content, correct: o.correct })),
  }))
}

const load = async (preferredId?: string) => {
  loading.value = true
  error.value = ''

  try {
    versions.value = await getQuizVersions(props.courseId, props.topicId)
    const quiz = versions.value.find(q => q.id === preferredId)
      ?? versions.value.find(q => q.status === 'DRAFT')
      ?? versions.value.find(q => q.status === 'PUBLISHED')
      ?? versions.value[0]

    if (quiz) selectQuiz(quiz)
    else newDraft()
  } catch (e) {
    error.value = handleApiError(e, 'Không tải được bài kiểm tra.').message
  } finally {
    loading.value = false
  }
}

const questionInputs = (): QuizQuestionInput[] => questions.value.map(q => ({
  type: q.type,
  content: q.content.trim(),
  explanation: q.explanation?.trim() || null,
  options: q.options.map(o => ({ content: o.content.trim(), correct: o.correct })),
}))

const saveQuestions = async () => {
  if (!canEditQuestions.value || saving.value) return
  saving.value = true
  error.value = ''
  success.value = ''

  try {
    const saved = selectedQuiz.value
      ? await updateQuizQuestions(props.courseId, props.topicId, selectedQuiz.value.id, questionInputs())
      : await createQuizDraft(props.courseId, props.topicId, {
          title: title.value.trim(), description: description.value.trim() || null,
          questions: questionInputs(),
        })

    await load(saved.id)
    success.value = 'Đã lưu nội dung bài kiểm tra.'
  } catch (e) {
    error.value = handleApiError(e, 'Không lưu được bài kiểm tra.').message
  } finally {
    saving.value = false
  }
}

const saveDetails = async () => {
  if (!selectedQuiz.value || saving.value) return
  saving.value = true
  error.value = ''
  success.value = ''

  try {
    const saved = await updateQuizDetails(props.courseId, props.topicId, selectedQuiz.value.id, {
      title: title.value.trim(),
      description: description.value,
      explanations: questions.value
        .filter((q): q is FormQuestion & { id: string } => Boolean(q.id))
        .map(q => ({ questionId: q.id, explanation: q.explanation })),
    })

    await load(saved.id)
    success.value = 'Đã lưu tên, hướng dẫn và lời giải.'
  } catch (e) {
    error.value = handleApiError(e, 'Không lưu được thông tin.').message
  } finally {
    saving.value = false
  }
}

const cloneVersion = async () => {
  const quiz = selectedQuiz.value
  if (!quiz || quiz.status === 'DRAFT' || saving.value) return
  saving.value = true
  error.value = ''
  success.value = ''

  try {
    const saved = await createQuizVersion(props.courseId, props.topicId, quiz.id)
    await load(saved.id)
    success.value = 'Đã tạo bản nháp phiên bản mới.'
  } catch (e) {
    error.value = handleApiError(e, 'Không tạo được phiên bản mới.').message
  } finally {
    saving.value = false
  }
}

const publish = async () => {
  const quiz = selectedQuiz.value
  if (!quiz || quiz.status !== 'DRAFT' || saving.value) return
  if (!window.confirm('Xuất bản phiên bản đã lưu? Đề đang công bố sẽ được lưu trữ.')) return

  saving.value = true
  error.value = ''
  success.value = ''

  try {
    const saved = await publishQuiz(props.courseId, props.topicId, quiz.id)
    await load(saved.id)
    success.value = 'Đã xuất bản bài kiểm tra.'
  } catch (e) {
    error.value = handleApiError(e, 'Không xuất bản được bài kiểm tra.').message
  } finally {
    saving.value = false
  }
}

watch(() => props.topicId, () => void load(), { immediate: true })
</script>

<template>
  <Teleport to="body">
    <div class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-950/60 p-4">
      <section role="dialog" aria-modal="true" aria-labelledby="quiz-modal-title"
        class="flex max-h-[94vh] w-full max-w-4xl flex-col rounded-panel bg-app-surface shadow-overlay">
        <header class="flex items-center justify-between gap-3 border-b border-app-border p-5">
          <div>
            <h2 id="quiz-modal-title" class="font-heading text-xl font-bold">Kiểm tra nhanh</h2>
            <p class="mt-1 text-sm text-app-text-muted">{{ topicTitle }}</p>
          </div>
          <button type="button" class="rounded-control p-2 hover:bg-app-surface-muted"
            :disabled="saving" @click="emit('close')">Đóng</button>
        </header>

        <div class="space-y-5 overflow-y-auto p-5">
          <p v-if="error" role="alert" class="text-sm text-danger">{{ error }}</p>
          <p v-if="success" role="status" class="text-sm text-ai">{{ success }}</p>
          <p v-if="loading">Đang tải...</p>

          <template v-else>
            <div class="flex flex-wrap items-center gap-3">
              <label for="quiz-version" class="text-sm font-medium">Phiên bản</label>
              <select id="quiz-version" v-model="selectedId"
                class="rounded-control border border-app-border bg-app-surface p-2 text-sm"
                @change="selectedQuiz ? selectQuiz(selectedQuiz) : newDraft()">
                <option :value="null">Bản nháp mới</option>
                <option v-for="quiz in versions" :key="quiz.id" :value="quiz.id">
                  v{{ quiz.version }} — {{ quiz.status }}
                </option>
              </select>

              <BaseButton v-if="selectedQuiz && selectedQuiz.status !== 'DRAFT'"
                type="button" variant="secondary" :disabled="saving" @click="cloneVersion">
                Tạo phiên bản mới từ đề này
              </BaseButton>
            </div>

            <p v-if="selectedQuiz?.hasAttempts" class="text-sm text-app-text-muted">
              Đã có bài nộp. Muốn thay đổi câu hỏi hoặc đáp án, hãy tạo phiên bản mới.
            </p>
            <p v-else-if="selectedQuiz?.status === 'ARCHIVED'" class="text-sm text-app-text-muted">
              Đề đã lưu trữ. Không thể thay đổi nội dung chấm điểm.
            </p>

            <div class="space-y-3 rounded-card border border-app-border p-4">
              <h3 class="font-semibold">Thông tin bài kiểm tra</h3>
              <label class="block space-y-1">
                <span class="text-sm font-medium">Tên hiển thị</span>
                <input v-model="title" maxlength="255"
                  class="w-full rounded-control border border-app-border bg-app-surface p-3 text-sm" />
              </label>
              <label class="block space-y-1">
                <span class="text-sm font-medium">Mô tả / hướng dẫn</span>
                <textarea v-model="description" rows="3"
                  class="w-full rounded-control border border-app-border bg-app-surface p-3 text-sm" />
              </label>
            </div>

            <article v-for="(question, index) in questions" :key="question.id ?? index"
              class="space-y-4 rounded-card border border-app-border p-4">
              <div class="flex items-center justify-between gap-3">
                <h3 class="font-semibold">Câu {{ index + 1 }}</h3>
                <button v-if="canEditQuestions" type="button" class="text-sm text-danger"
                  @click="questions.splice(index, 1)">Xóa câu</button>
              </div>

              <label class="block space-y-1">
                <span class="text-sm font-medium">Nội dung câu hỏi</span>
                <textarea v-model="question.content" rows="2" :disabled="!canEditQuestions"
                  class="w-full rounded-control border border-app-border bg-app-surface p-3 text-sm" />
              </label>

              <label class="block space-y-1">
                <span class="text-sm font-medium">Loại câu hỏi</span>
                <select v-model="question.type" :disabled="!canEditQuestions"
                  class="w-full rounded-control border border-app-border bg-app-surface p-2 text-sm"
                  @change="question.options.forEach(o => o.correct = false)">
                  <option value="SINGLE_CHOICE">Chọn một đáp án</option>
                  <option value="MULTIPLE_CHOICE">Chọn nhiều đáp án</option>
                </select>
              </label>

              <div class="space-y-2">
                <p class="text-sm font-medium">Phương án và đáp án đúng</p>
                <div v-for="(option, optionIndex) in question.options" :key="optionIndex"
                  class="flex items-center gap-2">
                  <input v-if="question.type === 'SINGLE_CHOICE'" type="radio"
                    :name="`correct-${index}`" :checked="option.correct" :disabled="!canEditQuestions"
                    @change="question.options.forEach((o, i) => o.correct = i === optionIndex)" />
                  <input v-else v-model="option.correct" type="checkbox" :disabled="!canEditQuestions" />

                  <input v-model="option.content" :disabled="!canEditQuestions"
                    class="min-w-0 flex-1 rounded-control border border-app-border bg-app-surface p-2 text-sm" />
                  <button v-if="canEditQuestions" type="button" class="text-sm text-danger"
                    @click="question.options.splice(optionIndex, 1)">Xóa</button>
                </div>
                <button v-if="canEditQuestions" type="button" class="text-sm font-medium text-secondary"
                  @click="question.options.push({ content: '', correct: false })">+ Thêm phương án</button>
              </div>

              <label class="block space-y-1">
                <span class="text-sm font-medium">Giải thích đáp án sau khi nộp</span>
                <textarea v-model="question.explanation" rows="2"
                  class="w-full rounded-control border border-app-border bg-app-surface p-3 text-sm" />
              </label>
            </article>

            <button v-if="canEditQuestions" type="button" class="text-sm font-medium text-secondary"
              @click="questions.push(emptyQuestion())">+ Thêm câu hỏi</button>
          </template>
        </div>

        <footer class="flex flex-wrap justify-end gap-3 border-t border-app-border p-5">
          <BaseButton type="button" variant="secondary" :disabled="saving" @click="emit('close')">Đóng</BaseButton>
          <BaseButton v-if="selectedQuiz" type="button" variant="secondary" :loading="saving"
            @click="saveDetails">Lưu thông tin &amp; lời giải</BaseButton>
          <BaseButton v-if="canEditQuestions" type="button" :loading="saving"
            @click="saveQuestions">{{ selectedQuiz ? 'Lưu câu hỏi & đáp án' : 'Tạo bản nháp' }}</BaseButton>
          <BaseButton v-if="selectedQuiz?.status === 'DRAFT'" type="button" variant="secondary"
            :disabled="saving" @click="publish">Xuất bản</BaseButton>
        </footer>
      </section>
    </div>
  </Teleport>
</template>
