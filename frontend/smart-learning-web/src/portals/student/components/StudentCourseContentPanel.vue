<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  BookOpen,
  ChevronDown,
  ChevronRight,
  Clock3,
  GraduationCap,
} from 'lucide-vue-next'

import BaseAlert from '@/shared/components/BaseAlert.vue'

import {
  getChapters,
  getTopics,
  type CourseChapter,
  type CourseTopic,
} from '../api/contentApi'

import { useStudentApiError } from '../composables/useStudentApiError'
import TopicContentViewer from './StudentTopicContentViewer.vue'

const props = defineProps<{
  courseId: string
}>()

const { handleApiError } = useStudentApiError()

interface ChapterWithTopics extends CourseChapter {
  topics: CourseTopic[]
}

const chapters = ref<ChapterWithTopics[]>([])
const loading = ref(true)
const message = ref('')

const selectedTopic = ref<CourseTopic | null>(null)
const expandedChapters = ref<Set<string>>(new Set())

const selectedChapter = computed(() => {
  if (!selectedTopic.value) return null

  return (
    chapters.value.find(
      (chapter) =>
        chapter.id === selectedTopic.value?.chapterId,
    ) ?? null
  )
})

const hasContent = computed(() => {
  const nodes = selectedTopic.value?.content?.content

  return Array.isArray(nodes) && nodes.length > 0
})

const toggleChapter = (chapterId: string) => {
  const next = new Set(expandedChapters.value)

  if (next.has(chapterId)) {
    next.delete(chapterId)
  } else {
    next.add(chapterId)
  }

  expandedChapters.value = next
}

const selectTopic = (
  chapter: ChapterWithTopics,
  topic: CourseTopic,
) => {
  expandedChapters.value = new Set([
    ...expandedChapters.value,
    chapter.id,
  ])

  selectedTopic.value = topic
}

const loadContent = async () => {
  loading.value = true
  message.value = ''

  try {
    const chapterData =
      await getChapters(props.courseId)

    const chaptersWithTopics =
      await Promise.all(
        chapterData.map(async (chapter) => ({
          ...chapter,
          topics: await getTopics(
            props.courseId,
            chapter.id,
          ),
        })),
      )

    chapters.value = chaptersWithTopics
    console.log(
        'TOPIC CONTENT:',
        chaptersWithTopics[0]?.topics[0]?.content,
        )

    const firstChapter =
      chaptersWithTopics.find(
        (chapter) => chapter.topics.length > 0,
      )

    const firstTopic = firstChapter?.topics[0]

    if (firstChapter && firstTopic) {
      expandedChapters.value =
        new Set([firstChapter.id])

      selectedTopic.value = firstTopic
    }
  } catch (error) {
    message.value = handleApiError(
      error,
      'Không thể tải nội dung khóa học.',
    ).message
  } finally {
    loading.value = false
  }
}

onMounted(() => void loadContent())
</script>

