<script setup lang="ts">
import {
  Pencil,
  Trash2,
} from 'lucide-vue-next'

import type {
  Course,
} from '@/shared/course'

import {
  BaseBadge,
  BaseEmptyState,
} from '@/shared/components'

import {
  formatDateTime,
} from '@/shared/utils/date'

import {
  courseStatusLabel,
  courseStatusTone,
  visibilityLabel,
} from '../../utils/courseUi'

defineProps<{
  courses: Course[]
  loading: boolean
  total: number
}>()

defineEmits<{
  edit: [course: Course]
  delete: [course: Course]
}>()
</script>

<template>
  <section
    class="overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card"
  >
    <header
      class="flex items-center justify-between border-b border-app-border px-5 py-4"
    >
      <div>
        <h2
          class="font-heading font-bold text-app-text"
        >
          Danh sách môn học
        </h2>

        <p
          class="mt-1 text-xs text-app-text-muted"
        >
          {{ total }} môn học
        </p>
      </div>
    </header>

    <!-- Loading -->
    <div
      v-if="loading"
      class="space-y-3 p-5"
    >
      <div
        v-for="index in 5"
        :key="index"
        class="h-16 animate-pulse rounded-control bg-app-surface-muted"
      />
    </div>

    <!-- Empty -->
    <div
      v-else-if="courses.length === 0"
      class="p-5"
    >
      <BaseEmptyState
        title="Không tìm thấy môn học"
        description="Thử thay đổi bộ lọc hoặc từ khóa tìm kiếm."
      />
    </div>

    <!-- Table -->
    <div
      v-else
      class="overflow-x-auto"
    >
      <table
        class="w-full min-w-[950px] text-left text-sm"
      >
        <thead
          class="bg-app-surface-muted text-xs uppercase tracking-wide text-app-text-muted"
        >
          <tr>
            <th class="px-5 py-3">
              Môn học
            </th>

            <th class="px-5 py-3">
              Cấp độ
            </th>

            <th class="px-5 py-3">
              Trạng thái
            </th>

            <th class="px-5 py-3">
              Phạm vi
            </th>

            <th class="px-5 py-3">
              Cập nhật
            </th>

            <th class="px-5 py-3 text-right">
              Thao tác
            </th>
          </tr>
        </thead>

        <tbody
          class="divide-y divide-app-border"
        >
          <tr
            v-for="course in courses"
            :key="course.id"
            class="transition hover:bg-app-surface-muted/50"
          >
            <!-- Course -->
            <td class="max-w-[350px] px-5 py-4">
              <p
                class="truncate font-semibold text-app-text"
              >
                {{ course.title }}
              </p>

              <p
                v-if="course.description"
                class="mt-1 line-clamp-1 text-xs text-app-text-muted"
              >
                {{ course.description }}
              </p>
            </td>

            <!-- Level -->
            <td
              class="px-5 py-4 text-app-text-muted"
            >
              {{ course.level || '—' }}
            </td>

            <!-- Status -->
            <td class="px-5 py-4">
              <BaseBadge
                :tone="
                  courseStatusTone(
                    course.status,
                  )
                "
              >
                {{
                  courseStatusLabel(
                    course.status,
                  )
                }}
              </BaseBadge>
            </td>

            <!-- Visibility -->
            <td class="px-5 py-4">
              {{
                visibilityLabel(
                  course.visibility,
                )
              }}
            </td>

            <!-- Updated -->
            <td
              class="whitespace-nowrap px-5 py-4 text-app-text-muted"
            >
              {{
                formatDateTime(
                  course.updatedAt,
                )
              }}
            </td>

            <!-- Actions -->
            <td class="px-5 py-4">
              <div
                class="flex justify-end gap-1"
              >
                <button
                  type="button"
                  :aria-label="
                    `Sửa ${course.title}`
                  "
                  class="flex h-9 w-9 items-center justify-center rounded-control text-app-text-muted transition hover:bg-secondary-soft hover:text-secondary"
                  @click="
                    $emit('edit', course)
                  "
                >
                  <Pencil :size="17" />
                </button>

                <button
                  type="button"
                  :aria-label="
                    `Xóa ${course.title}`
                  "
                  class="flex h-9 w-9 items-center justify-center rounded-control text-app-text-muted transition hover:bg-danger-soft hover:text-danger"
                  @click="
                    $emit('delete', course)
                  "
                >
                  <Trash2 :size="17" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>