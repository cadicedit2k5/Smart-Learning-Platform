<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, BookOpen, CalendarDays, CheckCircle2, ChevronDown, Clock3, GraduationCap, Layers3, LockKeyhole, Send } from 'lucide-vue-next'

import { BaseAlert, BaseButton, BaseEmptyState } from '@/shared/components'
import CourseCover from '@/shared/course/CourseCover.vue'
import { hasRichTextContent, parseStoredRichText, RichTextViewer, storedRichTextToPlainText } from '@/shared/rich-text'
import { formatDate } from '@/shared/utils'

import { getPublicCourseDetail, requestToJoinCourse, type PublicCourseChapterOutline, type PublicCourseDetail } from '../api/courseApi'
import { useStudentApiError } from '../composables/useStudentApiError'
import PublicCourseAdvisor from '../components/PublicCourseAdvisor.vue'

const route = useRoute()
const { handleApiError } = useStudentApiError()

const courseId = computed(() => String(route.params.courseId))
const course = ref<PublicCourseDetail | null>(null)
const loading = ref(true)
const requesting = ref(false)
const loadError = ref('')
const actionError = ref('')
const successMessage = ref('')

const descriptionContent = computed(() => parseStoredRichText(course.value?.description))
const hasDescription = computed(() => hasRichTextContent(descriptionContent.value))

const descriptionPreview = computed(() => {
  const description = storedRichTextToPlainText(course.value?.description ?? null)
  return description || 'Khám phá nội dung và chương trình học của khóa học này.'
})

const chapterCount = computed(() => course.value?.chapters.length ?? 0)
const topicCount = computed(() => course.value?.chapters.reduce((total, chapter) => total + chapter.topics.length, 0) ?? 0)

const totalEstimatedMinutes = computed(() =>
  course.value?.chapters.reduce(
    (courseTotal, chapter) =>
      courseTotal + chapter.topics.reduce((chapterTotal, topic) => chapterTotal + (topic.estimatedMinutes ?? 0), 0),
    0,
  ) ?? 0,
)

const formatDuration = (minutes: number) => {
  if (!minutes) return 'Chưa xác định'
  if (minutes < 60) return `${minutes} phút`

  const hours = Math.floor(minutes / 60)
  const remainingMinutes = minutes % 60
  return remainingMinutes ? `${hours}h ${remainingMinutes}p` : `${hours} giờ`
}

const estimatedDuration = computed(() => formatDuration(totalEstimatedMinutes.value))

const chapterDuration = (chapter: PublicCourseChapterOutline) => {
  const minutes = chapter.topics.reduce((total, topic) => total + (topic.estimatedMinutes ?? 0), 0)
  return formatDuration(minutes)
}

const loadCourse = async () => {
  loading.value = true
  loadError.value = ''

  try {
    course.value = await getPublicCourseDetail(courseId.value)
  } catch (error) {
    loadError.value = handleApiError(error, 'Không thể tải thông tin khóa học.').message
  } finally {
    loading.value = false
  }
}

const requestJoin = async () => {
  if (!course.value) return

  requesting.value = true
  actionError.value = ''
  successMessage.value = ''

  try {
    const membership = await requestToJoinCourse(course.value.id)
    course.value.currentUserMembershipStatus = membership.status
    successMessage.value = 'Yêu cầu tham gia đã được gửi. Vui lòng chờ giảng viên phê duyệt.'
  } catch (error) {
    actionError.value = handleApiError(error, 'Không thể gửi yêu cầu tham gia khóa học.').message
  } finally {
    requesting.value = false
  }
}

watch(courseId, () => void loadCourse())
onMounted(() => void loadCourse())
</script>

