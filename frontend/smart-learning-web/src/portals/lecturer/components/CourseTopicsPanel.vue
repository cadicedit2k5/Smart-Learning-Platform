<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Pencil, Plus, Trash2, X } from 'lucide-vue-next'

import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import BaseInput from '@/shared/components/BaseInput.vue'

import {
  createTopic,
  deleteTopic,
  getTopics,
  updateTopic,
  type ContentStatus,
  type CourseTopic,
  type TopicInput,
} from '../api/contentApi'
import { useLecturerApiError } from '../composables/useLecturerApiError'

const props = defineProps<{ courseId: string; chapterId: string }>()
const { handleApiError } = useLecturerApiError()

const topics = ref<CourseTopic[]>([])
const loading = ref(true)
const saving = ref(false)
const message = ref('')
const editingId = ref('')

const emptyForm = () => ({
  title: '',
  description: '',
  orderIndex: '',
  estimatedMinutes: '',
  status: 'DRAFT' as ContentStatus,
})
const createForm = reactive(emptyForm())
const editForm = reactive(emptyForm())

const toInput = (form: ReturnType<typeof emptyForm>): TopicInput => ({
  title: form.title.trim(),
  description: form.description.trim() || undefined,
  orderIndex: form.orderIndex === '' ? undefined : Number(form.orderIndex),
  estimatedMinutes: form.estimatedMinutes === '' ? undefined : Number(form.estimatedMinutes),
  status: form.status,
})

const loadTopics = async () => {
  loading.value = true
  message.value = ''
  try {
    topics.value = await getTopics(props.courseId, props.chapterId)
  } catch (error) {
    message.value = handleApiError(error, 'Không thể tải danh sách chủ đề.').message
  } finally {
    loading.value = false
  }
}

const handleCreate = async () => {
  if (!createForm.title.trim()) {
    message.value = 'Tên chủ đề không được để trống.'
    return
  }
  saving.value = true
  message.value = ''
  try {
    await createTopic(props.courseId, props.chapterId, toInput(createForm))
    Object.assign(createForm, emptyForm())
    await loadTopics()
  } catch (error) {
    message.value = handleApiError(error, 'Không thể tạo chủ đề.').message
  } finally {
    saving.value = false
  }
}

const startEdit = (topic: CourseTopic) => {
  editingId.value = topic.id
  Object.assign(editForm, {
    title: topic.title,
    description: topic.description ?? '',
    orderIndex: String(topic.orderIndex),
    estimatedMinutes: topic.estimatedMinutes == null ? '' : String(topic.estimatedMinutes),
    status: topic.status,
  })
}

const saveEdit = async () => {
  if (!editingId.value || !editForm.title.trim()) return
  saving.value = true
  message.value = ''
  try {
    await updateTopic(props.courseId, props.chapterId, editingId.value, toInput(editForm))
    editingId.value = ''
    await loadTopics()
  } catch (error) {
    message.value = handleApiError(error, 'Không thể cập nhật chủ đề.').message
  } finally {
    saving.value = false
  }
}

const handleDelete = async (topic: CourseTopic) => {
  if (!window.confirm(`Xóa chủ đề “${topic.title}”?`)) return
  message.value = ''
  try {
    await deleteTopic(props.courseId, props.chapterId, topic.id)
    await loadTopics()
  } catch (error) {
    message.value = handleApiError(error, 'Không thể xóa chủ đề.').message
  }
}

onMounted(() => void loadTopics())
</script>

<template>
  <div class="space-y-4 border-t border-app-border bg-app-surface-muted/40 p-4 sm:p-5">
    <BaseAlert v-if="message">{{ message }}</BaseAlert>

    <form
      class="grid gap-3 rounded-control border border-app-border bg-app-surface p-4 md:grid-cols-[minmax(0,1fr)_8rem_9rem_auto]"
      @submit.prevent="handleCreate"
    >
      <BaseInput v-model="createForm.title" placeholder="Tên chủ đề" required />
      <BaseInput v-model="createForm.orderIndex" type="number" min="0" placeholder="Thứ tự" />
      <BaseInput
        v-model="createForm.estimatedMinutes"
        type="number"
        min="1"
        placeholder="Số phút"
      />
      <BaseButton type="submit" :loading="saving"
        ><template #leading><Plus :size="16" /></template>Thêm</BaseButton
      >
    </form>

    <p v-if="loading" class="py-5 text-center text-sm text-app-text-muted">Đang tải chủ đề...</p>
    <p v-else-if="topics.length === 0" class="py-5 text-center text-sm text-app-text-muted">
      Chưa có chủ đề.
    </p>

    <div
      v-for="topic in topics"
      v-else
      :key="topic.id"
      class="rounded-control border border-app-border bg-app-surface p-4"
    >
      <form v-if="editingId === topic.id" class="space-y-3" @submit.prevent="saveEdit">
        <BaseInput v-model="editForm.title" label="Tên chủ đề" required />
        <textarea
          v-model="editForm.description"
          rows="2"
          maxlength="10000"
          class="w-full rounded-control border border-app-border bg-app-surface px-3 py-2 text-sm"
          placeholder="Mô tả"
        />
        <div class="grid gap-3 sm:grid-cols-3">
          <BaseInput v-model="editForm.orderIndex" type="number" min="0" label="Thứ tự" />
          <BaseInput v-model="editForm.estimatedMinutes" type="number" min="1" label="Số phút" />
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
          <BaseButton type="submit" :loading="saving">Lưu</BaseButton>
          <BaseButton variant="secondary" @click="editingId = ''"
            ><template #leading><X :size="16" /></template>Hủy</BaseButton
          >
        </div>
      </form>
      <div v-else class="flex items-start gap-3">
        <span
          class="flex h-8 w-8 shrink-0 items-center justify-center rounded-pill bg-secondary-soft text-sm font-bold text-secondary"
          >{{ topic.orderIndex + 1 }}</span
        >
        <div class="min-w-0 flex-1">
          <p class="font-semibold text-app-text">{{ topic.title }}</p>
          <p v-if="topic.description" class="mt-1 text-sm text-app-text-muted">
            {{ topic.description }}
          </p>
          <p class="mt-2 text-xs text-app-text-muted">
            {{ topic.estimatedMinutes ? `${topic.estimatedMinutes} phút · ` : ''
            }}{{ topic.status }}
          </p>
        </div>
        <button
          type="button"
          class="rounded-control p-2 text-app-text-muted hover:bg-app-surface-muted"
          aria-label="Sửa chủ đề"
          @click="startEdit(topic)"
        >
          <Pencil :size="16" />
        </button>
        <button
          type="button"
          class="rounded-control p-2 text-danger hover:bg-danger-soft"
          aria-label="Xóa chủ đề"
          @click="handleDelete(topic)"
        >
          <Trash2 :size="16" />
        </button>
      </div>
    </div>
  </div>
</template>
