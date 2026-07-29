<script setup lang="ts">
import { ref } from 'vue'
import { Bell, CircleHelp, Grid3X3, Menu, Search, Sparkles } from 'lucide-vue-next'

const emit = defineEmits<{
  toggleSidebar: []
}>()

const searchKeyword = ref('')

const handleSearch = () => {
  const keyword = searchKeyword.value.trim()

  if (!keyword) {
    return
  }

  console.log('Search:', keyword)
}
</script>

<template>
  <header
    class="fixed left-0 right-0 top-0 z-30 h-16 border-b border-slate-200 bg-white/95 backdrop-blur lg:left-[248px]"
  >
    <div class="flex h-full items-center gap-3 px-4 sm:px-6">
      <!-- Mobile sidebar button -->
      <button
        type="button"
        aria-label="Open sidebar"
        class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg text-slate-600 transition hover:bg-slate-100 lg:hidden"
        @click="emit('toggleSidebar')"
      >
        <Menu :size="21" />
      </button>

      <!-- Search -->
      <form class="relative min-w-0 flex-1 lg:max-w-[600px]" @submit.prevent="handleSearch">
        <Search
          :size="17"
          stroke-width="1.8"
          class="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400"
        />

        <input
          v-model="searchKeyword"
          type="search"
          placeholder="Search students, courses, or insights..."
          class="h-10 w-full rounded-xl border border-transparent bg-slate-50 pl-10 pr-4 text-[13px] text-slate-800 outline-none transition placeholder:text-slate-400 hover:bg-slate-100 focus:border-violet-200 focus:bg-white focus:ring-4 focus:ring-violet-100/70"
        />
      </form>

      <div class="ml-auto flex shrink-0 items-center gap-1 sm:gap-2">
        <!-- Notifications -->
        <button
          type="button"
          aria-label="Notifications"
          class="relative flex h-10 w-10 items-center justify-center rounded-xl text-slate-500 transition hover:bg-slate-100 hover:text-slate-800"
        >
          <Bell :size="18" stroke-width="1.8" />

          <span
            class="absolute right-[9px] top-[8px] h-1.5 w-1.5 rounded-full bg-red-500 ring-2 ring-white"
          />
        </button>

        <!-- Help -->
        <button
          type="button"
          aria-label="Help"
          class="hidden h-10 w-10 items-center justify-center rounded-xl text-slate-500 transition hover:bg-slate-100 hover:text-slate-800 sm:flex"
        >
          <CircleHelp :size="18" stroke-width="1.8" />
        </button>

        <!-- Applications -->
        <button
          type="button"
          aria-label="Applications"
          class="hidden h-10 w-10 items-center justify-center rounded-xl text-slate-500 transition hover:bg-slate-100 hover:text-slate-800 md:flex"
        >
          <Grid3X3 :size="18" stroke-width="1.8" />
        </button>

        <div class="mx-1 hidden h-6 w-px bg-slate-200 sm:block" />

        <!-- AI Assistant -->
        <RouterLink
          to="/lecturer/ai-assistant"
          class="flex h-10 items-center justify-center gap-2 rounded-xl bg-violet-600 px-3 text-xs font-semibold text-white shadow-sm shadow-violet-200 transition hover:bg-violet-700 sm:px-4"
        >
          <Sparkles :size="15" stroke-width="2.2" />

          <span class="hidden sm:inline">AI Assistant</span>
        </RouterLink>

        <!-- User profile -->
        <button
          type="button"
          aria-label="Open user menu"
          class="ml-1 flex items-center gap-2 rounded-xl p-1.5 transition hover:bg-slate-100"
        >
          <div class="relative">
            <img
              src="https://i.pravatar.cc/100?img=12"
              alt="Lecturer avatar"
              class="h-8 w-8 rounded-full object-cover ring-2 ring-white"
            />

            <span
              class="absolute bottom-0 right-0 h-2.5 w-2.5 rounded-full border-2 border-white bg-emerald-500"
            />
          </div>
        </button>
      </div>
    </div>
  </header>
</template>
