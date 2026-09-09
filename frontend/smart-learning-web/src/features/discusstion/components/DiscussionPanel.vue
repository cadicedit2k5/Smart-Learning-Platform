<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  ArrowLeft,
  BookOpen,
  MessageCircle,
  MessagesSquare,
  Plus,
  Reply as ReplyIcon,
  Send,
} from 'lucide-vue-next'

import { BaseAlert, BaseButton, BaseInput, BaseModal } from '@/shared/components'
import { parseApiError } from '@/shared/api'
import {
  getChapters,
  getTopics,
  type CourseChapter,
  type CourseTopic,
  type TopicContent,
} from '@/shared/course-content'
import {
  createEmptyRichText,
  hasRichTextContent,
  RichTextEditor,
  RichTextViewer,
} from '@/shared/rich-text'
import { formatDateTime } from '@/shared/utils/date'

import {
  createDiscussion,
  createDiscussionReply,
  getDiscussions,
} from '../api'
import type { Discussion } from '../types'

const props = defineProps<{
  courseId: string
}>()

interface TopicGroup {
  chapter: CourseChapter
  topics: CourseTopic[]
}

const discussions = ref<Discussion[]>([])
const selectedDiscussion = ref<Discussion | null>(null)
const topicGroups = ref<TopicGroup[]>([])

const loading = ref(true)
const submitting = ref(false)
const replying = ref(false)
const message = ref('')
const successMessage = ref('')

const createOpen = ref(false)
const topicsLoaded = ref(false)

const title = ref('')
const topicId = ref('')
const content = ref<TopicContent>(createEmptyRichText())

const replyOpen = ref(false)
const replyContent = ref<TopicContent>(createEmptyRichText())

const discussionCount = computed(() => discussions.value.length)

const errorMessage = (error: unknown, fallback: string) => {
  const parsed = parseApiError(error)
  return parsed.statusCode ? parsed.message : fallback
}

const authorName = (discussion: Discussion) => {
  return discussion.author?.fullName || 'Thành viên'
}

const initial = (name?: string | null) => {
  return name?.trim().charAt(0).toUpperCase() || '?'
}

const extractText = (value: unknown): string => {
  if (Array.isArray(value)) {
    return value.map(extractText).join(' ')
  }

  if (value && typeof value === 'object') {
    const record = value as Record<string, unknown>

    if (typeof record.text === 'string') {
      return record.text
    }

    if (record.content) {
      return extractText(record.content)
    }
  }

  return ''
}

const preview = (content: TopicContent) => {
  const text = extractText(content).replace(/\s+/g, ' ').trim()
  return text || 'Không có nội dung xem trước.'
}

const loadDiscussions = async () => {
  loading.value = true
  message.value = ''

  try {
    discussions.value = await getDiscussions(props.courseId)

    if (selectedDiscussion.value) {
      selectedDiscussion.value =
        discussions.value.find((item) => item.id === selectedDiscussion.value?.id) ?? null
    }
  } catch (error) {
    message.value = errorMessage(error, 'Không thể tải danh sách thảo luận.')
  } finally {
    loading.value = false
  }
}

const loadTopicGroups = async () => {
  if (topicsLoaded.value) return

  try {
    const chapters = await getChapters(props.courseId)

    topicGroups.value = await Promise.all(
      chapters.map(async (chapter) => ({
        chapter,
        topics: await getTopics(props.courseId, chapter.id),
      })),
    )

    topicsLoaded.value = true
  } catch {
    topicGroups.value = []
  }
}

const resetDiscussionForm = () => {
  title.value = ''
  topicId.value = ''
  content.value = createEmptyRichText()
}

const openCreate = async () => {
  message.value = ''
  successMessage.value = ''
  resetDiscussionForm()
  createOpen.value = true
  await loadTopicGroups()
}

const closeCreate = () => {
  if (submitting.value) return

  createOpen.value = false
  resetDiscussionForm()
}

const submitDiscussion = async () => {
  if (!title.value.trim()) {
    message.value = 'Vui lòng nhập tiêu đề thảo luận.'
    return
  }

  if (!hasRichTextContent(content.value)) {
    message.value = 'Vui lòng nhập nội dung thảo luận.'
    return
  }

  submitting.value = true
  message.value = ''
  successMessage.value = ''

  try {
    const created = await createDiscussion(props.courseId, {
      topicId: topicId.value || null,
      title: title.value.trim(),
      content: content.value,
    })

    discussions.value.unshift(created)
    selectedDiscussion.value = created

    createOpen.value = false
    resetDiscussionForm()

    successMessage.value = 'Đã tạo thảo luận.'
  } catch (error) {
    message.value = errorMessage(error, 'Không thể tạo thảo luận.')
  } finally {
    submitting.value = false
  }
}

