<script setup lang="ts">
import { ref } from 'vue'
import SideBar from './SideBar.vue'
import TopBar from './TopBar.vue'

const sidebarOpen = ref(false)

const toggleSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value
}

const closeSidebar = () => {
  sidebarOpen.value = false
}
</script>

<template>
  <div class="min-h-screen bg-[#F8FAFC]">
    <SideBar
      :open="sidebarOpen"
      @close="closeSidebar"
    />

    <TopBar @toggle-sidebar="toggleSidebar" />

    <main class="min-h-screen pt-16 lg:pl-[248px]">
      <div class="p-4 sm:p-6 lg:p-8">
        <RouterView />
      </div>
    </main>
  </div>
</template>