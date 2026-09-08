<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ChevronDown, ChevronRight, Pencil, Plus, Trash2, X } from 'lucide-vue-next'

import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import BaseInput from '@/shared/components/BaseInput.vue'
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

const props = defineProps<{ courseId: string }>()
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
const message = ref('')
const formMessage = ref('')
const expandedId = ref('')
const formOpen = ref(false)
const editingId = ref<string | null>(null)

const form = reactive(emptyForm())

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
    message.value = handleApiError(error, 'Không thể tải danh sách chương.').message
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  editingId.value = null
  formMessage.value = ''
  Object.assign(form, emptyForm())
  formOpen.value = true
}

const openEdit = (chapter: CourseChapter) => {
  editingId.value = chapter.id
  formMessage.value = ''

  Object.assign(form, {
    title: chapter.title,
    description: chapter.description ?? '',
    learningObjectives: chapter.learningObjectives ?? '',
    orderIndex: chapter.orderIndex == null ? '' : String(chapter.orderIndex),
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

  const isEditing = editingId.value !== null

  saving.value = true
  formMessage.value = ''

  try {
    if (isEditing) {
      await updateChapter(props.courseId, editingId.value!, toInput())
    } else {
      await createChapter(props.courseId, toInput())
    }

    formOpen.value = false
    editingId.value = null
    Object.assign(form, emptyForm())

    await loadChapters()
  } catch (error) {
    formMessage.value = handleApiError(
      error,
      isEditing ? 'Không thể cập nhật chương.' : 'Không thể tạo chương.',
    ).message
  } finally {
    saving.value = false
  }
}

const handleDelete = async (chapter: CourseChapter) => {
  if (!window.confirm(`Xóa chương “${chapter.title}” và các chủ đề bên trong?`)) return

  message.value = ''

  try {
    await deleteChapter(props.courseId, chapter.id)

    if (expandedId.value === chapter.id) {
      expandedId.value = ''
    }

    await loadChapters()
  } catch (error) {
    message.value = handleApiError(error, 'Không thể xóa chương.').message
  }
}

onMounted(() => void loadChapters())
</script>

<template>
  <section class="space-y-5">
    <header
      class="flex flex-col gap-5 rounded-panel border border-app-border bg-app-surface p-6 shadow-card sm:flex-row sm:items-center sm:justify-between"
    >
      <div>
        <div class="flex items-center gap-3">
          <div
            class="flex h-11 w-11 items-center justify-center rounded-card bg-secondary-soft text-secondary"
          >
            <Layers3 :size="22" />
          </div>

          <div>
            <h2
              class="font-heading text-xl font-bold text-app-text"
            >
              Nội dung khóa học
            </h2>

            <p
              class="mt-1 text-sm text-app-text-muted"
            >
              Xây dựng chương, chủ đề và nội dung bài học.
            </p>
          </div>
        </div>
      </div>

      <BaseButton @click="openCreate">
        <template #leading>
          <Plus :size="17" />
        </template>

        Thêm chương
      </BaseButton>
    </header>

    <BaseAlert v-if="message">{{ message }}</BaseAlert>

    <div v-if="loading" class="space-y-3">
      <div
        v-for="index in 3"
        :key="index"
        class="h-24 animate-pulse rounded-card bg-app-surface-muted"
      />
    </div>

    <div
      v-else-if="chapters.length === 0"
      class="rounded-card border border-dashed border-app-border p-12 text-center"
    >
      <p class="text-sm font-medium text-app-text">Chưa có nội dung</p>
      <p class="mt-1 text-sm text-app-text-muted">
        Thêm chương đầu tiên để bắt đầu xây dựng nội dung khóa học.
      </p>
    </div>

    <div v-else class="space-y-3">
      <article
        v-for="chapter in chapters"
        :key="chapter.id"
        class="overflow-hidden rounded-panel border border-app-border bg-app-surface shadow-card transition hover:border-secondary/30"
      >
        <div class="flex items-start gap-3 p-5">
          <button
            type="button"
            class="mt-0.5 rounded-control p-1 text-app-text-muted hover:bg-app-surface-muted"
            :aria-label="expandedId === chapter.id ? 'Đóng chủ đề' : 'Mở chủ đề'"
            @click="expandedId = expandedId === chapter.id ? '' : chapter.id"
          >
            <ChevronDown v-if="expandedId === chapter.id" :size="20" />
            <ChevronRight v-else :size="20" />
          </button>

          <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-center gap-2">
              <span
                class="rounded-pill bg-secondary-soft px-2.5 py-1 text-xs font-semibold text-secondary"
              >
                Chương {{ chapter.orderIndex + 1 }}
              </span>

              <h3
                class="font-heading text-lg font-bold text-app-text"
              >
                {{ chapter.title }}
              </h3>
            </div>

            <p v-if="chapter.description" class="mt-2 text-sm leading-6 text-app-text-muted">
              {{ chapter.description }}
            </p>

            <p v-if="chapter.learningObjectives" class="mt-2 text-sm leading-6 text-app-text">
              <span class="font-semibold">Mục tiêu:</span>
              {{ chapter.learningObjectives }}
            </p>
          </div>

          <div class="flex shrink-0 items-center">
            <button
              type="button"
              class="rounded-control p-2 text-app-text-muted hover:bg-app-surface-muted"
              aria-label="Sửa chương"
              @click="openEdit(chapter)"
            >
              <Pencil :size="17" />
            </button>

            <button
              type="button"
              class="rounded-control p-2 text-danger hover:bg-danger-soft"
              aria-label="Xóa chương"
              @click="handleDelete(chapter)"
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
  </section>

  <Teleport to="body">
    <div
      v-if="formOpen"
      class="fixed inset-0 z-[80] flex items-center justify-center bg-slate-950/40 p-4"
      @mousedown.self="closeForm"
    >
      <form
        class="w-full max-w-2xl rounded-card bg-app-surface p-6 shadow-overlay"
        @submit.prevent="handleSubmit"
      >
        <div class="flex items-center justify-between gap-4">
          <div>
            <h2 class="font-heading text-xl font-bold text-app-text">
              {{ editingId ? 'Chỉnh sửa chương' : 'Thêm chương' }}
            </h2>

            <p class="mt-1 text-sm text-app-text-muted">
              {{
                editingId
                  ? 'Cập nhật thông tin của chương.'
                  : 'Thêm một chương mới vào khóa học.'
              }}
            </p>
          </div>

          <button
            type="button"
            aria-label="Đóng"
            class="rounded-control p-2 text-app-text-muted hover:bg-app-surface-muted"
            :disabled="saving"
            @click="closeForm"
          >
            <X :size="19" />
          </button>
        </div>

        <BaseAlert v-if="formMessage" class="mt-4">
          {{ formMessage }}
        </BaseAlert>

        <div class="mt-5 space-y-4">
          <BaseInput
            v-model="form.title"
            label="Tên chương"
            maxlength="255"
            required
          />

          <div class="grid gap-4 sm:grid-cols-2">
            <BaseInput
              v-model="form.orderIndex"
              type="number"
              min="0"
              label="Thứ tự"
            />
          </div>

          <label class="block">
            <span class="mb-1.5 block text-sm font-medium text-app-text">
              Mô tả
            </span>

            <textarea
              v-model="form.description"
              rows="3"
              maxlength="10000"
              class="w-full resize-y rounded-control border border-app-border bg-app-surface px-3 py-2 text-sm text-app-text outline-none focus:border-secondary focus:ring-2 focus:ring-secondary/20"
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
              class="w-full resize-y rounded-control border border-app-border bg-app-surface px-3 py-2 text-sm text-app-text outline-none focus:border-secondary focus:ring-2 focus:ring-secondary/20"
              placeholder="Học viên sẽ đạt được gì sau chương này?"
            />
          </label>
        </div>

        <div class="mt-6 flex justify-end gap-2 border-t border-app-border pt-5">
          <BaseButton variant="secondary" type="button" :disabled="saving" @click="closeForm">
            Hủy
          </BaseButton>

          <BaseButton type="submit" :loading="saving">
            {{ editingId ? 'Lưu thay đổi' : 'Tạo chương' }}
          </BaseButton>
        </div>
      </form>
    </div>
  </Teleport>
</template>