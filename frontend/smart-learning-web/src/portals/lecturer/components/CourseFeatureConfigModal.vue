<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, watch } from 'vue'
import { Settings2, X } from 'lucide-vue-next'

import BaseButton from '@/shared/components/BaseButton.vue'
import type { CourseFeatureConfig } from '@/shared/course'

const props = withDefaults(defineProps<{
  open: boolean
  config: CourseFeatureConfig
  loading?: boolean
}>(), {
  loading: false,
})

const emit = defineEmits<{
  close: []
  save: [config: CourseFeatureConfig]
}>()

const form = reactive<CourseFeatureConfig>({ ...props.config })

const features: Array<{
  key: keyof CourseFeatureConfig
  label: string
  description: string
}> = [
  { key: 'announcements', label: 'Thông báo', description: 'Thông báo và cập nhật dành cho khóa học.' },
  { key: 'content', label: 'Bài học', description: 'Nội dung chương, bài học và tiến độ học tập.' },
  { key: 'assignments', label: 'Bài tập', description: 'Bài tập, bài nộp và chấm điểm.' },
  { key: 'documents', label: 'Tài liệu', description: 'Tài liệu được chia sẻ trong khóa học.' },
  { key: 'discussion', label: 'Thảo luận', description: 'Khu vực trao đổi giữa các thành viên.' },
  { key: 'aiTutor', label: 'AI Tutor', description: 'Trợ lý AI hỗ trợ quá trình học tập.' },
]

const changed = computed(() =>
  features.some(feature => form[feature.key] !== props.config[feature.key]),
)

watch(
  () => [props.open, props.config] as const,
  ([open]) => {
    if (open) Object.assign(form, props.config)
  },
  { deep: true },
)

const close = () => {
  if (!props.loading) emit('close')
}

const save = () => {
  if (changed.value && !props.loading) emit('save', { ...form })
}

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && props.open) close()
}

onMounted(() => window.addEventListener('keydown', handleKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', handleKeydown))
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
        class="fixed inset-0 z-[90] flex items-center justify-center bg-slate-950/50 p-4"
        @mousedown.self="close"
      >
        <section
          role="dialog"
          aria-modal="true"
          aria-labelledby="feature-settings-title"
          class="flex max-h-[calc(100vh-2rem)] w-full max-w-xl flex-col overflow-hidden rounded-panel bg-app-surface shadow-overlay"
        >
          <header class="flex items-start justify-between gap-4 border-b border-app-border px-5 py-4 sm:px-6">
            <div class="flex min-w-0 gap-3">
              <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-control bg-secondary-soft text-secondary">
                <Settings2 :size="19" />
              </div>

              <div class="min-w-0">
                <h2 id="feature-settings-title" class="font-heading text-xl font-bold text-app-text">
                  Cài đặt tính năng
                </h2>
              </div>
            </div>

            <button
              type="button"
              aria-label="Đóng"
              class="flex h-9 w-9 shrink-0 items-center justify-center rounded-control text-app-text-muted transition hover:bg-app-surface-muted hover:text-app-text"
              :disabled="loading"
              @click="close"
            >
              <X :size="19" />
            </button>
          </header>

          <div class="overflow-y-auto p-5 sm:p-6">
            <div class="space-y-2">
              <label
                v-for="feature in features"
                :key="feature.key"
                class="flex cursor-pointer items-center justify-between gap-5 rounded-card border border-app-border p-4 transition hover:border-secondary/30 hover:bg-app-surface-muted/40"
              >
                <span class="min-w-0">
                  <span class="block text-sm font-semibold text-app-text">
                    {{ feature.label }}
                  </span>

                  <span class="mt-1 block text-xs leading-5 text-app-text-muted">
                    {{ feature.description }}
                  </span>
                </span>

                <span class="flex shrink-0 items-center gap-3">
                  <span class="hidden text-xs font-medium text-app-text-muted sm:block">
                    {{ form[feature.key] ? 'Đang bật' : 'Đang tắt' }}
                  </span>

                  <span class="relative inline-flex">
                    <input
                      v-model="form[feature.key]"
                      type="checkbox"
                      role="switch"
                      :aria-label="feature.label"
                      class="peer sr-only"
                      :disabled="loading"
                    />

                    <span class="h-6 w-11 rounded-full bg-app-border transition peer-checked:bg-secondary peer-disabled:opacity-50" />
                    <span class="pointer-events-none absolute left-1 top-1 h-4 w-4 rounded-full bg-white shadow-sm transition-transform peer-checked:translate-x-5" />
                  </span>
                </span>
              </label>
            </div>

            <p class="mt-4 text-xs leading-5 text-app-text-muted">
              Khi tắt một tính năng, tab tương ứng sẽ được ẩn khỏi khóa học. Dữ liệu đã tạo trước đó không bị xóa.
            </p>
          </div>

          <footer class="flex justify-end gap-3 border-t border-app-border px-5 py-4 sm:px-6">
            <BaseButton variant="secondary" :disabled="loading" @click="close">
              Hủy
            </BaseButton>

            <BaseButton :loading="loading" :disabled="!changed" @click="save">
              Lưu thay đổi
            </BaseButton>
          </footer>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>