<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import {
  MessageCircle,
  MessagesSquare,
  Plus,
  Reply as ReplyIcon,
  Send,
} from 'lucide-vue-next'

import { BaseAlert, BaseButton, BaseInput } from '@/shared/components'
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
const topicGroups = ref<TopicGroup[]>([])

const loading = ref(true)
const submitting = ref(false)
const replyingId = ref<string | null>(null)
const message = ref('')

const title = ref('')
const topicId = ref('')
const content = ref<TopicContent>(createEmptyRichText())

const replyContent = ref<TopicContent>(createEmptyRichText())

const errorMessage = (error: unknown, fallback: string) => {
  const parsed = parseApiError(error)
  return parsed.statusCode ? parsed.message : fallback
}

const authorName = (discussion: Discussion) =>
  discussion.author?.fullName || 'Thành viên'

const initial = (name?: string | null) =>
  name?.trim().charAt(0).toUpperCase() || '?'

const loadDiscussions = async () => {
  loading.value = true
  message.value = ''

  try {
    discussions.value = await getDiscussions(props.courseId)
  } catch (error) {
    message.value = errorMessage(
      error,
      'Không thể tải danh sách thảo luận.',
    )
  } finally {
    loading.value = false
  }
}

const loadTopicGroups = async () => {
  try {
    const chapters = await getChapters(props.courseId)

    topicGroups.value = await Promise.all(
      chapters.map(async (chapter) => ({
        chapter,
        topics: await getTopics(props.courseId, chapter.id),
      })),
    )
  } catch {
    topicGroups.value = []
  }
}

const resetDiscussionForm = () => {
  title.value = ''
  topicId.value = ''
  content.value = createEmptyRichText()
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

  try {
    const created = await createDiscussion(props.courseId, {
      topicId: topicId.value || null,
      title: title.value.trim(),
      content: content.value,
    })

    discussions.value.unshift(created)
    resetDiscussionForm()
  } catch (error) {
    message.value = errorMessage(
      error,
      'Không thể tạo thảo luận.',
    )
  } finally {
    submitting.value = false
  }
}

const openReply = (discussionId: string) => {
  replyingId.value = discussionId
  replyContent.value = createEmptyRichText()
}

const cancelReply = () => {
  replyingId.value = null
  replyContent.value = createEmptyRichText()
}

const submitReply = async (discussion: Discussion) => {
  if (!hasRichTextContent(replyContent.value)) {
    message.value = 'Vui lòng nhập nội dung phản hồi.'
    return
  }

  message.value = ''

  try {
    const updated = await createDiscussionReply(
      props.courseId,
      discussion.id,
      replyContent.value,
    )

    const index = discussions.value.findIndex(
      (item) => item.id === updated.id,
    )

    if (index >= 0) {
      discussions.value.splice(index, 1, updated)
    }

    cancelReply()
  } catch (error) {
    message.value = errorMessage(
      error,
      'Không thể gửi phản hồi.',
    )
  }
}

const reload = () => {
  resetDiscussionForm()
  cancelReply()
  void loadDiscussions()
  void loadTopicGroups()
}

watch(() => props.courseId, reload)

onMounted(reload)
</script>

