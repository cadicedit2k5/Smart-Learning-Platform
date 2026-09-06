<script setup lang="ts">
import {
  RefreshCw,
  Search,
} from 'lucide-vue-next'

import type {
  CourseStatus,
  CourseVisibility,
} from '@/shared/course'

import {
  BaseButton,
} from '@/shared/components'

export interface CourseFilterModel {
  keyword: string
  status: CourseStatus | ''
  visibility: CourseVisibility | ''
  updatedAtOrder: 'ASC' | 'DESC'
}

const model =
  defineModel<CourseFilterModel>({
    required: true,
  })

defineProps<{
  loading: boolean
}>()

defineEmits<{
  apply: []
  reset: []
}>()
</script>

<template>
  <section
    class="rounded-card border border-app-border bg-app-surface p-5 shadow-card"
  >
    <form
      class="space-y-4"
      @submit.prevent="$emit('apply')"
    >
      <div
        class="grid gap-3 lg:grid-cols-[minmax(280px,2fr)_1fr_1fr]"
      >
        <!-- Keyword -->
        <div class="relative">
          <Search
            :size="17"
            class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-app-text-muted"
          />

          <input
            v-model="model.keyword"
            type="search"
            placeholder="Tìm tên hoặc mô tả môn học..."
            class="h-11 w-full rounded-control border border-app-border bg-app-surface pl-10 pr-3 text-sm outline-none transition focus:border-secondary focus:ring-2 focus:ring-secondary/20"
          />
        </div>

        <!-- Status -->
        <select
          v-model="model.status"
          class="h-11 rounded-control border border-app-border bg-app-surface px-3 text-sm outline-none transition focus:border-secondary focus:ring-2 focus:ring-secondary/20"
        >
          <option value="">
            Tất cả trạng thái
          </option>

          <option value="DRAFT">
            Bản nháp
          </option>

          <option value="PUBLISHED">
            Đã xuất bản
          </option>

          <option value="ARCHIVED">
            Đã lưu trữ
          </option>
        </select>

        <!-- Visibility -->
        <select
          v-model="model.visibility"
          class="h-11 rounded-control border border-app-border bg-app-surface px-3 text-sm outline-none transition focus:border-secondary focus:ring-2 focus:ring-secondary/20"
        >
          <option value="">
            Tất cả phạm vi
          </option>

          <option value="PUBLIC">
            Công khai
          </option>

          <option value="PRIVATE">
            Riêng tư
          </option>

          <option value="INVITE_ONLY">
            Chỉ lời mời
          </option>
        </select>
      </div>

      <div
        class="flex flex-wrap items-end justify-between gap-3 border-t border-app-border pt-4"
      >
        <div class="space-y-1.5">
          <label
            class="text-xs font-medium text-app-text-muted"
          >
            Sắp xếp
          </label>

          <select
            v-model="model.updatedAtOrder"
            class="h-11 rounded-control border border-app-border bg-app-surface px-3 text-sm"
          >
            <option value="DESC">
              Cập nhật mới nhất
            </option>

            <option value="ASC">
              Cập nhật cũ nhất
            </option>
          </select>
        </div>

        <div class="flex gap-2">
          <BaseButton
            type="submit"
            :disabled="loading"
          >
            Lọc
          </BaseButton>

          <button
            type="button"
            aria-label="Đặt lại bộ lọc"
            class="flex h-11 w-11 items-center justify-center rounded-control border border-app-border text-app-text-muted transition hover:bg-app-surface-muted"
            :disabled="loading"
            @click="$emit('reset')"
          >
            <RefreshCw :size="17" />
          </button>
        </div>
      </div>
    </form>
  </section>
</template>