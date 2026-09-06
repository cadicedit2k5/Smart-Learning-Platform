<script setup lang="ts">
import { ArrowRight, Eye, EyeOff, GraduationCap, LockKeyhole, Mail } from 'lucide-vue-next'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/features/auth/stores'
import { getPortalHomeRoute } from '@/portals/routeHelpers'
import { parseApiError } from '@/shared/api'
import { BaseAlert, BaseButton, BaseCard, BaseInput } from '@/shared/components'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  email: '',
  password: '',
})

const errors = reactive({
  email: '',
  password: '',
  general: '',
})

const showPassword = ref(false)
const isSubmitting = ref(false)

const clearErrors = () => {
  errors.email = ''
  errors.password = ''
  errors.general = ''
}

const handleSubmit = async () => {
  clearErrors()

  if (!form.email.trim()) errors.email = 'Vui lòng nhập email.'
  if (!form.password) errors.password = 'Vui lòng nhập mật khẩu.'
  if (errors.email || errors.password) return

  isSubmitting.value = true

  try {
    const user = await authStore.login({
      email: form.email.trim(),
      password: form.password,
    })

    await router.replace(getPortalHomeRoute(user.role.code))
  } catch (error) {
    const apiError = parseApiError(error)

    for (const detail of apiError.errors) {
      if (detail.field === 'email') errors.email = detail.message
      if (detail.field === 'password') errors.password = detail.message
    }

    if (!errors.email && !errors.password) errors.general = apiError.message
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="w-full max-w-[25rem]">
    <RouterLink
      to="/login"
      class="mb-8 inline-flex items-center gap-2 text-sm font-semibold text-app-text lg:hidden"
    >
      <span class="flex h-8 w-8 items-center justify-center rounded-control bg-primary text-on-primary">
        <GraduationCap :size="17" :stroke-width="2" />
      </span>

      <span>Smart Learning</span>
    </RouterLink>

    <BaseCard class="shadow-overlay">
      <header class="text-center">
        <h1 class="font-heading text-2xl font-bold tracking-tight text-app-text">
          Chào mừng bạn trở lại
        </h1>

        <p class="mt-2 text-sm text-app-text-muted">
          Đăng nhập để tiếp tục học tập cùng Smart Learning.
        </p>
      </header>

      <form class="mt-8 space-y-5" novalidate @submit.prevent="handleSubmit">
        <BaseInput
          v-model="form.email"
          label="Email"
          type="email"
          autocomplete="username"
          placeholder="Nhập địa chỉ email"
          :error="errors.email"
          required
        >
          <template #leading>
            <Mail :size="17" :stroke-width="1.8" />
          </template>
        </BaseInput>

        <BaseInput
          v-model="form.password"
          label="Mật khẩu"
          :type="showPassword ? 'text' : 'password'"
          autocomplete="current-password"
          placeholder="Nhập mật khẩu"
          minlength="8"
          maxlength="36"
          :error="errors.password"
          required
        >
          <template #leading>
            <LockKeyhole :size="17" :stroke-width="1.8" />
          </template>

          <template #trailing>
            <button
              type="button"
              :aria-label="showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
              class="flex h-8 w-8 items-center justify-center rounded-control text-app-text-muted transition hover:bg-app-surface-muted hover:text-app-text"
              @click="showPassword = !showPassword"
            >
              <EyeOff v-if="showPassword" :size="17" />
              <Eye v-else :size="17" />
            </button>
          </template>
        </BaseInput>

        <BaseAlert v-if="errors.general" variant="error">
          {{ errors.general }}
        </BaseAlert>

        <BaseButton type="submit" block :loading="isSubmitting">
          <span>Đăng nhập</span>

          <template #trailing>
            <ArrowRight :size="16" />
          </template>
        </BaseButton>
      </form>

      <footer class="mt-8 border-t border-app-border pt-6 text-center">
        <p class="text-sm text-app-text-muted">
          Chưa có tài khoản?
          <RouterLink to="/register" class="font-semibold text-secondary hover:underline">
            Đăng ký
          </RouterLink>
        </p>
      </footer>
    </BaseCard>
  </div>
</template>