<template>
  <section class="space-y-5">
    <header>
      <h2 class="font-heading text-xl font-bold text-app-text">
        Thảo luận
      </h2>

      <p class="mt-1 text-sm text-app-text-muted">
        Đặt câu hỏi và trao đổi với các thành viên trong khóa học.
      </p>
    </header>

    <BaseAlert v-if="message">
      {{ message }}
    </BaseAlert>

    <!-- Create discussion -->
    <form
      class="rounded-card border border-app-border bg-app-surface p-5 shadow-card"
      @submit.prevent="submitDiscussion"
    >
      <div class="flex items-center gap-2">
        <Plus :size="18" class="text-secondary" />
        <h3 class="font-semibold text-app-text">
          Tạo thảo luận mới
        </h3>
      </div>

      <div class="mt-5 grid gap-4 md:grid-cols-[minmax(0,1fr)_18rem]">
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
        </label>
      </div>

      <div class="mt-4">
        <p class="mb-2 text-sm font-semibold text-app-text">
          Nội dung
        </p>

        <RichTextEditor
          v-model="content"
          :disabled="submitting"
        />
      </div>

      <div class="mt-4 flex justify-end">
        <BaseButton type="submit" :loading="submitting">
          <template #leading>
            <MessageCircle :size="16" />
          </template>
          Đăng thảo luận
        </BaseButton>
      </div>
    </form>

    <!-- Loading -->
    <div v-if="loading" class="space-y-3">
      <div
        v-for="index in 3"
        :key="index"
        class="h-44 animate-pulse rounded-card bg-app-surface-muted"
      />
    </div>

    <!-- Empty -->
    <div
      v-else-if="discussions.length === 0"
      class="rounded-card border border-dashed border-app-border bg-app-surface px-6 py-12 text-center"
    >
      <MessagesSquare
        :size="36"
        class="mx-auto text-app-text-muted/40"
      />

      <p class="mt-3 font-semibold text-app-text">
        Chưa có thảo luận
      </p>

      <p class="mt-1 text-sm text-app-text-muted">
        Hãy bắt đầu câu hỏi hoặc chủ đề đầu tiên.
      </p>
    </div>

    <!-- Discussions -->
    <div v-else class="space-y-4">
      <article
        v-for="discussion in discussions"
        :key="discussion.id"
        class="overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card"
      >
        <div class="p-5">
          <div class="flex items-start gap-3">
            <span
              class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-secondary-soft text-sm font-bold text-secondary"
            >
              {{ initial(authorName(discussion)) }}
            </span>

            <div class="min-w-0 flex-1">
              <p class="text-sm font-semibold text-app-text">
                {{ authorName(discussion) }}
              </p>

              <p class="mt-0.5 text-xs text-app-text-muted">
                {{ formatDateTime(discussion.createdAt) }}
              </p>
            </div>
          </div>

          <div class="mt-4">
            <span
              v-if="discussion.topicTitle"
              class="mb-3 inline-flex rounded-pill bg-secondary-soft px-3 py-1 text-xs font-semibold text-secondary"
            >
              Bài học: {{ discussion.topicTitle }}
            </span>

            <h3 class="font-heading text-lg font-bold text-app-text">
              {{ discussion.title }}
            </h3>

            <div class="mt-3">
              <RichTextViewer :content="discussion.content" />
            </div>
          </div>
        </div>

        <!-- Replies -->
        <div class="border-t border-app-border bg-app-surface-muted/30 px-5 py-4">
          <div class="flex items-center justify-between gap-3">
            <p class="text-sm font-semibold text-app-text">
              {{ discussion.replies.length }} phản hồi
            </p>

            <button
              type="button"
              class="inline-flex items-center gap-1.5 text-sm font-semibold text-secondary hover:underline"
              @click="openReply(discussion.id)"
            >
              <ReplyIcon :size="15" />
              Trả lời
            </button>
          </div>

          <div
            v-if="discussion.replies.length > 0"
            class="mt-4 space-y-4"
          >
            <div
              v-for="reply in discussion.replies"
              :key="reply.id"
              class="flex gap-3"
            >
              <span
                class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-app-surface-muted text-xs font-bold text-app-text"
              >
                {{ initial(reply.author?.fullName) }}
              </span>

              <div class="min-w-0 flex-1 rounded-card border border-app-border bg-app-surface p-3">
                <div class="flex flex-wrap items-center gap-x-2 gap-y-1">
                  <span class="text-sm font-semibold text-app-text">
                    {{ reply.author?.fullName || 'Thành viên' }}
                  </span>

                  <span class="text-xs text-app-text-muted">
                    {{ formatDateTime(reply.createdAt) }}
                  </span>
                </div>

                <div class="mt-2">
                  <RichTextViewer
                    :content="reply.content"
                    compact
                  />
                </div>
              </div>
            </div>
          </div>

          <p
            v-else
            class="mt-3 text-sm text-app-text-muted"
          >
            Chưa có phản hồi nào.
          </p>

          <!-- Reply editor -->
          <form
            v-if="replyingId === discussion.id"
            class="mt-4 rounded-card border border-app-border bg-app-surface p-4"
            @submit.prevent="submitReply(discussion)"
          >
            <p class="mb-2 text-sm font-semibold text-app-text">
              Viết phản hồi
            </p>

            <RichTextEditor
              v-model="replyContent"
              compact
            />

            <div class="mt-3 flex justify-end gap-2">
              <BaseButton
                type="button"
                variant="secondary"
                @click="cancelReply"
              >
                Hủy
              </BaseButton>

              <BaseButton type="submit">
                <template #leading>
                  <Send :size="15" />
                </template>
                Gửi phản hồi
              </BaseButton>
            </div>
          </form>
        </div>
      </article>
    </div>
  </section>
</template>