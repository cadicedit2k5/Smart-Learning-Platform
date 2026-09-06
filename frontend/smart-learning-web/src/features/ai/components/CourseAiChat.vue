<script setup lang="ts">
import {
  computed,
  nextTick,
  onMounted,
  ref,
} from 'vue'
import {
  Bot,
  FileText,
  MessageSquare,
  Plus,
  Send,
  Sparkles,
  UserRound,
} from 'lucide-vue-next'

import type { PaginatedData } from '@/shared/api'
import {
  BaseAlert,
  BaseButton,
} from '@/shared/components'
import { formatDateTime } from '@/shared/utils'

import {
  createConversation,
  getConversationMessages,
  getConversations,
  sendConversationMessage,
} from '../api'

import type {
  AiConversation,
  AiMessage,
} from '../types'
import AiMessageContent from './AiMessageContent.vue'
import AiMessageCitations from './AiMessageCitations.vue'

interface ParsedApiError {
  message: string
  fieldErrors: Record<string, string>
}

const props = withDefaults(
  defineProps<{
    courseId: string

    eyebrow?: string
    title?: string
    description?: string

    handleError: (
      error: unknown,
      fallbackMessage: string,
    ) => ParsedApiError
  }>(),
  {
    eyebrow: 'AI Tutor',
    title: 'Hỏi đáp với AI',
    description:
      'Đặt câu hỏi dựa trên nội dung và tài liệu của khóa học.',
  },
)

const emptyPage = <T,>(): PaginatedData<T> => ({
  content: [],
  pageable: {
    page: 1,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  },
})

/* -------------------------------------------------------------------------- */
/* State                                                                      */
/* -------------------------------------------------------------------------- */

const conversationsPage =
  ref<PaginatedData<AiConversation>>(
    emptyPage(),
  )

const messagesPage =
  ref<PaginatedData<AiMessage>>(
    emptyPage(),
  )

/**
 * Với infinite scroll:
 *
 * conversationPage = page cuối cùng của sidebar đã tải.
 * messagePage      = page lịch sử cũ nhất hiện đã tải.
 */
const conversationPage = ref(1)
const messagePage = ref(1)

const selectedConversationId =
  ref<string | null>(null)

const prompt = ref('')
const pendingPrompt = ref('')

const loadingConversations = ref(true)
const loadingMoreConversations = ref(false)

const loadingMessages = ref(false)
const loadingOlderMessages = ref(false)

const sending = ref(false)

const conversationError = ref('')
const messageError = ref('')
const promptError = ref('')

const messageList =
  ref<HTMLElement | null>(null)
const messageScrollReady = ref(false)

/**
 * Dùng để bỏ qua response cũ nếu user đổi conversation
 * trước khi request load message hoàn tất.
 */
let messageRequestToken = 0

const suggestedPrompts = [
  'Tóm tắt những nội dung quan trọng của khóa học',
  'Giải thích một khái niệm khó theo cách dễ hiểu',
  'Tạo 5 câu hỏi để tôi tự ôn tập',
]

/* -------------------------------------------------------------------------- */
/* Computed                                                                   */
/* -------------------------------------------------------------------------- */

const selectedConversation = computed(() =>
  conversationsPage.value.content.find(
    (conversation) =>
      conversation.id ===
      selectedConversationId.value,
  ),
)

const hasMoreConversations = computed(() => {
  return (
    conversationPage.value <
    conversationsPage.value.pageable.totalPages
  )
})

const hasOlderMessages = computed(() => {
  return (
    messagePage.value <
    messagesPage.value.pageable.totalPages
  )
})

const canSubmit = computed(() => {
  return (
    !sending.value &&
    prompt.value.trim().length > 0
  )
})

/* -------------------------------------------------------------------------- */
/* Scroll helpers                                                             */
/* -------------------------------------------------------------------------- */

const scrollToLatest = async () => {
  await nextTick()

  const element = messageList.value

  if (!element) {
    return
  }

  element.scrollTop =
    element.scrollHeight
}