const openDiscussion = (discussion: Discussion) => {
  selectedDiscussion.value = discussion
  replyOpen.value = false
  replyContent.value = createEmptyRichText()
  message.value = ''
  successMessage.value = ''
}

const backToDiscussions = () => {
  selectedDiscussion.value = null
  replyOpen.value = false
  replyContent.value = createEmptyRichText()
  message.value = ''
  successMessage.value = ''
}

const openReply = () => {
  replyContent.value = createEmptyRichText()
  replyOpen.value = true
  message.value = ''
}

const cancelReply = () => {
  if (replying.value) return

  replyOpen.value = false
  replyContent.value = createEmptyRichText()
}

const submitReply = async () => {
  if (!selectedDiscussion.value) return

  if (!hasRichTextContent(replyContent.value)) {
    message.value = 'Vui lòng nhập nội dung phản hồi.'
    return
  }

  replying.value = true
  message.value = ''
  successMessage.value = ''

  try {
    const updated = await createDiscussionReply(
      props.courseId,
      selectedDiscussion.value.id,
      replyContent.value,
    )

    const index = discussions.value.findIndex((item) => item.id === updated.id)

    if (index >= 0) {
      discussions.value[index] = updated
    }

    selectedDiscussion.value = updated
    replyOpen.value = false
    replyContent.value = createEmptyRichText()

    successMessage.value = 'Đã gửi phản hồi.'
  } catch (error) {
    message.value = errorMessage(error, 'Không thể gửi phản hồi.')
  } finally {
    replying.value = false
  }
}

const resetForCourse = () => {
  discussions.value = []
  selectedDiscussion.value = null
  topicGroups.value = []
  topicsLoaded.value = false
  createOpen.value = false
  replyOpen.value = false
  message.value = ''
  successMessage.value = ''
  resetDiscussionForm()
  replyContent.value = createEmptyRichText()
  void loadDiscussions()
}

watch(() => props.courseId, resetForCourse)

onMounted(() => void loadDiscussions())
</script>

