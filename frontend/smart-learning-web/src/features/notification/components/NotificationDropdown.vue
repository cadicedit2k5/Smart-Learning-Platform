<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Bell, CheckCheck, ClipboardList, GraduationCap, Megaphone } from 'lucide-vue-next'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/features/auth/stores'
import { formatDateTime } from '@/shared/utils/date'

import {
  getNotifications,
  getUnreadCount,
  markAllNotificationsAsRead,
  markNotificationAsRead,
} from '../api'

import type { Notification, NotificationType } from '../types'

const router = useRouter()
const authStore = useAuthStore()

const open = ref(false)
const loading = ref(false)
const notifications = ref<Notification[]>([])
const unreadCount = ref(0)

const load = async () => {
  loading.value = true

  try {
    const [items, count] = await Promise.all([
      getNotifications(),
      getUnreadCount(),
    ])

    notifications.value = items
    unreadCount.value = count
  } finally {
    loading.value = false
  }
}

const toggle = async () => {
  open.value = !open.value

  if (open.value) {
    await load()
  }
}

const iconFor = (type: NotificationType) => {
  if (type === 'ANNOUNCEMENT_CREATED') return Megaphone
  if (type === 'ASSIGNMENT_CREATED' || type === 'ASSIGNMENT_GRADED') return ClipboardList
  return GraduationCap
}

const handleNotification = async (notification: Notification) => {
  if (!notification.read) {
    await markNotificationAsRead(notification.id)
    notification.read = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }

  open.value = false

  if (!notification.courseId) return

  if (authStore.user?.role.code === 'STUDENT') {
    await router.push({
      name: 'student-course-detail',
      params: { courseId: notification.courseId },
    })
  } else if (authStore.user?.role.code === 'LECTURER') {
    await router.push({
      name: 'lecturer-course-detail',
      params: { courseId: notification.courseId },
    })
  }
}

const markAll = async () => {
  if (unreadCount.value === 0) return

  await markAllNotificationsAsRead()

  notifications.value = notifications.value.map((item) => ({
    ...item,
    read: true,
  }))

  unreadCount.value = 0
}

onMounted(() => void load())
</script>

<template>
  <div class="relative">
    <button
      type="button"
      class="relative flex h-10 w-10 items-center justify-center rounded-control text-app-text-muted transition hover:bg-app-surface-muted hover:text-app-text"
      aria-label="Thông báo"
      @click="toggle"
    >
      <Bell :size="20" />

      <span
        v-if="unreadCount > 0"
        class="absolute right-0 top-0 flex min-h-4 min-w-4 items-center justify-center rounded-full bg-danger px-1 text-[10px] font-bold text-white"
      >
        {{ unreadCount > 99 ? '99+' : unreadCount }}
      </span>
    </button>

    <div
      v-if="open"
      class="absolute right-0 top-12 z-50 w-[22rem] overflow-hidden rounded-card border border-app-border bg-app-surface shadow-overlay sm:w-96"
    >
      <header class="flex items-center justify-between border-b border-app-border px-4 py-3">
        <div>
          <h2 class="font-heading font-bold text-app-text">
            Thông báo
          </h2>

          <p class="mt-0.5 text-xs text-app-text-muted">
            {{ unreadCount }} thông báo chưa đọc
          </p>
        </div>

        <button
          v-if="unreadCount > 0"
          type="button"
          class="inline-flex items-center gap-1.5 text-xs font-semibold text-secondary hover:underline"
          @click="markAll"
        >
          <CheckCheck :size="14" />
          Đọc tất cả
        </button>
      </header>

      <div v-if="loading" class="space-y-2 p-4">
        <div
          v-for="index in 4"
          :key="index"
          class="h-16 animate-pulse rounded-control bg-app-surface-muted"
        />
      </div>

      <div
        v-else-if="notifications.length === 0"
        class="px-5 py-10 text-center"
      >
        <Bell :size="30" class="mx-auto text-app-text-muted/40" />

        <p class="mt-3 text-sm font-semibold text-app-text">
          Chưa có thông báo
        </p>
      </div>

      <div v-else class="max-h-[28rem] overflow-y-auto">
        <button
          v-for="notification in notifications"
          :key="notification.id"
          type="button"
          class="flex w-full gap-3 border-b border-app-border px-4 py-3 text-left transition last:border-b-0 hover:bg-app-surface-muted"
          :class="{ 'bg-secondary-soft/40': !notification.read }"
          @click="handleNotification(notification)"
        >
          <span
            class="mt-1 flex h-9 w-9 shrink-0 items-center justify-center rounded-full"
            :class="notification.read
              ? 'bg-app-surface-muted text-app-text-muted'
              : 'bg-secondary-soft text-secondary'"
          >
            <component :is="iconFor(notification.type)" :size="17" />
          </span>

          <div class="min-w-0 flex-1">
            <div class="flex items-start gap-2">
              <p class="flex-1 text-sm font-semibold text-app-text">
                {{ notification.title }}
              </p>

              <span
                v-if="!notification.read"
                class="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-secondary"
              />
            </div>

            <p class="mt-1 line-clamp-2 text-xs leading-5 text-app-text-muted">
              {{ notification.message }}
            </p>

            <p class="mt-1.5 text-[11px] text-app-text-muted">
              {{ formatDateTime(notification.createdAt) }}
            </p>
          </div>
        </button>
      </div>
    </div>
  </div>
</template>