/* -------------------------------------------------------------------------- */
/* Conversations                                                              */
/* -------------------------------------------------------------------------- */

/**
 * Load page 1.
 *
 * Dùng khi component mount hoặc cần reset sidebar.
 */
const loadConversations = async () => {
  loadingConversations.value = true
  conversationError.value = ''
  conversationPage.value = 1

  try {
    conversationsPage.value =
      await getConversations(
        props.courseId,
        1,
      )
  } catch (error) {
    conversationError.value =
      props.handleError(
        error,
        'Không thể tải danh sách hội thoại.',
      ).message
  } finally {
    loadingConversations.value = false
  }
}

/**
 * Refresh page 1 nhưng KHÔNG xóa các page đã load phía dưới.
 *
 * Dùng sau khi gửi message để:
 * - conversation mới xuất hiện đầu sidebar;
 * - lastMessageAt được cập nhật;
 * - conversation được đưa lại đúng thứ tự.
 */
const refreshConversations = async () => {
  try {
    const result =
      await getConversations(
        props.courseId,
        1,
      )

    const firstPageIds =
      new Set(
        result.content.map(
          (conversation) =>
            conversation.id,
        ),
      )

    const remaining =
      conversationsPage.value.content.filter(
        (conversation) =>
          !firstPageIds.has(
            conversation.id,
          ),
      )

    const merged = [
      ...result.content,
      ...remaining,
    ]

    merged.sort(
      (a, b) =>
        new Date(
          b.lastMessageAt,
        ).getTime() -
        new Date(
          a.lastMessageAt,
        ).getTime(),
    )

    conversationsPage.value = {
      ...result,
      content: merged,
    }
  } catch (error) {
    conversationError.value =
      props.handleError(
        error,
        'Không thể cập nhật lịch sử hội thoại.',
      ).message
  }
}

/**
 * Infinite scroll sidebar:
 *
 * page 1
 *   ↓
 * page 2 append
 *   ↓
 * page 3 append
 */
const loadMoreConversations = async () => {
  if (
    loadingConversations.value ||
    loadingMoreConversations.value ||
    sending.value ||
    !hasMoreConversations.value
  ) {
    return
  }

  const nextPage =
    conversationPage.value + 1

  loadingMoreConversations.value = true

  try {
    const result =
      await getConversations(
        props.courseId,
        nextPage,
      )

    const existingIds = new Set(
      conversationsPage.value.content.map(
        (conversation) =>
          conversation.id,
      ),
    )

    const newConversations =
      result.content.filter(
        (conversation) =>
          !existingIds.has(
            conversation.id,
          ),
      )

    conversationsPage.value = {
      ...result,
      content: [
        ...conversationsPage.value.content,
        ...newConversations,
      ],
    }

    conversationPage.value = nextPage
  } catch (error) {
    conversationError.value =
      props.handleError(
        error,
        'Không thể tải thêm hội thoại.',
      ).message
  } finally {
    loadingMoreConversations.value = false
  }
}

const handleConversationScroll = (
  event: Event,
) => {
  const element =
    event.currentTarget as HTMLElement

  const distanceToBottom =
    element.scrollHeight -
    element.scrollTop -
    element.clientHeight

  /**
   * Không chờ chạm đúng đáy.
   * Fetch trước 96px để UX mượt hơn.
   */
  if (distanceToBottom <= 96) {
    void loadMoreConversations()
  }
}

/* -------------------------------------------------------------------------- */
/* Messages                                                                   */
/* -------------------------------------------------------------------------- */

/**
 * Load page 1 của conversation.
 *
 * page 1 được xem là nhóm message mới nhất.
 */
