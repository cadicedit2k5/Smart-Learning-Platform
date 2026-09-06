<script setup lang="ts">
import LoginHero from '../components/LoginHero.vue'
import RegisterForm from '../components/RegisterForm.vue'
</script>

<template>
  <main class="min-h-screen bg-app-bg lg:grid lg:grid-cols-2">
    <LoginHero />

    <section class="grid min-h-screen place-items-center px-5 py-10 sm:px-8">
      <div class="w-full">
        <div class="flex justify-center">
          <RegisterForm />
        </div>
      </div>
    </section>
  </main>
</template>