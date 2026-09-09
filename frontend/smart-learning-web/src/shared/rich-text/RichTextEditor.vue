<script setup lang="ts">
import { watch } from 'vue'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import {
  Bold, Code2, Heading1, Heading2, Heading3, Italic,
  List, ListOrdered, Quote, Redo2, Strikethrough, Undo2,
} from 'lucide-vue-next'

import type { TopicContent } from '@/shared/course-content'
import { topicContentExtensions } from './topicContentExtensions'
import { createEmptyRichText } from './utils'

const props = withDefaults(defineProps<{
  modelValue: TopicContent | null
  compact?: boolean
  disabled?: boolean
}>(), {
  compact: false,
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: TopicContent]
}>()

const editor = useEditor({
  extensions: topicContentExtensions,
  content: props.modelValue ?? createEmptyRichText(),
  editable: !props.disabled,
  editorProps: {
    attributes: {
      class: props.compact
        ? 'topic-content rich-text-editor rich-text-editor-compact'
        : 'topic-content rich-text-editor',
    },
  },
  onUpdate({ editor }) {
    emit('update:modelValue', editor.getJSON() as TopicContent)
  },
})

watch(() => props.modelValue, (content) => {
  if (!editor.value) return

  const next = content ?? createEmptyRichText()

  if (JSON.stringify(editor.value.getJSON()) === JSON.stringify(next)) {
    return
  }

  editor.value.commands.setContent(next, { emitUpdate: false })
}, { deep: true })

watch(() => props.disabled, (disabled) => {
  editor.value?.setEditable(!disabled)
})
</script>

<template>
  <div class="overflow-hidden rounded-card border border-app-border bg-app-surface">
    <div class="flex flex-wrap items-center gap-1 border-b border-app-border bg-app-surface-muted/60 p-2">
      <template v-if="!compact">
        <button type="button" class="editor-tool"
          :class="{ 'editor-tool-active': editor?.isActive('heading', { level: 1 }) }"
          :disabled="disabled" title="Tiêu đề 1"
          @click="editor?.chain().focus().toggleHeading({ level: 1 }).run()">
          <Heading1 :size="17" />
        </button>

        <button type="button" class="editor-tool"
          :class="{ 'editor-tool-active': editor?.isActive('heading', { level: 2 }) }"
          :disabled="disabled" title="Tiêu đề 2"
          @click="editor?.chain().focus().toggleHeading({ level: 2 }).run()">
          <Heading2 :size="17" />
        </button>

        <button type="button" class="editor-tool"
          :class="{ 'editor-tool-active': editor?.isActive('heading', { level: 3 }) }"
          :disabled="disabled" title="Tiêu đề 3"
          @click="editor?.chain().focus().toggleHeading({ level: 3 }).run()">
          <Heading3 :size="17" />
        </button>

        <span class="mx-1 h-6 w-px bg-app-border" />
      </template>

      <button type="button" class="editor-tool"
        :class="{ 'editor-tool-active': editor?.isActive('bold') }"
        :disabled="disabled" title="In đậm"
        @click="editor?.chain().focus().toggleBold().run()">
        <Bold :size="17" />
      </button>

      <button type="button" class="editor-tool"
        :class="{ 'editor-tool-active': editor?.isActive('italic') }"
        :disabled="disabled" title="In nghiêng"
        @click="editor?.chain().focus().toggleItalic().run()">
        <Italic :size="17" />
      </button>

      <button v-if="!compact" type="button" class="editor-tool"
        :class="{ 'editor-tool-active': editor?.isActive('strike') }"
        :disabled="disabled" title="Gạch ngang"
        @click="editor?.chain().focus().toggleStrike().run()">
        <Strikethrough :size="17" />
      </button>

      <button type="button" class="editor-tool"
        :class="{ 'editor-tool-active': editor?.isActive('code') }"
        :disabled="disabled" title="Code"
        @click="editor?.chain().focus().toggleCode().run()">
        <Code2 :size="17" />
      </button>

      <span class="mx-1 h-6 w-px bg-app-border" />

      <button type="button" class="editor-tool"
        :class="{ 'editor-tool-active': editor?.isActive('bulletList') }"
        :disabled="disabled" title="Danh sách"
        @click="editor?.chain().focus().toggleBulletList().run()">
        <List :size="17" />
      </button>

      <button type="button" class="editor-tool"
        :class="{ 'editor-tool-active': editor?.isActive('orderedList') }"
        :disabled="disabled" title="Danh sách đánh số"
        @click="editor?.chain().focus().toggleOrderedList().run()">
        <ListOrdered :size="17" />
      </button>

      <button type="button" class="editor-tool"
        :class="{ 'editor-tool-active': editor?.isActive('blockquote') }"
        :disabled="disabled" title="Trích dẫn"
        @click="editor?.chain().focus().toggleBlockquote().run()">
        <Quote :size="17" />
      </button>

      <button v-if="!compact" type="button" class="editor-tool"
        :class="{ 'editor-tool-active': editor?.isActive('codeBlock') }"
        :disabled="disabled" title="Khối code"
        @click="editor?.chain().focus().toggleCodeBlock().run()">
        <Code2 :size="17" />
      </button>

      <span class="mx-1 h-6 w-px bg-app-border" />

      <button type="button" class="editor-tool" :disabled="disabled"
        title="Hoàn tác" @click="editor?.chain().focus().undo().run()">
        <Undo2 :size="17" />
      </button>

      <button type="button" class="editor-tool" :disabled="disabled"
        title="Làm lại" @click="editor?.chain().focus().redo().run()">
        <Redo2 :size="17" />
      </button>
    </div>

    <EditorContent :editor="editor" />
  </div>
</template>

<style scoped>
.editor-tool {
  display: inline-flex;
  width: 2.25rem;
  height: 2.25rem;
  align-items: center;
  justify-content: center;
  border-radius: 0.5rem;
  color: var(--color-app-text-muted);
  transition: background-color 120ms ease, color 120ms ease;
}

.editor-tool:hover:not(:disabled) {
  background: var(--color-app-surface-muted);
  color: var(--color-app-text);
}

.editor-tool:disabled {
  opacity: 0.5;
}

.editor-tool-active {
  background: var(--color-secondary-soft);
  color: var(--color-secondary);
}

:deep(.rich-text-editor) {
  min-height: 10rem;
  padding: 1rem;
}

:deep(.rich-text-editor-compact) {
  min-height: 6rem;
  font-size: 0.875rem;
  line-height: 1.7;
}
</style>