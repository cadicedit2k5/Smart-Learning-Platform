<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import {
  ChevronDown,
  ChevronRight,
  Layers3,
  Pencil,
  Plus,
  Trash2,
} from 'lucide-vue-next'

import {
  BaseAlert,
  BaseButton,
  BaseInput,
  BaseModal,
  ConfirmDialog,
} from '@/shared/components'
import {
  getChapters,
  type CourseChapter,
} from '@/shared/course-content'
import {
  createChapter,
  deleteChapter,
  updateChapter,
  type ChapterInput,
} from '../api/contentApi'
import { useLecturerApiError } from '../composables/useLecturerApiError'
import CourseTopicsPanel from './CourseTopicsPanel.vue'

const props = defineProps<{
  courseId: string
}>()

const { handleApiError } = useLecturerApiError()

const emptyForm = () => ({
  title: '',
  description: '',
  learningObjectives: '',
  orderIndex: '',
})

const chapters = ref<CourseChapter[]>([])
const loading = ref(true)
const saving = ref(false)
const deleting = ref(false)

const message = ref('')
const successMessage = ref('')
const formMessage = ref('')
const deleteMessage = ref('')

const expandedId = ref('')
const formOpen = ref(false)
const editingId = ref<string | null>(null)

const deleteOpen = ref(false)
const deletingChapter = ref<CourseChapter | null>(null)

const form = reactive(emptyForm())

const nextOrderIndex = () => {
  if (!chapters.value.length) return 0
  return Math.max(...chapters.value.map((chapter) => chapter.orderIndex)) + 1
}

const toInput = (): ChapterInput => ({
  title: form.title.trim(),
  description: form.description.trim() || undefined,
  learningObjectives: form.learningObjectives.trim() || undefined,
  orderIndex: form.orderIndex === '' ? undefined : Number(form.orderIndex),
})

const loadChapters = async () => {
  loading.value = true
  message.value = ''

  try {
    chapters.value = await getChapters(props.courseId)
  } catch (error) {
    message.value = handleApiError(
      error,
      'Không thể tải danh sách chương.',
    ).message
  } finally {
    loading.value = false
  }
}

const toggleChapter = (chapterId: string) => {
  expandedId.value = expandedId.value === chapterId ? '' : chapterId
}

const openCreate = () => {
  editingId.value = null
  formMessage.value = ''

  Object.assign(form, emptyForm(), {
    orderIndex: String(nextOrderIndex()),
  })

  formOpen.value = true
}

const openEdit = (chapter: CourseChapter) => {
  editingId.value = chapter.id
  formMessage.value = ''

  Object.assign(form, {
    title: chapter.title,
    description: chapter.description ?? '',
    learningObjectives: chapter.learningObjectives ?? '',
    orderIndex: String(chapter.orderIndex),
  })

  formOpen.value = true
}

const closeForm = () => {
  if (saving.value) return

  formOpen.value = false
  editingId.value = null
  formMessage.value = ''
  Object.assign(form, emptyForm())
}

const handleSubmit = async () => {
  if (!form.title.trim()) {
    formMessage.value = 'Tên chương không được để trống.'
    return
  }

  const chapterId = editingId.value
  const isEditing = chapterId !== null

  saving.value = true
  formMessage.value = ''
  successMessage.value = ''

  try {
    if (chapterId) {
      await updateChapter(props.courseId, chapterId, toInput())
    } else {
      await createChapter(props.courseId, toInput())
    }

    formOpen.value = false
    editingId.value = null
    Object.assign(form, emptyForm())

    await loadChapters()

    successMessage.value = isEditing
      ? 'Đã cập nhật chương.'
      : 'Đã tạo chương mới.'
  } catch (error) {
    formMessage.value = handleApiError(
      error,
      isEditing
        ? 'Không thể cập nhật chương.'
        : 'Không thể tạo chương.',
    ).message
  } finally {
    saving.value = false
  }
}

const openDelete = (chapter: CourseChapter) => {
  deletingChapter.value = chapter
  deleteMessage.value = ''
  deleteOpen.value = true
}