<template>
  <section class="mx-auto max-w-[76rem] space-y-7 pb-10">
    <RouterLink :to="{ name: 'student-public-courses' }" class="inline-flex items-center gap-2 text-sm font-semibold text-app-text-muted transition hover:text-secondary">
      <ArrowLeft :size="17" />
      Khám phá khóa học
    </RouterLink>

    <template v-if="loading">
      <div class="h-[28rem] animate-pulse rounded-[1.75rem] bg-app-surface-muted" />

      <div class="grid gap-6 lg:grid-cols-[minmax(0,1fr)_20rem]">
        <div class="space-y-5">
          <div class="h-52 animate-pulse rounded-panel bg-app-surface-muted" />
          <div class="h-80 animate-pulse rounded-panel bg-app-surface-muted" />
        </div>

        <div class="h-80 animate-pulse rounded-panel bg-app-surface-muted" />
      </div>
    </template>

    <BaseAlert v-else-if="loadError">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <span>{{ loadError }}</span>
        <BaseButton variant="secondary" @click="loadCourse">Thử lại</BaseButton>
      </div>
    </BaseAlert>

    <template v-else-if="course">
      <header class="relative overflow-hidden rounded-[1.75rem] border border-app-border bg-gradient-to-br from-white via-slate-50 to-secondary-soft/60 shadow-card">
        <div class="pointer-events-none absolute -right-24 -top-24 h-72 w-72 rounded-full bg-secondary/10 blur-3xl" />
        <div class="pointer-events-none absolute -bottom-28 left-1/4 h-64 w-64 rounded-full bg-primary/5 blur-3xl" />

        <div class="relative grid items-center gap-8 p-6 sm:p-8 lg:grid-cols-[minmax(0,1fr)_25rem] lg:p-10">
          <div class="max-w-3xl">
            <div class="flex flex-wrap items-center gap-2">
              <span class="rounded-pill border border-secondary/15 bg-secondary-soft px-3 py-1.5 text-xs font-semibold text-secondary">
                Khóa học công khai
              </span>

              <span v-if="course.level" class="rounded-pill border border-app-border bg-white/80 px-3 py-1.5 text-xs font-semibold text-app-text-muted">
                {{ course.level }}
              </span>
            </div>

            <h1 class="mt-6 max-w-3xl font-heading text-3xl font-bold leading-tight tracking-tight text-app-text sm:text-4xl lg:text-5xl">
              {{ course.title }}
            </h1>

            <div class="mt-8 flex flex-wrap gap-x-6 gap-y-4">
              <div class="flex items-center gap-2 text-sm text-app-text-muted">
                <Layers3 :size="18" class="text-secondary" />
                <span><strong class="font-semibold text-app-text">{{ chapterCount }}</strong> chương</span>
              </div>

              <div class="flex items-center gap-2 text-sm text-app-text-muted">
                <BookOpen :size="18" class="text-secondary" />
                <span><strong class="font-semibold text-app-text">{{ topicCount }}</strong> bài học</span>
              </div>

              <div class="flex items-center gap-2 text-sm text-app-text-muted">
                <Clock3 :size="18" class="text-secondary" />
                <span>{{ estimatedDuration }}</span>
              </div>

              <div class="flex items-center gap-2 text-sm text-app-text-muted">
                <CalendarDays :size="18" class="text-secondary" />
                <span>{{ formatDate(course.publishedAt, 'Chưa cập nhật') }}</span>
              </div>
            </div>
          </div>

          <div class="overflow-hidden rounded-[1.25rem] border border-app-border bg-white p-2 shadow-overlay">
            <CourseCover :image-url="course.imageUrl" :title="course.title" class="rounded-[1rem]" />
          </div>
        </div>
      </header>

      <BaseAlert v-if="actionError">{{ actionError }}</BaseAlert>

      <BaseAlert v-if="successMessage" variant="success" title="Đã gửi yêu cầu">
        {{ successMessage }}
      </BaseAlert>

      <div class="grid items-start gap-7 lg:grid-cols-[minmax(0,1fr)_20rem]">
        <main class="min-w-0 space-y-8">
          <section class="rounded-[1.25rem] border border-app-border bg-app-surface p-6 shadow-card sm:p-7">
            <div class="flex items-start gap-4">
              <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-secondary-soft text-secondary">
                <GraduationCap :size="22" />
              </div>

              <div>
                <h2 class="mt-1 font-heading text-2xl font-bold text-app-text">Giới thiệu chung</h2>
              </div>
            </div>

            <div class="mt-6">
              <RichTextViewer v-if="hasDescription" :content="descriptionContent" />
              <p v-else class="text-sm leading-7 text-app-text-muted">
                Khóa học chưa có thông tin giới thiệu chi tiết.
              </p>
            </div>
          </section>

          <PublicCourseAdvisor :course="course" />

          <section>
            <div class="mb-5 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <h2 class="mt-1 font-heading text-2xl font-bold text-app-text">Chương trình học</h2>
                <p class="mt-2 text-sm text-app-text-muted">Xem cấu trúc khóa học trước khi gửi yêu cầu tham gia.</p>
              </div>

              <p v-if="chapterCount" class="shrink-0 text-sm font-medium text-app-text-muted">
                {{ chapterCount }} chương · {{ topicCount }} bài học
              </p>
            </div>

            <BaseEmptyState
              v-if="course.chapters.length === 0"
              title="Khóa học chưa có nội dung"
              description="Giảng viên chưa công bố chương trình học cho khóa học này."
            >
              <template #icon><Layers3 :size="24" /></template>
            </BaseEmptyState>

            <div v-else class="space-y-3">
              <details
                v-for="(chapter, index) in course.chapters"
                :key="chapter.id"
                :open="index === 0"
                class="group overflow-hidden rounded-[1.1rem] border border-app-border bg-app-surface shadow-card transition hover:border-secondary/30"
              >
                <summary class="flex cursor-pointer list-none items-center gap-4 px-5 py-5 sm:px-6">
                  <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-app-surface-muted font-heading text-sm font-bold text-app-text">
                    {{ String(index + 1).padStart(2, '0') }}
                  </div>

                  <div class="min-w-0 flex-1">
                    <h3 class="font-heading text-base font-bold text-app-text sm:text-lg">{{ chapter.title }}</h3>

                    <div class="mt-1.5 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-app-text-muted">
                      <span>{{ chapter.topics.length }} bài học</span>

                      <span v-if="chapterDuration(chapter) !== 'Chưa xác định'" class="flex items-center gap-1">
                        <span class="text-app-border">•</span>
                        {{ chapterDuration(chapter) }}
                      </span>
                    </div>
                  </div>

                  <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-app-text-muted transition group-hover:bg-app-surface-muted">
                    <ChevronDown :size="19" class="transition-transform duration-200 group-open:rotate-180" />
                  </div>
                </summary>

                <div class="border-t border-app-border bg-slate-50/60 px-5 py-5 sm:px-6">
                  <div
                    v-if="chapter.description || chapter.learningObjectives"
                    class="mb-5 grid gap-4 rounded-xl border border-app-border bg-app-surface p-4 sm:grid-cols-2"
                  >
                    <div v-if="chapter.description">
                      <p class="text-xs font-bold uppercase tracking-wide text-app-text-muted">Giới thiệu chương</p>
                      <p class="mt-2 whitespace-pre-wrap text-sm leading-6 text-app-text">{{ chapter.description }}</p>
                    </div>

                    <div v-if="chapter.learningObjectives">
                      <p class="text-xs font-bold uppercase tracking-wide text-app-text-muted">Mục tiêu học tập</p>
                      <p class="mt-2 whitespace-pre-wrap text-sm leading-6 text-app-text">{{ chapter.learningObjectives }}</p>
                    </div>
                  </div>

                  <div v-if="chapter.topics.length" class="overflow-hidden rounded-xl border border-app-border bg-app-surface">
                    <div
                      v-for="(topic, topicIndex) in chapter.topics"
                      :key="topic.id"
                      class="flex items-start gap-4 border-b border-app-border px-4 py-4 last:border-b-0 sm:px-5"
                    >
                      <div class="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-app-surface-muted text-app-text-muted">
                        <LockKeyhole :size="15" />
                      </div>

                      <div class="min-w-0 flex-1">
                        <div class="flex flex-col gap-1 sm:flex-row sm:items-start sm:justify-between sm:gap-4">
                          <div class="min-w-0">
                            <p class="text-xs font-medium text-app-text-muted">Bài {{ topicIndex + 1 }}</p>
                            <h4 class="mt-0.5 font-semibold leading-6 text-app-text">{{ topic.title }}</h4>
                          </div>

                          <span v-if="topic.estimatedMinutes" class="flex shrink-0 items-center gap-1.5 text-xs text-app-text-muted">
                            <Clock3 :size="14" />
                            {{ topic.estimatedMinutes }} phút
                          </span>
                        </div>

                        <p v-if="topic.description" class="mt-1.5 line-clamp-2 text-sm leading-6 text-app-text-muted">
                          {{ topic.description }}
                        </p>
                      </div>
                    </div>
                  </div>

                  <p v-else class="rounded-xl border border-dashed border-app-border bg-app-surface px-4 py-5 text-center text-sm text-app-text-muted">
                    Chương này chưa có bài học.
                  </p>
                </div>
              </details>
            </div>
          </section>
        </main>

        <aside class="order-first lg:order-last lg:sticky lg:top-6">
          <section class="overflow-hidden rounded-[1.25rem] border border-app-border bg-app-surface shadow-overlay">
            <div class="p-5">
              <h2 class="mt-2 font-heading text-xl font-bold text-app-text">Bắt đầu học cùng lớp</h2>

              <p class="mt-2 text-sm leading-6 text-app-text-muted">
                Gửi yêu cầu tham gia để truy cập đầy đủ nội dung và các hoạt động của khóa học.
              </p>

              <div class="mt-5">
                <BaseButton v-if="course.currentUserMembershipStatus === null" block :loading="requesting" @click="requestJoin">
                  <template #leading><Send :size="17" /></template>
                  Yêu cầu tham gia
                </BaseButton>

                <BaseButton
                  v-else-if="course.currentUserMembershipStatus === 'PENDING'"
                  block
                  disabled
                  variant="secondary"
                >
                  <template #leading><Clock3 :size="17" /></template>
                  Đang chờ phê duyệt
                </BaseButton>

                <RouterLink
                  v-else-if="course.currentUserMembershipStatus === 'ACTIVE'"
                  :to="{ name: 'student-course-detail', params: { courseId: course.id } }"
                  class="block"
                >
                  <BaseButton block>
                    <template #leading><CheckCircle2 :size="17" /></template>
                    Vào khóa học
                  </BaseButton>
                </RouterLink>

                <BaseButton v-else block :loading="requesting" @click="requestJoin">
                  <template #leading><Send :size="17" /></template>
                  Gửi lại yêu cầu
                </BaseButton>
              </div>
            </div>

            <div class="border-t border-app-border px-5 py-5">
              <p class="text-sm font-semibold text-app-text">Thông tin khóa học</p>

              <div class="mt-4 space-y-4">
                <div class="flex items-center gap-3">
                  <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-app-surface-muted text-app-text-muted">
                    <GraduationCap :size="17" />
                  </div>

                  <div>
                    <p class="text-xs text-app-text-muted">Cấp độ</p>
                    <p class="text-sm font-semibold text-app-text">{{ course.level || 'Chưa xác định' }}</p>
                  </div>
                </div>

                <div class="flex items-center gap-3">
                  <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-app-surface-muted text-app-text-muted">
                    <Layers3 :size="17" />
                  </div>

                  <div>
                    <p class="text-xs text-app-text-muted">Chương trình</p>
                    <p class="text-sm font-semibold text-app-text">{{ chapterCount }} chương</p>
                  </div>
                </div>

                <div class="flex items-center gap-3">
                  <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-app-surface-muted text-app-text-muted">
                    <BookOpen :size="17" />
                  </div>

                  <div>
                    <p class="text-xs text-app-text-muted">Bài học</p>
                    <p class="text-sm font-semibold text-app-text">{{ topicCount }} bài</p>
                  </div>
                </div>

                <div class="flex items-center gap-3">
                  <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-app-surface-muted text-app-text-muted">
                    <Clock3 :size="17" />
                  </div>

                  <div>
                    <p class="text-xs text-app-text-muted">Thời lượng dự kiến</p>
                    <p class="text-sm font-semibold text-app-text">{{ estimatedDuration }}</p>
                  </div>
                </div>
              </div>
            </div>

            <div
              v-if="course.currentUserMembershipStatus !== 'ACTIVE'"
              class="border-t border-app-border bg-app-surface-muted px-5 py-4"
            >
              <div class="flex gap-3">
                <LockKeyhole :size="17" class="mt-0.5 shrink-0 text-app-text-muted" />

                <p class="text-xs leading-5 text-app-text-muted">
                  Nội dung chi tiết của bài học sẽ được mở sau khi bạn được chấp nhận vào khóa học.
                </p>
              </div>
            </div>
          </section>
        </aside>
      </div>
    </template>
  </section>
</template>