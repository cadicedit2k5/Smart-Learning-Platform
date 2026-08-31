<script setup lang="ts">
import {
  computed,
  reactive,
  watch,
} from 'vue'

import type {
  AuthRole,
  Permission,
} from '@/features/auth/types'

import {
  BaseAlert,
  BaseButton,
  BaseInput,
  BaseModal,
} from '@/shared/components'

import type {
  AdminUpdateRoleRequest,
} from '../api/roleApi'

import PermissionChecklist
  from './PermissionChecklist.vue'

const props = defineProps<{
  open: boolean
  role: AuthRole | null
  permissions: Permission[]
  loading: boolean
  serverMessage?: string
}>()

const emit = defineEmits<{
  close: []
  update: [
    id: number,
    request: AdminUpdateRoleRequest,
  ]
}>()

const form = reactive({
  name: '',
  permissionIds: [] as number[],
})

const isAdmin = computed(() => {
  return props.role?.code === 'ADMIN'
})

const title = computed(() => {
  if (!props.role) {
    return 'Chi tiết vai trò'
  }

  return isAdmin.value
    ? 'Quyền của Administrator'
    : `Cập nhật ${props.role.name}`
})

const description = computed(() => {
  return props.role?.code ?? ''
})

watch(
  () => [
    props.open,
    props.role,
  ],
  () => {
    if (!props.open || !props.role) {
      return
    }

    form.name = props.role.name

    form.permissionIds =
      props.role.permissions.map(
        (permission) => permission.id,
      )
  },
  {
    immediate: true,
  },
)

const submit = () => {
  if (
    !props.role ||
    isAdmin.value ||
    props.loading
  ) {
    return
  }

  emit(
    'update',
    props.role.id,
    {
      name: form.name.trim(),
      permissionIds: [
        ...form.permissionIds,
      ],
    },
  )
}
</script>

<template>
  <BaseModal
    :open="open"
    :title="title"
    :description="description"
    :loading="loading"
    max-width="max-w-2xl"
    @close="$emit('close')"
  >
    <BaseAlert
      v-if="isAdmin"
      variant="info"
      class="mb-5"
    >
      Vai trò Administrator được hệ thống bảo vệ.
      Bạn chỉ có thể xem các quyền hiện tại.
    </BaseAlert>

    <BaseAlert
      v-if="serverMessage"
      variant="error"
      class="mb-5"
    >
      {{ serverMessage }}
    </BaseAlert>

    <form
      id="role-update-form"
      class="space-y-6"
      @submit.prevent="submit"
    >
      <BaseInput
        v-model="form.name"
        label="Tên vai trò"
        :disabled="loading || isAdmin"
        required
      />

      <div>
        <div
          class="mb-3 flex items-center justify-between gap-4"
        >
          <div>
            <h3
              class="text-sm font-semibold text-app-text"
            >
              Quyền truy cập
            </h3>

            <p
              class="mt-1 text-xs text-app-text-muted"
            >
              Chọn các chức năng mà vai trò
              được phép truy cập.
            </p>
          </div>

          <span
            class="shrink-0 text-xs font-medium text-app-text-muted"
          >
            {{ form.permissionIds.length }}
            /
            {{ permissions.length }}
            quyền
          </span>
        </div>

        <PermissionChecklist
          v-model="form.permissionIds"
          :permissions="permissions"
          :disabled="loading || isAdmin"
        />
      </div>
    </form>

    <template #footer>
      <div
        class="flex justify-end gap-3"
      >
        <BaseButton
          variant="secondary"
          :disabled="loading"
          @click="$emit('close')"
        >
          {{ isAdmin ? 'Đóng' : 'Hủy' }}
        </BaseButton>

        <BaseButton
          v-if="!isAdmin"
          type="submit"
          form="role-update-form"
          :loading="loading"
        >
          Lưu thay đổi
        </BaseButton>
      </div>
    </template>
  </BaseModal>
</template>