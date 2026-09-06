import { httpClient, type ApiResponse, type PaginatedData,} from '@/shared/api'

import type {
  AiChatTurn,
  AiConversation,
  AiMessage,
} from './types'

const conversationsPath = (courseId: string) => `/courses/${courseId}/ai/conversations`

export const getConversations = async (
  courseId: string,
  page = 1,
): Promise<PaginatedData<AiConversation>> => {
  const response = await httpClient.get<
    ApiResponse<PaginatedData<AiConversation>>>(conversationsPath(courseId),
    {
      params: { page },
    },
  )

  return response.data.data;
}

export const getConversationMessages = async (
  courseId: string,
  conversationId: string,
  page = 1,
): Promise<PaginatedData<AiMessage>> => {
  const response = await httpClient.get<
    ApiResponse<PaginatedData<AiMessage>>>(`${conversationsPath(courseId)}/${conversationId}/messages`,
    {
      params: { page },
    },
  )

  return response.data.data;
}

export const createConversation = async (
  courseId: string,
  content: string,
): Promise<AiChatTurn> => {
  const response =
    await httpClient.post<ApiResponse<AiChatTurn>>(conversationsPath(courseId),
      { content },
    )

  return response.data.data;
}

export const sendConversationMessage = async (
  courseId: string,
  conversationId: string,
  content: string,
): Promise<AiChatTurn> => {
  const response =
    await httpClient.post<ApiResponse<AiChatTurn>>(`${conversationsPath(courseId)}/${conversationId}/messages`,
      { content },
    )

  return response.data.data;
}