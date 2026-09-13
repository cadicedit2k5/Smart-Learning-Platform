<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { Bot, ChevronDown, MessageCircle, SendHorizontal, Sparkles, X } from 'lucide-vue-next'
import { storedRichTextToPlainText } from '@/shared/rich-text'
import type { PublicCourseDetail } from '../api/courseApi'

const props = defineProps<{ course: PublicCourseDetail }>()

interface ChatMessage {
  id: number
  role: 'assistant' | 'user'
  content: string
}

const opened = ref(false)
const question = ref('')
const messageList = ref<HTMLElement | null>(null)
let nextId = 1

const suggestions = [
  'Khóa học này dành cho ai?',
  'Khóa học này học những gì?',
  'Chương trình gồm những gì?',
  'Học khóa này mất bao lâu?',
]

const messages = ref<ChatMessage[]>([
  {
    id: nextId++,
    role: 'assistant',
    content: `Xin chào! Mình là trợ lý tư vấn cho khóa học "${props.course.title}". Bạn có thể hỏi mình về nội dung, cấp độ, chương trình học hoặc thời lượng của khóa học.`,
  },
])

const normalize = (value: string) =>
  value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').toLowerCase().trim()

const totalTopics = () => props.course.chapters.reduce((total, chapter) => total + chapter.topics.length, 0)

const totalMinutes = () =>
  props.course.chapters.reduce(
    (total, chapter) =>
      total + chapter.topics.reduce((chapterTotal, topic) => chapterTotal + (topic.estimatedMinutes ?? 0), 0),
    0,
  )

const formatDuration = (minutes: number) => {
  if (!minutes) return 'chưa được xác định'
  if (minutes < 60) return `${minutes} phút`

  const hours = Math.floor(minutes / 60)
  const remaining = minutes % 60
  return remaining ? `${hours} giờ ${remaining} phút` : `${hours} giờ`
}

const curriculum = () => {
  if (!props.course.chapters.length) return 'Khóa học hiện chưa công bố chương trình học.'

  return props.course.chapters
    .map((chapter, index) => {
      const topics = chapter.topics.map((topic) => topic.title).join(', ')
      return `${index + 1}. ${chapter.title}${topics ? ` — ${topics}` : ''}`
    })
    .join('\n')
}

const findChapter = (value: string) => {
  const match = value.match(/chuong\s*(\d+)/)
  if (!match) return null

  const index = Number(match[1]) - 1
  const chapter = props.course.chapters[index]

  if (!chapter) return `Khóa học không có chương ${index + 1}.`

  const result = [`Chương ${index + 1}: ${chapter.title}.`]

  if (chapter.description) result.push(chapter.description)
  if (chapter.learningObjectives) result.push(`Mục tiêu học tập: ${chapter.learningObjectives}.`)

  if (chapter.topics.length) {
    result.push(`Chương này gồm ${chapter.topics.length} bài: ${chapter.topics.map((topic) => topic.title).join(', ')}.`)
  }

  return result.join('\n')
}

const findTopic = (rawQuestion: string) => {
  const value = normalize(rawQuestion)

  const stopWords = new Set([
    'khoa', 'hoc', 'nay', 'co', 'khong', 've', 'noi', 'dung', 'bai', 'chuong',
    'nao', 'gi', 'la', 'duoc', 'trong', 'chuong', 'trinh', 'lien', 'quan', 'den',
  ])

  const keywords = value
    .replace(/[?.,!:/()[\]]/g, ' ')
    .split(/\s+/)
    .filter((word) => word.length > 2 && !stopWords.has(word))

  if (!keywords.length) return null

  const matches: string[] = []

  props.course.chapters.forEach((chapter, chapterIndex) => {
    const chapterContent = normalize(
      `${chapter.title} ${chapter.description ?? ''} ${chapter.learningObjectives ?? ''}`,
    )

    if (keywords.some((keyword) => chapterContent.includes(keyword))) {
      matches.push(`Chương ${chapterIndex + 1}: ${chapter.title}`)
    }

    chapter.topics.forEach((topic) => {
      const topicContent = normalize(`${topic.title} ${topic.description ?? ''}`)

      if (keywords.some((keyword) => topicContent.includes(keyword))) {
        matches.push(`Chương ${chapterIndex + 1} · ${topic.title}`)
      }
    })
  })

  if (!matches.length) return null

  return `Mình tìm thấy một số nội dung liên quan trong chương trình:\n${[...new Set(matches)]
    .slice(0, 6)
    .map((item) => `• ${item}`)
    .join('\n')}`
}

