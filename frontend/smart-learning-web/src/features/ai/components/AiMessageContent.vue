<script setup lang="ts">
import { computed } from 'vue'
import DOMPurify from 'dompurify'
import { marked } from 'marked'

const props = defineProps<{ content: string }>()

const renderedHtml = computed(() => {
  if (!props.content) return ''

  const rawHtml = marked.parse(props.content, {
    async: false,
    breaks: true,
    gfm: true,
  })

  return DOMPurify.sanitize(rawHtml)
})
</script>

<template>
  <div class="ai-message-content" v-html="renderedHtml"></div>
</template>

<style scoped>
.ai-message-content {
  line-height: 1.75;
  color: var(--color-app-text);
}

.ai-message-content :deep(p) {
  margin: 0.65rem 0;
}

.ai-message-content :deep(p:first-child) {
  margin-top: 0;
}

.ai-message-content :deep(p:last-child) {
  margin-bottom: 0;
}

.ai-message-content :deep(h1),
.ai-message-content :deep(h2),
.ai-message-content :deep(h3) {
  margin: 1.25rem 0 0.5rem;
  font-family: var(--font-heading);
  font-weight: 700;
  line-height: 1.35;
}

.ai-message-content :deep(h1) {
  font-size: 1.25rem;
}

.ai-message-content :deep(h2) {
  font-size: 1.125rem;
}

.ai-message-content :deep(h3) {
  font-size: 1rem;
}

.ai-message-content :deep(ul),
.ai-message-content :deep(ol) {
  margin: 0.75rem 0;
  padding-left: 1.5rem;
}

.ai-message-content :deep(ul) {
  list-style-type: disc;
}

.ai-message-content :deep(ol) {
  list-style-type: decimal;
}

.ai-message-content :deep(li) {
  margin: 0.25rem 0;
}

.ai-message-content :deep(strong) {
  font-weight: 700;
}

.ai-message-content :deep(blockquote) {
  margin: 1rem 0;
  border-left: 3px solid var(--color-ai);
  padding-left: 1rem;
  color: var(--color-app-text-muted);
}

.ai-message-content :deep(code) {
  border-radius: 0.35rem;
  background: var(--color-app-surface-muted);
  padding: 0.15rem 0.35rem;
  font-size: 0.875em;
}

.ai-message-content :deep(pre) {
  margin: 1rem 0;
  overflow-x: auto;
  border: 1px solid var(--color-app-border);
  border-radius: var(--radius-card);
  background: var(--color-app-surface-muted);
  padding: 1rem;
}

.ai-message-content :deep(pre code) {
  background: transparent;
  padding: 0;
}

.ai-message-content :deep(a) {
  color: var(--color-ai);
  text-decoration: underline;
  text-underline-offset: 2px;
}
</style>