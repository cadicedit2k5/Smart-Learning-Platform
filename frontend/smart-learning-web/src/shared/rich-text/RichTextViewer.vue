<script setup lang="ts">
import { watch } from 'vue'
import { EditorContent, useEditor } from '@tiptap/vue-3'

import type { TopicContent } from '@/shared/course-content'
import { topicContentExtensions } from './topicContentExtensions'
import { createEmptyRichText } from './utils'

const props = withDefaults(defineProps<{
  content: TopicContent | null
  compact?: boolean
}>(), {
  compact: false,
})

const editor = useEditor({
  extensions: topicContentExtensions,
  content: props.content ?? createEmptyRichText(),
  editable: false,
  editorProps: {
    attributes: {
      class: props.compact
        ? 'topic-content rich-text-viewer rich-text-viewer-compact'
        : 'topic-content rich-text-viewer',
    },
  },
})

watch(() => props.content, (content) => {
  editor.value?.commands.setContent(
    content ?? createEmptyRichText(),
    { emitUpdate: false },
  )
}, { deep: true })
</script>

<template>
  <EditorContent :editor="editor" />
</template>

<style scoped>
:deep(.rich-text-viewer-compact) {
  font-size: 0.875rem;
  line-height: 1.7;
}

:deep(.rich-text-viewer-compact p) {
  margin: 0.4rem 0;
}
</style>