const loadMessages = async () => {
  const conversationId =
    selectedConversationId.value

  if (!conversationId) {
    return
  }

  const requestToken =
    ++messageRequestToken

  loadingMessages.value = true
  loadingOlderMessages.value = false
  messageScrollReady.value = false

  messageError.value = ''
  messagePage.value = 1

  try {
    const result =
      await getConversationMessages(
        props.courseId,
        conversationId,
        1,
      )

    if (
      requestToken !== messageRequestToken ||
      selectedConversationId.value !==
        conversationId
    ) {
      return
    }

    /*
     * Backend:
     * newest -> oldest
     *
     * UI:
     * oldest -> newest
     */
    messagesPage.value = {
      ...result,
      content: [
        ...result.content,
      ].reverse(),
    }

    /*
     * QUAN TRỌNG:
     * tắt initial loading trước để Vue
     * render message thật thay vì skeleton.
     */
    loadingMessages.value = false

    /*
     * Chờ message thật render vào DOM.
     */
    await nextTick()

    if (
      requestToken !== messageRequestToken ||
      selectedConversationId.value !==
        conversationId
    ) {
      return
    }

    const element =
      messageList.value

    if (!element) {
      return
    }

    /*
     * Scroll tới message mới nhất.
     */
    element.scrollTop =
      element.scrollHeight

    /*
     * Đợi browser hoàn tất layout + scroll.
     * Infinite scroll vẫn đang bị khóa.
     */
    await new Promise<void>((resolve) => {
        requestAnimationFrame(() => {
            requestAnimationFrame(() => resolve())
        })
    })

    if (
      requestToken !== messageRequestToken ||
      selectedConversationId.value !==
        conversationId
    ) {
      return
    }

    /*
     * Scroll lại lần cuối vì font/image/layout
     * có thể làm chiều cao thay đổi nhẹ.
     */
    element.scrollTop = element.scrollHeight

    /*
     * Chỉ sau khi đã thực sự ở bottom
     * mới mở infinite scroll.
     */
    previousMessageScrollTop = element.scrollTop

    messageScrollReady.value = true
  } catch (error) {
    if (
      requestToken !== messageRequestToken
    ) {
      return
    }

    messageError.value =
      props.handleError(
        error,
        'Không thể tải lịch sử tin nhắn.',
      ).message

    loadingMessages.value = false
  }
}

/**
 * Infinite scroll message theo chiều ngược:
 *
 *       older
 *         ↑
 * page 3 prepend
 * page 2 prepend
 * page 1 newest
 *         ↓
 *      composer
 */
const loadOlderMessages = async () => {
  const conversationId =
    selectedConversationId.value

  const element =
    messageList.value

  if (
    !conversationId ||
    !element ||
    loadingMessages.value ||
    loadingOlderMessages.value ||
    sending.value ||
    !hasOlderMessages.value
  ) {
    return
  }

  const nextPage =
    messagePage.value + 1

  const previousScrollHeight =
    element.scrollHeight

  const previousScrollTop =
    element.scrollTop

  const requestToken =
    messageRequestToken

  loadingOlderMessages.value = true

  try {
    const result =
      await getConversationMessages(
        props.courseId,
        conversationId,
        nextPage,
      )

    if (
      requestToken !== messageRequestToken ||
      selectedConversationId.value !==
        conversationId
    ) {
      return
    }

    const existingIds = new Set(
      messagesPage.value.content.map(
        (message) => message.id,
      ),
    )

    /*
     * Backend page:
     *
     * m10
     * m9
     * ...
     * m1
     *
     * Reverse thành:
     *
     * m1
     * m2
     * ...
     * m10
     */
    const olderMessages = [
      ...result.content,
    ]
      .reverse()
      .filter(
        (message) =>
          !existingIds.has(message.id),
      )

    messagesPage.value = {
      ...result,

      content: [
        ...olderMessages,
        ...messagesPage.value.content,
      ],
    }

    messagePage.value = nextPage

    /*
     * Loader phải biến mất trước khi tính
     * scrollHeight mới.
     */
    loadingOlderMessages.value = false

    await nextTick()

    const currentElement =
      messageList.value

    if (
      !currentElement ||
      selectedConversationId.value !==
        conversationId
    ) {
      return
    }

    const heightDifference =
      currentElement.scrollHeight -
      previousScrollHeight

    /*
     * Giữ đúng vị trí message user đang đọc.
     */
    currentElement.scrollTop =
      previousScrollTop +
      heightDifference
  } catch (error) {
    if (
      requestToken === messageRequestToken
    ) {
      messageError.value =
        props.handleError(
          error,
          'Không thể tải thêm lịch sử tin nhắn.',
        ).message
    }
  } finally {
    loadingOlderMessages.value = false
  }
}

