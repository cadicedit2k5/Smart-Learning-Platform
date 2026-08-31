<script setup lang="ts">
import {
  Eye,
  Settings2,
  ShieldCheck,
} from 'lucide-vue-next'

import type {
  AuthRole,
} from '@/features/auth/types'

import {
  BaseBadge,
  BaseEmptyState,
} from '@/shared/components'

import {
  isAdminRole,
  roleLabel,
  roleTone,
} from '../../utils/roleUi'

defineProps<{
  roles: AuthRole[]
  loading: boolean
}>()

defineEmits<{
  select: [role: AuthRole]
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
          Vai trò hệ thống
        </h2>

        <p
          class="mt-1 text-xs text-app-text-muted"
        >
          {{ roles.length }} vai trò
        </p>
      </div>

      <ShieldCheck
        :size="20"
        class="text-secondary"
      />
    </header>

    <div
      v-if="loading"
      class="space-y-3 p-5"
    >
      <div
        v-for="index in 3"
        :key="index"
        class="h-16 animate-pulse rounded-control bg-app-surface-muted"
      />
    </div>

    <div
      v-else-if="roles.length === 0"
      class="p-5"
    >
      <BaseEmptyState
        title="Không có vai trò"
        description="Không tìm thấy cấu hình vai trò hệ thống."
      />
    </div>

    <div
      v-else
      class="overflow-x-auto"
    >
      <table
        class="w-full min-w-[760px] text-left text-sm"
      >
        <thead
          class="bg-app-surface-muted text-xs uppercase tracking-wide text-app-text-muted"
        >
          <tr>
            <th class="px-5 py-3">
              Vai trò
            </th>

            <th class="px-5 py-3">
              Quyền
            </th>

            <th class="px-5 py-3">
              Số quyền
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
            v-for="role in roles"
            :key="role.id"
            class="transition hover:bg-app-surface-muted/50"
          >
            <td class="px-5 py-4">
              <div class="space-y-1.5">
                <BaseBadge
                  :tone="roleTone(role.code)"
                >
                  {{ roleLabel(role.code) }}
                </BaseBadge>

                <p
                  class="font-mono text-xs text-app-text-muted"
                >
                  {{ role.code }}
                </p>
              </div>
            </td>

            <td class="px-5 py-4">
              <div
                class="flex flex-wrap gap-1.5"
              >
                <BaseBadge
                  v-for="permission in role.permissions.slice(
                    0,
                    3,
                  )"
                  :key="permission.id"
                  tone="neutral"
                >
                  {{ permission.code }}
                </BaseBadge>

                <BaseBadge
                  v-if="
                    role.permissions.length > 3
                  "
                  tone="secondary"
                >
                  +{{
                    role.permissions.length - 3
                  }}
                </BaseBadge>

                <span
                  v-if="
                    role.permissions.length === 0
                  "
                  class="text-app-text-muted"
                >
                  Chưa được cấp quyền
                </span>
              </div>
            </td>

            <td
              class="px-5 py-4 text-app-text-muted"
            >
              {{ role.permissions.length }}
            </td>

            <td class="px-5 py-4">
              <div class="flex justify-end">
                <button
                  type="button"
                  class="inline-flex items-center gap-2 rounded-control px-3 py-2 text-sm font-semibold text-secondary transition hover:bg-secondary-soft"
                  @click="$emit('select', role)"
                >
                  <Eye
                    v-if="
                      isAdminRole(role.code)
                    "
                    :size="16"
                  />

                  <Settings2
                    v-else
                    :size="16"
                  />

                  {{
                    isAdminRole(role.code)
                      ? 'Xem quyền'
                      : 'Cấu hình'
                  }}
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>