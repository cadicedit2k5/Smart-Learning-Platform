<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  BookOpen,
  Check,
  CheckCircle2,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Clock3,
  GraduationCap,
} from 'lucide-vue-next'

import BaseButton from '@/shared/components/BaseButton.vue'
import {
  getChapters,
  getTopics,
  type CourseChapter,
  type CourseTopic,
} from '@/shared/course-content'
import { useToastStore } from '@/shared/toast'

import {
  completeTopic,
  getCourseProgress,
  recordTopicActivity,
  startTopicActivity,
  type CourseLearningProgress,
  type TopicLearningProgress,
} from '../api/learningProgressApi'

import { useStudentApiError } from '../composables/useStudentApiError'
import TopicContentViewer from './StudentTopicContentViewer.vue'

const props = defineProps<{ courseId: string }>()

const ACTIVITY_INTERVAL_MS = 30_000
const IDLE_LIMIT_MS = 5 * 60_000

const toast = useToastStore()
const { handleApiError } = useStudentApiError()

interface ChapterWithTopics extends CourseChapter {
  topics: CourseTopic[]
}

const chapters = ref<ChapterWithTopics[]>([])
const loading = ref(true)
const progress = ref<CourseLearningProgress | null>(null)

const completingTopicId = ref('')
const selectedTopic = ref<CourseTopic | null>(null)
const expandedChapters = ref<Set<string>>(new Set())
const lastInteractionAt = ref(Date.now())

let activityTimer: ReturnType<typeof setInterval> | null = null
let activityRequest: Promise<void> | null = null

const selectedChapter = computed(() => {
  if (!selectedTopic.value) return null
  return chapters.value.find(chapter => chapter.id === selectedTopic.value?.chapterId) ?? null
})

const hasMeaningfulContent = (value: unknown): boolean => {
  if (Array.isArray(value)) return value.some(hasMeaningfulContent)

  if (value && typeof value === 'object') {
    const record = value as Record<string, unknown>

    if (typeof record.text === 'string' && record.text.trim()) return true
    if (record.content) return hasMeaningfulContent(record.content)
  }

  return false
}

const hasContent = computed(() => hasMeaningfulContent(selectedTopic.value?.content))

const progressByTopic = computed(() => {
  const map = new Map<string, TopicLearningProgress>()

  for (const item of progress.value?.topics ?? []) {
    map.set(item.topicId, item)
  }

  return map
})

const topicProgress = (topicId: string) => progressByTopic.value.get(topicId)
const isTopicCompleted = (topicId: string) => topicProgress(topicId)?.status === 'COMPLETED'

const selectedTopicProgress = computed(() => {
  if (!selectedTopic.value) return null
  return topicProgress(selectedTopic.value.id) ?? null
})

const allTopics = computed(() => chapters.value.flatMap(chapter => chapter.topics))

const selectedTopicIndex = computed(() => {
  if (!selectedTopic.value) return -1
  return allTopics.value.findIndex(topic => topic.id === selectedTopic.value?.id)
})

const previousTopic = computed(() => {
  const index = selectedTopicIndex.value
  return index > 0 ? allTopics.value[index - 1] : null
})

const nextTopic = computed(() => {
  const index = selectedTopicIndex.value
  if (index < 0 || index >= allTopics.value.length - 1) return null

  return allTopics.value[index + 1]
})

