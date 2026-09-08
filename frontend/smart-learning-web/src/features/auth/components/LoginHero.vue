<script setup lang="ts">
import { GraduationCap } from 'lucide-vue-next'

import authBrain from '@/assets/images/auth-brain.png'
</script>

<template>
  <section
    class="relative hidden overflow-hidden border-r border-app-border bg-app-surface lg:flex lg:flex-col"
  >
    <div
      aria-hidden="true"
      class="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_12%_14%,rgba(167,243,208,0.42),transparent_28%),radial-gradient(circle_at_82%_80%,rgba(233,221,255,0.55),transparent_32%)]"
    />

    <header class="relative z-10 px-10 py-8 xl:px-14">
      <RouterLink
        to="/"
        class="inline-flex items-center gap-2 text-sm font-semibold text-app-text"
      >
        <span class="flex h-8 w-8 items-center justify-center rounded-control bg-primary text-on-primary">
          <GraduationCap :size="17" :stroke-width="2" />
        </span>

        <span>Smart Learning</span>
      </RouterLink>
    </header>

    <div class="relative z-10 flex flex-1 flex-col justify-center px-10 pb-8 xl:px-14">
      <div class="max-w-xl">
        <h1
          class="max-w-lg font-heading text-4xl font-bold leading-tight tracking-tight text-app-text xl:text-5xl"
        >
          Học tập cùng trợ lý AI của bạn.
        </h1>

        <p class="mt-5 max-w-lg text-sm leading-6 text-app-text-muted">
          Truy cập các khóa học, khám phá tài liệu học tập, củng cố kiến thức và nhận hỗ trợ
          từ AI Tutor dựa trên nội dung khóa học.
        </p>
      </div>

      <div class="relative mt-8 flex justify-center">
        <div
          aria-hidden="true"
          class="absolute bottom-12 h-44 w-80 rounded-full bg-ai/10 blur-3xl"
        />

        <img
          :src="authBrain"
          alt=""
          class="relative z-10 max-h-[390px] w-full max-w-xl object-contain drop-shadow-[0_24px_32px_rgba(0,38,23,0.16)]"
        />
      </div>
    </div>
  </section>
</template>