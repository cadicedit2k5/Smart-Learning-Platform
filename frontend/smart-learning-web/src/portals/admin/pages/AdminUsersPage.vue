<script setup lang="ts">
import {
  computed,
  onMounted,
  reactive,
  ref,
} from 'vue'
import {
  Plus,
} from 'lucide-vue-next'

import type { UserRole } from '@/features/auth/role'
import type { AuthUser, UserStatus } from '@/features/auth/types'
import { parseApiError } from '@/shared/api'
import BaseButton from '@/shared/components/BaseButton.vue'

import {
  createUser,
  deleteUser,
  getUsers,
  updateUser,
  type AdminCreateUserRequest,
  type AdminUpdateUserRequest,
  type UsersPage,
} from '../api/userApi'
import UserFormModal from '../components/UserFormModal.vue'
import UserTable from '../components/UserTable.vue'
import { BaseAlert, BasePageHeader, BasePagination } from '@/shared/components/index.ts'
import UserFilters from '../components/UserFilters.vue'

const usersPage = ref<UsersPage>({
  content: [],
  pageable: {
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  },
})

const currentPage = ref(1)
const loading = ref(false)
const saving = ref(false)
const deleting = ref(false)

const errorMessage = ref('')
const successMessage = ref('')
const deleteError = ref('')

const formOpen = ref(false)
const editingUser = ref<AuthUser | null>(null)
const deleteTarget = ref<AuthUser | null>(null)

const serverErrors =
  ref<Record<string, string>>({})
const serverMessage = ref('')

const filters = reactive({
  keyword: '',
  roleCode: '' as UserRole | '',
  status: '' as UserStatus | '',
  createdFrom: '',
  createdTo: '',
  createdAtOrder: 'DESC' as 'ASC' | 'DESC',
})

const totalPages = computed(() => {
  return usersPage.value.pageable.totalPages
})

const toIso = (value: string) => {
  return value
    ? new Date(value).toISOString()
    : undefined
}

let latestLoadId = 0

const loadUsers = async () => {
  const loadId = ++latestLoadId

  loading.value = true
  errorMessage.value = ''

  try {
    const result = await getUsers({
      page: currentPage.value,
      keyword: filters.keyword.trim() || undefined,
      roleCode: filters.roleCode || undefined,
      status: filters.status || undefined,
      createdFrom: toIso(filters.createdFrom),
      createdTo: toIso(filters.createdTo),
      createdAtOrder: filters.createdAtOrder,
    });

    if (loadId === latestLoadId) {
      usersPage.value = result
    }
  } catch (error) {
    if (loadId === latestLoadId) {
      errorMessage.value =
        parseApiError(error).message
    }
  } finally {
    if (loadId === latestLoadId) {
      loading.value = false
    }
  }
}

const applyFilters = () => {
  currentPage.value = 1
  void loadUsers()
}

const resetFilters = () => {
  filters.keyword = ''
  filters.roleCode = ''
  filters.status = ''
  filters.createdFrom = ''
  filters.createdTo = ''
  filters.createdAtOrder = 'DESC'

  currentPage.value = 1
  void loadUsers()
}

const goToPage = (page: number) => {
  if (
    page < 1 ||
    page > totalPages.value ||
    page === currentPage.value
  ) {
    return
  }

  currentPage.value = page
  void loadUsers()
}

const openCreate = () => {
  editingUser.value = null
  serverErrors.value = {}
  serverMessage.value = ''
  successMessage.value = ''
  formOpen.value = true
}

const openEdit = (user: AuthUser) => {
  editingUser.value = user
  serverErrors.value = {}
  serverMessage.value = ''
  successMessage.value = ''
  formOpen.value = true
}

const handleFormError = (error: unknown) => {
  const parsed = parseApiError(error)

  serverErrors.value = Object.fromEntries(
    parsed.errors
      .filter((item) => item.field)
      .map((item) => [
        item.field as string,
        item.message,
      ]),
  )

  if (parsed.statusCode === 409) {
    serverMessage.value =
      'Email này đã được sử dụng.'
    return
  }

  if (parsed.statusCode === 403) {
    serverMessage.value =
      'Bạn không có quyền quản trị người dùng.'
    return
  }

  if (parsed.statusCode === 404) {
    serverMessage.value =
      'Không tìm thấy người dùng.'
    return
  }

  serverMessage.value = parsed.message
}

const handleCreate = async (
  request: AdminCreateUserRequest,
) => {
  saving.value = true
  serverErrors.value = {}
  serverMessage.value = ''
  successMessage.value = ''

  try {
    await createUser(request)

    formOpen.value = false
    successMessage.value =
      'Tạo người dùng thành công.'

    currentPage.value = 1
    await loadUsers()
  } catch (error) {
    handleFormError(error)
  } finally {
    saving.value = false
  }
}

