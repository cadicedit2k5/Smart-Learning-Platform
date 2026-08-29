<script setup lang="ts">
import { watch } from 'vue'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'

import type { TopicContent } from '../api/contentApi'

const props = defineProps<{
  content: TopicContent | null
}>()

const emptyContent = {
  type: 'doc',
  content: [{ type: 'paragraph' }],
}

const editor = useEditor({
  extensions: [
    StarterKit.configure({
      heading: {
        levels: [1, 2, 3],
      },
    }),
  ],

  content: props.content ?? emptyContent,

  editable: false,

  editorProps: {
    attributes: {
      class: 'student-topic-content',
    },
  },
})

watch(
  () => props.content,
  (content) => {
    editor.value?.commands.setContent(
      content ?? emptyContent,
      {
        emitUpdate: false,
      },
    )
  },
  { deep: true },
)
</script>

<template>
  <EditorContent :editor="editor" />
</template>

<style scoped>
:deep(.student-topic-content) {
  color: var(--color-app-text);
  font-size: 1rem;
  line-height: 1.8;
  outline: none;
}

:deep(.student-topic-content > *:first-child) {
  margin-top: 0;
}

:deep(.student-topic-content h1) {
  margin: 2.5rem 0 1rem;
  font-family: var(--font-heading);
  font-size: 2rem;
  font-weight: 700;
  line-height: 1.25;
  letter-spacing: -0.025em;
}

:deep(.student-topic-content h2) {
  margin: 2.25rem 0 0.9rem;
  font-family: var(--font-heading);
  font-size: 1.55rem;
  font-weight: 700;
  line-height: 1.35;
}

:deep(.student-topic-content h3) {
  margin: 1.75rem 0 0.75rem;
  font-size: 1.2rem;
  font-weight: 700;
}

:deep(.student-topic-content p) {
  margin: 0.9rem 0;
}

:deep(.student-topic-content strong) {
  font-weight: 700;
}

:deep(.student-topic-content ul) {
  margin: 1rem 0;
  list-style: disc;
  padding-left: 1.75rem;
}

:deep(.student-topic-content ol) {
  margin: 1rem 0;
  list-style: decimal;
  padding-left: 1.75rem;
}

:deep(.student-topic-content li) {
  margin: 0.35rem 0;
}

:deep(.student-topic-content blockquote) {
  margin: 1.5rem 0;
  border-left: 4px solid var(--color-secondary);
  border-radius: 0 0.5rem 0.5rem 0;
  background: var(--color-secondary-soft);
  padding: 1rem 1.25rem;
}

:deep(.student-topic-content code) {
  border-radius: 0.35rem;
  background: var(--color-app-surface-muted);
  padding: 0.15rem 0.4rem;
  font-size: 0.875em;
}

:deep(.student-topic-content pre) {
  margin: 1.5rem 0;
  overflow-x: auto;
  border-radius: 0.75rem;
  background: #0f172a;
  padding: 1.25rem 1.5rem;
  color: #f8fafc;
}

:deep(.student-topic-content pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}

:deep(.student-topic-content hr) {
  margin: 2rem 0;
  border-color: var(--color-app-border);
}
</style>