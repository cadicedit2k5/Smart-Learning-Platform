<script setup lang="ts">
import {
  RefreshCw,
  Search,
} from 'lucide-vue-next'

import type {
  UserRole,
} from '@/features/auth/role'

import type {
  UserStatus,
} from '@/features/auth/types'

import {
  BaseButton,
} from '@/shared/components'

export interface UserFilterModel {
  keyword: string
  roleCode: UserRole | ''
  status: UserStatus | ''
  createdFrom: string
  createdTo: string
  createdAtOrder: 'ASC' | 'DESC'
}

const model =
  defineModel<UserFilterModel>({
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
      <!-- Hàng 1 -->
      <div
        class="grid gap-3 lg:grid-cols-[minmax(260px,2fr)_1fr_1fr]"
      >
        <!-- Search -->
        <div class="relative">
          <Search
            :size="17"
            class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-app-text-muted"
          />

          <input
            v-model="model.keyword"
            type="search"
            placeholder="Tìm email hoặc họ tên..."
            class="h-11 w-full rounded-control border border-app-border bg-app-surface pl-10 pr-3 text-sm outline-none transition focus:border-secondary focus:ring-2 focus:ring-secondary/20"
          />
        </div>

        <!-- Role -->
        <select
          v-model="model.roleCode"
          class="h-11 rounded-control border border-app-border bg-app-surface px-3 text-sm outline-none transition focus:border-secondary focus:ring-2 focus:ring-secondary/20"
        >
          <option value="">
            Tất cả vai trò
          </option>

          <option value="STUDENT">
            Học viên
          </option>

          <option value="LECTURER">
            Giảng viên
          </option>

          <option value="ADMIN">
            Quản trị viên
          </option>
        </select>

        <!-- Status -->
        <select
          v-model="model.status"
          class="h-11 rounded-control border border-app-border bg-app-surface px-3 text-sm outline-none transition focus:border-secondary focus:ring-2 focus:ring-secondary/20"
        >
          <option value="">
            Tất cả trạng thái
          </option>

          <option value="ACTIVE">
            Đang hoạt động
          </option>

          <option value="DELETED">
            Đã vô hiệu hóa
          </option>
        </select>
      </div>

      <!-- Hàng 2 -->
      <div
        class="grid gap-3 border-t border-app-border pt-4 md:grid-cols-2 xl:grid-cols-[1fr_1fr_1fr_auto]"
      >
        <div class="space-y-1.5">
          <label
            class="text-xs font-medium text-app-text-muted"
          >
            Tạo từ
          </label>

          <input
            v-model="model.createdFrom"
            type="datetime-local"
            class="h-11 w-full rounded-control border border-app-border bg-app-surface px-3 text-sm"
          />
        </div>

        <div class="space-y-1.5">
          <label
            class="text-xs font-medium text-app-text-muted"
          >
            Tạo đến
          </label>

          <input
            v-model="model.createdTo"
            type="datetime-local"
            class="h-11 w-full rounded-control border border-app-border bg-app-surface px-3 text-sm"
          />
        </div>

        <div class="space-y-1.5">
          <label
            class="text-xs font-medium text-app-text-muted"
          >
            Sắp xếp
          </label>

          <select
            v-model="model.createdAtOrder"
            class="h-11 w-full rounded-control border border-app-border bg-app-surface px-3 text-sm"
          >
            <option value="DESC">
              Mới nhất trước
            </option>

            <option value="ASC">
              Cũ nhất trước
            </option>
          </select>
        </div>

        <div
          class="flex items-end gap-2"
        >
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
            <RefreshCw
              :size="17"
            />
          </button>
        </div>
      </div>
    </form>
  </section>
</template>