<script setup lang="ts">
import {
  onMounted,
  ref,
} from 'vue'

import type {
  AuthRole,
  Permission,
} from '@/features/auth/types'

import {
  parseApiError,
} from '@/shared/api'

import {
  BaseAlert,
  BasePageHeader,
} from '@/shared/components'

import {
  getPermissions,
  getRoles,
  updateRole,
  type AdminUpdateRoleRequest,
} from '../api/roleApi.ts'

import RoleFormModal
  from '../components/RoleFormModal.vue'

import RoleTable
  from '../components/RoleTable.vue'

const roles = ref<AuthRole[]>([])
const permissions = ref<Permission[]>([])

const loading = ref(false)
const saving = ref(false)

const selectedRole =
  ref<AuthRole | null>(null)

const modalOpen = ref(false)

const errorMessage = ref('')
const successMessage = ref('')
const modalError = ref('')

const loadData = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    const [
      rolesResult,
      permissionsResult,
    ] = await Promise.all([
      getRoles(),
      getPermissions(),
    ])

    roles.value = rolesResult

    permissions.value =
      permissionsResult
  } catch (error) {
    errorMessage.value =
      parseApiError(error).message
  } finally {
    loading.value = false
  }
}

const openRole = (
  role: AuthRole,
) => {
  selectedRole.value = role

  modalError.value = ''
  successMessage.value = ''

  modalOpen.value = true
}

const closeModal = () => {
  if (saving.value) {
    return
  }

  modalOpen.value = false
  selectedRole.value = null
  modalError.value = ''
}

const handleUpdate = async (
  id: number,
  request: AdminUpdateRoleRequest,
) => {
  saving.value = true

  modalError.value = ''
  successMessage.value = ''

  try {
    const updatedRole =
      await updateRole(
        id,
        request,
      )

    roles.value =
      roles.value.map(
        (role) =>
          role.id === updatedRole.id
            ? updatedRole
            : role,
      )

    modalOpen.value = false
    selectedRole.value = null

    successMessage.value =
      'Cập nhật vai trò và phân quyền thành công.'
  } catch (error) {
    const parsed =
      parseApiError(error)

    modalError.value =
      parsed.message
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <section
    class="mx-auto max-w-[90rem] space-y-6"
  >
    <BasePageHeader
      eyebrow="Quản trị hệ thống"
      title="Vai trò & phân quyền"
      description="Quản lý quyền truy cập của các vai trò trong hệ thống."
    />

    <BaseAlert
      v-if="errorMessage"
      variant="error"
    >
      {{ errorMessage }}
    </BaseAlert>

    <BaseAlert
      v-if="successMessage"
      variant="success"
    >
      {{ successMessage }}
    </BaseAlert>

    <RoleTable
      :roles="roles"
      :loading="loading"
      @select="openRole"
    />

    <RoleFormModal
      :open="modalOpen"
      :role="selectedRole"
      :permissions="permissions"
      :loading="saving"
      :server-message="modalError"
      @close="closeModal"
      @update="handleUpdate"
    />
  </section>
</template>