<template>
  <section class="space-y-5">
    <BaseAlert v-if="message">
      {{ message }}
    </BaseAlert>

    <BaseAlert v-if="successMessage" variant="ai">
      {{ successMessage }}
    </BaseAlert>

    <!-- =====================================================
         DISCUSSION LIST
         ===================================================== -->
    <template v-if="!selectedDiscussion">
      <header class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 class="font-heading text-xl font-bold text-app-text">
            Thảo luận
          </h2>

          <p class="mt-1 text-sm text-app-text-muted">
            Đặt câu hỏi và trao đổi với các thành viên trong khóa học.
          </p>
        </div>

        <BaseButton @click="openCreate">
          <template #leading>
            <Plus :size="17" />
          </template>
          Tạo thảo luận
        </BaseButton>
      </header>

      <div v-if="loading" class="space-y-3">
        <div
          v-for="index in 4"
          :key="index"
          class="h-32 animate-pulse rounded-card bg-app-surface-muted"
        />
      </div>

      <div
        v-else-if="discussions.length === 0"
        class="rounded-panel border border-dashed border-app-border bg-app-surface px-6 py-14 text-center"
      >
        <MessagesSquare :size="40" class="mx-auto text-app-text-muted/40" />

        <h3 class="mt-4 font-heading text-lg font-bold text-app-text">
          Chưa có thảo luận
        </h3>

        <p class="mt-2 text-sm text-app-text-muted">
          Hãy bắt đầu câu hỏi hoặc chủ đề đầu tiên.
        </p>

        <BaseButton class="mt-5" @click="openCreate">
          <template #leading>
            <Plus :size="17" />
          </template>
          Tạo thảo luận
        </BaseButton>
      </div>

      <div v-else class="space-y-3">
        <div class="flex items-center justify-between">
          <p class="text-sm text-app-text-muted">
            {{ discussionCount }} thảo luận
          </p>
        </div>

        <button
          v-for="discussion in discussions"
          :key="discussion.id"
          type="button"
          class="group w-full rounded-card border border-app-border bg-app-surface p-5 text-left shadow-card transition hover:border-secondary/40 hover:shadow-md"
          @click="openDiscussion(discussion)"
        >
          <div class="flex gap-4">
            <span
              class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-secondary-soft text-sm font-bold text-secondary"
            >
              {{ initial(authorName(discussion)) }}
            </span>

            <div class="min-w-0 flex-1">
              <div class="flex flex-wrap items-center gap-x-2 gap-y-1">
                <span class="text-sm font-semibold text-app-text">
                  {{ authorName(discussion) }}
                </span>

                <span class="text-xs text-app-text-muted">
                  · {{ formatDateTime(discussion.createdAt) }}
                </span>
              </div>

              <div class="mt-3 flex flex-wrap items-center gap-2">
                <h3 class="font-heading text-lg font-bold text-app-text">
                  {{ discussion.title }}
                </h3>

                <span
                  v-if="discussion.topicTitle"
                  class="rounded-pill bg-secondary-soft px-2.5 py-1 text-xs font-semibold text-secondary"
                >
                  {{ discussion.topicTitle }}
                </span>
              </div>

              <p class="mt-2 line-clamp-2 max-w-4xl text-sm leading-6 text-app-text-muted">
                {{ preview(discussion.content) }}
              </p>

              <div class="mt-4 flex items-center justify-between gap-4">
                <span class="inline-flex items-center gap-1.5 text-sm text-app-text-muted">
                  <MessageCircle :size="15" />
                  {{ discussion.replies.length }}
                  {{ discussion.replies.length === 1 ? 'phản hồi' : 'phản hồi' }}
                </span>

                <span class="text-sm font-semibold text-secondary transition group-hover:translate-x-1">
                  Xem thảo luận →
                </span>
              </div>
            </div>
          </div>
        </button>
      </div>
    </template>

    <!-- =====================================================
         DISCUSSION DETAIL
         ===================================================== -->
    <template v-else>
      <button
        type="button"
        class="inline-flex items-center gap-2 text-sm font-semibold text-app-text-muted transition hover:text-secondary"
        @click="backToDiscussions"
      >
        <ArrowLeft :size="16" />
        Danh sách thảo luận
      </button>

      <article class="rounded-card border border-app-border bg-app-surface p-6 shadow-card">
        <div class="flex items-start gap-3">
          <span
            class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-secondary-soft font-bold text-secondary"
          >
            {{ initial(authorName(selectedDiscussion)) }}
          </span>

          <div class="min-w-0 flex-1">
            <p class="font-semibold text-app-text">
              {{ authorName(selectedDiscussion) }}
            </p>

            <p class="mt-0.5 text-xs text-app-text-muted">
              {{ formatDateTime(selectedDiscussion.createdAt) }}
            </p>
          </div>
        </div>

        <div class="mt-5">
          <div class="flex flex-wrap items-center gap-2">
            <span
              v-if="selectedDiscussion.topicTitle"
              class="inline-flex items-center gap-1.5 rounded-pill bg-secondary-soft px-3 py-1 text-xs font-semibold text-secondary"
            >
              <BookOpen :size="13" />
              {{ selectedDiscussion.topicTitle }}
            </span>
          </div>

          <h2 class="mt-3 font-heading text-2xl font-bold text-app-text">
            {{ selectedDiscussion.title }}
          </h2>

          <div class="mt-5">
            <RichTextViewer :content="selectedDiscussion.content" />
          </div>
        </div>
      </article>

      <!-- ===================================================
           REPLIES
           =================================================== -->
      <section class="rounded-card border border-app-border bg-app-surface shadow-card">
        <header class="flex flex-wrap items-center justify-between gap-4 border-b border-app-border px-5 py-4">
          <div>
            <h3 class="font-heading text-lg font-bold text-app-text">
              Phản hồi
            </h3>

            <p class="mt-1 text-sm text-app-text-muted">
              {{ selectedDiscussion.replies.length }} phản hồi
            </p>
          </div>

          <BaseButton v-if="!replyOpen" variant="secondary" @click="openReply">
            <template #leading>
              <ReplyIcon :size="16" />
            </template>
            Trả lời
          </BaseButton>
        </header>

        <div
          v-if="selectedDiscussion.replies.length === 0"
          class="px-6 py-10 text-center"
        >
          <MessageCircle :size="32" class="mx-auto text-app-text-muted/30" />

          <p class="mt-3 font-semibold text-app-text">
            Chưa có phản hồi
          </p>

          <p class="mt-1 text-sm text-app-text-muted">
            Hãy là người đầu tiên phản hồi thảo luận này.
          </p>
        </div>

        <div v-else class="divide-y divide-app-border">
          <article
            v-for="reply in selectedDiscussion.replies"
            :key="reply.id"
            class="flex gap-3 px-5 py-5"
          >
            <span
              class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-app-surface-muted text-xs font-bold text-app-text"
            >
              {{ initial(reply.author?.fullName) }}
            </span>

            <div class="min-w-0 flex-1">
              <div class="flex flex-wrap items-center gap-x-2 gap-y-1">
                <span class="text-sm font-semibold text-app-text">
                  {{ reply.author?.fullName || 'Thành viên' }}
                </span>

                <span class="text-xs text-app-text-muted">
                  {{ formatDateTime(reply.createdAt) }}
                </span>
              </div>

              <div class="mt-2">
                <RichTextViewer :content="reply.content" compact />
              </div>
            </div>
          </article>
        </div>

        <!-- Reply composer -->
        <form
          v-if="replyOpen"
          class="border-t border-app-border p-5"
          @submit.prevent="submitReply"
        >
          <div class="flex items-center gap-2">
            <ReplyIcon :size="17" class="text-secondary" />

            <p class="font-semibold text-app-text">
              Viết phản hồi
            </p>
          </div>

          <div class="mt-3">
            <RichTextEditor
              v-model="replyContent"
              compact
              :disabled="replying"
            />
          </div>

          <div class="mt-4 flex justify-end gap-3">
            <BaseButton
              type="button"
              variant="secondary"
              :disabled="replying"
              @click="cancelReply"
            >
              Hủy
            </BaseButton>

            <BaseButton type="submit" :loading="replying">
              <template #leading>
                <Send :size="15" />
              </template>
              Gửi phản hồi
            </BaseButton>
          </div>
        </form>
      </section>
    </template>

    <!-- =====================================================
         CREATE DISCUSSION MODAL
         ===================================================== -->
    <BaseModal
      :open="createOpen"
      title="Tạo thảo luận"
      description="Đặt câu hỏi hoặc bắt đầu một chủ đề trao đổi với các thành viên trong khóa học."
      :loading="submitting"
      max-width="max-w-3xl"
      @close="closeCreate"
    >
      <form class="space-y-5" @submit.prevent="submitDiscussion">
        <BaseInput
          v-model="title"
          label="Tiêu đề"
          maxlength="255"
          placeholder="Ví dụ: Em chưa hiểu Dependency Injection"
          :disabled="submitting"
        />

        <label class="block">
          <span class="mb-2 block text-sm font-semibold text-app-text">
            Bài học liên quan
          </span>

          <select
            v-model="topicId"
            :disabled="submitting"
            class="h-11 w-full rounded-control border border-app-border bg-app-surface px-3 text-sm text-app-text outline-none focus:border-secondary focus:ring-2 focus:ring-secondary/20"
          >
            <option value="">
              Thảo luận chung
            </option>

            <optgroup
              v-for="group in topicGroups"
              :key="group.chapter.id"
              :label="group.chapter.title"
            >
              <option
                v-for="topic in group.topics"
                :key="topic.id"
                :value="topic.id"
              >
                {{ topic.title }}
              </option>
            </optgroup>
          </select>

          <p class="mt-1.5 text-xs text-app-text-muted">
            Không bắt buộc. Chọn bài học nếu câu hỏi liên quan đến một nội dung cụ thể.
          </p>
        </label>

        <div>
          <p class="mb-2 text-sm font-semibold text-app-text">
            Nội dung
          </p>

          <RichTextEditor
            v-model="content"
            :disabled="submitting"
          />
        </div>

        <div class="flex justify-end gap-3 border-t border-app-border pt-5">
          <BaseButton
            type="button"
            variant="secondary"
            :disabled="submitting"
            @click="closeCreate"
          >
            Hủy
          </BaseButton>

          <BaseButton type="submit" :loading="submitting">
            <template #leading>
              <Plus :size="16" />
            </template>
            Đăng thảo luận
          </BaseButton>
        </div>
      </form>
    </BaseModal>
  </section>
</template>