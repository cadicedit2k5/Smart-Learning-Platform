<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { Download, FileText, X } from 'lucide-vue-next'

const props = defineProps<{
  blob: Blob
  title: string
  fileName: string
  mimeType: string
}>()

const emit = defineEmits<{ close: [] }>()

const fileUrl = ref('')

const effectiveMimeType = computed(() => props.mimeType || props.blob.type)
const isPdf = computed(() => effectiveMimeType.value === 'application/pdf')
const isImage = computed(() => effectiveMimeType.value.startsWith('image/'))
const isText = computed(() => effectiveMimeType.value.startsWith('text/'))
const canPreview = computed(() => isPdf.value || isImage.value || isText.value)

const revokeFileUrl = () => {
  if (!fileUrl.value) return
  URL.revokeObjectURL(fileUrl.value)
  fileUrl.value = ''
}

watch(() => props.blob, blob => {
  revokeFileUrl()
  fileUrl.value = URL.createObjectURL(blob)
}, { immediate: true })

const download = () => {
  if (!fileUrl.value) return

  const link = document.createElement('a')
  link.href = fileUrl.value
  link.download = props.fileName
  link.click()
}

onBeforeUnmount(revokeFileUrl)
</script>

<template>
  <Teleport to="body">
    <div class="fixed inset-0 z-[100] flex flex-col bg-black/75">
      <header class="flex items-center justify-between gap-4 border-b border-app-border bg-app-surface px-4 py-3 sm:px-6">
        <div class="min-w-0">
          <h3 class="truncate font-semibold text-app-text">{{ title }}</h3>
          <p class="mt-0.5 truncate text-xs text-app-text-muted">{{ fileName }}</p>
        </div>

        <div class="flex shrink-0 items-center gap-2">
          <button type="button" class="rounded-control p-2 text-app-text-muted transition hover:bg-app-surface-muted hover:text-app-text" aria-label="Tải xuống" @click="download">
            <Download :size="19" />
          </button>

          <button type="button" class="rounded-control p-2 text-app-text-muted transition hover:bg-app-surface-muted hover:text-app-text" aria-label="Đóng" @click="emit('close')">
            <X :size="20" />
          </button>
        </div>
      </header>

      <main class="min-h-0 flex-1 p-3 sm:p-5">
        <iframe v-if="isPdf || isText" :src="fileUrl" class="h-full w-full rounded-card border-0 bg-white" :title="title" />

        <div v-else-if="isImage" class="flex h-full items-center justify-center overflow-auto rounded-card bg-black/20 p-4">
          <img :src="fileUrl" :alt="title" class="max-h-full max-w-full object-contain" />
        </div>

        <div v-else-if="!canPreview" class="flex h-full items-center justify-center">
          <div class="w-full max-w-md rounded-card bg-app-surface p-8 text-center shadow-card">
            <div class="mx-auto flex h-12 w-12 items-center justify-center rounded-card bg-app-surface-muted text-app-text-muted">
              <FileText :size="23" />
            </div>

            <h4 class="mt-4 font-semibold text-app-text">Không thể xem trực tiếp tài liệu này</h4>
            <p class="mt-2 text-sm leading-6 text-app-text-muted">Định dạng {{ mimeType || 'không xác định' }} chưa được trình duyệt hỗ trợ xem trực tiếp.</p>

            <button type="button" class="mt-5 inline-flex items-center gap-2 rounded-control bg-secondary px-4 py-2 text-sm font-semibold text-white" @click="download">
              <Download :size="17" />
              Tải xuống
            </button>
          </div>
        </div>
      </main>
    </div>
  </Teleport>
</template>