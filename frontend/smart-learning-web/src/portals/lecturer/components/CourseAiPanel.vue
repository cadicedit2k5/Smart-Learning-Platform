<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import {
  Bot,
  ChevronLeft,
  ChevronRight,
  MessageSquare,
  Plus,
  Send,
  Sparkles,
  UserRound,
} from 'lucide-vue-next'

import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import type { PaginatedData } from '@/shared/api'

import {
  createConversation,
  getConversationMessages,
  getConversations,
  sendConversationMessage,
  type AiConversation,
  type AiMessage,
} from '../api/courseApi'
import { useLecturerApiError } from '../composables/useLecturerApiError'
import { formatDateTime } from '@/shared/utils'

const props = defineProps<{
  courseId: string
}>()

const { handleApiError } = useLecturerApiError()

const conversationsPage = ref<PaginatedData<AiConversation>>({
  content: [],
  pageable: { page: 0, size: 10, totalElements: 0, totalPages: 0 },
})
const messagesPage = ref<PaginatedData<AiMessage>>({
  content: [],
  pageable: { page: 0, size: 10, totalElements: 0, totalPages: 0 },
})
const conversationPage = ref(1)
const messagePage = ref(1)
const selectedConversationId = ref<string | null>(null)
const prompt = ref('')
const loadingConversations = ref(true)
const loadingMessages = ref(false)
const sending = ref(false)
const conversationError = ref('')
const messageError = ref('')
const promptError = ref('')
const messageList = ref<HTMLElement | null>(null)

const conversationTotalPages = computed(() => {
  return Math.max(1, conversationsPage.value.pageable.totalPages)
})

const messageTotalPages = computed(() => {
  return Math.max(1, messagesPage.value.pageable.totalPages)
})

const selectedConversation = computed(() => {
  return conversationsPage.value.content.find(
    (conversation) => conversation.id === selectedConversationId.value,
  )
})

const loadConversations = async () => {
  loadingConversations.value = true
  conversationError.value = ''

  try {
    conversationsPage.value = await getConversations(props.courseId, conversationPage.value)
  } catch (error) {
    conversationError.value = handleApiError(error, 'Không thể tải danh sách hội thoại.').message
  } finally {
    loadingConversations.value = false
  }
}

const scrollToLatest = async () => {
  await nextTick()

  if (messageList.value) {
    messageList.value.scrollTop = messageList.value.scrollHeight
  }
}

const loadMessages = async () => {
  if (!selectedConversationId.value) {
    return
  }

  loadingMessages.value = true
  messageError.value = ''

  try {
    messagesPage.value = await getConversationMessages(
      props.courseId,
      selectedConversationId.value,
      messagePage.value,
    )
    await scrollToLatest()
  } catch (error) {
    messageError.value = handleApiError(error, 'Không thể tải lịch sử tin nhắn.').message
  } finally {
    loadingMessages.value = false
  }
}

const selectConversation = (conversationId: string) => {
  if (selectedConversationId.value === conversationId) {
    return
  }

  selectedConversationId.value = conversationId
  messagePage.value = 1
  messagesPage.value.content = []
  void loadMessages()
}

const startNewConversation = () => {
  selectedConversationId.value = null
  messagePage.value = 1
  messagesPage.value = {
    content: [],
    pageable: { page: 0, size: 10, totalElements: 0, totalPages: 0 },
  }
  prompt.value = ''
  promptError.value = ''
  messageError.value = ''
}

const goToConversationPage = (page: number) => {
  if (page < 1 || page > conversationTotalPages.value || page === conversationPage.value) {
    return
  }

  conversationPage.value = page
  void loadConversations()
}

const goToMessagePage = (page: number) => {
  if (page < 1 || page > messageTotalPages.value || page === messagePage.value) {
    return
  }

  messagePage.value = page
  void loadMessages()
}

const submitPrompt = async () => {
  const content = prompt.value.trim()
  promptError.value = ''
  messageError.value = ''

  if (!content) {
    promptError.value = 'Vui lòng nhập câu hỏi cho trợ lý AI.'
    return
  }

  if (content.length > 10000) {
    promptError.value = 'Tin nhắn không được vượt quá 10.000 ký tự.'
    return
  }

  sending.value = true

  try {
    const turn = selectedConversationId.value
      ? await sendConversationMessage(props.courseId, selectedConversationId.value, content)
      : await createConversation(props.courseId, content)

    selectedConversationId.value = turn.conversationId
    messagesPage.value.content.push(turn.userMessage, turn.assistantMessage)
    messagesPage.value.pageable.totalElements += 2
    prompt.value = ''
    await scrollToLatest()

    conversationPage.value = 1
    await loadConversations()
  } catch (error) {
    const parsed = handleApiError(error, 'Trợ lý AI chưa thể trả lời lúc này.')
    messageError.value = parsed.message
    promptError.value = parsed.fieldErrors.content ?? ''
  } finally {
    sending.value = false
  }
}

const handlePromptKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()

    if (!sending.value) {
      void submitPrompt()
    }
  }
}

onMounted(() => {
  void loadConversations()
})
</script>

<template>
  <section class="space-y-5">
    <header>
      <div class="flex items-center gap-2 text-ai">
        <Sparkles :size="19" />
        <span class="text-sm font-semibold">Trợ lý theo ngữ cảnh khóa học</span>
      </div>
      <h2 class="mt-2 font-heading text-xl font-bold text-app-text">Hỏi đáp với AI</h2>
      <p class="mt-1 text-sm text-app-text-muted">
        Câu trả lời ưu tiên các tài liệu đã được lập chỉ mục trong khóa học này.
      </p>
    </header>

    <div
      class="grid min-h-[620px] overflow-hidden rounded-panel border border-app-border bg-app-surface shadow-card lg:grid-cols-[18rem_minmax(0,1fr)]"
    >
      <aside
        class="flex min-h-0 flex-col border-b border-app-border bg-app-surface-muted/60 lg:border-b-0 lg:border-r"
      >
        <div class="border-b border-app-border p-4">
          <BaseButton variant="ai" block @click="startNewConversation">
            <template #leading><Plus :size="18" /></template>
            Hội thoại mới
          </BaseButton>
        </div>

        <BaseAlert v-if="conversationError" class="m-3">
          {{ conversationError }}
        </BaseAlert>

        <div v-if="loadingConversations" class="space-y-2 p-3">
          <div
            v-for="index in 5"
            :key="index"
            class="h-16 animate-pulse rounded-control bg-app-surface"
          />
        </div>

        <div v-else-if="conversationsPage.content.length === 0" class="p-6 text-center">
          <MessageSquare :size="30" class="mx-auto text-app-text-muted/50" />
          <p class="mt-3 text-sm font-medium text-app-text">Chưa có hội thoại</p>
          <p class="mt-1 text-xs text-app-text-muted">Đặt câu hỏi đầu tiên để bắt đầu.</p>
        </div>

        <nav
          v-else
          aria-label="Danh sách hội thoại"
          class="min-h-0 flex-1 space-y-1 overflow-y-auto p-3"
        >
          <button
            v-for="conversation in conversationsPage.content"
            :key="conversation.id"
            type="button"
            class="w-full rounded-control px-3 py-3 text-left transition"
            :class="
              selectedConversationId === conversation.id
                ? 'bg-ai-soft text-ai'
                : 'text-app-text hover:bg-app-surface'
            "
            @click="selectConversation(conversation.id)"
          >
            <span class="line-clamp-2 text-sm font-semibold">{{ conversation.title }}</span>
            <span class="mt-1 block text-xs opacity-70">{{
              formatDateTime(conversation.lastMessageAt)
            }}</span>
          </button>
        </nav>

        <footer
          v-if="conversationsPage.pageable.totalElements > 0"
          class="flex items-center justify-between border-t border-app-border p-3 text-xs text-app-text-muted"
        >
          <button
            type="button"
            aria-label="Trang hội thoại trước"
            class="rounded-control p-2 hover:bg-app-surface disabled:opacity-40"
            :disabled="conversationPage <= 1"
            @click="goToConversationPage(conversationPage - 1)"
          >
            <ChevronLeft :size="17" />
          </button>
          <span>{{ conversationPage }}/{{ conversationTotalPages }}</span>
          <button
            type="button"
            aria-label="Trang hội thoại sau"
            class="rounded-control p-2 hover:bg-app-surface disabled:opacity-40"
            :disabled="conversationPage >= conversationTotalPages"
            @click="goToConversationPage(conversationPage + 1)"
          >
            <ChevronRight :size="17" />
          </button>
        </footer>
      </aside>

      <div class="flex min-h-[620px] min-w-0 flex-col">
        <header class="border-b border-app-border px-5 py-4">
          <h3 class="truncate font-semibold text-app-text">
            {{ selectedConversation?.title ?? 'Hội thoại mới' }}
          </h3>
          <p class="mt-1 text-xs text-app-text-muted">
            Nhấn Enter để gửi, Shift + Enter để xuống dòng.
          </p>
        </header>

        <div ref="messageList" class="min-h-0 flex-1 space-y-5 overflow-y-auto p-4 sm:p-6">
          <BaseAlert v-if="messageError">{{ messageError }}</BaseAlert>

          <div v-if="loadingMessages" class="space-y-4">
            <div class="h-20 w-3/4 animate-pulse rounded-card bg-app-surface-muted" />
            <div class="ml-auto h-16 w-2/3 animate-pulse rounded-card bg-secondary-soft/60" />
          </div>

          <div
            v-else-if="messagesPage.content.length === 0"
            class="flex min-h-72 flex-col items-center justify-center text-center"
          >
            <span
              class="flex h-14 w-14 items-center justify-center rounded-panel bg-ai-soft text-ai"
            >
              <Bot :size="28" />
            </span>
            <h3 class="mt-4 font-heading text-lg font-bold text-app-text">
              Bạn muốn tìm hiểu điều gì?
            </h3>
            <p class="mt-2 max-w-md text-sm leading-6 text-app-text-muted">
              Hỏi về khái niệm, yêu cầu tóm tắt hoặc tạo câu hỏi ôn tập từ tài liệu khóa học.
            </p>
          </div>

          <template v-else>
            <article
              v-for="message in messagesPage.content"
              :key="message.id"
              class="flex gap-3"
              :class="message.role === 'USER' ? 'flex-row-reverse' : ''"
            >
              <span
                class="flex h-9 w-9 shrink-0 items-center justify-center rounded-pill"
                :class="
                  message.role === 'USER'
                    ? 'bg-secondary-soft text-secondary'
                    : 'bg-ai-soft text-ai'
                "
              >
                <UserRound v-if="message.role === 'USER'" :size="18" />
                <Bot v-else :size="18" />
              </span>

              <div class="max-w-[min(85%,48rem)]">
                <div
                  class="whitespace-pre-wrap rounded-card px-4 py-3 text-sm leading-6"
                  :class="
                    message.role === 'USER'
                      ? 'bg-secondary text-white'
                      : 'border border-app-border bg-app-surface-muted text-app-text'
                  "
                >
                  {{ message.content }}
                </div>

                <div v-if="message.citations?.length" class="mt-2 flex flex-wrap gap-2">
                  <span
                    v-for="citation in message.citations"
                    :key="citation.chunkId"
                    class="rounded-pill border border-ai/20 bg-ai-soft px-2.5 py-1 text-xs font-medium text-ai"
                  >
                    {{ citation.label }}
                  </span>
                </div>

                <p
                  class="mt-1 text-xs text-app-text-muted"
                  :class="message.role === 'USER' ? 'text-right' : ''"
                >
                  {{ formatDateTime(message.createdAt) }}
                </p>
              </div>
            </article>
          </template>
        </div>

        <div
          v-if="selectedConversationId && messagesPage.pageable.totalElements > 0"
          class="flex items-center justify-center gap-3 border-t border-app-border px-4 py-2 text-xs text-app-text-muted"
        >
          <button
            type="button"
            class="rounded-control p-1 disabled:opacity-40"
            :disabled="messagePage <= 1 || loadingMessages"
            @click="goToMessagePage(messagePage - 1)"
          >
            <ChevronLeft :size="16" />
          </button>
          <span>Trang lịch sử {{ messagePage }}/{{ messageTotalPages }}</span>
          <button
            type="button"
            class="rounded-control p-1 disabled:opacity-40"
            :disabled="messagePage >= messageTotalPages || loadingMessages"
            @click="goToMessagePage(messagePage + 1)"
          >
            <ChevronRight :size="16" />
          </button>
        </div>

        <form class="border-t border-app-border p-4" @submit.prevent="submitPrompt">
          <div
            class="flex items-end gap-2 rounded-card border bg-app-surface px-3 py-2 focus-within:border-ai focus-within:ring-2 focus-within:ring-ai/15"
            :class="promptError ? 'border-danger' : 'border-app-border'"
          >
            <textarea
              v-model="prompt"
              rows="2"
              maxlength="10000"
              placeholder="Nhập câu hỏi về khóa học..."
              class="max-h-36 min-h-12 flex-1 resize-none border-0 bg-transparent py-2 text-sm outline-none"
              :disabled="sending"
              @keydown="handlePromptKeydown"
            />

            <button
              type="submit"
              aria-label="Gửi tin nhắn"
              class="mb-1 flex h-10 w-10 shrink-0 items-center justify-center rounded-control bg-ai text-white transition hover:bg-ai-hover disabled:opacity-50"
              :disabled="sending || !prompt.trim()"
            >
              <span
                v-if="sending"
                class="h-4 w-4 animate-spin rounded-full border-2 border-current border-r-transparent"
              />
              <Send v-else :size="18" />
            </button>
          </div>
          <p v-if="promptError" role="alert" class="mt-2 text-sm text-danger">{{ promptError }}</p>
        </form>
      </div>
    </div>
  </section>
</template>
