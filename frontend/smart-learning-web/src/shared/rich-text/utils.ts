import type { TopicContent } from '@/shared/course-content'

export const createEmptyRichText = (): TopicContent => ({
  type: 'doc',
  content: [{ type: 'paragraph' }],
})

export const hasRichTextContent = (value: unknown): boolean => {
  if (Array.isArray(value)) {
    return value.some(hasRichTextContent)
  }

  if (value && typeof value === 'object') {
    const record = value as Record<string, unknown>

    if (typeof record.text === 'string' && record.text.trim()) {
      return true
    }

    return Object.values(record).some(hasRichTextContent)
  }

  return false
}