const answerQuestion = (rawQuestion: string) => {
  const value = normalize(rawQuestion)
  const description = storedRichTextToPlainText(props.course.description)

  const chapterAnswer = findChapter(value)
  if (chapterAnswer) return chapterAnswer

  if (/(danh cho ai|phu hop|doi tuong|nen hoc)/.test(value)) {
    const level = props.course.level ? ` Khóa học hiện được xếp ở cấp độ ${props.course.level}.` : ''

    return description
      ? `Dựa trên thông tin công khai, khóa học tập trung vào: ${description}.${level}`
      : `Giảng viên chưa mô tả rõ đối tượng học viên phù hợp.${level}`
  }

  if (/(hoc gi|noi dung|gioi thieu|khoa hoc nay la gi)/.test(value)) {
    return description
      ? `Khóa học "${props.course.title}" tập trung vào: ${description}`
      : 'Giảng viên chưa cung cấp mô tả chi tiết cho khóa học này.'
  }

  if (/(cap do|level|trinh do)/.test(value)) {
    return props.course.level
      ? `Khóa học hiện có cấp độ ${props.course.level}.`
      : 'Giảng viên chưa xác định cấp độ của khóa học.'
  }

  if (/(bao nhieu chuong|so chuong|bao nhieu bai|so bai)/.test(value)) {
    return `Chương trình hiện có ${props.course.chapters.length} chương và ${totalTopics()} bài học.`
  }

  if (/(chuong trinh|cac chuong|gom nhung gi|lo trinh)/.test(value)) {
    return `Chương trình học đang được công bố gồm:\n${curriculum()}`
  }

  if (/(bao lau|thoi luong|thoi gian|mat bao nhieu)/.test(value)) {
    const minutes = totalMinutes()

    return minutes
      ? `Tổng thời lượng dự kiến của các bài học hiện được công bố là khoảng ${formatDuration(minutes)}. Thời gian học thực tế có thể khác tùy tốc độ học của bạn.`
      : 'Giảng viên chưa cung cấp đủ thông tin để tính thời lượng dự kiến.'
  }

  const topicAnswer = findTopic(rawQuestion)
  if (topicAnswer) return topicAnswer

  return 'Mình chưa tìm thấy thông tin phù hợp trong dữ liệu công khai của khóa học. Bạn có thể hỏi về nội dung, cấp độ, chương trình, số bài học hoặc thời lượng.'
}

const scrollBottom = async () => {
  await nextTick()
  if (messageList.value) messageList.value.scrollTop = messageList.value.scrollHeight
}

const openChat = async () => {
  opened.value = true
  await scrollBottom()
}

