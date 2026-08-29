<script setup lang="ts">
import { watch } from 'vue'
import {Bold, Code2, Heading1, Heading2, Heading3, Italic, List, ListOrdered, Quote, Redo2, Strikethrough, Undo2} from 'lucide-vue-next'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'

import type { TopicContent } from '../api/contentApi'

const props = defineProps<{
  modelValue: TopicContent | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: TopicContent]
}>()

const emptyContent = (): TopicContent => ({
  type: 'doc',
  content: [{ type: 'paragraph' }],
})

const editor = useEditor({
  extensions: [
    StarterKit.configure({
      heading: {
        levels: [1, 2, 3],
      },
    }),
  ],

  content: props.modelValue ?? emptyContent(),

  editorProps: {
    attributes: {
      class: 'topic-editor-content',
    },
  },

  onUpdate({ editor }) {
    emit('update:modelValue', editor.getJSON() as TopicContent)
  },
})

watch(() => props.modelValue, (content) => {
    if (!editor.value) return;

    const nextContent = content ?? emptyContent();

    if (JSON.stringify(editor.value.getJSON()) === JSON.stringify(nextContent)) {
      return
    }

    editor.value.commands.setContent(nextContent, {
      emitUpdate: false,
    })
  },
  { deep: true },
)
</script>

<template>
  <div class="overflow-hidden rounded-card border border-app-border bg-app-surface">
    <!-- Toolbar -->
    <div
      class="flex flex-wrap items-center gap-1 border-b border-app-border bg-app-surface-muted/60 p-2"
    >
      <button
        type="button"
        class="editor-tool"
        :class="{ 'editor-tool-active': editor?.isActive('heading', { level: 1 }) }"
        title="Tiêu đề 1"
        @click="editor?.chain().focus().toggleHeading({ level: 1 }).run()"
      >
        <Heading1 :size="17" />
      </button>

      <button
        type="button"
        class="editor-tool"
        :class="{ 'editor-tool-active': editor?.isActive('heading', { level: 2 }) }"
        title="Tiêu đề 2"
        @click="editor?.chain().focus().toggleHeading({ level: 2 }).run()"
      >
        <Heading2 :size="17" />
      </button>

      <button
        type="button"
        class="editor-tool"
        :class="{ 'editor-tool-active': editor?.isActive('heading', { level: 3 }) }"
        title="Tiêu đề 3"
        @click="editor?.chain().focus().toggleHeading({ level: 3 }).run()"
      >
        <Heading3 :size="17" />
      </button>

      <span class="mx-1 h-6 w-px bg-app-border" />

      <button
        type="button"
        class="editor-tool"
        :class="{ 'editor-tool-active': editor?.isActive('bold') }"
        title="In đậm"
        @click="editor?.chain().focus().toggleBold().run()"
      >
        <Bold :size="17" />
      </button>

      <button
        type="button"
        class="editor-tool"
        :class="{ 'editor-tool-active': editor?.isActive('italic') }"
        title="In nghiêng"
        @click="editor?.chain().focus().toggleItalic().run()"
      >
        <Italic :size="17" />
      </button>

      <button
        type="button"
        class="editor-tool"
        :class="{ 'editor-tool-active': editor?.isActive('strike') }"
        title="Gạch ngang"
        @click="editor?.chain().focus().toggleStrike().run()"
      >
        <Strikethrough :size="17" />
      </button>

      <button
        type="button"
        class="editor-tool"
        :class="{ 'editor-tool-active': editor?.isActive('code') }"
        title="Code"
        @click="editor?.chain().focus().toggleCode().run()"
      >
        <Code2 :size="17" />
      </button>

      <span class="mx-1 h-6 w-px bg-app-border" />

      <button
        type="button"
        class="editor-tool"
        :class="{ 'editor-tool-active': editor?.isActive('bulletList') }"
        title="Danh sách"
        @click="editor?.chain().focus().toggleBulletList().run()"
      >
        <List :size="17" />
      </button>

      <button
        type="button"
        class="editor-tool"
        :class="{ 'editor-tool-active': editor?.isActive('orderedList') }"
        title="Danh sách đánh số"
        @click="editor?.chain().focus().toggleOrderedList().run()"
      >
        <ListOrdered :size="17" />
      </button>

      <button
        type="button"
        class="editor-tool"
        :class="{ 'editor-tool-active': editor?.isActive('blockquote') }"
        title="Trích dẫn"
        @click="editor?.chain().focus().toggleBlockquote().run()"
      >
        <Quote :size="17" />
      </button>

      <button
        type="button"
        class="editor-tool"
        :class="{ 'editor-tool-active': editor?.isActive('codeBlock') }"
        title="Khối code"
        @click="editor?.chain().focus().toggleCodeBlock().run()"
      >
        <Code2 :size="17" />
      </button>

      <span class="mx-1 h-6 w-px bg-app-border" />

      <button
        type="button"
        class="editor-tool"
        title="Hoàn tác"
        @click="editor?.chain().focus().undo().run()"
      >
        <Undo2 :size="17" />
      </button>

      <button
        type="button"
        class="editor-tool"
        title="Làm lại"
        @click="editor?.chain().focus().redo().run()"
      >
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
  transition:
    background-color 120ms ease,
    color 120ms ease;
}

.editor-tool:hover {
  background: var(--color-app-surface-muted);
  color: var(--color-app-text);
}

.editor-tool-active {
  background: var(--color-secondary-soft);
  color: var(--color-secondary);
}

:deep(.topic-editor-content) {
  min-height: 28rem;
  padding: 2rem 2.5rem;
  outline: none;
  color: var(--color-app-text);
  line-height: 1.75;
}

:deep(.topic-editor-content > *:first-child) {
  margin-top: 0;
}

:deep(.topic-editor-content h1) {
  margin: 2rem 0 1rem;
  font-size: 1.9rem;
  font-weight: 700;
  line-height: 1.25;
}

:deep(.topic-editor-content h2) {
  margin: 1.75rem 0 0.75rem;
  font-size: 1.5rem;
  font-weight: 700;
  line-height: 1.35;
}

:deep(.topic-editor-content h3) {
  margin: 1.5rem 0 0.65rem;
  font-size: 1.2rem;
  font-weight: 700;
}

:deep(.topic-editor-content p) {
  margin: 0.75rem 0;
}

:deep(.topic-editor-content ul) {
  margin: 0.75rem 0;
  list-style: disc;
  padding-left: 1.75rem;
}

:deep(.topic-editor-content ol) {
  margin: 0.75rem 0;
  list-style: decimal;
  padding-left: 1.75rem;
}

:deep(.topic-editor-content blockquote) {
  margin: 1rem 0;
  border-left: 3px solid var(--color-secondary);
  background: var(--color-app-surface-muted);
  padding: 0.75rem 1rem;
  color: var(--color-app-text-muted);
}

:deep(.topic-editor-content code) {
  border-radius: 0.3rem;
  background: var(--color-app-surface-muted);
  padding: 0.15rem 0.35rem;
  font-size: 0.875em;
}

:deep(.topic-editor-content pre) {
  margin: 1rem 0;
  overflow-x: auto;
  border-radius: 0.75rem;
  background: #111827;
  padding: 1rem 1.25rem;
  color: #f8fafc;
}

:deep(.topic-editor-content pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}
</style>