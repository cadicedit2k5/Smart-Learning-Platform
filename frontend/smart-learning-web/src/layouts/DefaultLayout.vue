<script setup lang="ts">
import { ref } from 'vue'
import { useCurrentPortal } from './composables/useCurrentPortals.ts'
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
    class="min-h-screen bg-[#F8FAFC]">
    <AppSidebar
    :open="sidebarOpen" 
    :navigation="portal.navigation"
    @close="closeSidebar" />

    <AppTopbar @toggle-sidebar="toggleSidebar" />

    <main class="min-h-screen pt-16 lg:pl-[248px]">
      <div class="p-4 sm:p-6 lg:p-8">
        <RouterView />
      </div>
    </main>
  </div>
</template>
