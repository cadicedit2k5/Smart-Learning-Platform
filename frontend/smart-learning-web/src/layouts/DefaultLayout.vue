<script setup lang="ts">
import { ref } from 'vue'

import AppSidebar from './components/AppSidebar.vue'
import AppTopbar from './components/AppTopbar.vue'
import { useCurrentPortal } from './composables/useCurrentPortal.ts'

const sidebarOpen = ref(false)
const sidebarCollapsed = ref(false)

const { portal } = useCurrentPortal()

const toggleSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value
}

const closeSidebar = () => {
  sidebarOpen.value = false
}

const toggleSidebarCollapse = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}
</script>

<template>
  <div
    v-if="portal"
    class="min-h-screen bg-app-bg"
  >
    <AppSidebar
      :open="sidebarOpen"
      :collapsed="sidebarCollapsed"
      :navigation="portal.navigation"
      :base-path="portal.basePath"
      @close="closeSidebar"
      @toggle-collapse="toggleSidebarCollapse"
    />

    <AppTopbar
      :config="portal.topbar"
      :sidebar-collapsed="sidebarCollapsed"
      @toggle-sidebar="toggleSidebar"
    />

    <main
      class="min-h-screen pt-16 transition-[padding] duration-300"
      :class="
        sidebarCollapsed
          ? 'lg:pl-sidebar-collapsed'
          : 'lg:pl-sidebar'
      "
    >
      <div class="px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
        <RouterView />
      </div>
    </main>
  </div>
</template>