const formatStudyTime = (seconds: number) => {
  if (seconds < 60) return `${seconds} giây`

  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes} phút`

  const hours = Math.floor(minutes / 60)
  const remainingMinutes = minutes % 60

  return remainingMinutes ? `${hours} giờ ${remainingMinutes} phút` : `${hours} giờ`
}

const toggleChapter = (chapterId: string) => {
  const next = new Set(expandedChapters.value)

  if (next.has(chapterId)) next.delete(chapterId)
  else next.add(chapterId)

  expandedChapters.value = next
}

const updateLocalProgress = (updated: TopicLearningProgress) => {
  if (!progress.value) return

  const index = progress.value.topics.findIndex(item => item.topicId === updated.topicId)

  if (index >= 0) progress.value.topics[index] = updated
  else progress.value.topics.push(updated)

  const completed = progress.value.topics.filter(item => item.status === 'COMPLETED').length

  progress.value.completedTopics = completed
  progress.value.progressPercentage = progress.value.totalTopics === 0
    ? 0
    : Math.round(completed * 100 / progress.value.totalTopics)

  progress.value.lastTopicId = updated.topicId
}

const markInteraction = () => {
  lastInteractionAt.value = Date.now()
}

const canTrackActivity = () =>
  document.visibilityState === 'visible' &&
  document.hasFocus() &&
  Date.now() - lastInteractionAt.value <= IDLE_LIMIT_MS

const startActivity = async (topicId: string) => {
  try {
    updateLocalProgress(await startTopicActivity(props.courseId, topicId))
  } catch {
    // Tracking không được làm gián đoạn màn hình học.
  }
}

const recordActivity = (topicId: string) => {
  if (activityRequest) return activityRequest

  activityRequest = recordTopicActivity(props.courseId, topicId)
    .then(updateLocalProgress)
    .catch(() => {})
    .finally(() => {
      activityRequest = null
    })

  return activityRequest
}

const trackSelectedTopic = async () => {
  if (!selectedTopic.value || !canTrackActivity()) return
  await recordActivity(selectedTopic.value.id)
}

const selectTopic = async (chapter: ChapterWithTopics, topic: CourseTopic) => {
  if (selectedTopic.value?.id === topic.id) return

  const previousTopicId = selectedTopic.value?.id

  if (previousTopicId) {
    await recordActivity(previousTopicId)
  }

  expandedChapters.value = new Set([...expandedChapters.value, chapter.id])
  selectedTopic.value = topic
  markInteraction()

  await startActivity(topic.id)
}

const findChapterByTopic = (topicId: string) =>
  chapters.value.find(chapter => chapter.topics.some(topic => topic.id === topicId))

const goToTopic = async (topic: CourseTopic | null) => {
  if (!topic) return

  const chapter = findChapterByTopic(topic.id)

  if (chapter) {
    await selectTopic(chapter, topic)
  }
}

const handleCompleteTopic = async () => {
  if (!selectedTopic.value) return

  const topicId = selectedTopic.value.id
  completingTopicId.value = topicId

  try {
    await recordActivity(topicId)

    const updated = await completeTopic(props.courseId, topicId)

    updateLocalProgress(updated)
    toast.success('Đã hoàn thành bài học.')
  } catch (error) {
    toast.error(handleApiError(error, 'Chưa thể hoàn thành bài học.').message)
  } finally {
    completingTopicId.value = ''
  }
}

const handleVisibilityChange = () => {
  const topicId = selectedTopic.value?.id
  if (!topicId) return

  if (document.visibilityState === 'hidden') {
    void recordActivity(topicId)
    return
  }

  markInteraction()

  void (async () => {
    if (activityRequest) await activityRequest
    await startActivity(topicId)
  })()
}

const loadContent = async () => {
  loading.value = true

  try {
    const [chapterData, progressData] = await Promise.all([
      getChapters(props.courseId),
      getCourseProgress(props.courseId),
    ])

    const chaptersWithTopics = await Promise.all(
      chapterData.map(async chapter => ({
        ...chapter,
        topics: await getTopics(props.courseId, chapter.id),
      })),
    )

    chapters.value = chaptersWithTopics
    progress.value = progressData

    let topicToOpen: CourseTopic | undefined
    let chapterToOpen: ChapterWithTopics | undefined

    if (progressData.lastTopicId) {
      chapterToOpen = chaptersWithTopics.find(chapter =>
        chapter.topics.some(topic => topic.id === progressData.lastTopicId),
      )

      topicToOpen = chapterToOpen?.topics.find(topic => topic.id === progressData.lastTopicId)
    }

    if (!topicToOpen) {
      chapterToOpen = chaptersWithTopics.find(chapter => chapter.topics.length > 0)
      topicToOpen = chapterToOpen?.topics[0]
    }

    if (chapterToOpen && topicToOpen) {
      expandedChapters.value = new Set([chapterToOpen.id])
      selectedTopic.value = topicToOpen

      await startActivity(topicToOpen.id)
    }
  } catch (error) {
    toast.error(handleApiError(error, 'Không thể tải nội dung khóa học.').message)
  } finally {
    loading.value = false
  }
}

const interactionEvents = ['pointerdown', 'keydown', 'scroll', 'touchstart'] as const

onMounted(async () => {
  interactionEvents.forEach(event => window.addEventListener(event, markInteraction, { passive: true }))
  window.addEventListener('focus', markInteraction)
  document.addEventListener('visibilitychange', handleVisibilityChange)

  await loadContent()

  activityTimer = setInterval(() => void trackSelectedTopic(), ACTIVITY_INTERVAL_MS)
})

onBeforeUnmount(() => {
  if (activityTimer) clearInterval(activityTimer)

  const topicId = selectedTopic.value?.id
  if (topicId) void recordActivity(topicId)

  interactionEvents.forEach(event => window.removeEventListener(event, markInteraction))
  window.removeEventListener('focus', markInteraction)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<template>
  <section>
    <div v-if="loading" class="grid gap-5 lg:grid-cols-[19rem_minmax(0,1fr)]">
      <div class="h-[36rem] animate-pulse rounded-panel bg-app-surface-muted" />
      <div class="h-[36rem] animate-pulse rounded-panel bg-app-surface-muted" />
    </div>

    <div
      v-else-if="chapters.length === 0"
      class="rounded-panel border border-dashed border-app-border bg-app-surface px-6 py-16 text-center"
    >
      <BookOpen :size="42" class="mx-auto text-app-text-muted/40" />

      <h2 class="mt-4 font-heading text-xl font-bold text-app-text">
        Khóa học chưa có bài học
      </h2>

      <p class="mt-2 text-sm text-app-text-muted">
        Giảng viên chưa thêm nội dung cho khóa học này.
      </p>
    </div>

    <div
      v-else
      class="overflow-hidden rounded-panel border border-app-border bg-app-surface shadow-card lg:grid lg:min-h-[40rem] lg:grid-cols-[20rem_minmax(0,1fr)]"
    >
      <aside class="border-b border-app-border bg-app-surface-muted/35 lg:border-r lg:border-b-0">
        <header class="border-b border-app-border px-5 py-5">
          <div class="flex items-center gap-2">
            <GraduationCap :size="19" class="text-secondary" />
            <h2 class="font-heading font-bold text-app-text">Nội dung khóa học</h2>
          </div>

          <p class="mt-1.5 text-xs leading-5 text-app-text-muted">
            Chọn một chủ đề để bắt đầu học.
          </p>
        </header>

        <div v-if="progress" class="mx-5 mt-4">
          <div class="flex items-center justify-between gap-3 text-xs">
            <span class="text-app-text-muted">Tiến độ</span>
            <span class="font-semibold text-secondary">{{ progress.progressPercentage }}%</span>
          </div>

          <div class="mt-2 h-2 overflow-hidden rounded-pill bg-app-surface">
            <div
              class="h-full rounded-pill bg-secondary transition-all"
              :style="{ width: `${progress.progressPercentage}%` }"
            />
          </div>

          <p class="mt-2 text-xs text-app-text-muted">
            {{ progress.completedTopics }} / {{ progress.totalTopics }} bài hoàn thành
          </p>
        </div>

        <nav class="max-h-[42rem] overflow-y-auto py-2">
          <section v-for="(chapter, chapterIndex) in chapters" :key="chapter.id">
            <button
              type="button"
              class="flex w-full items-center gap-3 px-4 py-3 text-left transition hover:bg-app-surface"
              @click="toggleChapter(chapter.id)"
            >
              <component
                :is="expandedChapters.has(chapter.id) ? ChevronDown : ChevronRight"
                :size="17"
                class="shrink-0 text-app-text-muted"
              />

              <div class="min-w-0">
                <p class="text-[11px] font-semibold uppercase tracking-wider text-secondary">
                  Chương {{ chapterIndex + 1 }}
                </p>

                <p class="mt-0.5 truncate text-sm font-semibold text-app-text">
                  {{ chapter.title }}
                </p>
              </div>
            </button>

            <div v-if="expandedChapters.has(chapter.id)" class="pb-2">
              <button
                v-for="(topic, topicIndex) in chapter.topics"
                :key="topic.id"
                type="button"
                class="relative flex w-full gap-3 py-3 pr-4 pl-11 text-left transition"
                :class="selectedTopic?.id === topic.id
                  ? 'bg-secondary-soft text-secondary'
                  : 'text-app-text-muted hover:bg-app-surface hover:text-app-text'"
                @click="selectTopic(chapter, topic)"
              >
                <span
                  class="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full border text-[11px] font-bold"
                  :class="isTopicCompleted(topic.id)
                    ? 'border-ai bg-ai text-white'
                    : selectedTopic?.id === topic.id
                      ? 'border-secondary bg-secondary text-white'
                      : 'border-app-border bg-app-surface'"
                >
                  <Check v-if="isTopicCompleted(topic.id)" :size="13" />
                  <span v-else>{{ topicIndex + 1 }}</span>
                </span>

                <div class="min-w-0 flex-1">
                  <p class="text-sm font-medium leading-5">{{ topic.title }}</p>

                  <p v-if="topic.estimatedMinutes" class="mt-1 flex items-center gap-1 text-[11px] opacity-75">
                    <Clock3 :size="12" />
                    {{ topic.estimatedMinutes }} phút
                  </p>
                </div>

                <div class="mt-1 flex flex-wrap items-center gap-2 text-[11px]">
                  <span v-if="isTopicCompleted(topic.id)" class="font-medium text-ai">
                    Đã hoàn thành
                  </span>

                  <span v-else-if="topicProgress(topic.id)" class="font-medium text-secondary">
                    Đang học
                  </span>
                </div>
              </button>

              <p v-if="chapter.topics.length === 0" class="px-11 py-3 text-xs text-app-text-muted">
                Chưa có chủ đề.
              </p>
            </div>
          </section>
        </nav>
      </aside>

      <main v-if="selectedTopic" class="min-w-0 bg-app-surface">
        <article class="mx-auto max-w-4xl px-6 py-8 sm:px-10 lg:px-12 lg:py-10">
          <header class="border-b border-app-border pb-7">
            <div class="flex flex-wrap items-center gap-2 text-xs font-medium text-app-text-muted">
              <span v-if="selectedChapter">{{ selectedChapter.title }}</span>
              <span>•</span>

              <span v-if="selectedTopic.estimatedMinutes" class="inline-flex items-center gap-1">
                <Clock3 :size="13" />
                {{ selectedTopic.estimatedMinutes }} phút
              </span>
            </div>

            <h1 class="mt-3 font-heading text-3xl font-bold tracking-tight text-app-text">
              {{ selectedTopic.title }}
            </h1>

            <p
              v-if="selectedTopic.description"
              class="mt-3 max-w-3xl text-sm leading-7 text-app-text-muted"
            >
              {{ selectedTopic.description }}
            </p>
          </header>

          <div class="pt-8">
            <TopicContentViewer v-if="hasContent" :content="selectedTopic.content" />

            <div
              v-else
              class="rounded-card border border-dashed border-app-border bg-app-surface-muted/40 px-6 py-12 text-center"
            >
              <BookOpen :size="32" class="mx-auto text-app-text-muted/40" />
              <p class="mt-3 font-semibold text-app-text">Nội dung đang được cập nhật</p>
              <p class="mt-1 text-sm text-app-text-muted">Giảng viên chưa soạn nội dung cho chủ đề này.</p>
            </div>
          </div>

          <footer class="mt-10 flex flex-col gap-4 border-t border-app-border pt-6 sm:flex-row sm:items-end sm:justify-between">
            <BaseButton variant="secondary" :disabled="!previousTopic" @click="goToTopic(previousTopic)">
              <template #leading><ChevronLeft :size="17" /></template>
              Bài trước
            </BaseButton>

            <div class="flex flex-col items-end gap-2">
              <p class="text-xs text-app-text-muted">
                Thời gian đã học: {{ formatStudyTime(selectedTopicProgress?.activeSeconds ?? 0) }}
              </p>

              <div
                v-if="selectedTopicProgress?.status === 'COMPLETED'"
                class="inline-flex h-11 items-center gap-2 rounded-control bg-ai-soft px-4 text-sm font-semibold text-ai"
              >
                <CheckCircle2 :size="18" />
                Đã hoàn thành
              </div>

              <BaseButton
                v-else
                :loading="completingTopicId === selectedTopic.id"
                @click="handleCompleteTopic"
              >
                <template #leading><CheckCircle2 :size="17" /></template>
                Hoàn thành bài học
              </BaseButton>
            </div>

            <BaseButton variant="secondary" :disabled="!nextTopic" @click="goToTopic(nextTopic)">
              Bài tiếp theo
              <template #trailing><ChevronRight :size="17" /></template>
            </BaseButton>
          </footer>
        </article>
      </main>

      <div v-else class="flex min-h-[32rem] items-center justify-center p-8 text-center">
        <div>
          <BookOpen :size="42" class="mx-auto text-app-text-muted/30" />
          <p class="mt-3 font-semibold text-app-text">Chưa có chủ đề để hiển thị</p>
        </div>
      </div>
    </div>
  </section>
</template>