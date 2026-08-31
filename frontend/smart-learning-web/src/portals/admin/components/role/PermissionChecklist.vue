<script setup lang="ts">
import type {
  Permission,
} from '@/features/auth/types'

const selected = defineModel<number[]>({required: true})

defineProps<{
  permissions: Permission[]
  disabled?: boolean
}>()

const togglePermission = (permissionId: number) => {
  if (selected.value.includes(permissionId)) {
    selected.value = selected.value.filter((id) => id !== permissionId)

    return
  }

  selected.value = [...selected.value, permissionId]
}
</script>

<template>
  <div class="grid gap-3">
    <label
      v-for="permission in permissions"
      :key="permission.id"
      class="flex items-start gap-3 rounded-control border p-4 transition"
      :class="[
        selected.includes(permission.id)
          ? 'border-secondary/30 bg-secondary-soft/40'
          : 'border-app-border bg-app-surface',
        disabled
          ? 'cursor-not-allowed opacity-70'
          : 'cursor-pointer hover:border-secondary/40',
      ]"
    >
      <input
        type="checkbox"
        class="mt-1 h-4 w-4 accent-secondary"
        :checked="
          selected.includes(permission.id)
        "
        :disabled="disabled"
        @change="
          togglePermission(permission.id)
        "
      />

      <span class="min-w-0">
        <span
          class="block text-sm font-semibold text-app-text"
        >
          {{ permission.description }}
        </span>

        <span
          class="mt-1 block font-mono text-xs text-app-text-muted"
        >
          {{ permission.code }}
        </span>
      </span>
    </label>
  </div>
</template>