const handleUpdate = async (
  id: string,
  request: AdminUpdateUserRequest,
) => {
  if (Object.keys(request).length === 0) {
    formOpen.value = false
    return
  }

  saving.value = true
  serverErrors.value = {}
  serverMessage.value = ''
  successMessage.value = ''

  try {
    await updateUser(id, request)

    formOpen.value = false
    successMessage.value =
      'Cập nhật người dùng thành công.'

    await loadUsers()
  } catch (error) {
    handleFormError(error)
  } finally {
    saving.value = false
  }
}

const openDelete = (user: AuthUser) => {
  deleteTarget.value = user
  deleteError.value = ''
  successMessage.value = ''
}

const handleDelete = async () => {
  if (!deleteTarget.value || deleting.value) {
    return
  }

  const target = deleteTarget.value

  deleting.value = true
  deleteError.value = ''

  try {
    await deleteUser(target.id)

    deleteTarget.value = null
    successMessage.value =
      `Đã xóa tài khoản ${target.email}.`

    if (
      usersPage.value.content.length === 1 &&
      currentPage.value > 1
    ) {
      currentPage.value -= 1
    }

    await loadUsers()
  } catch (error) {
    const parsed = parseApiError(error)

    deleteError.value =
      parsed.statusCode === 404
        ? 'Người dùng không tồn tại hoặc đã bị xóa.'
        : parsed.message
  } finally {
    deleting.value = false
  }
}

onMounted(loadUsers)
</script>

<template>
  <section class="mx-auto max-w-[90rem] space-y-6">
    <BasePageHeader
      eyebrow="Quản trị hệ thống"
      title="Quản lý người dùng"
      description="Quản lý tài khoản, vai trò và trạng thái truy cập hệ thống."
    >
      <template #actions>
        <BaseButton @click="openCreate">
          <template #leading>
            <Plus :size="18" />
          </template>
          Thêm người dùng
        </BaseButton>
      </template>
    </BasePageHeader>

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

    <UserFilters
      v-model="filters"
      :loading="loading"
      @apply="applyFilters"
      @reset="resetFilters"
    />

    <UserTable
      :users="usersPage.content"
      :loading="loading"
      :total="
        usersPage.pageable.totalElements
      "
      @edit="openEdit"
      @disable="openDelete"
    />

    <BasePagination
      v-if="
        usersPage.pageable.totalElements > 0
      "
      :page="currentPage"
      :total-pages="totalPages"
      :total-elements="
        usersPage.pageable.totalElements
      "
      @previous="
        goToPage(currentPage - 1)
      "
      @next="
        goToPage(currentPage + 1)
      "
    />

    <UserFormModal
      :open="formOpen"
      :user="editingUser"
      :loading="saving"
      :server-errors="serverErrors"
      :server-message="serverMessage"
      @close="formOpen = false"
      @create="handleCreate"
      @update="handleUpdate"
    />

    <Teleport to="body">
      <div
        v-if="deleteTarget"
        class="fixed inset-0 z-[80] flex items-center justify-center bg-slate-950/50 p-4"
        @mousedown.self="
          !deleting && (deleteTarget = null)
        "
      >
        <section
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="delete-title"
          class="w-full max-w-md rounded-panel bg-app-surface p-6 shadow-overlay"
        >
          <h2
            id="delete-title"
            class="font-heading text-xl font-bold text-app-text"
          >
            Xóa người dùng?
          </h2>

          <p
            class="mt-2 text-sm leading-6 text-app-text-muted"
          >
            Tài khoản
            <strong class="text-app-text">
              {{ deleteTarget.email }}
            </strong>
            sẽ bị vô hiệu hóa.
          </p>

          <p
            v-if="deleteError"
            role="alert"
            class="mt-4 rounded-control bg-danger-soft p-3 text-sm text-danger"
          >
            {{ deleteError }}
          </p>

          <div class="mt-6 flex justify-end gap-3">
            <BaseButton
              variant="secondary"
              :disabled="deleting"
              @click="deleteTarget = null"
            >
              Hủy
            </BaseButton>

            <button
              type="button"
              class="inline-flex h-11 items-center justify-center rounded-control bg-danger px-4 text-sm font-semibold text-white disabled:opacity-60"
              :disabled="deleting"
              @click="handleDelete"
            >
              {{
                deleting
                  ? 'Đang xóa...'
                  : 'Xóa người dùng'
              }}
            </button>
          </div>
        </section>
      </div>
    </Teleport>
  </section>
</template>