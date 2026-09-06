<script setup lang="ts">
import { X } from 'lucide-vue-next'

withDefaults(
  defineProps<{
    open: boolean
    title: string
    description?: string
    loading?: boolean
    maxWidth?: string
  }>(),
  {
    loading: false,
    maxWidth: 'max-w-xl',
  },
)

const emit = defineEmits<{
  close: []
}>()
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-200"
      enter-from-class="opacity-0"
      leave-active-class="transition duration-150"
      leave-to-class="opacity-0"
    >
      <div
        v-if="open"
        class="fixed inset-0 z-[70] flex items-center justify-center bg-slate-950/50 p-4"
        @mousedown.self="
          !loading && emit('close')
        "
      >
        <section
          role="dialog"
          aria-modal="true"
          class="max-h-[calc(100vh-2rem)] w-full overflow-y-auto rounded-panel bg-app-surface shadow-overlay"
          :class="maxWidth"
        >
          <header
            class="flex items-start justify-between gap-4 border-b border-app-border px-6 py-5"
          >
            <div>
              <h2
                class="font-heading text-xl font-bold text-app-text"
              >
                {{ title }}
              </h2>

              <p
                v-if="description"
                class="mt-1 text-sm leading-6 text-app-text-muted"
              >
                {{ description }}
              </p>
            </div>

            <button
              type="button"
              aria-label="Đóng"
              class="flex h-9 w-9 shrink-0 items-center justify-center rounded-control text-app-text-muted transition hover:bg-app-surface-muted"
              :disabled="loading"
              @click="emit('close')"
            >
              <X :size="20" />
            </button>
          </header>

          <main class="p-6">
            <slot />
          </main>

          <footer
            v-if="$slots.footer"
            class="border-t border-app-border px-6 py-4"
          >
            <slot name="footer" />
          </footer>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>