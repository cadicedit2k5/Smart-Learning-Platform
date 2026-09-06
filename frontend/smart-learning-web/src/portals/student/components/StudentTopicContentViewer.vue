<script setup lang="ts">
import { watch } from 'vue'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import type { TopicContent } from '@/shared/course-content'
import { topicContentExtensions } from '@/shared/rich-text';


const props = defineProps<{
  content: TopicContent | null
}>()

const emptyContent = {
  type: 'doc',
  content: [{ type: 'paragraph' }],
}

const editor = useEditor({
  extensions: topicContentExtensions,

  content: props.content ?? emptyContent,

  editable: false,

  editorProps: {
    attributes: {
      class: 'topic-content topic-content-viewer',
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
