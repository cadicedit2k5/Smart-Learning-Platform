<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { BookOpen } from 'lucide-vue-next'
import { httpClient } from '@/shared/api'

const props = withDefaults(defineProps<{
  imageUrl?: string | null
  title?: string
}>(), {
  imageUrl: null,
  title: 'Khóa học',
})

const displayedUrl = ref('')
const failed = ref(false)
const loading = ref(false)

let ownedObjectUrl = ''
let requestId = 0

const revokeOwnedUrl = () => {
  if (!ownedObjectUrl) return
  URL.revokeObjectURL(ownedObjectUrl)
  ownedObjectUrl = ''
}

const loadImage = async () => {
  const currentRequestId = ++requestId
  const value = props.imageUrl?.trim() ?? ''

  revokeOwnedUrl()
  displayedUrl.value = ''
  failed.value = false

  if (!value) return

  if (value.startsWith('blob:') || value.startsWith('data:')) {
    displayedUrl.value = value
    return
  }

  loading.value = true

  try {
    const response = await httpClient.get<Blob>(value, { responseType: 'blob' })
    if (currentRequestId !== requestId) return

    ownedObjectUrl = URL.createObjectURL(response.data)
    displayedUrl.value = ownedObjectUrl
  } catch {
    if (currentRequestId === requestId) failed.value = true
  } finally {
    if (currentRequestId === requestId) loading.value = false
  }
}

watch(() => props.imageUrl, () => void loadImage(), { immediate: true })

onBeforeUnmount(() => {
  requestId++
  revokeOwnedUrl()
})
</script>

<template>
  <div class="relative aspect-[16/9] overflow-hidden bg-gradient-to-br from-primary via-primary/90 to-secondary">
    <img
      v-if="displayedUrl && !failed"
      :src="displayedUrl"
      :alt="title"
      class="h-full w-full object-cover"
      @error="failed = true"
    />

    <div v-else class="flex h-full w-full items-center justify-center">
      <div class="text-center text-white">
        <div class="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-white/15">
          <BookOpen :size="28" />
        </div>

        <p class="mt-3 max-w-64 truncate px-4 text-sm font-semibold text-white/90">
          {{ loading ? 'Đang tải ảnh...' : title }}
        </p>
      </div>
    </div>
  </div>
</template>