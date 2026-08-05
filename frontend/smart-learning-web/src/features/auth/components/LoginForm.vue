<script setup lang="ts">
import {
  ArrowRight,
  Eye,
  EyeOff,
  GraduationCap,
  LockKeyhole,
  Mail,
} from 'lucide-vue-next'
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/features/auth/stores'
import { getPortalHomeRoute } from '@/portals/routeHelpers'
import { parseApiError } from '@/shared/api'
import {
  BaseAlert,
  BaseButton,
  BaseCard,
  BaseInput,
} from '@/shared/components'

const router = useRouter();
const authStore = useAuthStore();

const form = reactive({
  email: '',
  password: '',
});

const errors = reactive({
  email: '',
  password: '',
  general: '',
});

const showPassword = ref(false);
const isSubmitting = ref(false);

const clearErrors = () => {
  errors.email = '';
  errors.password = '';
  errors.general = '';
}

const handleSubmit = async () => {
  clearErrors();

  if (!form.email.trim()) {
    errors.email = 'Email is required.';
  }

  if (!form.password) {
    errors.password = 'Password is required.';
  }

  if (errors.email || errors.password) {
    return;
  }

  isSubmitting.value = true;

  try {
    const user = await authStore.login({
      email: form.email.trim(),
      password: form.password,
    });

    await router.replace(getPortalHomeRoute(user.role.code));
  } catch (error) {
    const apiError = parseApiError(error);

    for (const detail of apiError.errors) {
      if (detail.field === 'email') {
        errors.email = detail.message;
      }

      if (detail.field === 'password') {
        errors.password = detail.message;
      }
    };

    if (!errors.email && !errors.password) {
      errors.general = apiError.message;
    }
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <div class="w-full max-w-[25rem]">
    <!-- Mobile brand -->
    <RouterLink
      to="/login"
      class="mb-8 inline-flex items-center gap-2 text-sm font-semibold text-app-text lg:hidden"
    >
      <span
        class="flex h-8 w-8 items-center justify-center rounded-control bg-primary text-on-primary"
      >
        <GraduationCap
          :size="17"
          :stroke-width="2"
        />
      </span>

      <span>Smart Learning</span>
    </RouterLink>

    <BaseCard class="shadow-overlay">
      <header class="text-center">
        <h1
          class="font-heading text-2xl font-bold tracking-tight text-app-text"
        >
          Welcome back
        </h1>

        <p class="mt-2 text-sm text-app-text-muted">
          Sign in to continue to Smart Learning.
        </p>
      </header>

      <form
        class="mt-8 space-y-5"
        novalidate
        @submit.prevent="handleSubmit"
      >
        <BaseInput
          v-model="form.email"
          label="Email"
          type="email"
          autocomplete="username"
          placeholder="student@university.edu"
          :error="errors.email"
          required
        >
          <template #leading>
            <Mail
              :size="17"
              :stroke-width="1.8"
            />
          </template>
        </BaseInput>

        <BaseInput
          v-model="form.password"
          label="Password"
          :type="showPassword ? 'text' : 'password'"
          autocomplete="current-password"
          placeholder="Enter your password"
          minlength="8"
          maxlength="36"
          :error="errors.password"
          required
        >
          <template #leading>
            <LockKeyhole
              :size="17"
              :stroke-width="1.8"
            />
          </template>

          <template #trailing>
            <button
              type="button"
              :aria-label="
                showPassword
                  ? 'Hide password'
                  : 'Show password'
              "
              class="flex h-8 w-8 items-center justify-center rounded-control text-app-text-muted transition hover:bg-app-surface-muted hover:text-app-text"
              @click="showPassword = !showPassword"
            >
              <EyeOff
                v-if="showPassword"
                :size="17"
              />

              <Eye
                v-else
                :size="17"
              />
            </button>
          </template>
        </BaseInput>

        <div class="flex justify-end">
          <RouterLink
            to="/login"
            class="text-xs font-semibold text-app-text-muted transition hover:text-secondary"
          >
            Forgot password?
          </RouterLink>
        </div>

        <BaseAlert
          v-if="errors.general"
          variant="error"
        >
          {{ errors.general }}
        </BaseAlert>

        <BaseButton
          type="submit"
          block
          :loading="isSubmitting"
        >
          <span>
            Sign in
          </span>

          <template #trailing>
            <ArrowRight :size="16" />
          </template>
        </BaseButton>
      </form>

      <footer
        class="mt-8 border-t border-app-border pt-6 text-center"
      >
        <p class="text-xs text-app-text-muted">
          Having trouble signing in?

          <a
            href="mailto:support@smartlearning.com"
            class="font-semibold text-app-text transition hover:text-secondary"
          >
            Contact Support
          </a>
        </p>
      </footer>
    </BaseCard>
  </div>
</template>