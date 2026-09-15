<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { Camera, X } from 'lucide-vue-next'

import { parseApiError } from '@/shared/api'
import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import BaseInput from '@/shared/components/BaseInput.vue'

import { useAuthStore } from '../stores'
import type { UpdateCurrentUserRequest } from '../types'

const authStore = useAuthStore()
const user = computed(() => authStore.user)

const form = reactive({ fullName: '', email: '', password: '', confirmPassword: '' })
const errors = reactive({ fullName: '', email: '', password: '', confirmPassword: '', avatar: '', general: '' })

const avatar = ref<File>()
const avatarPreview = ref('')
const avatarLoadFailed = ref(false)
const avatarInputKey = ref(0)
const saving = ref(false)
const successMessage = ref('')

const initials = computed(() => {
  const name = user.value?.fullName?.trim()
  if (!name) return 'U'
  return name.split(/\s+/).slice(-2).map((part) => part[0]).join('').toUpperCase()
})

const displayedAvatar = computed(() => {
  if (avatarPreview.value) return avatarPreview.value
  if (!avatarLoadFailed.value && user.value?.avatar) return user.value.avatar
  return ''
})

const hasChanges = computed(() => {
  if (!user.value) return false
  return form.fullName.trim() !== user.value.fullName ||
    form.email.trim().toLowerCase() !== user.value.email.toLowerCase() ||
    Boolean(form.password || form.confirmPassword || avatar.value)
})

watch(() => authStore.user, (currentUser) => {
  if (!currentUser) return
  form.fullName = currentUser.fullName
  form.email = currentUser.email
  avatarLoadFailed.value = false
}, { immediate: true })

const clearErrors = () => {
  errors.fullName = ''
  errors.email = ''
  errors.password = ''
  errors.confirmPassword = ''
  errors.avatar = ''
  errors.general = ''
}

const clearAvatarPreview = () => {
  if (!avatarPreview.value) return
  URL.revokeObjectURL(avatarPreview.value)
  avatarPreview.value = ''
}

const handleAvatarChange = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  errors.avatar = ''
  if (!file.type.startsWith('image/')) {
    errors.avatar = 'Vui lòng chọn một tệp hình ảnh.'
    input.value = ''
    return
  }

  clearAvatarPreview()
  avatar.value = file
  avatarPreview.value = URL.createObjectURL(file)
  avatarLoadFailed.value = false
  successMessage.value = ''
}

const removeSelectedAvatar = () => {
  avatar.value = undefined
  clearAvatarPreview()
  avatarInputKey.value += 1
}

const validate = () => {
  clearErrors()
  const fullName = form.fullName.trim()
  const email = form.email.trim()

  if (!fullName) errors.fullName = 'Vui lòng nhập họ và tên.'
  else if (fullName.length > 100) errors.fullName = 'Họ tên không được vượt quá 100 ký tự.'

  if (!email) errors.email = 'Vui lòng nhập email.'

  if (form.password) {
    if (form.password.length < 8 || form.password.length > 36) errors.password = 'Mật khẩu phải có từ 8 đến 36 ký tự.'
    if (!form.confirmPassword) errors.confirmPassword = 'Vui lòng xác nhận mật khẩu mới.'
    else if (form.password !== form.confirmPassword) errors.confirmPassword = 'Mật khẩu xác nhận không khớp.'
  } else if (form.confirmPassword) {
    errors.password = 'Vui lòng nhập mật khẩu mới.'
  }

  return !errors.fullName && !errors.email && !errors.password && !errors.confirmPassword && !errors.avatar
}

const handleSubmit = async () => {
  if (!user.value || saving.value || !validate()) return

  const fullName = form.fullName.trim()
  const email = form.email.trim()
  const request: UpdateCurrentUserRequest = {}

  if (fullName !== user.value.fullName) request.fullName = fullName
  if (email.toLowerCase() !== user.value.email.toLowerCase()) request.email = email
  if (form.password) request.password = form.password
  if (avatar.value) request.avatar = avatar.value

  if (Object.keys(request).length === 0) return

  saving.value = true
  successMessage.value = ''
  errors.general = ''

  try {
    const updatedUser = await authStore.updateProfile(request)
    form.fullName = updatedUser.fullName
    form.email = updatedUser.email
    form.password = ''
    form.confirmPassword = ''
    avatar.value = undefined
    clearAvatarPreview()
    avatarInputKey.value += 1
    avatarLoadFailed.value = false
    successMessage.value = 'Thông tin cá nhân đã được cập nhật.'
  } catch (error) {
    const parsed = parseApiError(error)

    for (const detail of parsed.errors) {
      if (detail.field === 'fullName') errors.fullName = detail.message
      if (detail.field === 'email') errors.email = detail.message
      if (detail.field === 'password') errors.password = detail.message
      if (detail.field === 'avatar') errors.avatar = detail.message
    }

    if (parsed.statusCode === 409) {
      errors.email = 'Email này đã được sử dụng.'
      return
    }

    if (!errors.fullName && !errors.email && !errors.password && !errors.avatar) errors.general = parsed.message
  } finally {
    saving.value = false
  }
}