let previousMessageScrollTop = 0

const handleMessageScroll = () => {
  const element =
    messageList.value

  if (
    !element ||
    !messageScrollReady.value ||
    loadingMessages.value ||
    loadingOlderMessages.value
  ) {
    return
  }

  const currentScrollTop =
    element.scrollTop

  const scrollingUp =
    currentScrollTop <
    previousMessageScrollTop

  previousMessageScrollTop =
    currentScrollTop

  if (
    scrollingUp &&
    currentScrollTop <= 96
  ) {
    void loadOlderMessages()
  }
}

/* -------------------------------------------------------------------------- */
/* Conversation selection                                                     */
/* -------------------------------------------------------------------------- */

const selectConversation = (
  conversationId: string,
) => {
  if (sending.value) {
    return
  }

  if (
    selectedConversationId.value ===
    conversationId
  ) {
    return
  }

  messageRequestToken += 1

  messageScrollReady.value = false

  selectedConversationId.value =
    conversationId

  messagePage.value = 1
  messagesPage.value = emptyPage()

  prompt.value = ''
  pendingPrompt.value = ''

  promptError.value = ''
  messageError.value = ''

  void loadMessages()
}

const startNewConversation = () => {
  if (sending.value) {
    return
  }

  messageRequestToken += 1

  messageScrollReady.value = false

  selectedConversationId.value = null

  messagePage.value = 1
  messagesPage.value = emptyPage()

  prompt.value = ''
  pendingPrompt.value = ''

  promptError.value = ''
  messageError.value = ''
}

/* -------------------------------------------------------------------------- */
/* Prompt                                                                     */
/* -------------------------------------------------------------------------- */

const selectSuggestion = (
  suggestion: string,
) => {
  prompt.value = suggestion
}

const submitPrompt = async () => {
  const content =
    prompt.value.trim()

  promptError.value = ''
  messageError.value = ''

  if (!content) {
    promptError.value =
      'Vui lòng nhập câu hỏi.'
    return
  }

  if (content.length > 10_000) {
    promptError.value =
      'Tin nhắn không được vượt quá 10.000 ký tự.'
    return
  }

  const conversationIdBeforeSend =
    selectedConversationId.value

  sending.value = true

  /**
   * Optimistic UI:
   * hiển thị message ngay lập tức.
   */
  pendingPrompt.value = content
  prompt.value = ''

  await scrollToLatest()

  try {
    const turn =
      conversationIdBeforeSend
        ? await sendConversationMessage(
            props.courseId,
            conversationIdBeforeSend,
            content,
          )
        : await createConversation(
            props.courseId,
            content,
          )

    selectedConversationId.value =
      turn.conversationId

    /**
     * pendingPrompt sẽ được thay bằng
     * userMessage thật từ server.
     */
    messagesPage.value.content.push(
      turn.userMessage,
      turn.assistantMessage,
    )

    const pageable =
      messagesPage.value.pageable

    pageable.totalElements += 2

    if (pageable.size > 0) {
      pageable.totalPages =
        Math.ceil(
          pageable.totalElements /
            pageable.size,
        )
    }

    pendingPrompt.value = ''

    await scrollToLatest()

    /**
     * Refresh page đầu sidebar:
     * cập nhật title / lastMessageAt / order.
     */
    await refreshConversations()
  } catch (error) {
    const parsed =
      props.handleError(
        error,
        'Trợ lý AI chưa thể trả lời lúc này.',
      )

    messageError.value =
      parsed.message

    promptError.value =
      parsed.fieldErrors.content ?? ''

    /**
     * Không làm user mất prompt khi request lỗi.
     */
    prompt.value =
      pendingPrompt.value

    pendingPrompt.value = ''
  } finally {
    sending.value = false

    await scrollToLatest()
  }
}

