<script setup lang="ts">
import { ref } from 'vue'
import { useCurrentPortal } from './composables/useCurrentPortal.ts'
import AppSidebar from './components/AppSidebar.vue'
import AppTopbar from './components/AppTopbar.vue'

const sidebarOpen = ref(false)

const {portal} = useCurrentPortal()

const toggleSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value
}

const closeSidebar = () => {
  sidebarOpen.value = false
}
</script>

<template>
  <div
    v-if="portal"
    class="min-h-screen bg-app-bg">
    <AppSidebar
    :open="sidebarOpen" 
    :navigation="portal.navigation"
    :base-path="portal.basePath"
    @close="closeSidebar" />

    <AppTopbar 
    :config="portal.topbar"
    @toggle-sidebar="toggleSidebar" />

    <main class="min-h-screen pt-16 lg:pl-sidebar">
      <div class="p-4 sm:p-6 lg:p-8">
        <RouterView />
      </div>
    </main>
  </div>
</template>
