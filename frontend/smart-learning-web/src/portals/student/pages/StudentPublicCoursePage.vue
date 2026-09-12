<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { BookOpen, CalendarDays, CheckCircle2, Clock3, Search, Send } from 'lucide-vue-next'

import {
  BaseAlert,
  BaseBadge,
  BaseButton,
  BaseEmptyState,
  BaseInput,
  BasePageHeader,
  BasePagination,
} from '@/shared/components'
import CourseCover from '@/shared/course/CourseCover.vue'
import type { PaginatedData } from '@/shared/api'
import { storedRichTextToPlainText } from '@/shared/rich-text'
import { formatDate } from '@/shared/utils'

import {
  getPublicCourses,
  requestToJoinCourse,
  type PublicCourse,
} from '../api/courseApi'
import { useStudentApiError } from '../composables/useStudentApiError'

const { handleApiError } = useStudentApiError()

const coursesPage = ref<PaginatedData<PublicCourse>>({
  content: [],
  pageable: { page: 1, size: 6, totalElements: 0, totalPages: 0 },
})

const loading = ref(true)
const loadError = ref('')
const keyword = ref('')
const appliedKeyword = ref('')
const requestingCourseId = ref<string | null>(null)
const actionError = ref('')

const descriptionPreview = (description: string | null) =>
  storedRichTextToPlainText(description) || 'Khóa học chưa có mô tả.'

const loadCourses = async (page = 1) => {
  loading.value = true
  loadError.value = ''

  try {
    coursesPage.value = await getPublicCourses({
      page,
      keyword: appliedKeyword.value || undefined,
    })
  } catch (error) {
    loadError.value = handleApiError(error, 'Không thể tải danh sách khóa học công khai.').message
  } finally {
    loading.value = false
  }
}

const applySearch = () => {
  appliedKeyword.value = keyword.value.trim()
  void loadCourses(1)
}

const resetSearch = () => {
  keyword.value = ''
  appliedKeyword.value = ''
  void loadCourses(1)
}

const requestJoin = async (course: PublicCourse) => {
  requestingCourseId.value = course.id
  actionError.value = ''

  try {
    const membership = await requestToJoinCourse(course.id)
    course.currentUserMembershipStatus = membership.status
  } catch (error) {
    actionError.value = handleApiError(error, 'Không thể gửi yêu cầu tham gia khóa học.').message
  } finally {
    requestingCourseId.value = null
  }
}

const goToPage = async (page: number) => {
  if (
    page < 1 ||
    page > coursesPage.value.pageable.totalPages ||
    page === coursesPage.value.pageable.page
  ) {
    return
  }

  await loadCourses(page)
}

onMounted(() => void loadCourses())
</script>

<template>
  <section class="mx-auto max-w-app space-y-6">
    <BasePageHeader title="Khóa học công khai" />

    <section class="rounded-card border border-app-border bg-app-surface p-4 shadow-card">
      <form class="flex gap-3" @submit.prevent="applySearch">
        <div class="min-w-0 flex-1">
          <BaseInput v-model="keyword" placeholder="Tìm theo tên, mô tả hoặc cấp độ...">
            <template #leading><Search :size="18" /></template>
          </BaseInput>
        </div>

        <BaseButton type="submit" variant="secondary">Tìm</BaseButton>
      </form>

      <button
        v-if="appliedKeyword"
        type="button"
        class="mt-3 text-sm font-semibold text-secondary hover:underline"
        @click="resetSearch"
      >
        Xóa tìm kiếm
      </button>
    </section>

    <BaseAlert v-if="actionError">{{ actionError }}</BaseAlert>

    <BaseAlert v-if="loadError">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <span>{{ loadError }}</span>

        <BaseButton variant="secondary" @click="loadCourses(coursesPage.pageable.page)">
          Thử lại
        </BaseButton>
      </div>
    </BaseAlert>

    <div v-if="loading" class="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
      <div v-for="index in 6" :key="index" class="h-96 animate-pulse rounded-card bg-app-surface-muted" />
    </div>

    <BaseEmptyState
      v-else-if="coursesPage.content.length === 0"
      :title="appliedKeyword ? 'Không tìm thấy khóa học phù hợp' : 'Chưa có khóa học công khai'"
      :description="
        appliedKeyword
          ? 'Thử tìm kiếm bằng từ khóa khác.'
          : 'Hiện chưa có khóa học nào được công khai.'
      "
    >
      <template #icon><BookOpen :size="24" /></template>
    </BaseEmptyState>

    <div v-else class="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
      <article
        v-for="course in coursesPage.content"
        :key="course.id"
        class="flex overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card"
      >
        <div class="flex min-h-0 w-full flex-col">
          <CourseCover :image-url="course.imageUrl" :title="course.title" />

          <div class="flex flex-1 flex-col p-5">
            <div class="flex items-center justify-between gap-3">
              <BaseBadge tone="secondary">Công khai</BaseBadge>
              <span v-if="course.level" class="text-xs font-semibold text-app-text-muted">
                {{ course.level }}
              </span>
            </div>

            <h2 class="mt-4 line-clamp-2 font-heading text-xl font-bold text-app-text">
              {{ course.title }}
            </h2>

            <p class="mt-2 line-clamp-3 text-sm leading-6 text-app-text-muted">
              {{ descriptionPreview(course.description) }}
            </p>

            <p class="mt-4 flex items-center gap-2 text-xs text-app-text-muted">
              <CalendarDays :size="15" />
              Công khai {{ formatDate(course.publishedAt, 'Chưa cập nhật') }}
            </p>

            <div class="mt-auto pt-6">
              <BaseButton
                v-if="course.currentUserMembershipStatus === null"
                block
                :loading="requestingCourseId === course.id"
                @click="requestJoin(course)"
              >
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
                Đang chờ duyệt
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

              <BaseButton
                v-else
                block
                :loading="requestingCourseId === course.id"
                @click="requestJoin(course)"
              >
                <template #leading><Send :size="17" /></template>
                Gửi lại yêu cầu
              </BaseButton>
            </div>
          </div>
        </div>
      </article>
    </div>

    <BasePagination
      v-if="!loading && coursesPage.pageable.totalElements > 0"
      :page="coursesPage.pageable.page"
      :total-pages="coursesPage.pageable.totalPages"
      :total-elements="coursesPage.pageable.totalElements"
      @previous="goToPage(coursesPage.pageable.page - 1)"
      @next="goToPage(coursesPage.pageable.page + 1)"
    />
  </section>
</template>