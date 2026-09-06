<script setup lang="ts">
import {
  Pencil,
  UserX,
} from 'lucide-vue-next'

import type {
  AuthUser,
} from '@/features/auth/types'

import {
  BaseBadge,
  BaseEmptyState,
} from '@/shared/components'

import {
  formatDateTime,
} from '@/shared/utils/date'

import {
  roleLabel,
  roleTone,
} from '../../utils/roleUi'

defineProps<{
  users: AuthUser[]
  loading: boolean
  total: number
}>()

defineEmits<{
  edit: [user: AuthUser]
  disable: [user: AuthUser]
}>()

const initials = (name: string) => {
  return name
    .trim()
    .split(/\s+/)
    .slice(-2)
    .map((part) => part.charAt(0))
    .join('')
    .toUpperCase()
}
</script>

<template>
  <section
    class="overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card"
  >
    <!-- Header -->
    <header
      class="flex items-center justify-between border-b border-app-border px-5 py-4"
    >
      <div>
        <h2
          class="font-heading font-bold text-app-text"
        >
          Danh sách người dùng
        </h2>

        <p
          class="mt-1 text-xs text-app-text-muted"
        >
          {{ total }} tài khoản
        </p>
      </div>
    </header>

    <!-- Loading -->
    <div
      v-if="loading"
      class="space-y-3 p-5"
    >
      <div
        v-for="index in 6"
        :key="index"
        class="h-16 animate-pulse rounded-control bg-app-surface-muted"
      />
    </div>

    <!-- Empty -->
    <div
      v-else-if="users.length === 0"
      class="p-5"
    >
      <BaseEmptyState
        title="Không tìm thấy người dùng"
        description="Thử thay đổi bộ lọc hoặc kiểm tra lại từ khóa tìm kiếm."
      />
    </div>

    <!-- Table -->
    <div
      v-else
      class="overflow-x-auto"
    >
      <table
        class="w-full min-w-[850px] text-left text-sm"
      >
        <thead
          class="bg-app-surface-muted text-xs uppercase tracking-wide text-app-text-muted"
        >
          <tr>
            <th class="px-5 py-3">
              Người dùng
            </th>

            <th class="px-5 py-3">
              Vai trò
            </th>

            <th class="px-5 py-3">
              Trạng thái
            </th>

            <th class="px-5 py-3">
              Ngày tạo
            </th>

            <th
              class="px-5 py-3 text-right"
            >
              Thao tác
            </th>
          </tr>
        </thead>

        <tbody
          class="divide-y divide-app-border"
        >
          <tr
            v-for="user in users"
            :key="user.id"
            class="transition hover:bg-app-surface-muted/50"
          >
            <!-- User -->
            <td class="px-5 py-4">
              <div
                class="flex items-center gap-3"
              >
                <img
                  v-if="user.avatar"
                  :src="user.avatar"
                  :alt="user.fullName"
                  class="h-10 w-10 shrink-0 rounded-full object-cover"
                />

                <div
                  v-else
                  class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-secondary-soft text-sm font-bold text-secondary"
                >
                  {{ initials(user.fullName) }}
                </div>

                <div class="min-w-0">
                  <p
                    class="truncate font-semibold text-app-text"
                  >
                    {{ user.fullName }}
                  </p>

                  <p
                    class="mt-0.5 truncate text-xs text-app-text-muted"
                  >
                    {{ user.email }}
                  </p>
                </div>
              </div>
            </td>

            <!-- Role -->
            <td class="px-5 py-4">
              <BaseBadge
                :tone="
                  roleTone(user.role.code)
                "
              >
                {{
                  roleLabel(
                    user.role.code,
                  )
                }}
              </BaseBadge>
            </td>

            <!-- Status -->
            <td class="px-5 py-4">
              <BaseBadge
                :tone="
                  user.status === 'ACTIVE'
                    ? 'success'
                    : 'neutral'
                "
              >
                {{
                  user.status === 'ACTIVE'
                    ? 'Đang hoạt động'
                    : 'Đã vô hiệu hóa'
                }}
              </BaseBadge>
            </td>

            <!-- Created At -->
            <td
              class="whitespace-nowrap px-5 py-4 text-app-text-muted"
            >
              {{
                formatDateTime(
                  user.createdAt,
                )
              }}
            </td>

            <!-- Actions -->
            <td class="px-5 py-4">
              <div
                class="flex justify-end gap-1"
              >
                <template
                  v-if="
                    user.status === 'ACTIVE'
                  "
                >
                  <button
                    type="button"
                    :aria-label="
                      `Sửa ${user.fullName}`
                    "
                    class="flex h-9 w-9 items-center justify-center rounded-control text-app-text-muted transition hover:bg-secondary-soft hover:text-secondary"
                    @click="
                      $emit(
                        'edit',
                        user,
                      )
                    "
                  >
                    <Pencil
                      :size="17"
                    />
                  </button>

                  <button
                    type="button"
                    :aria-label="
                      `Vô hiệu hóa ${user.fullName}`
                    "
                    class="flex h-9 w-9 items-center justify-center rounded-control text-app-text-muted transition hover:bg-danger-soft hover:text-danger"
                    @click="
                      $emit(
                        'disable',
                        user,
                      )
                    "
                  >
                    <UserX
                      :size="17"
                    />
                  </button>
                </template>

                <span
                  v-else
                  class="text-xs text-app-text-muted"
                >
                  Đã vô hiệu hóa
                </span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>