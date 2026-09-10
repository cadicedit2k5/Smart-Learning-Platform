<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ArrowLeft, Bell, ChevronRight, Pencil, Plus, Trash2 } from 'lucide-vue-next'

import {
  BaseAlert,
  BaseButton,
  BasePagination,
  ConfirmDialog,
} from '@/shared/components'
import type { PaginatedData } from '@/shared/api'
import type { CourseAnnouncement } from '@/shared/announcement/types'
import { formatDateTime } from '@/shared/utils/date'

import {
  createAnnouncement,
  deleteAnnouncement,
  getAnnouncements,
  updateAnnouncement,
  type AnnouncementInput,
} from '../api/announcementApi'
import { useLecturerApiError } from '../composables/useLecturerApiError'
import AnnouncementFormModal from './AnnouncementFormModal.vue'

const props = defineProps<{
  courseId: string
}>()

const { handleApiError } = useLecturerApiError()

const announcementsPage = ref<PaginatedData<CourseAnnouncement>>({
  content: [],
  pageable: {
    page: 1,
    size: 0,
    totalElements: 0,
    totalPages: 0,
  },
})

const page = ref(1)
const selectedAnnouncement = ref<CourseAnnouncement | null>(null)

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

const announcements = computed(() => announcementsPage.value.content)
const totalPages = computed(() => announcementsPage.value.pageable.totalPages)

const load = async () => {
  loading.value = true
  message.value = ''

  try {
    announcementsPage.value = await getAnnouncements(props.courseId, page.value)
  } catch (error) {
    message.value = handleApiError(error, 'Không thể tải thông báo.').message
  } finally {
    loading.value = false
  }
}

const goToPage = (newPage: number) => {
  if (newPage < 1 || newPage > totalPages.value || newPage === page.value) return

  page.value = newPage
  void load()
}

const openDetail = (announcement: CourseAnnouncement) => {
  selectedAnnouncement.value = announcement
  successMessage.value = ''
}

