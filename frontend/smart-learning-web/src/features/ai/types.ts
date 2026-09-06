export interface AiCitation {
  label: string
  chunkId: string
  documentId: string
  documentVersionId: string
  locator: Record<string, unknown>
}

export interface AiMessage {
  id: string
  role: 'USER' | 'ASSISTANT'
  accessScope: 'PREVIEW' | 'FULL'
  content: string
  citations: AiCitation[]
  createdAt: string
}

export interface AiConversation {
  id: string
  title: string
  lastMessageAt: string
  createdAt: string
}

export interface AiChatTurn {
  conversationId: string
  userMessage: AiMessage
  assistantMessage: AiMessage
}