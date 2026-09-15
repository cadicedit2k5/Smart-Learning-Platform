<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  ArrowLeft,
  Bell,
  ChevronRight,
  Megaphone,
  Pencil,
  Plus,
  Trash2,
} from 'lucide-vue-next'

import {
  BaseAlert,
  BaseButton,
  BasePagination,
  ConfirmDialog,
} from '@/shared/components'
import type { PaginatedData } from '@/shared/api'
import type { CourseAnnouncement } from '@/shared/announcement/types'
import {
  parseStoredRichText,
  RichTextViewer,
  storedRichTextToPlainText,
} from '@/shared/rich-text'
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

const props = defineProps<{ courseId: string }>()

const { handleApiError } = useLecturerApiError()

const announcementsPage = ref<PaginatedData<CourseAnnouncement>>({
  content: [],
  pageable: { page: 1, size: 0, totalElements: 0, totalPages: 0 },
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
const selectedContent = computed(() =>
  parseStoredRichText(selectedAnnouncement.value?.content),
)

const preview = (value: string | null) =>
  storedRichTextToPlainText(value) || 'Thông báo chưa có nội dung.'

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
      const updated = await updateAnnouncement(props.courseId, editing.value.id, input)
      const index = announcementsPage.value.content.findIndex((item) => item.id === updated.id)
      if (index >= 0) announcementsPage.value.content[index] = updated
      if (selectedAnnouncement.value?.id === updated.id) selectedAnnouncement.value = updated
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
  <section class="space-y-6">
    <BaseAlert v-if="message">{{ message }}</BaseAlert>
    <BaseAlert v-if="successMessage" variant="success">{{ successMessage }}</BaseAlert>

    <template v-if="!selectedAnnouncement">
      <header class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 class="mt-1 font-heading text-2xl font-bold text-app-text">
            Thông báo
          </h2>

          <p class="mt-1 text-sm text-app-text-muted">
            Chia sẻ cập nhật và những thông tin quan trọng đến học viên.
          </p>
        </div>

        <BaseButton @click="openCreate">
          <template #leading><Plus :size="17" /></template>
          Tạo thông báo
        </BaseButton>
      </header>

      <div v-if="loading" class="space-y-3">
        <div
          v-for="index in 4"
          :key="index"
          class="h-32 animate-pulse rounded-[1.1rem] bg-app-surface-muted"
        />
      </div>

      <div
        v-else-if="announcementsPage.pageable.totalElements === 0"
        class="rounded-[1.25rem] border border-dashed border-app-border bg-app-surface px-6 py-14 text-center"
      >
        <div class="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-secondary-soft text-secondary">
          <Megaphone :size="25" />
        </div>

        <h3 class="mt-4 font-heading text-lg font-bold text-app-text">
          Chưa có thông báo
        </h3>

        <p class="mt-1 text-sm text-app-text-muted">
          Đăng thông báo đầu tiên để cập nhật thông tin cho lớp học.
        </p>

        <BaseButton class="mt-5" @click="openCreate">
          <template #leading><Plus :size="17" /></template>
          Tạo thông báo
        </BaseButton>
      </div>

      <template v-else>
        <div class="space-y-3">
          <button
            v-for="announcement in announcements"
            :key="announcement.id"
            type="button"
            class="group grid w-full gap-4 rounded-[1.1rem] border border-app-border bg-app-surface p-5 text-left shadow-card transition hover:border-secondary/30 hover:shadow-md sm:grid-cols-[auto_minmax(0,1fr)_auto]"
            @click="openDetail(announcement)"
          >
            <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-secondary-soft text-secondary">
              <Bell :size="19" />
            </div>

            <div class="min-w-0">
              <div class="flex flex-wrap items-center gap-x-3 gap-y-1">
                <h3 class="font-heading text-base font-bold text-app-text">
                  {{ announcement.title }}
                </h3>

                <span class="text-xs text-app-text-muted">
                  {{ formatDateTime(announcement.createdAt) }}
                </span>
              </div>

              <p class="mt-2 line-clamp-2 max-w-4xl text-sm leading-6 text-app-text-muted">
                {{ preview(announcement.content) }}
              </p>
            </div>

            <ChevronRight
              :size="19"
              class="hidden self-center text-app-text-muted transition group-hover:translate-x-1 group-hover:text-secondary sm:block"
            />
          </button>
        </div>

        <BasePagination
          :page="page"
          :total-pages="totalPages"
          :total-elements="announcementsPage.pageable.totalElements"
          @previous="goToPage(page - 1)"
          @next="goToPage(page + 1)"
        />
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

      <article class="overflow-hidden rounded-[1.35rem] border border-app-border bg-app-surface shadow-card">
        <header class="border-b border-app-border bg-gradient-to-br from-white to-secondary-soft/35 px-6 py-6 sm:px-8">
          <div class="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
            <div class="min-w-0">
              <div class="flex items-center gap-2 text-secondary">
                <Bell :size="17" />
                <span class="text-xs font-bold uppercase tracking-[0.12em]">
                  Thông báo khóa học
                </span>
              </div>

              <h2 class="mt-3 max-w-4xl font-heading text-2xl font-bold leading-tight text-app-text sm:text-3xl">
                {{ selectedAnnouncement.title }}
              </h2>

              <p class="mt-2 text-sm text-app-text-muted">
                {{ formatDateTime(selectedAnnouncement.createdAt) }}
                <template v-if="selectedAnnouncement.updatedAt !== selectedAnnouncement.createdAt">
                  · Đã chỉnh sửa
                </template>
              </p>
            </div>

            <div class="flex shrink-0 gap-2">
              <BaseButton variant="secondary" @click="openEdit(selectedAnnouncement)">
                <template #leading><Pencil :size="16" /></template>
                Chỉnh sửa
              </BaseButton>

              <button
                type="button"
                title="Xóa thông báo"
                class="flex h-11 w-11 items-center justify-center rounded-control border border-app-border text-app-text-muted transition hover:border-danger/30 hover:bg-danger-soft hover:text-danger"
                @click="openDelete(selectedAnnouncement)"
              >
                <Trash2 :size="17" />
              </button>
            </div>
          </div>
        </header>

        <div class="px-6 py-7 sm:px-8">
          <RichTextViewer :content="selectedContent" />
        </div>
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
      :description="deletingAnnouncement ? `Bạn có chắc muốn xóa “${deletingAnnouncement.title}”?` : ''"
      confirm-text="Xóa thông báo"
      :loading="deleting"
      @close="closeDelete"
      @confirm="handleDelete"
    />
  </section>
</template>