const backToList = () => {
  selectedAnnouncement.value = null
  message.value = ''
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

const closeForm = () => {
  if (saving.value) return

  formOpen.value = false
  editing.value = null
  formMessage.value = ''
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

      const index = announcementsPage.value.content.findIndex(
        (item) => item.id === updated.id,
      )

      if (index >= 0) {
        announcementsPage.value.content[index] = updated
      }

      if (selectedAnnouncement.value?.id === updated.id) {
        selectedAnnouncement.value = updated
      }

      successMessage.value = 'Đã cập nhật thông báo.'
    } else {
      await createAnnouncement(props.courseId, input)
      page.value = 1
      await load()
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

const closeDelete = () => {
  if (deleting.value) return

  deleteOpen.value = false
  deletingAnnouncement.value = null
}

const handleDelete = async () => {
  if (!deletingAnnouncement.value) return

  const deletedId = deletingAnnouncement.value.id

  deleting.value = true
  message.value = ''
  successMessage.value = ''

  try {
    await deleteAnnouncement(props.courseId, deletedId)

    deleteOpen.value = false
    deletingAnnouncement.value = null

    if (selectedAnnouncement.value?.id === deletedId) {
      selectedAnnouncement.value = null
    }

    await load()

    if (announcements.value.length === 0 && page.value > 1) {
      page.value -= 1
      await load()
    }

    successMessage.value = 'Đã xóa thông báo.'
  } catch (error) {
    message.value = handleApiError(error, 'Không thể xóa thông báo.').message
  } finally {
    deleting.value = false
  }
}

const reset = () => {
  page.value = 1
  selectedAnnouncement.value = null
  formOpen.value = false
  editing.value = null
  deleteOpen.value = false
  deletingAnnouncement.value = null
  message.value = ''
  successMessage.value = ''
  void load()
}

watch(() => props.courseId, reset)

onMounted(() => void load())
</script>

<template>
  <section class="space-y-5">
    <BaseAlert v-if="message">
      {{ message }}
    </BaseAlert>

    <BaseAlert v-if="successMessage" variant="ai">
      {{ successMessage }}
    </BaseAlert>

    <template v-if="!selectedAnnouncement">
      <header class="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 class="font-heading text-xl font-bold text-app-text">
            Thông báo
          </h2>

          <p class="mt-1 text-sm text-app-text-muted">
            Cập nhật những thông tin quan trọng cho học viên trong khóa học.
          </p>
        </div>

        <BaseButton @click="openCreate">
          <template #leading>
            <Plus :size="17" />
          </template>
          Tạo thông báo
        </BaseButton>
      </header>

      <div v-if="loading" class="space-y-3">
        <div
          v-for="index in 4"
          :key="index"
          class="h-28 animate-pulse rounded-card bg-app-surface-muted"
        />
      </div>

      <div
        v-else-if="announcementsPage.pageable.totalElements === 0"
        class="rounded-card border border-dashed border-app-border bg-app-surface px-6 py-12 text-center"
      >
        <Bell :size="36" class="mx-auto text-app-text-muted/40" />

        <h3 class="mt-3 font-heading text-lg font-bold text-app-text">
          Chưa có thông báo
        </h3>

        <p class="mt-1 text-sm text-app-text-muted">
          Hãy đăng thông báo đầu tiên cho học viên.
        </p>

        <BaseButton class="mt-5" @click="openCreate">
          <template #leading>
            <Plus :size="17" />
          </template>
          Tạo thông báo
        </BaseButton>
      </div>

      <template v-else>
        <div class="overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card">
          <button
            v-for="announcement in announcements"
            :key="announcement.id"
            type="button"
            class="group flex w-full items-start gap-4 border-b border-app-border px-5 py-5 text-left transition last:border-b-0 hover:bg-app-surface-muted/40"
            @click="openDetail(announcement)"
          >
            <span
              class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-secondary-soft text-secondary"
            >
              <Bell :size="18" />
            </span>

            <div class="min-w-0 flex-1">
              <div class="flex flex-wrap items-center justify-between gap-2">
                <h3 class="font-heading font-bold text-app-text">
                  {{ announcement.title }}
                </h3>

                <span class="text-xs text-app-text-muted">
                  {{ formatDateTime(announcement.createdAt) }}
                </span>
              </div>

              <p class="mt-2 line-clamp-2 whitespace-pre-line text-sm leading-6 text-app-text-muted">
                {{ announcement.content }}
              </p>
            </div>

            <ChevronRight
              :size="18"
              class="mt-2 shrink-0 text-app-text-muted transition group-hover:translate-x-1 group-hover:text-secondary"
            />
          </button>
        </div>

        <div class="rounded-card border border-app-border bg-app-surface p-4">
          <BasePagination
            :page="page"
            :total-pages="totalPages"
            :total-elements="announcementsPage.pageable.totalElements"
            @previous="goToPage(page - 1)"
            @next="goToPage(page + 1)"
          />
        </div>
      </template>
    </template>

    <template v-else>
      <button
        type="button"
        class="inline-flex items-center gap-2 text-sm font-semibold text-app-text-muted transition hover:text-secondary"
        @click="backToList"
      >
        <ArrowLeft :size="16" />
        Danh sách thông báo
      </button>

      <article class="rounded-card border border-app-border bg-app-surface p-6 shadow-card">
        <div class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div class="min-w-0">
            <div class="flex items-center gap-2 text-secondary">
              <Bell :size="18" />
              <span class="text-sm font-semibold">Thông báo khóa học</span>
            </div>

            <h2 class="mt-3 font-heading text-2xl font-bold text-app-text">
              {{ selectedAnnouncement.title }}
            </h2>

            <p class="mt-2 text-sm text-app-text-muted">
              Đăng lúc {{ formatDateTime(selectedAnnouncement.createdAt) }}
            </p>

            <p
              v-if="selectedAnnouncement.updatedAt !== selectedAnnouncement.createdAt"
              class="mt-1 text-xs text-app-text-muted"
            >
              Cập nhật {{ formatDateTime(selectedAnnouncement.updatedAt) }}
            </p>
          </div>

          <div class="flex shrink-0 gap-2">
            <BaseButton
              variant="secondary"
              @click="openEdit(selectedAnnouncement)"
            >
              <template #leading>
                <Pencil :size="16" />
              </template>
              Chỉnh sửa
            </BaseButton>

            <button
              type="button"
              class="inline-flex h-11 items-center gap-2 rounded-control px-4 text-sm font-semibold text-danger transition hover:bg-danger-soft"
              @click="openDelete(selectedAnnouncement)"
            >
              <Trash2 :size="16" />
              Xóa
            </button>
          </div>
        </div>

        <div class="my-6 border-t border-app-border" />

        <p class="whitespace-pre-wrap text-sm leading-7 text-app-text">
          {{ selectedAnnouncement.content }}
        </p>
      </article>
    </template>

    <AnnouncementFormModal
      :open="formOpen"
      :announcement="editing"
      :loading="saving"
      :server-message="formMessage"
      @close="closeForm"
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
      @close="closeDelete"
      @confirm="handleDelete"
    />
  </section>
</template>