<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Bell, Pencil, Plus, Trash2 } from 'lucide-vue-next'

import {
  BaseAlert,
  BaseButton,
  ConfirmDialog,
} from '@/shared/components'

import type { CourseAnnouncement } from '@/shared/announcement/types'
import { formatDateTime } from '@/shared/utils/date'

import {
  createAnnouncement,
  deleteAnnouncement,
  getAnnouncements,
  updateAnnouncement,
  type AnnouncementInput,
} from '../api/announcementApi'

import AnnouncementFormModal from './AnnouncementFormModal.vue'
import { useLecturerApiError } from '../composables/useLecturerApiError'

const props = defineProps<{
  courseId: string
}>()

const { handleApiError } = useLecturerApiError()

const announcements = ref<CourseAnnouncement[]>([])
const loading = ref(true)
const saving = ref(false)
const deleting = ref(false)

const message = ref('')
const successMessage = ref('')
const formMessage = ref('')

const formOpen = ref(false)
const editing = ref<CourseAnnouncement | null>(null)

const deleteOpen = ref(false)
const deletingAnnouncement = ref<CourseAnnouncement | null>(null)

const load = async () => {
  loading.value = true
  message.value = ''

  try {
    announcements.value = await getAnnouncements(props.courseId)
  } catch (error) {
    message.value = handleApiError(error, 'Không thể tải thông báo.').message
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  editing.value = null
  formMessage.value = ''
  formOpen.value = true
}

const openEdit = (announcement: CourseAnnouncement) => {
  editing.value = announcement
  formMessage.value = ''
  formOpen.value = true
}

const submit = async (input: AnnouncementInput) => {
  saving.value = true
  formMessage.value = ''
  successMessage.value = ''

  try {
    if (editing.value) {
      const updated = await updateAnnouncement(
        props.courseId,
        editing.value.id,
        input,
      )

      const index = announcements.value.findIndex((item) => item.id === updated.id)

      if (index >= 0) {
        announcements.value[index] = updated
      }

      successMessage.value = 'Đã cập nhật thông báo.'
    } else {
      const created = await createAnnouncement(props.courseId, input)
      announcements.value.unshift(created)
      successMessage.value = 'Đã đăng thông báo.'
    }

    formOpen.value = false
    editing.value = null
  } catch (error) {
    formMessage.value = handleApiError(error, 'Không thể lưu thông báo.').message
  } finally {
    saving.value = false
  }
}

const openDelete = (announcement: CourseAnnouncement) => {
  deletingAnnouncement.value = announcement
  deleteOpen.value = true
}

const handleDelete = async () => {
  if (!deletingAnnouncement.value) return

  deleting.value = true
  message.value = ''

  try {
    await deleteAnnouncement(
      props.courseId,
      deletingAnnouncement.value.id,
    )

    announcements.value = announcements.value.filter(
      (item) => item.id !== deletingAnnouncement.value?.id,
    )

    deleteOpen.value = false
    deletingAnnouncement.value = null
    successMessage.value = 'Đã xóa thông báo.'
  } catch (error) {
    message.value = handleApiError(error, 'Không thể xóa thông báo.').message
  } finally {
    deleting.value = false
  }
}

onMounted(() => void load())
</script>

<template>
  <section class="space-y-4">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h2 class="font-heading text-xl font-bold text-app-text">
          Thông báo
        </h2>

        <p class="mt-1 text-sm text-app-text-muted">
          Cập nhật thông tin quan trọng cho học viên trong khóa học.
        </p>
      </div>

      <BaseButton @click="openCreate">
        <template #leading>
          <Plus :size="17" />
        </template>
        Tạo thông báo
      </BaseButton>
    </div>

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
        class="h-28 animate-pulse rounded-card bg-app-surface-muted"
      />
    </div>

    <div
      v-else-if="announcements.length === 0"
      class="rounded-card border border-dashed border-app-border bg-app-surface px-6 py-10 text-center"
    >
      <Bell :size="34" class="mx-auto text-app-text-muted/40" />

      <p class="mt-3 font-semibold text-app-text">
        Chưa có thông báo
      </p>

      <p class="mt-1 text-sm text-app-text-muted">
        Hãy đăng thông báo đầu tiên cho học viên.
      </p>
    </div>

    <div v-else class="space-y-3">
      <article
        v-for="announcement in announcements"
        :key="announcement.id"
        class="rounded-card border border-app-border bg-app-surface p-5 shadow-card"
      >
        <div class="flex items-start justify-between gap-4">
          <div class="min-w-0">
            <h3 class="font-heading text-lg font-bold text-app-text">
              {{ announcement.title }}
            </h3>

            <p class="mt-1 text-xs text-app-text-muted">
              {{ formatDateTime(announcement.createdAt) }}
            </p>
          </div>

          <div class="flex shrink-0 gap-1">
            <button
              type="button"
              class="rounded-control p-2 text-app-text-muted hover:bg-app-surface-muted hover:text-secondary"
              aria-label="Chỉnh sửa thông báo"
              @click="openEdit(announcement)"
            >
              <Pencil :size="16" />
            </button>

            <button
              type="button"
              class="rounded-control p-2 text-danger hover:bg-danger-soft"
              aria-label="Xóa thông báo"
              @click="openDelete(announcement)"
            >
              <Trash2 :size="16" />
            </button>
          </div>
        </div>

        <p class="mt-4 whitespace-pre-wrap text-sm leading-7 text-app-text-muted">
          {{ announcement.content }}
        </p>
      </article>
    </div>

    <AnnouncementFormModal
      :open="formOpen"
      :announcement="editing"
      :loading="saving"
      :server-message="formMessage"
      @close="formOpen = false"
      @submit="submit"
    />

    <ConfirmDialog
      :open="deleteOpen"
      title="Xóa thông báo?"
      :description="
        deletingAnnouncement
          ? `Bạn có chắc muốn xóa “${deletingAnnouncement.title}”?`
          : ''
      "
      confirm-text="Xóa thông báo"
      :loading="deleting"
      @close="!deleting && (deleteOpen = false)"
      @confirm="handleDelete"
    />
  </section>
</template>