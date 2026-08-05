<script setup lang="ts">
import LoginForm from '../components/LoginForm.vue'
import LoginHero from '../components/LoginHero.vue'
</script>

<template>
  <main
    class="min-h-screen bg-app-bg lg:grid lg:grid-cols-2"
  >
    <LoginHero />

    <section
      class="relative grid min-h-screen place-items-center overflow-hidden px-5 py-10 sm:px-8"
    >
      <!-- Subtle background -->
      <div
        aria-hidden="true"
        class="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_82%_16%,rgba(219,234,254,0.55),transparent_32%),radial-gradient(circle_at_16%_82%,rgba(233,221,255,0.42),transparent_34%)]"
      />

      <div class="relative z-10 w-full">
        <div class="flex justify-center">
          <LoginForm />
        </div>
      </div>
    </section>
  </main>
</template>