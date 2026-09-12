import type { TopicContent } from '@/shared/course-content'

export const createEmptyRichText = (): TopicContent => ({
  type: 'doc',
  content: [{ type: 'paragraph' }],
})

export const hasRichTextContent = (value: unknown): boolean => {
  if (Array.isArray(value)) return value.some(hasRichTextContent)

  if (value && typeof value === 'object') {
    const record = value as Record<string, unknown>

    if (typeof record.text === 'string' && record.text.trim()) return true
    return Object.values(record).some(hasRichTextContent)
  }

  return false
}

const legacyTextToRichText = (value: string): TopicContent => ({
  type: 'doc',
  content: value.split(/\r?\n/).map((line) =>
    line
      ? { type: 'paragraph', content: [{ type: 'text', text: line }] }
      : { type: 'paragraph' },
  ),
})

export const parseStoredRichText = (value: string | null | undefined): TopicContent => {
  if (!value?.trim()) return createEmptyRichText()

  try {
    const parsed = JSON.parse(value) as unknown

    if (parsed && typeof parsed === 'object' && (parsed as Record<string, unknown>).type === 'doc') {
      return parsed as TopicContent
    }
  } catch {
    // Legacy plain-text descriptions are converted below.
  }

  return legacyTextToRichText(value)
}

export const serializeRichText = (value: TopicContent | null): string => {
  if (!value || !hasRichTextContent(value)) return ''
  return JSON.stringify(value)
}

const extractPlainText = (value: unknown): string => {
  if (Array.isArray(value)) return value.map(extractPlainText).join('')
  if (!value || typeof value !== 'object') return ''

  const record = value as Record<string, unknown>
  if (typeof record.text === 'string') return record.text

  const content = extractPlainText(record.content)
  const blockTypes = ['paragraph', 'heading', 'listItem', 'blockquote', 'codeBlock']

  return blockTypes.includes(String(record.type)) ? `${content}\n` : content
}

export const richTextToPlainText = (value: unknown): string =>
  extractPlainText(value).replace(/[ \t]+\n/g, '\n').replace(/\n{2,}/g, '\n').trim()

export const storedRichTextToPlainText = (value: string | null | undefined): string =>
  richTextToPlainText(parseStoredRichText(value))