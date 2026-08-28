<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  BookOpen,
  CalendarDays,
  CheckCircle2,
  Clock3,
  Send,
} from 'lucide-vue-next'

import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import BaseCard from '@/shared/components/BaseCard.vue'
import type { PageableData } from '@/shared/api'

import {
  getPublicCourses,
  requestToJoinCourse,
  type PublicCourse,
} from '../api/courseApi'
import { useStudentApiError } from '../composables/useStudentApiError'

const { handleApiError } = useStudentApiError()

const courses = ref<PublicCourse[]>([])

const pageable = ref<PageableData>({
  page: 1,
  size: 10,
  totalElements: 0,
  totalPages: 0,
})

const loading = ref(true)
const loadError = ref('')

const requestingCourseId = ref<string | null>(null)
const actionError = ref('')

const canGoPrevious = computed(
  () => pageable.value.page > 1,
)

const canGoNext = computed(
  () => pageable.value.page < pageable.value.totalPages,
)

const loadCourses = async (page = 1) => {
  loading.value = true
  loadError.value = ''

  try {
    const result = await getPublicCourses(page)

    courses.value = result.content
    pageable.value = result.pageable
  } catch (error) {
    loadError.value = handleApiError(
      error,
      'Không thể tải danh sách khóa học công khai.',
    ).message
  } finally {
    loading.value = false
  }
}

const requestJoin = async (course: PublicCourse) => {
  requestingCourseId.value = course.id
  actionError.value = ''

  try {
    const membership = await requestToJoinCourse(course.id)

    course.currentUserMembershipStatus = membership.status
  } catch (error) {
    actionError.value = handleApiError(
      error,
      'Không thể gửi yêu cầu tham gia khóa học.',
    ).message
  } finally {
    requestingCourseId.value = null
  }
}

const goToPage = async (page: number) => {
  if (
    page < 1 ||
    page > pageable.value.totalPages ||
    page === pageable.value.page
  ) {
    return
  }

  await loadCourses(page)
}

const formatDate = (value: string | null) => {
  if (!value) return 'Chưa cập nhật'

  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'medium',
  }).format(new Date(value))
}

onMounted(() => void loadCourses())
</script>

<template>
  <section class="mx-auto max-w-app space-y-6">
    <header>
      <p class="text-sm font-semibold text-secondary">
        Khám phá
      </p>

      <h1
        class="mt-1 font-heading text-3xl font-bold tracking-tight text-app-text"
      >
        Khóa học công khai
      </h1>

      <p class="mt-2 max-w-2xl text-sm text-app-text-muted">
        Khám phá các khóa học công khai và gửi yêu cầu tham gia.
        Bạn có thể truy cập khóa học sau khi được giảng viên chấp nhận.
      </p>
    </header>

    <BaseAlert
      v-if="actionError"
      class="mb-4"
    >
      {{ actionError }}
    </BaseAlert>

    <BaseAlert v-if="loadError">
      <div
        class="flex flex-wrap items-center justify-between gap-3"
      >
        <span>{{ loadError }}</span>

        <BaseButton
          variant="secondary"
          @click="loadCourses(pageable.page)"
        >
          Thử lại
        </BaseButton>
      </div>
    </BaseAlert>

    <div
      v-if="loading"
      class="grid gap-5 md:grid-cols-2 xl:grid-cols-3"
    >
      <div
        v-for="index in 6"
        :key="index"
        class="h-72 animate-pulse rounded-card bg-app-surface-muted"
      />
    </div>

    <BaseCard v-else-if="courses.length === 0">
      <div class="py-12 text-center">
        <BookOpen
          :size="44"
          class="mx-auto text-app-text-muted/40"
        />

        <h2
          class="mt-4 font-heading text-xl font-bold text-app-text"
        >
          Chưa có khóa học công khai
        </h2>

        <p class="mt-2 text-sm text-app-text-muted">
          Hiện chưa có khóa học nào được công khai.
        </p>
      </div>
    </BaseCard>

    <div
      v-else
      class="grid gap-5 md:grid-cols-2 xl:grid-cols-3"
    >
      <BaseCard
        v-for="course in courses"
        :key="course.id"
      >
        <div class="flex h-full flex-col">
          <div>
            <span
              class="inline-flex rounded-pill bg-secondary-soft px-2.5 py-1 text-xs font-semibold text-secondary"
            >
              Công khai
            </span>

            <h2
              class="mt-4 line-clamp-2 font-heading text-xl font-bold text-app-text"
            >
              {{ course.title }}
            </h2>

            <p
              class="mt-2 line-clamp-3 text-sm leading-6 text-app-text-muted"
            >
              {{
                course.description ||
                'Khóa học chưa có mô tả.'
              }}
            </p>
          </div>

          <div
            class="mt-5 space-y-2 text-sm text-app-text-muted"
          >
            <p
              v-if="course.level"
              class="font-medium text-app-text"
            >
              {{ course.level }}
            </p>

            <p class="flex items-center gap-2">
              <CalendarDays :size="16" />
              Công khai {{ formatDate(course.publishedAt) }}
            </p>
          </div>

          <div class="mt-auto pt-6">
            <BaseButton
              v-if="
                course.currentUserMembershipStatus === null
              "
              block
              :loading="requestingCourseId === course.id"
              @click="requestJoin(course)"
            >
              <template #leading>
                <Send :size="17" />
              </template>

              Yêu cầu tham gia
            </BaseButton>

            <BaseButton
              v-else-if="
                course.currentUserMembershipStatus ===
                'PENDING'
              "
              block
              disabled
              variant="secondary"
            >
              <template #leading>
                <Clock3 :size="17" />
              </template>

              Đang chờ duyệt
            </BaseButton>

            <RouterLink
              v-else-if="
                course.currentUserMembershipStatus ===
                'ACTIVE'
              "
              :to="{
                name: 'student-course-detail',
                params: { courseId: course.id },
              }"
              class="block"
            >
              <BaseButton block>
                <template #leading>
                  <CheckCircle2 :size="17" />
                </template>

                Vào khóa học
              </BaseButton>
            </RouterLink>

            <BaseButton
              v-else
              block
              :loading="requestingCourseId === course.id"
              @click="requestJoin(course)"
            >
              <template #leading>
                <Send :size="17" />
              </template>

              Gửi lại yêu cầu
            </BaseButton>
          </div>
        </div>
      </BaseCard>
    </div>

    <div
      v-if="
        !loading &&
        courses.length > 0 &&
        pageable.totalPages > 1
      "
      class="flex items-center justify-between gap-4"
    >
      <p class="text-sm text-app-text-muted">
        Trang
        <span class="font-semibold text-app-text">
          {{ pageable.page }}
        </span>
        /
        {{ pageable.totalPages }}
        ·
        {{ pageable.totalElements }} khóa học
      </p>

      <div class="flex gap-2">
        <BaseButton
          variant="secondary"
          :disabled="!canGoPrevious"
          @click="goToPage(pageable.page - 1)"
        >
          Trước
        </BaseButton>

        <BaseButton
          variant="secondary"
          :disabled="!canGoNext"
          @click="goToPage(pageable.page + 1)"
        >
          Sau
        </BaseButton>
      </div>
    </div>
  </section>
</template>