const sendQuestion = async (suggestion?: string) => {
  const content = (suggestion ?? question.value).trim()
  if (!content) return

  messages.value.push({ id: nextId++, role: 'user', content })
  question.value = ''
  await scrollBottom()

  messages.value.push({ id: nextId++, role: 'assistant', content: answerQuestion(content) })
  await scrollBottom()
}
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="translate-y-4 opacity-0 scale-95"
      enter-to-class="translate-y-0 opacity-100 scale-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="translate-y-0 opacity-100 scale-100"
      leave-to-class="translate-y-4 opacity-0 scale-95"
    >
      <section
        v-if="opened"
        class="fixed bottom-24 right-5 z-[70] flex h-[min(620px,calc(100vh-8rem))] w-[calc(100vw-2rem)] max-w-[400px] origin-bottom-right flex-col overflow-hidden rounded-[1.25rem] border border-app-border bg-app-surface shadow-2xl sm:right-7"
      >
        <header class="flex items-center gap-3 bg-primary px-4 py-4 text-white">
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white/15">
            <Bot :size="20" />
          </div>

          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-1.5">
              <h2 class="truncate font-heading text-sm font-bold">Trợ lý khóa học</h2>
              <Sparkles :size="14" class="text-white/80" />
            </div>

            <div class="mt-0.5 flex items-center gap-1.5 text-xs text-white/70">
              <span class="h-1.5 w-1.5 rounded-full bg-emerald-400" />
              Sẵn sàng tư vấn
            </div>
          </div>

          <button
            type="button"
            class="flex h-9 w-9 items-center justify-center rounded-lg text-white/80 transition hover:bg-white/10 hover:text-white"
            aria-label="Đóng trợ lý"
            @click="opened = false"
          >
            <X :size="19" />
          </button>
        </header>

        <div ref="messageList" class="min-h-0 flex-1 space-y-4 overflow-y-auto bg-app-bg px-4 py-5">
          <div
            v-for="message in messages"
            :key="message.id"
            class="flex gap-2.5"
            :class="message.role === 'user' ? 'justify-end' : 'justify-start'"
          >
            <div
              v-if="message.role === 'assistant'"
              class="mt-1 flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-emerald-50 text-ai"
            >
              <Bot :size="14" />
            </div>

            <div
              class="max-w-[82%] whitespace-pre-line rounded-[1rem] px-3.5 py-2.5 text-sm leading-6"
              :class="
                message.role === 'user'
                  ? 'rounded-br-sm bg-primary text-white'
                  : 'rounded-bl-sm border border-app-border bg-app-surface text-app-text'
              "
            >
              {{ message.content }}
            </div>
          </div>
        </div>

        <div class="border-t border-app-border bg-app-surface px-4 py-3">
          <div class="mb-3 flex gap-2 overflow-x-auto pb-1">
            <button
              v-for="suggestion in suggestions"
              :key="suggestion"
              type="button"
              class="shrink-0 rounded-pill border border-app-border bg-app-surface-muted px-3 py-1.5 text-xs font-medium text-app-text-muted transition hover:border-secondary/30 hover:text-secondary"
              @click="sendQuestion(suggestion)"
            >
              {{ suggestion }}
            </button>
          </div>

          <form class="flex items-end gap-2" @submit.prevent="sendQuestion()">
            <textarea
              v-model="question"
              rows="1"
              maxlength="500"
              placeholder="Hỏi về khóa học..."
              class="max-h-28 min-h-11 min-w-0 flex-1 resize-none rounded-control border border-app-border bg-app-surface px-3 py-2.5 text-sm leading-5 text-app-text outline-none transition placeholder:text-app-text-muted focus:border-secondary focus:ring-2 focus:ring-secondary/10"
              @keydown.enter.exact.prevent="sendQuestion()"
            />

            <button
              type="submit"
              :disabled="!question.trim()"
              class="flex h-11 w-11 shrink-0 items-center justify-center rounded-control bg-primary text-white transition hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-40"
              aria-label="Gửi câu hỏi"
            >
              <SendHorizontal :size="18" />
            </button>
          </form>
        </div>
      </section>
    </Transition>

    <div class="fixed bottom-5 right-5 z-[70] sm:right-7">
      <Transition
        mode="out-in"
        enter-active-class="transition duration-150"
        enter-from-class="scale-75 opacity-0"
        leave-active-class="transition duration-150"
        leave-to-class="scale-75 opacity-0"
      >
        <button
          v-if="!opened"
          key="open"
          type="button"
          class="group flex h-14 items-center gap-2.5 rounded-pill bg-primary px-4 text-white shadow-xl transition hover:-translate-y-0.5 hover:bg-primary-hover hover:shadow-2xl"
          @click="openChat"
        >
          <span class="relative flex h-8 w-8 items-center justify-center rounded-full bg-white/15">
            <MessageCircle :size="19" />
            <span class="absolute -right-0.5 -top-0.5 h-2.5 w-2.5 rounded-full border-2 border-primary bg-emerald-400" />
          </span>

          <span class="hidden pr-1 text-sm font-semibold sm:inline">Tư vấn khóa học</span>
        </button>

        <button
          v-else
          key="close"
          type="button"
          class="flex h-12 w-12 items-center justify-center rounded-full bg-primary text-white shadow-xl transition hover:bg-primary-hover"
          aria-label="Thu nhỏ trợ lý"
          @click="opened = false"
        >
          <ChevronDown :size="20" />
        </button>
      </Transition>
    </div>
  </Teleport>
</template>