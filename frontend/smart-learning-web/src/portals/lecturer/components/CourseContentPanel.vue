<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ChevronDown, ChevronRight, Layers3, Pencil, Plus, Trash2, X } from 'lucide-vue-next'

import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import BaseInput from '@/shared/components/BaseInput.vue'

import {
  createChapter,
  deleteChapter,
  getChapters,
  updateChapter,
  type ChapterInput,
  type ContentStatus,
  type CourseChapter,
} from '../api/contentApi'
import { useLecturerApiError } from '../composables/useLecturerApiError'
import CourseTopicsPanel from './CourseTopicsPanel.vue'

const props = defineProps<{ courseId: string }>()
const { handleApiError } = useLecturerApiError()

const chapters = ref<CourseChapter[]>([])
const loading = ref(true)
const saving = ref(false)
const message = ref('')
const expandedId = ref('')
const editingId = ref('')

const emptyForm = () => ({
  title: '',
  description: '',
  learningObjectives: '',
  orderIndex: '',
  status: 'DRAFT' as ContentStatus,
})
const createForm = reactive(emptyForm())
const editForm = reactive(emptyForm())

const toInput = (form: ReturnType<typeof emptyForm>): ChapterInput => ({
  title: form.title.trim(),
  description: form.description.trim() || undefined,
  learningObjectives: form.learningObjectives.trim() || undefined,
  orderIndex: form.orderIndex === '' ? undefined : Number(form.orderIndex),
  status: form.status,
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

const handleCreate = async () => {
  if (!createForm.title.trim()) {
    message.value = 'Tên chương không được để trống.'
    return
  }
  saving.value = true
  message.value = ''
  try {
    await createChapter(props.courseId, toInput(createForm))
    Object.assign(createForm, emptyForm())
    await loadChapters()
  } catch (error) {
    message.value = handleApiError(error, 'Không thể tạo chương.').message
  } finally {
    saving.value = false
  }
}

const startEdit = (chapter: CourseChapter) => {
  editingId.value = chapter.id
  Object.assign(editForm, {
    title: chapter.title,
    description: chapter.description ?? '',
    learningObjectives: chapter.learningObjectives ?? '',
    orderIndex: String(chapter.orderIndex),
    status: chapter.status,
  })
}

const saveEdit = async () => {
  if (!editingId.value || !editForm.title.trim()) return
  saving.value = true
  message.value = ''
  try {
    await updateChapter(props.courseId, editingId.value, toInput(editForm))
    editingId.value = ''
    await loadChapters()
  } catch (error) {
    message.value = handleApiError(error, 'Không thể cập nhật chương.').message
  } finally {
    saving.value = false
  }
}

const handleDelete = async (chapter: CourseChapter) => {
  if (!window.confirm(`Xóa chương “${chapter.title}” và các chủ đề bên trong?`)) return
  message.value = ''
  try {
    await deleteChapter(props.courseId, chapter.id)
    if (expandedId.value === chapter.id) expandedId.value = ''
    await loadChapters()
  } catch (error) {
    message.value = handleApiError(error, 'Không thể xóa chương.').message
  }
}

onMounted(() => void loadChapters())
</script>

<template>
  <section class="space-y-5">
    <header>
      <div class="flex items-center gap-2 text-secondary">
        <Layers3 :size="20" /><span class="text-sm font-semibold">Cấu trúc khóa học</span>
      </div>
      <h2 class="mt-2 font-heading text-xl font-bold text-app-text">Chương và chủ đề</h2>
    </header>

    <BaseAlert v-if="message">{{ message }}</BaseAlert>

    <form
      class="space-y-4 rounded-card border border-app-border bg-app-surface p-5 shadow-card"
      @submit.prevent="handleCreate"
    >
      <h3 class="font-semibold text-app-text">Thêm chương</h3>
      <div class="grid gap-4 md:grid-cols-[minmax(0,1fr)_8rem_10rem]">
        <BaseInput v-model="createForm.title" label="Tên chương" maxlength="255" required />
        <BaseInput v-model="createForm.orderIndex" type="number" min="0" label="Thứ tự" />
        <select
          v-model="createForm.status"
          class="h-11 self-end rounded-control border border-app-border bg-app-surface px-3 text-sm"
        >
          <option value="DRAFT">Bản nháp</option>
          <option value="PUBLISHED">Đã xuất bản</option>
          <option value="ARCHIVED">Đã lưu trữ</option>
        </select>
      </div>
      <textarea
        v-model="createForm.description"
        rows="2"
        maxlength="10000"
        class="w-full rounded-control border border-app-border bg-app-surface px-3 py-2 text-sm"
        placeholder="Mô tả"
      />
      <textarea
        v-model="createForm.learningObjectives"
        rows="2"
        maxlength="10000"
        class="w-full rounded-control border border-app-border bg-app-surface px-3 py-2 text-sm"
        placeholder="Mục tiêu học tập"
      />
      <BaseButton type="submit" :loading="saving"
        ><template #leading><Plus :size="17" /></template>Thêm chương</BaseButton
      >
    </form>

    <div v-if="loading" class="space-y-3">
      <div
        v-for="index in 3"
        :key="index"
        class="h-24 animate-pulse rounded-card bg-app-surface-muted"
      />
    </div>
    <div
      v-else-if="chapters.length === 0"
      class="rounded-card border border-dashed border-app-border p-12 text-center text-sm text-app-text-muted"
    >
      Chưa có chương.
    </div>

    <article
      v-for="chapter in chapters"
      v-else
      :key="chapter.id"
      class="overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card"
    >
      <form v-if="editingId === chapter.id" class="space-y-4 p-5" @submit.prevent="saveEdit">
        <BaseInput v-model="editForm.title" label="Tên chương" required />
        <textarea
          v-model="editForm.description"
          rows="2"
          maxlength="10000"
          class="w-full rounded-control border border-app-border px-3 py-2 text-sm"
          placeholder="Mô tả"
        />
        <textarea
          v-model="editForm.learningObjectives"
          rows="2"
          maxlength="10000"
          class="w-full rounded-control border border-app-border px-3 py-2 text-sm"
          placeholder="Mục tiêu học tập"
        />
        <div class="grid gap-3 sm:grid-cols-2">
          <BaseInput v-model="editForm.orderIndex" type="number" min="0" label="Thứ tự" />
          <select
            v-model="editForm.status"
            class="h-11 self-end rounded-control border border-app-border bg-app-surface px-3 text-sm"
          >
            <option value="DRAFT">Bản nháp</option>
            <option value="PUBLISHED">Đã xuất bản</option>
            <option value="ARCHIVED">Đã lưu trữ</option>
          </select>
        </div>
        <div class="flex gap-2">
          <BaseButton type="submit" :loading="saving">Lưu</BaseButton
          ><BaseButton variant="secondary" @click="editingId = ''"
            ><template #leading><X :size="16" /></template>Hủy</BaseButton
          >
        </div>
      </form>
      <template v-else>
        <div class="flex items-start gap-3 p-5">
          <button
            type="button"
            class="mt-0.5 rounded-control p-1 text-app-text-muted"
            :aria-label="expandedId === chapter.id ? 'Đóng chủ đề' : 'Mở chủ đề'"
            @click="expandedId = expandedId === chapter.id ? '' : chapter.id"
          >
            <ChevronDown v-if="expandedId === chapter.id" :size="20" /><ChevronRight
              v-else
              :size="20"
            />
          </button>
          <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-center gap-2">
              <h3 class="font-heading text-lg font-bold text-app-text">{{ chapter.title }}</h3>
              <span
                class="rounded-pill bg-app-surface-muted px-2 py-0.5 text-xs font-semibold text-app-text-muted"
                >{{ chapter.status }}</span
              >
            </div>
            <p v-if="chapter.description" class="mt-2 text-sm text-app-text-muted">
              {{ chapter.description }}
            </p>
            <p v-if="chapter.learningObjectives" class="mt-2 text-sm text-app-text">
              <strong>Mục tiêu:</strong> {{ chapter.learningObjectives }}
            </p>
          </div>
          <button
            type="button"
            class="rounded-control p-2 text-app-text-muted hover:bg-app-surface-muted"
            aria-label="Sửa chương"
            @click="startEdit(chapter)"
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
        <CourseTopicsPanel
          v-if="expandedId === chapter.id"
          :key="chapter.id"
          :course-id="courseId"
          :chapter-id="chapter.id"
        />
      </template>
    </article>
  </section>
</template>
