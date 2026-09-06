<script setup lang="ts">
import {
  AlertTriangle,
} from 'lucide-vue-next'

import {
  useRouter,
} from 'vue-router'

import {
  BaseButton,
} from '@/shared/components'

withDefaults(
  defineProps<{
    code?: string | number
    title?: string
    description?: string
  }>(),
  {
    code: 404,
    title: 'Không tìm thấy trang',
    description:
      'Trang bạn đang tìm kiếm không tồn tại hoặc đã được di chuyển.',
  },
)

const router = useRouter()

const goBack = () => {
  router.back()
}
</script>

<template>
  <main
    class="flex min-h-screen items-center justify-center bg-app-bg px-4"
  >
    <section
      class="w-full max-w-md text-center"
    >
      <div
        class="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-secondary-soft text-secondary"
      >
        <AlertTriangle
          :size="30"
        />
      </div>

      <p
        class="mt-6 text-sm font-bold text-secondary"
      >
        {{ code }}
      </p>

      <h1
        class="mt-2 font-heading text-3xl font-bold text-app-text"
      >
        {{ title }}
      </h1>

      <p
        class="mt-3 text-sm leading-6 text-app-text-muted"
      >
        {{ description }}
      </p>

      <BaseButton
        class="mt-6"
        @click="goBack"
      >
        Quay lại
      </BaseButton>
    </section>
  </main>
</template>