const closeDelete = () => {
  if (deleting.value) return

  deleteOpen.value = false
  deletingChapter.value = null
  deleteMessage.value = ''
}

const handleDelete = async () => {
  if (!deletingChapter.value) return

  const chapter = deletingChapter.value

  deleting.value = true
  deleteMessage.value = ''
  successMessage.value = ''

  try {
    await deleteChapter(props.courseId, chapter.id)

    if (expandedId.value === chapter.id) {
      expandedId.value = ''
    }

    deleteOpen.value = false
    deletingChapter.value = null

    await loadChapters()
    successMessage.value = 'Đã xóa chương.'
  } catch (error) {
    deleteMessage.value = handleApiError(
      error,
      'Không thể xóa chương.',
    ).message
  } finally {
    deleting.value = false
  }
}

const reset = () => {
  expandedId.value = ''
  formOpen.value = false
  editingId.value = null
  deleteOpen.value = false
  deletingChapter.value = null
  message.value = ''
  successMessage.value = ''
  Object.assign(form, emptyForm())

  void loadChapters()
}

watch(() => props.courseId, reset)

onMounted(() => void loadChapters())
</script>

<template>
  <section class="space-y-5">
    <header class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div class="flex items-center gap-3">
        <div
          class="flex h-11 w-11 shrink-0 items-center justify-center rounded-card bg-secondary-soft text-secondary"
        >
          <Layers3 :size="21" />
        </div>

        <div>
          <h2 class="font-heading text-xl font-bold text-app-text">
            Nội dung khóa học
          </h2>

          <p class="mt-1 text-sm text-app-text-muted">
            Quản lý chương, bài học và nội dung giảng dạy.
          </p>
        </div>
      </div>

      <BaseButton @click="openCreate">
        <template #leading>
          <Plus :size="17" />
        </template>

        Thêm chương
      </BaseButton>
    </header>

    <BaseAlert v-if="message">
      {{ message }}
    </BaseAlert>

    <BaseAlert v-if="successMessage" variant="ai">
      {{ successMessage }}
    </BaseAlert>

    <div v-if="loading" class="space-y-3">
      <div
        v-for="index in 3"
        :key="index"
        class="h-24 animate-pulse rounded-card bg-app-surface-muted"
      />
    </div>

    <div
      v-else-if="chapters.length === 0"
      class="rounded-card border border-dashed border-app-border bg-app-surface px-6 py-12 text-center"
    >
      <Layers3 :size="36" class="mx-auto text-app-text-muted/40" />

      <h3 class="mt-3 font-heading text-lg font-bold text-app-text">
        Chưa có nội dung
      </h3>

      <p class="mt-1 text-sm text-app-text-muted">
        Tạo chương đầu tiên để bắt đầu xây dựng khóa học.
      </p>

      <BaseButton class="mt-5" @click="openCreate">
        <template #leading>
          <Plus :size="17" />
        </template>

        Thêm chương
      </BaseButton>
    </div>

    <div v-else class="space-y-3">
      <article
        v-for="chapter in chapters"
        :key="chapter.id"
        class="overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card"
      >
        <div
          class="flex items-start gap-3 p-5 transition"
          :class="expandedId === chapter.id ? 'bg-app-surface-muted/30' : ''"
        >
          <button
            type="button"
            class="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-control text-app-text-muted transition hover:bg-app-surface-muted hover:text-secondary"
            :aria-label="
              expandedId === chapter.id
                ? 'Thu gọn chương'
                : 'Mở chương'
            "
            @click="toggleChapter(chapter.id)"
          >
            <ChevronDown
              v-if="expandedId === chapter.id"
              :size="19"
            />

            <ChevronRight
              v-else
              :size="19"
            />
          </button>

          <button
            type="button"
            class="min-w-0 flex-1 text-left"
            @click="toggleChapter(chapter.id)"
          >
            <div class="flex flex-wrap items-center gap-2">
              <span
                class="rounded-pill bg-secondary-soft px-2.5 py-1 text-xs font-semibold text-secondary"
              >
                Chương {{ chapter.orderIndex + 1 }}
              </span>

              <h3 class="font-heading text-lg font-bold text-app-text">
                {{ chapter.title }}
              </h3>
            </div>

            <p
              v-if="chapter.description"
              class="mt-2 line-clamp-2 text-sm leading-6 text-app-text-muted"
            >
              {{ chapter.description }}
            </p>

            <p
              v-if="chapter.learningObjectives"
              class="mt-2 line-clamp-2 text-sm leading-6 text-app-text-muted"
            >
              <span class="font-semibold text-app-text">
                Mục tiêu:
              </span>
              {{ chapter.learningObjectives }}
            </p>
          </button>

          <div class="flex shrink-0 items-center gap-1">
            <button
              type="button"
              class="rounded-control p-2 text-app-text-muted transition hover:bg-app-surface-muted hover:text-secondary"
              aria-label="Chỉnh sửa chương"
              @click="openEdit(chapter)"
            >
              <Pencil :size="17" />
            </button>

            <button
              type="button"
              class="rounded-control p-2 text-danger transition hover:bg-danger-soft"
              aria-label="Xóa chương"
              @click="openDelete(chapter)"
            >
              <Trash2 :size="17" />
            </button>
          </div>
        </div>

        <CourseTopicsPanel
          v-if="expandedId === chapter.id"
          :key="chapter.id"
          :course-id="courseId"
          :chapter-id="chapter.id"
        />
      </article>
    </div>

    <BaseModal
      :open="formOpen"
      :title="editingId ? 'Chỉnh sửa chương' : 'Thêm chương'"
      :description="
        editingId
          ? 'Cập nhật thông tin của chương.'
          : 'Tạo một chương mới trong khóa học.'
      "
      :loading="saving"
      max-width="max-w-2xl"
      @close="closeForm"
    >
      <form class="space-y-5" @submit.prevent="handleSubmit">
        <BaseAlert v-if="formMessage">
          {{ formMessage }}
        </BaseAlert>

        <BaseInput
          v-model="form.title"
          label="Tên chương"
          maxlength="255"
          required
          :disabled="saving"
        />

        <BaseInput
          v-model="form.orderIndex"
          type="number"
          min="0"
          label="Thứ tự"
          :disabled="saving"
        />

        <label class="block">
          <span class="mb-1.5 block text-sm font-medium text-app-text">
            Mô tả
          </span>

          <textarea
            v-model="form.description"
            rows="3"
            maxlength="10000"
            :disabled="saving"
            class="w-full resize-y rounded-control border border-app-border bg-app-surface px-3 py-2.5 text-sm text-app-text outline-none transition focus:border-secondary focus:ring-2 focus:ring-secondary/20"
            placeholder="Mô tả nội dung của chương..."
          />
        </label>

        <label class="block">
          <span class="mb-1.5 block text-sm font-medium text-app-text">
            Mục tiêu học tập
          </span>

          <textarea
            v-model="form.learningObjectives"
            rows="3"
            maxlength="10000"
            :disabled="saving"
            class="w-full resize-y rounded-control border border-app-border bg-app-surface px-3 py-2.5 text-sm text-app-text outline-none transition focus:border-secondary focus:ring-2 focus:ring-secondary/20"
            placeholder="Học viên sẽ đạt được gì sau chương này?"
          />
        </label>

        <div class="flex justify-end gap-3 border-t border-app-border pt-5">
          <BaseButton
            type="button"
            variant="secondary"
            :disabled="saving"
            @click="closeForm"
          >
            Hủy
          </BaseButton>

          <BaseButton
            type="submit"
            :loading="saving"
          >
            {{ editingId ? 'Lưu thay đổi' : 'Tạo chương' }}
          </BaseButton>
        </div>
      </form>
    </BaseModal>

    <ConfirmDialog
      :open="deleteOpen"
      title="Xóa chương?"
      :description="
        deletingChapter
          ? `Chương “${deletingChapter.title}” và các chủ đề bên trong sẽ bị xóa.`
          : ''
      "
      confirm-text="Xóa chương"
      :loading="deleting"
      :error="deleteMessage"
      @close="closeDelete"
      @confirm="handleDelete"
    />
  </section>
</template>