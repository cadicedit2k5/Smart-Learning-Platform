<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { BookOpenText, Clock3, FilePenLine, Plus, Trash2, X} from 'lucide-vue-next'

import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import BaseInput from '@/shared/components/BaseInput.vue'

import {
  createTopic,
  deleteTopic,
  getTopics,
  updateTopic,
  type CourseTopic,
  type TopicContent,
  type TopicInput,
} from '../api/contentApi'

import { useLecturerApiError } from '../composables/useLecturerApiError'
import TopicContentEditor from './TopicContentEditor.vue'

const props = defineProps<{
  courseId: string
  chapterId: string
}>()

const { handleApiError } = useLecturerApiError()

const topics = ref<CourseTopic[]>([])
const loading = ref(true)
const saving = ref(false)
const message = ref('')

const editorOpen = ref(false)
const editingTopic = ref<CourseTopic | null>(null)

const emptyContent = (): TopicContent => ({
  type: 'doc',
  content: [{ type: 'paragraph' }],
})

const emptyForm = () => ({
  title: '',
  description: '',
  orderIndex: '',
  estimatedMinutes: '',
  content: emptyContent(),
})

const form = reactive(emptyForm())

const modalTitle = computed(() =>
  editingTopic.value ? 'Chỉnh sửa chủ đề' : 'Thêm chủ đề',
)

const nextOrderIndex = computed(() => {
  if (!topics.value.length) return 0

  return (
    Math.max(
      ...topics.value.map((topic) => topic.orderIndex),
    ) + 1
  )
})

const cloneContent = (
  content: TopicContent | null,
): TopicContent => {
  return JSON.parse(
    JSON.stringify(content ?? emptyContent()),
  ) as TopicContent
}

const hasContent = (topic: CourseTopic) => {
  const content = topic.content?.content
  return Array.isArray(content) && content.length > 0
}

