<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Eye, EyeOff, LockKeyhole, Mail, UserRound } from 'lucide-vue-next'

import { register } from '../authApi'
import { parseApiError } from '@/shared/api'
import { BaseAlert, BaseButton, BaseCard, BaseInput } from '@/shared/components'

const router = useRouter()

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
  general: '',
})

const showPassword = ref(false)
const loading = ref(false)

const clearErrors = () => {
  errors.fullName = ''
  errors.email = ''
  errors.password = ''
  errors.confirmPassword = ''
  errors.general = ''
}

const validate = () => {
  clearErrors()

  if (!form.fullName.trim()) {
    errors.fullName = 'Vui lòng nhập họ và tên.'
  }

  if (!form.email.trim()) {
    errors.email = 'Vui lòng nhập email.'
  }

  if (!form.password) {
    errors.password = 'Vui lòng nhập mật khẩu.'
  } else if (form.password.length < 8 || form.password.length > 36) {
    errors.password = 'Mật khẩu phải có từ 8 đến 36 ký tự.'
  }

  if (!form.confirmPassword) {
    errors.confirmPassword = 'Vui lòng xác nhận mật khẩu.'
  } else if (form.password !== form.confirmPassword) {
    errors.confirmPassword = 'Mật khẩu xác nhận không khớp.'
  }

  return !errors.fullName &&
    !errors.email &&
    !errors.password &&
    !errors.confirmPassword
}

const handleSubmit = async () => {
  if (!validate()) return

  loading.value = true

  try {
    await register({
      fullName: form.fullName.trim(),
      email: form.email.trim(),
      password: form.password,
    })

    await router.replace({
      name: 'login',
      query: { registered: 'true' },
    })
  } catch (error) {
    const parsed = parseApiError(error)

    for (const detail of parsed.errors) {
      if (detail.field === 'fullName') errors.fullName = detail.message
      if (detail.field === 'email') errors.email = detail.message
      if (detail.field === 'password') errors.password = detail.message
    }

    if (!errors.fullName && !errors.email && !errors.password) {
      errors.general = parsed.message
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="w-full max-w-[27rem]">
    <BaseCard class="shadow-overlay">
      <header class="text-center">
        <h1 class="font-heading text-2xl font-bold text-app-text">
          Tạo tài khoản
        </h1>

        <p class="mt-2 text-sm text-app-text-muted">
          Đăng ký tài khoản học viên để bắt đầu học tập.
        </p>
      </header>

      <form class="mt-7 space-y-4" novalidate @submit.prevent="handleSubmit">
        <BaseInput
          v-model="form.fullName"
          label="Họ và tên"
          autocomplete="name"
          placeholder="Nguyễn Văn An"
          :error="errors.fullName"
          required
        >
          <template #leading><UserRound :size="17" /></template>
        </BaseInput>

        <BaseInput
          v-model="form.email"
          label="Email"
          type="email"
          autocomplete="email"
          placeholder="student@example.com"
          :error="errors.email"
          required
        >
          <template #leading><Mail :size="17" /></template>
        </BaseInput>

        <BaseInput
          v-model="form.password"
          label="Mật khẩu"
          :type="showPassword ? 'text' : 'password'"
          autocomplete="new-password"
          minlength="8"
          maxlength="36"
          :error="errors.password"
          required
        >
          <template #leading><LockKeyhole :size="17" /></template>

          <template #trailing>
            <button
              type="button"
              class="flex h-8 w-8 items-center justify-center text-app-text-muted"
              @click="showPassword = !showPassword"
            >
              <EyeOff v-if="showPassword" :size="17" />
              <Eye v-else :size="17" />
            </button>
          </template>
        </BaseInput>

        <BaseInput
          v-model="form.confirmPassword"
          label="Xác nhận mật khẩu"
          :type="showPassword ? 'text' : 'password'"
          autocomplete="new-password"
          :error="errors.confirmPassword"
          required
        />

        <BaseAlert v-if="errors.general" variant="error">
          {{ errors.general }}
        </BaseAlert>

        <BaseButton type="submit" block :loading="loading">
          Đăng ký
        </BaseButton>
      </form>

      <footer class="mt-6 border-t border-app-border pt-5 text-center text-sm text-app-text-muted">
        Đã có tài khoản?

        <RouterLink to="/login" class="font-semibold text-secondary">
          Đăng nhập
        </RouterLink>
      </footer>
    </BaseCard>
  </div>
</template>