<script setup lang="ts">
import {
  computed,
  onBeforeUnmount,
  reactive,
  ref,
  watch,
} from 'vue'
import {
  Camera,
  LockKeyhole,
  Mail,
  UserRound,
  X,
} from 'lucide-vue-next'

import { parseApiError } from '@/shared/api'
import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import BaseInput from '@/shared/components/BaseInput.vue'

import { useAuthStore } from '../stores'
import type { UpdateCurrentUserRequest } from '../types'

const authStore = useAuthStore()

const form = reactive({
  fullName: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const errors = reactive({
  fullName: '',
  email: '',
  password: '',
  confirmPassword: '',
  avatar: '',
  general: '',
})

const avatar = ref<File>()
const avatarPreview = ref('')

const saving = ref(false)
const successMessage = ref('')

const user = computed(() => authStore.user)

watch(
  () => authStore.user,
  (user) => {
    if (!user) return

    form.fullName = user.fullName
    form.email = user.email
  },
  { immediate: true },
)

const clearErrors = () => {
  errors.fullName = ''
  errors.email = ''
  errors.password = ''
  errors.confirmPassword = ''
  errors.avatar = ''
  errors.general = ''
}

const clearAvatarPreview = () => {
  if (avatarPreview.value) {
    URL.revokeObjectURL(avatarPreview.value)
    avatarPreview.value = ''
  }
}

const handleAvatarChange = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]

  if (!file) {
    return
  }

  errors.avatar = ''

  if (!file.type.startsWith('image/')) {
    errors.avatar = 'Vui lòng chọn một tệp hình ảnh.'
    input.value = ''
    return
  }

  clearAvatarPreview()

  avatar.value = file
  avatarPreview.value = URL.createObjectURL(file)
}

const removeSelectedAvatar = () => {
  avatar.value = undefined
  clearAvatarPreview()
}

const validate = () => {
  clearErrors()

  const fullName = form.fullName.trim()
  const email = form.email.trim()

  if (!fullName) {
    errors.fullName = 'Vui lòng nhập họ và tên.'
  } else if (fullName.length > 100) {
    errors.fullName =
      'Họ tên không được vượt quá 100 ký tự.'
  }

  if (!email) {
    errors.email = 'Vui lòng nhập email.'
  }

  if (form.password) {
    if (
      form.password.length < 8 ||
      form.password.length > 36
    ) {
      errors.password =
        'Mật khẩu phải có từ 8 đến 36 ký tự.'
    }

    if (!form.confirmPassword) {
      errors.confirmPassword =
        'Vui lòng xác nhận mật khẩu mới.'
    } else if (
      form.password !== form.confirmPassword
    ) {
      errors.confirmPassword =
        'Mật khẩu xác nhận không khớp.'
    }
  } else if (form.confirmPassword) {
    errors.password =
      'Vui lòng nhập mật khẩu mới.'
  }

  return (
    !errors.fullName &&
    !errors.email &&
    !errors.password &&
    !errors.confirmPassword &&
    !errors.avatar
  )
}

const handleSubmit = async () => {
  if (!user.value || saving.value) {
    return
  }

  if (!validate()) {
    return
  }

  const fullName = form.fullName.trim()
  const email = form.email.trim()

  const request: UpdateCurrentUserRequest = {}

  if (fullName !== user.value.fullName) {
    request.fullName = fullName
  }

  if (
    email.toLowerCase() !==
    user.value.email.toLowerCase()
  ) {
    request.email = email
  }

  if (form.password) {
    request.password = form.password
  }

  if (avatar.value) {
    request.avatar = avatar.value
  }

  if (Object.keys(request).length === 0) {
    successMessage.value =
      'Không có thông tin nào thay đổi.'
    return
  }

  saving.value = true
  successMessage.value = ''
  errors.general = ''

  try {
    const updatedUser =
      await authStore.updateProfile(request)

    form.fullName = updatedUser.fullName
    form.email = updatedUser.email
    form.password = ''
    form.confirmPassword = ''

    avatar.value = undefined

    successMessage.value =
      'Cập nhật thông tin thành công.'
  } catch (error) {
    const parsed = parseApiError(error)

    for (const detail of parsed.errors) {
      if (detail.field === 'fullName') {
        errors.fullName = detail.message
      }

      if (detail.field === 'email') {
        errors.email = detail.message
      }

      if (detail.field === 'password') {
        errors.password = detail.message
      }

      if (detail.field === 'avatar') {
        errors.avatar = detail.message
      }
    }

    if (parsed.statusCode === 409) {
      errors.email = 'Email này đã được sử dụng.'
      return
    }

    if (
      !errors.fullName &&
      !errors.email &&
      !errors.password &&
      !errors.avatar
    ) {
      errors.general = parsed.message
    }
  } finally {
    saving.value = false
  }
}

onBeforeUnmount(() => {
  clearAvatarPreview()
})
</script>