<template>
  <section>
    <BaseAlert v-if="message" class="mb-5">
      {{ message }}
    </BaseAlert>

    <div
      v-if="loading"
      class="grid gap-5 lg:grid-cols-[19rem_minmax(0,1fr)]"
    >
      <div
        class="h-[36rem] animate-pulse rounded-panel bg-app-surface-muted"
      />

      <div
        class="h-[36rem] animate-pulse rounded-panel bg-app-surface-muted"
      />
    </div>

    <div
      v-else-if="chapters.length === 0"
      class="rounded-panel border border-dashed border-app-border bg-app-surface px-6 py-16 text-center"
    >
      <BookOpen
        :size="42"
        class="mx-auto text-app-text-muted/40"
      />

      <h2
        class="mt-4 font-heading text-xl font-bold text-app-text"
      >
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
      <!-- Curriculum -->
      <aside
        class="border-b border-app-border bg-app-surface-muted/35 lg:border-r lg:border-b-0"
      >
        <header class="border-b border-app-border px-5 py-5">
          <div class="flex items-center gap-2">
            <GraduationCap
              :size="19"
              class="text-secondary"
            />

            <h2
              class="font-heading font-bold text-app-text"
            >
              Nội dung khóa học
            </h2>
          </div>

          <p
            class="mt-1.5 text-xs leading-5 text-app-text-muted"
          >
            Chọn một chủ đề để bắt đầu học.
          </p>
        </header>

        <nav
          class="max-h-[42rem] overflow-y-auto py-2"
        >
          <section
            v-for="(chapter, chapterIndex) in chapters"
            :key="chapter.id"
          >
            <button
              type="button"
              class="flex w-full items-center gap-3 px-4 py-3 text-left transition hover:bg-app-surface"
              @click="toggleChapter(chapter.id)"
            >
              <component
                :is="
                  expandedChapters.has(chapter.id)
                    ? ChevronDown
                    : ChevronRight
                "
                :size="17"
                class="shrink-0 text-app-text-muted"
              />

              <div class="min-w-0">
                <p
                  class="text-[11px] font-semibold uppercase tracking-wider text-secondary"
                >
                  Chương {{ chapterIndex + 1 }}
                </p>

                <p
                  class="mt-0.5 truncate text-sm font-semibold text-app-text"
                >
                  {{ chapter.title }}
                </p>
              </div>
            </button>

            <div
              v-if="expandedChapters.has(chapter.id)"
              class="pb-2"
            >
              <button
                v-for="(topic, topicIndex) in chapter.topics"
                :key="topic.id"
                type="button"
                class="relative flex w-full gap-3 py-3 pr-4 pl-11 text-left transition"
                :class="
                  selectedTopic?.id === topic.id
                    ? 'bg-secondary-soft text-secondary'
                    : 'text-app-text-muted hover:bg-app-surface hover:text-app-text'
                "
                @click="selectTopic(chapter, topic)"
              >
                <span
                  class="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full border text-[11px] font-bold"
                  :class="
                    selectedTopic?.id === topic.id
                      ? 'border-secondary bg-secondary text-white'
                      : 'border-app-border bg-app-surface'
                  "
                >
                  {{ topicIndex + 1 }}
                </span>

                <div class="min-w-0 flex-1">
                  <p class="text-sm font-medium leading-5">
                    {{ topic.title }}
                  </p>

                  <p
                    v-if="topic.estimatedMinutes"
                    class="mt-1 flex items-center gap-1 text-[11px] opacity-75"
                  >
                    <Clock3 :size="12" />
                    {{ topic.estimatedMinutes }} phút
                  </p>
                </div>
              </button>

              <p
                v-if="chapter.topics.length === 0"
                class="px-11 py-3 text-xs text-app-text-muted"
              >
                Chưa có chủ đề.
              </p>
            </div>
          </section>
        </nav>
      </aside>

      <!-- Lesson -->
      <main
        v-if="selectedTopic"
        class="min-w-0 bg-app-surface"
      >
        <article
          class="mx-auto max-w-4xl px-6 py-8 sm:px-10 lg:px-12 lg:py-10"
        >
          <header
            class="border-b border-app-border pb-7"
          >
            <div
              class="flex flex-wrap items-center gap-2 text-xs font-medium text-app-text-muted"
            >
              <span v-if="selectedChapter">
                {{ selectedChapter.title }}
              </span>

              <span>•</span>

              <span
                v-if="selectedTopic.estimatedMinutes"
                class="inline-flex items-center gap-1"
              >
                <Clock3 :size="13" />
                {{ selectedTopic.estimatedMinutes }} phút
              </span>
            </div>

            <h1
              class="mt-3 font-heading text-3xl font-bold tracking-tight text-app-text"
            >
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
            <TopicContentViewer
              v-if="hasContent"
              :content="selectedTopic.content"
            />

            <div
              v-else
              class="rounded-card border border-dashed border-app-border bg-app-surface-muted/40 px-6 py-12 text-center"
            >
              <BookOpen
                :size="32"
                class="mx-auto text-app-text-muted/40"
              />

              <p
                class="mt-3 font-semibold text-app-text"
              >
                Nội dung đang được cập nhật
              </p>

              <p
                class="mt-1 text-sm text-app-text-muted"
              >
                Giảng viên chưa soạn nội dung cho chủ đề này.
              </p>
            </div>
          </div>
        </article>
      </main>

      <div
        v-else
        class="flex min-h-[32rem] items-center justify-center p-8 text-center"
      >
        <div>
          <BookOpen
            :size="42"
            class="mx-auto text-app-text-muted/30"
          />

          <p
            class="mt-3 font-semibold text-app-text"
          >
            Chưa có chủ đề để hiển thị
          </p>
        </div>
      </div>
    </div>
  </section>
</template>