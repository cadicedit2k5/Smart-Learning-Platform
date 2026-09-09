<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Bell } from 'lucide-vue-next'

import { BaseAlert } from '@/shared/components'
import type { CourseAnnouncement } from '@/shared/announcement/types'
import { formatDateTime } from '@/shared/utils/date'

import { useStudentApiError } from '../composables/useStudentApiError'
import { getAnnouncements } from '@/portals/lecturer/api/announcementApi'

const props = defineProps<{
  courseId: string
}>()

const { handleApiError } = useStudentApiError()

const announcements = ref<CourseAnnouncement[]>([])
const loading = ref(true)
const message = ref('')

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

onMounted(() => void load())
</script>

<template>
  <section class="space-y-4">
    <div class="flex items-center gap-2">
      <Bell :size="20" class="text-secondary" />

      <h2 class="font-heading text-xl font-bold text-app-text">
        Thông báo
      </h2>
    </div>

    <BaseAlert v-if="message">
      {{ message }}
    </BaseAlert>

    <div v-if="loading" class="space-y-3">
      <div
        v-for="index in 2"
        :key="index"
        class="h-24 animate-pulse rounded-card bg-app-surface-muted"
      />
    </div>

    <p
      v-else-if="announcements.length === 0"
      class="rounded-card border border-dashed border-app-border bg-app-surface px-5 py-8 text-center text-sm text-app-text-muted"
    >
      Khóa học chưa có thông báo.
    </p>

    <div v-else class="space-y-3">
      <article
        v-for="announcement in announcements"
        :key="announcement.id"
        class="rounded-card border border-app-border bg-app-surface p-5 shadow-card"
      >
        <h3 class="font-semibold text-app-text">
          {{ announcement.title }}
        </h3>

        <p class="mt-1 text-xs text-app-text-muted">
          {{ formatDateTime(announcement.createdAt) }}
        </p>

        <p class="mt-3 whitespace-pre-wrap text-sm leading-7 text-app-text-muted">
          {{ announcement.content }}
        </p>
      </article>
    </div>
  </section>
</template>