onBeforeUnmount(clearAvatarPreview)
</script>

<template>
  <section class="mx-auto max-w-[68rem]">
    <header class="max-w-2xl">
      <h1 class="mt-1 font-heading text-3xl font-bold tracking-tight text-app-text">Hồ sơ cá nhân</h1>
      <p class="mt-2 text-sm leading-6 text-app-text-muted">Quản lý thông tin được sử dụng để nhận diện và bảo vệ tài khoản của bạn.</p>
    </header>

    <div class="mt-7 space-y-4">
      <BaseAlert v-if="successMessage" variant="success">
        <div class="flex items-center justify-between gap-4">
          <span>{{ successMessage }}</span>
          <button type="button" aria-label="Đóng thông báo" class="shrink-0 opacity-70 transition hover:opacity-100" @click="successMessage = ''">
            <X :size="17" />
          </button>
        </div>
      </BaseAlert>

      <BaseAlert v-if="errors.general" variant="error">{{ errors.general }}</BaseAlert>
    </div>

    <form class="mt-8 grid gap-8 lg:grid-cols-[15rem_minmax(0,1fr)] lg:gap-12" @submit.prevent="handleSubmit">
      <aside class="lg:sticky lg:top-24 lg:self-start">
        <div class="flex items-center gap-4 lg:block">
          <div class="relative h-24 w-24 shrink-0 lg:h-28 lg:w-28">
            <div class="flex h-full w-full items-center justify-center overflow-hidden rounded-full bg-secondary-soft font-heading text-2xl font-bold text-secondary ring-1 ring-app-border">
              <img v-if="displayedAvatar" :src="displayedAvatar" :alt="`Ảnh đại diện của ${user?.fullName || 'người dùng'}`" class="h-full w-full object-cover" @error="avatarLoadFailed = true" />
              <span v-else aria-hidden="true">{{ initials }}</span>
            </div>

            <label for="profile-avatar" title="Thay ảnh đại diện" class="absolute bottom-0 right-0 flex h-9 w-9 cursor-pointer items-center justify-center rounded-full border-2 border-app-bg bg-primary text-white transition hover:bg-primary-hover">
              <Camera :size="16" />
              <span class="sr-only">Thay ảnh đại diện</span>
            </label>

            <input :key="avatarInputKey" id="profile-avatar" type="file" accept="image/*" class="sr-only" :disabled="saving" @change="handleAvatarChange" />
          </div>

          <div class="min-w-0 lg:mt-4">
            <h2 class="truncate font-heading text-lg font-bold text-app-text">{{ user?.fullName }}</h2>
            <p class="mt-0.5 truncate text-sm text-app-text-muted">{{ user?.email }}</p>
            <span v-if="user?.role.name" class="mt-2 inline-block text-xs font-semibold uppercase tracking-wide text-app-text-muted">{{ user.role.name }}</span>
          </div>
        </div>
      </aside>

      <div class="min-w-0">
        <section>
          <div class="max-w-xl">
            <h2 class="font-heading text-xl font-bold text-app-text">Thông tin cá nhân</h2>
            <p class="mt-1 text-sm leading-6 text-app-text-muted">Tên và email được sử dụng để nhận diện tài khoản trong hệ thống.</p>
          </div>

          <div class="mt-6 grid gap-5 sm:grid-cols-2">
            <BaseInput v-model="form.fullName" label="Họ và tên" autocomplete="name" maxlength="100" :error="errors.fullName" :disabled="saving" required />
            <BaseInput v-model="form.email" label="Email" type="email" autocomplete="email" :error="errors.email" :disabled="saving" required />
          </div>
        </section>

        <section class="mt-10 border-t border-app-border pt-8">
          <div class="max-w-xl">
            <h2 class="font-heading text-xl font-bold text-app-text">Bảo mật</h2>
            <p class="mt-1 text-sm leading-6 text-app-text-muted">Chỉ nhập mật khẩu mới khi bạn muốn thay đổi mật khẩu hiện tại.</p>
          </div>

          <div class="mt-6 grid gap-5 sm:grid-cols-2">
            <BaseInput v-model="form.password" label="Mật khẩu mới" type="password" autocomplete="new-password" minlength="8" maxlength="36" :error="errors.password" :disabled="saving" />
            <BaseInput v-model="form.confirmPassword" label="Xác nhận mật khẩu" type="password" autocomplete="new-password" minlength="8" maxlength="36" :error="errors.confirmPassword" :disabled="saving" />
          </div>

          <p class="mt-3 text-xs leading-5 text-app-text-muted">Mật khẩu mới phải có từ 8 đến 36 ký tự.</p>
        </section>

        <footer class="mt-10 flex items-center justify-end border-t border-app-border pt-6">
          <BaseButton type="submit" :loading="saving" :disabled="!hasChanges || saving">Lưu thay đổi</BaseButton>
        </footer>
      </div>
    </form>
  </section>
</template>