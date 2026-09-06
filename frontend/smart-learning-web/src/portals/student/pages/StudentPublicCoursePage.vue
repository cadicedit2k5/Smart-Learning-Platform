<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  BookOpen,
  CalendarDays,
  CheckCircle2,
  Clock3,
  Send,
} from 'lucide-vue-next'

import {
  BaseAlert,
  BaseBadge,
  BaseButton,
  BaseCard,
  BaseEmptyState,
  BasePageHeader,
  BasePagination,
} from '@/shared/components'
import type { PageableData } from '@/shared/api'

import {
  getPublicCourses,
  requestToJoinCourse,
  type PublicCourse,
} from '../api/courseApi'
import { useStudentApiError } from '../composables/useStudentApiError'
import { formatDate } from '@/shared/utils'

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

onMounted(() => void loadCourses())
</script>

<template>
  <section class="mx-auto max-w-app space-y-6">
    <BasePageHeader
      title="Khóa học công khai"
    />

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

    <BaseEmptyState
      v-else-if="courses.length === 0"
      title="Chưa có khóa học công khai"
      description="Hiện chưa có khóa học nào được công khai."
    >
      <template #icon>
        <BookOpen :size="24" />
      </template>
    </BaseEmptyState>

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
            <BaseBadge tone="secondary">
              Công khai
            </BaseBadge>

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
              Công khai {{ formatDate(course.publishedAt, "Chưa cập nhật") }}
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

    <BasePagination
      v-if="
        !loading &&
        courses.length > 0 &&
        pageable.totalPages > 1
      "
      :page="pageable.page"
      :total-pages="pageable.totalPages"
      :total-elements="pageable.totalElements"
      @previous="goToPage(pageable.page - 1)"
      @next="goToPage(pageable.page + 1)"
    />
  </section>
</template>