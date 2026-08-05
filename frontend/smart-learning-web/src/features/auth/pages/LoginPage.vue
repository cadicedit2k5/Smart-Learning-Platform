<script setup lang="ts">
import LoginForm from '../components/LoginForm.vue'
import LoginHero from '../components/LoginHero.vue'
</script>

<template>
  <main class="h-screen bg-app-bg lg:grid lg:grid-cols-2">
    <LoginHero />

    <section
      class="relative grid h-full place-items-center px-5 py-10 sm:px-8"
    >
      <div
        aria-hidden="true"
        class="absolute inset-0"
      />

      <div class="relative z-10 w-full">
        <div class="flex justify-center">
          <LoginForm />
        </div>
      </div>
    </section>
  </main>
</template>