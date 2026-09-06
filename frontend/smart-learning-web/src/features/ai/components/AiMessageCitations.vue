<script setup lang="ts">
import { computed } from 'vue'
import { FileText } from 'lucide-vue-next'

import type { AiCitation } from '../types'

const props = defineProps<{ citations: AiCitation[] }>()

const uniqueCitations = computed(() => {
  const chunkIds = new Set<string>()
  return props.citations.filter(citation => {
    if (chunkIds.has(citation.chunkId)) return false
    chunkIds.add(citation.chunkId)
    return true
  })
})
</script>

<template>
  <div v-if="uniqueCitations.length" class="mt-5 border-t border-app-border/70 pt-4">
    <div class="mb-2.5 flex items-center gap-2">
      <FileText :size="14" class="text-ai" />

      <p class="text-[11px] font-semibold uppercase tracking-[0.12em] text-app-text-muted">
        Nguồn tham khảo
      </p>

      <span class="rounded-pill bg-ai-soft px-1.5 py-0.5 text-[10px] font-semibold text-ai">
        {{ uniqueCitations.length }}
      </span>
    </div>

    <div class="flex flex-wrap gap-2">
      <div
        v-for="(citation, index) in uniqueCitations"
        :key="citation.chunkId"
        :title="citation.label"
        class="inline-flex max-w-full items-center gap-2 rounded-control border border-app-border bg-app-surface px-3 py-2 text-xs shadow-card"
      >
        <span class="flex h-5 w-5 shrink-0 items-center justify-center rounded-pill bg-ai-soft text-[10px] font-bold text-ai">
          {{ index + 1 }}
        </span>

        <span class="truncate font-medium text-app-text">
          {{ citation.label }}
        </span>
      </div>
    </div>
  </div>
</template>