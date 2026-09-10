<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ArrowLeft, Bell, ChevronRight } from 'lucide-vue-next'

import { BaseAlert, BasePagination } from '@/shared/components'
import type { PaginatedData } from '@/shared/api'
import { getAnnouncements } from '@/shared/announcement/api'
import type { CourseAnnouncement } from '@/shared/announcement/types'
import { formatDateTime } from '@/shared/utils/date'
import { useStudentApiError } from '../composables/useStudentApiError'

const props = defineProps<{
  courseId: string
}>()

const { handleApiError } = useStudentApiError()

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
const message = ref('')

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
}

const backToList = () => {
  selectedAnnouncement.value = null
}

const reset = () => {
  page.value = 1
  selectedAnnouncement.value = null
  message.value = ''
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

    <template v-if="!selectedAnnouncement">
      <header>
        <div class="flex items-center gap-2">
          <Bell :size="20" class="text-secondary" />

          <h2 class="font-heading text-xl font-bold text-app-text">
            Thông báo
          </h2>
        </div>

        <p class="mt-1 text-sm text-app-text-muted">
          Những cập nhật và thông tin quan trọng từ giảng viên.
        </p>
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
          Giảng viên chưa đăng thông báo nào cho khóa học.
        </p>
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
        <div class="flex items-center gap-2 text-secondary">
          <Bell :size="18" />
          <span class="text-sm font-semibold">
            Thông báo khóa học
          </span>
        </div>

        <h2 class="mt-3 font-heading text-2xl font-bold text-app-text">
          {{ selectedAnnouncement.title }}
        </h2>

        <p class="mt-2 text-sm text-app-text-muted">
          Đăng lúc {{ formatDateTime(selectedAnnouncement.createdAt) }}
        </p>

        <div class="my-6 border-t border-app-border" />

        <p class="whitespace-pre-wrap text-sm leading-7 text-app-text">
          {{ selectedAnnouncement.content }}
        </p>
      </article>
    </template>
  </section>
</template>