const handlePromptKeydown = (
  event: KeyboardEvent,
) => {
  if (
    event.key === 'Enter' &&
    !event.shiftKey
  ) {
    event.preventDefault()

    if (canSubmit.value) {
      void submitPrompt()
    }
  }
}

/* -------------------------------------------------------------------------- */
/* Lifecycle                                                                  */
/* -------------------------------------------------------------------------- */

onMounted(() => {
  void loadConversations()
})
</script>

<template>
  <section class="space-y-5">
    <!-- AI feature header -->
    <header
      class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between"
    >
      <div class="flex items-start gap-3">
        <div
          class="flex h-11 w-11 shrink-0 items-center justify-center rounded-card bg-ai-soft text-ai"
        >
          <Sparkles :size="21" />
        </div>

        <div>
          <p
            class="text-xs font-semibold uppercase tracking-[0.12em] text-ai"
          >
            {{ eyebrow }}
          </p>

          <h2
            class="mt-1 font-heading text-xl font-bold tracking-tight text-app-text"
          >
            {{ title }}
          </h2>

          <p
            class="mt-1 max-w-2xl text-sm leading-6 text-app-text-muted"
          >
            {{ description }}
          </p>
        </div>
      </div>

      <span
        class="inline-flex w-fit shrink-0 items-center gap-2 rounded-pill bg-ai-soft px-3 py-1.5 text-xs font-semibold text-ai"
      >
        <span
          class="h-1.5 w-1.5 rounded-full bg-ai"
        />

        Dựa trên tài liệu khóa học
      </span>
    </header>

    <!-- Chat workspace -->
    <div
      class="grid overflow-hidden rounded-panel border border-app-border bg-app-surface shadow-card lg:h-[44rem] lg:grid-cols-[19rem_minmax(0,1fr)]"
    >
      <!-- ================================================================ -->
      <!-- Conversation sidebar                                             -->
      <!-- ================================================================ -->

      <aside
        class="flex max-h-80 min-h-0 flex-col border-b border-app-border bg-app-surface lg:max-h-none lg:border-r lg:border-b-0"
      >
        <div
          class="border-b border-app-border p-4"
        >
          <BaseButton
            variant="ai"
            block
            :disabled="sending"
            @click="startNewConversation"
          >
            <template #leading>
              <Plus :size="17" />
            </template>

            Hội thoại mới
          </BaseButton>
        </div>

        <div
          class="px-4 pt-4 pb-2"
        >
          <p
            class="text-[11px] font-semibold uppercase tracking-[0.12em] text-app-text-muted"
          >
            Lịch sử hội thoại
          </p>
        </div>

        <BaseAlert
          v-if="conversationError"
          class="mx-3 mb-3"
        >
          {{ conversationError }}
        </BaseAlert>

        <!-- Initial sidebar loading -->
        <div
          v-if="loadingConversations"
          class="space-y-2 px-3 pb-3"
        >
          <div
            v-for="index in 5"
            :key="index"
            class="h-[4.5rem] animate-pulse rounded-control bg-app-surface-muted"
          />
        </div>

        <!-- No conversations -->
        <div
          v-else-if="
            conversationsPage.content.length ===
            0
          "
          class="flex flex-1 flex-col items-center justify-center px-6 py-10 text-center"
        >
          <div
            class="flex h-10 w-10 items-center justify-center rounded-card bg-app-surface-muted text-app-text-muted"
          >
            <MessageSquare :size="19" />
          </div>

          <p
            class="mt-3 text-sm font-semibold text-app-text"
          >
            Chưa có hội thoại
          </p>

          <p
            class="mt-1 text-xs leading-5 text-app-text-muted"
          >
            Đặt câu hỏi đầu tiên để bắt đầu.
          </p>
        </div>

        <!-- Infinite conversation list -->
        <nav
          v-else
          aria-label="Danh sách hội thoại"
          class="min-h-0 flex-1 space-y-1 overflow-y-auto px-3 pb-3"
          @scroll.passive="
            handleConversationScroll
          "
        >
          <button
            v-for="
              conversation in
              conversationsPage.content
            "
            :key="conversation.id"
            type="button"
            :disabled="sending"
            class="group w-full rounded-control px-3 py-3 text-left transition disabled:cursor-not-allowed disabled:opacity-50"
            :class="
              selectedConversationId ===
              conversation.id
                ? 'bg-ai-soft text-ai'
                : 'text-app-text hover:bg-app-surface-muted'
            "
            @click="
              selectConversation(
                conversation.id,
              )
            "
          >
            <div
              class="flex items-start gap-2.5"
            >
              <MessageSquare
                :size="16"
                class="mt-0.5 shrink-0 opacity-65"
              />

              <div class="min-w-0">
                <p
                  class="line-clamp-2 text-sm font-semibold leading-5"
                >
                  {{ conversation.title }}
                </p>

                <p
                  class="mt-1 text-[11px] opacity-60"
                >
                  {{
                    formatDateTime(
                      conversation.lastMessageAt,
                    )
                  }}
                </p>
              </div>
            </div>
          </button>

          <!-- Load-more indicator -->
          <div
            v-if="
              loadingMoreConversations
            "
            class="flex items-center justify-center gap-2 py-3 text-xs text-app-text-muted"
          >
            <span
              class="h-3.5 w-3.5 animate-spin rounded-full border-2 border-current border-r-transparent"
            />

            Đang tải thêm...
          </div>
        </nav>
      </aside>

      <!-- ================================================================ -->
      <!-- Main chat                                                        -->
      <!-- ================================================================ -->

      <div
        class="flex min-h-[42rem] min-w-0 flex-col bg-app-surface lg:min-h-0"
      >
        <!-- Conversation header -->
        <header
          class="flex min-h-[4.5rem] items-center justify-between gap-4 border-b border-app-border px-5 py-3"
        >
          <div class="min-w-0">
            <h3
              class="truncate text-sm font-semibold text-app-text"
            >
              {{
                selectedConversation?.title ??
                'Hội thoại mới'
              }}
            </h3>

            <p
              class="mt-1 text-xs text-app-text-muted"
            >
              AI có thể mắc lỗi. Hãy kiểm tra
              nguồn khi cần.
            </p>
          </div>

          <div
            class="flex h-9 w-9 shrink-0 items-center justify-center rounded-pill bg-ai-soft text-ai"
          >
            <Bot :size="18" />
          </div>
        </header>

        <!-- ============================================================ -->
        <!-- Message scroll area                                          -->
        <!-- ============================================================ -->

        <main
          ref="messageList"
          class="relative min-h-0 flex-1 overflow-y-auto bg-app-bg/35 px-4 py-6 sm:px-6"
          @scroll.passive="
            handleMessageScroll
          "
        >
          <!-- Older messages loading -->
          <div
            v-if="loadingOlderMessages"
            class="pointer-events-none absolute top-3 left-1/2 z-10 -translate-x-1/2"
          >
            <div
              class="flex items-center gap-2 rounded-pill border border-app-border bg-app-surface px-3 py-1.5 text-xs text-app-text-muted shadow-card"
            >
              <span
                class="h-3 w-3 animate-spin rounded-full border-2 border-current border-r-transparent"
              />

              Đang tải tin nhắn cũ
            </div>
          </div>

          <div
            class="mx-auto w-full max-w-4xl space-y-6"
          >
            <BaseAlert
              v-if="messageError"
            >
              {{ messageError }}
            </BaseAlert>

            <!-- Initial message loading -->
            <div
              v-if="loadingMessages"
              class="space-y-6"
            >
              <div class="flex gap-3">
                <div
                  class="h-9 w-9 shrink-0 animate-pulse rounded-pill bg-ai-soft"
                />

                <div
                  class="h-24 w-3/4 animate-pulse rounded-card bg-app-surface-muted"
                />
              </div>

              <div
                class="flex flex-row-reverse gap-3"
              >
                <div
                  class="h-9 w-9 shrink-0 animate-pulse rounded-pill bg-secondary-soft"
                />

                <div
                  class="h-16 w-1/2 animate-pulse rounded-card bg-secondary-soft/70"
                />
              </div>
            </div>

            <!-- AI empty state -->
            <div
              v-else-if="
                messagesPage.content.length ===
                  0 &&
                !pendingPrompt &&
                !sending
              "
              class="flex min-h-[28rem] flex-col items-center justify-center py-8 text-center"
            >
              <div
                class="flex h-14 w-14 items-center justify-center rounded-panel bg-ai-soft text-ai"
              >
                <Sparkles :size="26" />
              </div>

              <h3
                class="mt-5 font-heading text-xl font-bold tracking-tight text-app-text"
              >
                Tôi có thể giúp gì cho bạn?
              </h3>

              <p
                class="mt-2 max-w-md text-sm leading-6 text-app-text-muted"
              >
                Hỏi về bài học, tài liệu hoặc
                yêu cầu AI tạo nội dung ôn tập
                dựa trên khóa học.
              </p>

              <div
                class="mt-6 grid w-full max-w-2xl gap-2 sm:grid-cols-3"
              >
                <button
                  v-for="
                    suggestion in
                    suggestedPrompts
                  "
                  :key="suggestion"
                  type="button"
                  class="rounded-card border border-app-border bg-app-surface p-4 text-left text-sm leading-5 text-app-text shadow-card transition hover:-translate-y-0.5 hover:border-ai/30 hover:bg-ai-soft/30"
                  @click="
                    selectSuggestion(
                      suggestion,
                    )
                  "
                >
                  {{ suggestion }}
                </button>
              </div>
            </div>

            <!-- Messages -->
            <template v-else>
              <article
                v-for="
                  message in
                  messagesPage.content
                "
                :key="message.id"
                class="flex w-full gap-3"
                :class="
                  message.role === 'USER'
                    ? 'flex-row-reverse'
                    : ''
                "
              >
                <!-- Avatar -->
                <div
                  class="flex h-9 w-9 shrink-0 items-center justify-center rounded-pill"
                  :class="
                    message.role === 'USER'
                      ? 'bg-secondary-soft text-secondary'
                      : 'bg-ai-soft text-ai'
                  "
                >
                  <UserRound
                    v-if="
                      message.role === 'USER'
                    "
                    :size="17"
                  />

                  <Bot
                    v-else
                    :size="17"
                  />
                </div>

                <!-- Message body -->
                <div
                  class="min-w-0"
                  :class="
                    message.role === 'USER'
                      ? 'max-w-[80%] sm:max-w-[70%]'
                      : 'max-w-[calc(100%-3rem)] flex-1'
                  "
                >
                  <!-- User -->
                  <div
                    v-if="
                      message.role === 'USER'
                    "
                    class="whitespace-pre-wrap rounded-[1rem] rounded-tr-sm bg-secondary px-4 py-3 text-sm leading-6 text-white shadow-card"
                  >
                    {{ message.content }}
                  </div>

                  <!-- Assistant -->
                  <div
                    v-else
                    class="text-sm leading-7 text-app-text"
                  >
                    <AiMessageContent :content="message.content" />
                  </div>

                  <p
                    class="mt-2 text-[11px] text-app-text-muted"
                    :class="
                      message.role === 'USER'
                        ? 'text-right'
                        : ''
                    "
                  >
                    {{
                      formatDateTime(
                        message.createdAt,
                      )
                    }}
                  </p>
                </div>
              </article>

              <!-- Optimistic user message -->
              <article
                v-if="pendingPrompt"
                class="flex w-full flex-row-reverse gap-3"
              >
                <div
                  class="flex h-9 w-9 shrink-0 items-center justify-center rounded-pill bg-secondary-soft text-secondary"
                >
                  <UserRound :size="17" />
                </div>

                <div
                  class="max-w-[80%] sm:max-w-[70%]"
                >
                  <div
                    class="whitespace-pre-wrap rounded-[1rem] rounded-tr-sm bg-secondary px-4 py-3 text-sm leading-6 text-white shadow-card"
                  >
                    {{ pendingPrompt }}
                  </div>

                  <p
                    class="mt-2 text-right text-[11px] text-app-text-muted"
                  >
                    Vừa gửi
                  </p>
                </div>
              </article>

              <!-- AI generating -->
              <article
                v-if="sending"
                class="flex w-full gap-3"
              >
                <div
                  class="flex h-9 w-9 shrink-0 items-center justify-center rounded-pill bg-ai-soft text-ai"
                >
                  <Bot :size="17" />
                </div>

                <div
                  class="min-w-0 flex-1"
                >
                  <div
                    class="inline-flex items-center gap-3 rounded-card border border-ai/10 bg-ai-soft/50 px-4 py-3"
                  >
                    <Sparkles
                      :size="16"
                      class="animate-pulse text-ai"
                    />

                    <div>
                      <div
                        class="flex items-center gap-2"
                      >
                        <p
                          class="text-sm font-medium text-app-text"
                        >
                          Đang tạo câu trả lời
                        </p>

                        <div
                          class="flex items-center gap-1"
                          aria-label="Đang tạo câu trả lời"
                        >
                          <span
                            class="ai-thinking-dot"
                          />
                          <span
                            class="ai-thinking-dot"
                          />
                          <span
                            class="ai-thinking-dot"
                          />
                        </div>
                      </div>

                      <p
                        class="mt-1 text-xs text-app-text-muted"
                      >
                        Đang xử lý thông tin liên quan
                        đến câu hỏi của bạn
                      </p>
                    </div>
                  </div>
                </div>
              </article>
            </template>
          </div>
        </main>

        <!-- ============================================================ -->
        <!-- Composer                                                     -->
        <!-- ============================================================ -->

        <form
          class="border-t border-app-border bg-app-surface px-4 py-4 sm:px-6"
          @submit.prevent="
            submitPrompt
          "
        >
          <div
            class="mx-auto max-w-4xl"
          >
            <div
              class="flex items-end gap-3 rounded-[1rem] border bg-app-surface px-4 py-2 shadow-card transition focus-within:border-ai focus-within:ring-2 focus-within:ring-ai/10"
              :class="
                promptError
                  ? 'border-danger'
                  : 'border-app-border'
              "
            >
              <textarea
                v-model="prompt"
                rows="1"
                maxlength="10000"
                placeholder="Hỏi AI về khóa học..."
                class="max-h-40 min-h-11 flex-1 resize-none bg-transparent py-2 text-sm leading-6 text-app-text outline-none placeholder:text-app-text-muted"
                :disabled="sending"
                @keydown="
                  handlePromptKeydown
                "
              />

              <button
                type="submit"
                aria-label="Gửi tin nhắn"
                class="mb-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-pill bg-ai text-white transition hover:bg-ai-hover disabled:cursor-not-allowed disabled:opacity-40"
                :disabled="
                  !canSubmit
                "
              >
                <Send :size="16" />
              </button>
            </div>

            <div
              class="mt-2 flex items-start justify-between gap-3 px-1"
            >
              <p
                v-if="promptError"
                role="alert"
                class="text-xs text-danger"
              >
                {{ promptError }}
              </p>

              <p
                v-else
                class="text-xs text-app-text-muted"
              >
                Enter để gửi · Shift + Enter
                để xuống dòng
              </p>

              <span
                class="shrink-0 text-[11px] text-app-text-muted"
              >
                {{ prompt.length }}/10000
              </span>
            </div>
          </div>
        </form>
      </div>
    </div>
  </section>
</template>

<style scoped>
.ai-thinking-dot {
  width: 0.3rem;
  height: 0.3rem;
  border-radius: 9999px;
  background: var(--color-ai);
  animation:
    ai-thinking 1.2s infinite ease-in-out;
}

.ai-thinking-dot:nth-child(2) {
  animation-delay: 150ms;
}

.ai-thinking-dot:nth-child(3) {
  animation-delay: 300ms;
}

@keyframes ai-thinking {
  0%,
  60%,
  100% {
    opacity: 0.25;
    transform: translateY(0);
  }

  30% {
    opacity: 1;
    transform: translateY(-0.2rem);
  }
}
</style>