const loadTopics = async () => {
  loading.value = true
  message.value = ''

  try {
    topics.value = await getTopics(props.courseId, props.chapterId,)
  } catch (error) {
    message.value = handleApiError(
      error,
      'Không thể tải danh sách chủ đề.',
    ).message
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  editingTopic.value = null

  Object.assign(form, emptyForm(), {
    orderIndex: String(nextOrderIndex.value),
  })

  editorOpen.value = true;
}

const openEdit = (topic: CourseTopic) => {
  editingTopic.value = topic

  Object.assign(form, {
    title: topic.title,
    description: topic.description ?? '',
    orderIndex: String(topic.orderIndex),
    estimatedMinutes:
      topic.estimatedMinutes == null
        ? ''
        : String(topic.estimatedMinutes),
    content: cloneContent(topic.content),
  })

  editorOpen.value = true
}

const closeEditor = () => {
  if (saving.value) return

  editorOpen.value = false
  editingTopic.value = null
  Object.assign(form, emptyForm())
}

const toInput = (): TopicInput => ({
  title: form.title.trim(),
  description: form.description.trim() || undefined,
  orderIndex:
    form.orderIndex === ''
      ? undefined
      : Number(form.orderIndex),
  estimatedMinutes:
    form.estimatedMinutes === ''
      ? undefined
      : Number(form.estimatedMinutes),
  content: form.content,
})

const handleSubmit = async () => {
  if (!form.title.trim()) {
    message.value = 'Tên chủ đề không được để trống.'
    return
  }

  saving.value = true
  message.value = ''

  try {
    if (editingTopic.value) {
      await updateTopic(
        props.courseId,
        props.chapterId,
        editingTopic.value.id,
        toInput(),
      )
    } else {
      await createTopic(
        props.courseId,
        props.chapterId,
        toInput(),
      )
    }

    closeEditor()
    await loadTopics()
  } catch (error) {
    message.value = handleApiError(
      error,
      editingTopic.value
        ? 'Không thể cập nhật chủ đề.'
        : 'Không thể tạo chủ đề.',
    ).message
  } finally {
    saving.value = false
  }
}

const handleDelete = async (topic: CourseTopic) => {
  if (
    !window.confirm(
      `Xóa chủ đề “${topic.title}”?`,
    )
  ) {
    return
  }

  message.value = ''

  try {
    await deleteTopic(
      props.courseId,
      props.chapterId,
      topic.id,
    )

    await loadTopics()
  } catch (error) {
    message.value = handleApiError(
      error,
      'Không thể xóa chủ đề.',
    ).message
  }
}

onMounted(() => void loadTopics())
</script>

<template>
  <div
    class="space-y-4 border-t border-app-border bg-app-surface-muted/30 p-5"
  >
    <BaseAlert v-if="message">
      {{ message }}
    </BaseAlert>

    <header
      class="flex flex-wrap items-center justify-between gap-3"
    >
      <div>
        <p class="font-semibold text-app-text">
          Chủ đề
        </p>

        <p class="mt-0.5 text-xs text-app-text-muted">
          {{ topics.length }} chủ đề trong chương này
        </p>
      </div>

      <BaseButton
        variant="secondary"
        @click="openCreate"
      >
        <template #leading>
          <Plus :size="16" />
        </template>

        Thêm chủ đề
      </BaseButton>
    </header>

    <div
      v-if="loading"
      class="space-y-2"
    >
      <div
        v-for="index in 3"
        :key="index"
        class="h-20 animate-pulse rounded-card bg-app-surface"
      />
    </div>

    <div
      v-else-if="topics.length === 0"
      class="rounded-card border border-dashed border-app-border bg-app-surface px-6 py-10 text-center"
    >
      <BookOpenText
        :size="28"
        class="mx-auto text-app-text-muted"
      />

      <p class="mt-3 font-semibold text-app-text">
        Chưa có chủ đề
      </p>

      <p class="mt-1 text-sm text-app-text-muted">
        Tạo chủ đề đầu tiên để bắt đầu xây dựng bài học.
      </p>
    </div>

    <div
      v-else
      class="space-y-2"
    >
      <article
        v-for="topic in topics"
        :key="topic.id"
        class="group flex flex-col gap-4 rounded-card border border-app-border bg-app-surface p-4 shadow-sm transition hover:border-secondary/40 sm:flex-row sm:items-center"
      >
        <div
          class="flex h-10 w-10 shrink-0 items-center justify-center rounded-pill bg-secondary-soft font-heading text-sm font-bold text-secondary"
        >
          {{ topic.orderIndex + 1 }}
        </div>

        <div class="min-w-0 flex-1">
          <h4 class="font-semibold text-app-text">
            {{ topic.title }}
          </h4>

          <p
            v-if="topic.description"
            class="mt-1 line-clamp-2 text-sm leading-6 text-app-text-muted"
          >
            {{ topic.description }}
          </p>

          <div
            class="mt-2 flex flex-wrap items-center gap-3 text-xs text-app-text-muted"
          >
            <span
              v-if="topic.estimatedMinutes"
              class="inline-flex items-center gap-1"
            >
              <Clock3 :size="14" />
              {{ topic.estimatedMinutes }} phút
            </span>

            <span
              v-if="hasContent(topic)"
              class="inline-flex items-center gap-1 rounded-pill bg-ai-soft px-2 py-1 font-medium text-ai"
            >
              <BookOpenText :size="13" />
              Đã có nội dung
            </span>

            <span
              v-else
              class="rounded-pill bg-app-surface-muted px-2 py-1"
            >
              Chưa soạn nội dung
            </span>
          </div>
        </div>

        <div class="flex shrink-0 items-center gap-1">
          <BaseButton
            variant="secondary"
            @click="openEdit(topic)"
          >
            <template #leading>
              <FilePenLine :size="16" />
            </template>

            Soạn nội dung
          </BaseButton>

          <button
            type="button"
            class="rounded-control p-2.5 text-danger hover:bg-danger-soft"
            aria-label="Xóa chủ đề"
            @click="handleDelete(topic)"
          >
            <Trash2 :size="17" />
          </button>
        </div>
      </article>
    </div>
  </div>

  <!-- Topic editor -->
  <Teleport to="body">
    <div
      v-if="editorOpen"
      class="fixed inset-0 z-[90] flex items-center justify-center bg-slate-950/50 p-3 sm:p-6"
      @mousedown.self="closeEditor"
    >
      <form
        class="flex max-h-[94vh] w-full max-w-6xl flex-col overflow-hidden rounded-panel border border-app-border bg-app-surface shadow-overlay"
        @submit.prevent="handleSubmit"
      >
        <!-- Header -->
        <header
          class="flex shrink-0 items-center justify-between gap-4 border-b border-app-border px-6 py-4"
        >
          <div>
            <h2
              class="font-heading text-xl font-bold text-app-text"
            >
              {{ modalTitle }}
            </h2>

            <p class="mt-1 text-sm text-app-text-muted">
              Soạn nội dung bài học và thông tin của chủ đề.
            </p>
          </div>

          <button
            type="button"
            class="rounded-control p-2 text-app-text-muted hover:bg-app-surface-muted"
            :disabled="saving"
            @click="closeEditor"
          >
            <X :size="20" />
          </button>
        </header>

        <!-- Main -->
        <div
          class="grid min-h-0 flex-1 overflow-y-auto lg:grid-cols-[minmax(0,1fr)_20rem]"
        >
          <!-- Rich editor -->
          <section
            class="min-w-0 border-b border-app-border p-5 lg:border-r lg:border-b-0 lg:p-6"
          >
            <div class="mb-3">
              <h3 class="font-semibold text-app-text">
                Nội dung bài học
              </h3>

              <p class="mt-1 text-xs text-app-text-muted">
                Sử dụng tiêu đề, danh sách, trích dẫn và
                code để trình bày nội dung.
              </p>
            </div>

            <TopicContentEditor
              v-model="form.content"
            />
          </section>

          <!-- Metadata -->
          <aside
            class="space-y-5 bg-app-surface-muted/30 p-5 lg:p-6"
          >
            <div>
              <h3 class="font-semibold text-app-text">
                Thông tin chủ đề
              </h3>

              <p class="mt-1 text-xs text-app-text-muted">
                Thông tin hiển thị trong mục lục khóa học.
              </p>
            </div>

            <BaseInput
              v-model="form.title"
              label="Tên chủ đề"
              maxlength="255"
              required
            />

            <label class="block">
              <span
                class="mb-1.5 block text-sm font-medium text-app-text"
              >
                Mô tả ngắn
              </span>

              <textarea
                v-model="form.description"
                rows="5"
                maxlength="10000"
                class="w-full resize-y rounded-control border border-app-border bg-app-surface px-3 py-2 text-sm text-app-text outline-none focus:border-secondary focus:ring-2 focus:ring-secondary/20"
                placeholder="Nội dung chính của chủ đề..."
              />
            </label>

            <BaseInput
              v-model="form.estimatedMinutes"
              type="number"
              min="1"
              label="Thời lượng dự kiến"
              placeholder="Ví dụ: 20"
            />

            <BaseInput
              v-model="form.orderIndex"
              type="number"
              min="0"
              label="Thứ tự"
            />
          </aside>
        </div>

        <!-- Footer -->
        <footer
          class="flex shrink-0 justify-end gap-2 border-t border-app-border px-6 py-4"
        >
          <BaseButton
            variant="secondary"
            type="button"
            :disabled="saving"
            @click="closeEditor"
          >
            Hủy
          </BaseButton>

          <BaseButton
            type="submit"
            :loading="saving"
          >
            {{
              editingTopic
                ? 'Lưu thay đổi'
                : 'Tạo chủ đề'
            }}
          </BaseButton>
        </footer>
      </form>
    </div>
  </Teleport>
</template>