<template>
  <section class="mx-auto max-w-3xl space-y-6">
    <header>
      <div
        class="mb-2 flex items-center gap-2 text-sm font-semibold text-secondary"
      >
        <UserRound :size="17" />
        Tài khoản
      </div>

      <h1
        class="font-heading text-2xl font-bold text-app-text sm:text-3xl"
      >
        Thông tin cá nhân
      </h1>

      <p class="mt-2 text-sm text-app-text-muted">
        Cập nhật thông tin tài khoản và mật khẩu của bạn.
      </p>
    </header>

    <div
      v-if="successMessage"
      role="status"
      class="flex items-center justify-between gap-4 rounded-control border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800"
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

    <BaseAlert
      v-if="errors.general"
      variant="error"
    >
      {{ errors.general }}
    </BaseAlert>

    <form
      class="rounded-card border border-app-border bg-app-surface shadow-card"
      @submit.prevent="handleSubmit"
    >
      <section class="p-5 sm:p-6">
        <h2
          class="font-heading text-lg font-bold text-app-text"
        >
          Ảnh đại diện
        </h2>

        <p class="mt-1 text-sm text-app-text-muted">
          Chọn hình ảnh đại diện cho tài khoản của bạn.
        </p>

        <div class="mt-5 flex flex-wrap items-center gap-5">
          <div
            class="flex h-20 w-20 shrink-0 items-center justify-center overflow-hidden rounded-full bg-secondary-soft text-xl font-bold text-secondary"
          >
            <img
              v-if="avatarPreview"
              :src="avatarPreview"
              alt="Ảnh đại diện mới"
              class="h-full w-full object-cover"
            />
          </div>

          <div>
            <div class="flex flex-wrap gap-2">
              <label
                class="inline-flex h-10 cursor-pointer items-center justify-center gap-2 rounded-control border border-app-border bg-app-surface px-4 text-sm font-semibold text-app-text transition hover:bg-app-surface-muted"
              >
                <Camera :size="17" />
                Chọn ảnh

                <input
                  type="file"
                  accept="image/*"
                  class="hidden"
                  :disabled="saving"
                  @change="handleAvatarChange"
                />
              </label>

              <button
                v-if="avatar"
                type="button"
                class="h-10 rounded-control px-3 text-sm font-semibold text-app-text-muted hover:bg-app-surface-muted"
                :disabled="saving"
                @click="removeSelectedAvatar"
              >
                Bỏ chọn
              </button>
            </div>

            <p
              v-if="avatar"
              class="mt-2 max-w-sm truncate text-xs text-app-text-muted"
            >
              {{ avatar.name }}
            </p>

            <p
              v-if="errors.avatar"
              role="alert"
              class="mt-2 text-sm text-danger"
            >
              {{ errors.avatar }}
            </p>
          </div>
        </div>
      </section>

      <section
        class="border-t border-app-border p-5 sm:p-6"
      >
        <div>
          <h2
            class="font-heading text-lg font-bold text-app-text"
          >
            Thông tin tài khoản
          </h2>

          <p class="mt-1 text-sm text-app-text-muted">
            Thông tin được sử dụng để nhận diện tài khoản của bạn.
          </p>
        </div>

        <div class="mt-5 grid gap-5 sm:grid-cols-2">
          <BaseInput
            v-model="form.fullName"
            label="Họ và tên"
            autocomplete="name"
            maxlength="100"
            :error="errors.fullName"
            :disabled="saving"
            required
          >
            <template #leading>
              <UserRound :size="17" />
            </template>
          </BaseInput>

          <BaseInput
            v-model="form.email"
            label="Email"
            type="email"
            autocomplete="email"
            :error="errors.email"
            :disabled="saving"
            required
          >
            <template #leading>
              <Mail :size="17" />
            </template>
          </BaseInput>
        </div>
      </section>

      <section
        class="border-t border-app-border p-5 sm:p-6"
      >
        <div>
          <h2
            class="font-heading text-lg font-bold text-app-text"
          >
            Đổi mật khẩu
          </h2>

          <p class="mt-1 text-sm text-app-text-muted">
            Để trống nếu bạn không muốn thay đổi mật khẩu.
          </p>
        </div>

        <div class="mt-5 grid gap-5 sm:grid-cols-2">
          <BaseInput
            v-model="form.password"
            label="Mật khẩu mới"
            type="password"
            autocomplete="new-password"
            minlength="8"
            maxlength="36"
            :error="errors.password"
            :disabled="saving"
          >
            <template #leading>
              <LockKeyhole :size="17" />
            </template>
          </BaseInput>

          <BaseInput
            v-model="form.confirmPassword"
            label="Xác nhận mật khẩu"
            type="password"
            autocomplete="new-password"
            minlength="8"
            maxlength="36"
            :error="errors.confirmPassword"
            :disabled="saving"
          >
            <template #leading>
              <LockKeyhole :size="17" />
            </template>
          </BaseInput>
        </div>
      </section>

      <footer
        class="flex justify-end border-t border-app-border bg-app-surface-muted/40 px-5 py-4 sm:px-6"
      >
        <BaseButton
          type="submit"
          :loading="saving"
        >
          Lưu thay đổi
        </BaseButton>
      </footer>
    </form>
  </section>
</template>