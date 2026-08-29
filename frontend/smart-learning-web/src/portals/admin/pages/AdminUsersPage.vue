<script setup lang="ts">
import {
  computed,
  onMounted,
  reactive,
  ref,
} from 'vue'
import {
  ChevronLeft,
  ChevronRight,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  Users,
  X,
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

const firstItem = computed(() => {
  if (
    usersPage.value.pageable.totalElements === 0
  ) {
    return 0
  }

  return (
    (currentPage.value - 1) *
      usersPage.value.pageable.size +
    1
  )
})

const lastItem = computed(() => {
  return Math.min(
    currentPage.value *
      usersPage.value.pageable.size,
    usersPage.value.pageable.totalElements,
  )
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

const formatDate = (value: string) => {
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

const roleLabel = (role: UserRole) => {
  const labels: Record<UserRole, string> = {
    STUDENT: 'Học viên',
    LECTURER: 'Giảng viên',
    ADMIN: 'Quản trị viên',
  }

  return labels[role]
}

const roleClass = (role: UserRole) => {
  const classes: Record<UserRole, string> = {
    STUDENT: 'bg-blue-50 text-blue-700',
    LECTURER: 'bg-amber-50 text-amber-700',
    ADMIN: 'bg-secondary-soft text-secondary',
  }

  return classes[role]
}

const statusLabel = (status: UserStatus) => {
  const labels: Record<UserStatus, string> = {
    ACTIVE: 'Đang hoạt động',
    DELETED: 'Đã vô hiệu hóa',
  }

  return labels[status]
}

const statusClass = (status: UserStatus) => {
  return status === 'ACTIVE'
    ? 'bg-emerald-50 text-emerald-700'
    : 'bg-slate-100 text-slate-600'
}

onMounted(loadUsers)
</script>

<template>
  <section class="mx-auto max-w-[90rem] space-y-6">
    <header
      class="flex flex-col justify-between gap-4 sm:flex-row sm:items-center"
    >
      <div>
        <div
          class="mb-2 flex items-center gap-2 text-sm font-semibold text-secondary"
        >
          <Users :size="17" />
          Quản trị hệ thống
        </div>

        <h1
          class="font-heading text-2xl font-bold text-app-text sm:text-3xl"
        >
          Quản lý người dùng
        </h1>

        <p class="mt-2 text-sm text-app-text-muted">
          Tạo tài khoản, phân quyền và quản lý người dùng trong hệ thống.
        </p>
      </div>

      <BaseButton @click="openCreate">
        <template #leading>
          <Plus :size="18" />
        </template>

        Thêm người dùng
      </BaseButton>
    </header>

    <div
      v-if="errorMessage"
      role="alert"
      class="flex justify-between gap-4 rounded-control border border-danger/20 bg-danger-soft px-4 py-3 text-sm text-on-danger-soft"
    >
      <span>{{ errorMessage }}</span>

      <button
        type="button"
        aria-label="Đóng lỗi"
        @click="errorMessage = ''"
      >
        <X :size="17" />
      </button>
    </div>

    <div
      v-if="successMessage"
      role="status"
      class="flex justify-between gap-4 rounded-control border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800"
    >
      <span>{{ successMessage }}</span>

      <button
        type="button"
        aria-label="Đóng thông báo"
        @click="successMessage = ''"
      >
        <X :size="17" />
      </button>
    </div>

    <section
      class="rounded-card border border-app-border bg-app-surface p-4 shadow-card sm:p-5"
    >
      <form
        class="grid gap-4 xl:grid-cols-[minmax(220px,1.5fr)_1fr_1fr_1fr_1fr_auto]"
        @submit.prevent="applyFilters"
      >
        <div class="relative">
          <Search
            :size="17"
            class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-app-text-muted"
          />

          <input
            v-model="filters.keyword"
            type="search"
            placeholder="Tìm email hoặc họ tên..."
            aria-label="Tìm theo email hoặc họ tên"
            class="h-11 w-full rounded-control border border-app-border bg-app-surface pl-10 pr-3 text-sm outline-none focus:border-secondary focus:ring-2 focus:ring-secondary/20"
          />
        </div>

        <select
          v-model="filters.roleCode"
          aria-label="Lọc theo vai trò"
          class="h-11 rounded-control border border-app-border bg-app-surface px-3 text-sm outline-none focus:border-secondary focus:ring-2 focus:ring-secondary/20"
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

        <select
          v-model="filters.status"
          aria-label="Lọc theo trạng thái"
          class="h-11 rounded-control border border-app-border bg-app-surface px-3 text-sm outline-none focus:border-secondary focus:ring-2 focus:ring-secondary/20"
        >
          <option value="">Tất cả trạng thái</option>
          <option value="ACTIVE">Đang hoạt động</option>
          <option value="DELETED">Đã vô hiệu hóa</option>
        </select>

        <input
          v-model="filters.createdFrom"
          type="datetime-local"
          aria-label="Tạo từ thời gian"
          class="h-11 rounded-control border border-app-border bg-app-surface px-3 text-sm outline-none focus:border-secondary focus:ring-2 focus:ring-secondary/20"
        />

        <input
          v-model="filters.createdTo"
          type="datetime-local"
          aria-label="Tạo đến thời gian"
          class="h-11 rounded-control border border-app-border bg-app-surface px-3 text-sm outline-none focus:border-secondary focus:ring-2 focus:ring-secondary/20"
        />

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
            class="flex h-11 w-11 items-center justify-center rounded-control border border-app-border text-app-text-muted hover:bg-app-surface-muted"
            :disabled="loading"
            @click="resetFilters"
          >
            <RefreshCw :size="17" />
          </button>
        </div>
      </form>

      <div
        class="mt-4 flex items-center gap-2 border-t border-app-border pt-4 text-sm"
      >
        <label
          for="created-order"
          class="text-app-text-muted"
        >
          Sắp xếp ngày tạo:
        </label>

        <select
          id="created-order"
          v-model="filters.createdAtOrder"
          class="rounded-control border border-app-border bg-app-surface px-2 py-1.5 font-medium"
          @change="applyFilters"
        >
          <option value="DESC">
            Mới nhất trước
          </option>
          <option value="ASC">
            Cũ nhất trước
          </option>
        </select>
      </div>
    </section>

    <section
      class="overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card"
    >
      <header
        class="flex items-center justify-between border-b border-app-border px-5 py-4"
      >
        <div>
          <h2 class="font-heading font-bold text-app-text">
            Danh sách người dùng
          </h2>

          <p class="mt-1 text-xs text-app-text-muted">
            {{ usersPage.pageable.totalElements }} tài khoản
          </p>
        </div>

        <span
          class="rounded-pill bg-app-surface-muted px-3 py-1 text-xs font-semibold text-app-text-muted"
        >
          10 / trang
        </span>
      </header>

      <div
        v-if="loading"
        class="space-y-3 p-5"
      >
        <div
          v-for="index in 6"
          :key="index"
          class="h-14 animate-pulse rounded-control bg-app-surface-muted"
        />
      </div>

      <div
        v-else-if="usersPage.content.length === 0"
        class="px-6 py-16 text-center"
      >
        <h3 class="font-semibold text-app-text">
          Không tìm thấy người dùng
        </h3>

        <p class="mt-1 text-sm text-app-text-muted">
          Thử thay đổi bộ lọc hoặc tạo tài khoản mới.
        </p>
      </div>

      <div v-else class="overflow-x-auto">
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
              <th class="px-5 py-3 text-right">
                Thao tác
              </th>
            </tr>
          </thead>

          <tbody class="divide-y divide-app-border">
            <tr
              v-for="user in usersPage.content"
              :key="user.id"
              class="hover:bg-app-surface-muted/50"
            >
              <td class="px-5 py-4">
                <p class="font-semibold text-app-text">
                  {{ user.fullName }}
                </p>

                <p class="mt-1 text-xs text-app-text-muted">
                  {{ user.email }}
                </p>
              </td>

              <td class="px-5 py-4">
                <span
                  class="rounded-pill px-2.5 py-1 text-xs font-semibold"
                  :class="roleClass(user.role.code)"
                >
                  {{ roleLabel(user.role.code) }}
                </span>
              </td>

              <td class="px-5 py-4">
                <span
                  class="rounded-pill px-2.5 py-1 text-xs font-semibold"
                  :class="statusClass(user.status)"
                >
                  {{ statusLabel(user.status) }}
                </span>
              </td>
              <td
                class="whitespace-nowrap px-5 py-4 text-app-text-muted"
              >
                {{ formatDate(user.createdAt) }}
              </td>

              <td class="px-5 py-4">
                <div class="flex justify-end gap-1">
                  <template v-if="user.status === 'ACTIVE'">
                    <button
                      type="button"
                      :aria-label="`Sửa ${user.fullName}`"
                      class="flex h-9 w-9 items-center justify-center rounded-control text-app-text-muted hover:bg-secondary-soft hover:text-secondary"
                      @click="openEdit(user)"
                    >
                      <Pencil :size="17" />
                    </button>

                    <button
                      type="button"
                      :aria-label="`Vô hiệu hóa ${user.fullName}`"
                      class="flex h-9 w-9 items-center justify-center rounded-control text-app-text-muted hover:bg-danger-soft hover:text-danger"
                      @click="openDelete(user)"
                    >
                      <Trash2 :size="17" />
                    </button>
                  </template>

                  <span v-else class="text-xs text-app-text-muted">
                    Đã vô hiệu hóa
                  </span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <footer
        v-if="usersPage.pageable.totalElements > 0"
        class="flex flex-col gap-3 border-t border-app-border px-5 py-4 text-sm sm:flex-row sm:items-center sm:justify-between"
      >
        <p class="text-app-text-muted">
          Hiển thị
          <strong class="text-app-text">
            {{ firstItem }}–{{ lastItem }}
          </strong>
          trong {{ usersPage.pageable.totalElements }}
        </p>

        <div class="flex items-center gap-2">
          <button
            type="button"
            aria-label="Trang trước"
            class="flex h-9 w-9 items-center justify-center rounded-control border border-app-border disabled:opacity-40"
            :disabled="currentPage <= 1 || loading"
            @click="goToPage(currentPage - 1)"
          >
            <ChevronLeft :size="18" />
          </button>

          <span class="min-w-24 text-center">
            Trang {{ currentPage }} /
            {{ totalPages }}
          </span>

          <button
            type="button"
            aria-label="Trang sau"
            class="flex h-9 w-9 items-center justify-center rounded-control border border-app-border disabled:opacity-40"
            :disabled="
              currentPage >= totalPages || loading
            "
            @click="goToPage(currentPage + 1)"
          >
            <ChevronRight :size="18" />
          </button>
        </div>
      </footer>
    </section>

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