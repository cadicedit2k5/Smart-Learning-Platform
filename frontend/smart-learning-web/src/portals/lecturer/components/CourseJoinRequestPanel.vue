<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  Check,
  Clock3,
  UserRoundX,
} from 'lucide-vue-next'

import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'

import {
  approveJoinRequest,
  getJoinRequests,
  rejectJoinRequest,
  type CourseMemberDetail,
} from '../api/memberApi'
import { useLecturerApiError } from '../composables/useLecturerApiError'
import { formatDateTime } from '@/shared/utils'
import type { PaginatedData } from '@/shared/api'
import { BasePagination } from '@/shared/components'

const props = defineProps<{
  courseId: string
}>()

const { handleApiError } = useLecturerApiError()

const requestsPage = ref<PaginatedData<CourseMemberDetail>>({
  content: [],
  pageable: {
    page: 1,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  },
})

const currentPage = ref(1)

const loading = ref(true)
const message = ref('')

const processingId = ref('')

const totalPages = computed(
  () => requestsPage.value.pageable.totalPages,
)

const loadRequests = async () => {
  loading.value = true
  message.value = ''

  try {
    requestsPage.value = await getJoinRequests(
      props.courseId,
      currentPage.value
    )
  } catch (error) {
    message.value = handleApiError(
      error,
      'Không thể tải yêu cầu tham gia.',
    ).message
  } finally {
    loading.value = false
  }
}

const goToPage = (page: number) => {
  if (
    page < 1 ||
    page > totalPages.value ||
    page === currentPage.value
  ) {
    return
  }

  currentPage.value = page
  void loadRequests()
}

const reloadAfterDecision = async () => {
  if (requestsPage.value.content.length === 1 &&
    currentPage.value > 1
  ) {
    currentPage.value -= 1
  }

  await loadRequests()
}

const handleApprove = async (
  request: CourseMemberDetail,
) => {
   if (processingId.value) return

  processingId.value = request.id
  message.value = ''

  try {
    await approveJoinRequest(
      props.courseId,
      request.id,
    )

    await reloadAfterDecision();
  } catch (error) {
    message.value = handleApiError(
      error,
      'Không thể chấp nhận yêu cầu.',
    ).message
  } finally {
    processingId.value = ''
  }
}

const handleReject = async (
  request: CourseMemberDetail,
) => {
  if (processingId.value) return

  processingId.value = request.id
  message.value = ''

  try {
    await rejectJoinRequest(
      props.courseId,
      request.id,
    )

    await reloadAfterDecision()
  } catch (error) {
    message.value = handleApiError(
      error,
      'Không thể từ chối yêu cầu tham gia.',
    ).message
  } finally {
    processingId.value = ''
  }
}


onMounted(() => {
  void loadRequests()
})
</script>

<template>
  <section
    class="overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card"
  >
    <header
      class="flex items-center justify-between border-b border-app-border px-5 py-4"
    >
      <div>
        <h2
          class="font-heading text-xl font-bold text-app-text"
        >
          Yêu cầu tham gia
        </h2>

        <p class="mt-1 text-sm text-app-text-muted">
          {{ requestsPage.pageable.totalElements }}
          yêu cầu đang chờ duyệt
        </p>
      </div>

      <Clock3
        :size="22"
        class="text-secondary"
      />
    </header>

    <BaseAlert
      v-if="message"
      class="m-4"
    >
      {{ message }}
    </BaseAlert>

    <div
      v-if="loading"
      class="space-y-3 p-5"
    >
      <div
        v-for="index in 3"
        :key="index"
        class="h-16 animate-pulse rounded-control bg-app-surface-muted"
      />
    </div>

    <div
      v-else-if="requestsPage.content.length === 0"
      class="p-10 text-center text-sm text-app-text-muted"
    >
      Không có yêu cầu tham gia đang chờ duyệt.
    </div>

    <ul
      v-else
      class="divide-y divide-app-border"
    >
      <li
        v-for="request in requestsPage.content"
        :key="request.id"
        class="flex flex-wrap items-center gap-4 px-5 py-4"
      >
        <div class="min-w-0 flex-1">
          <p
            class="truncate font-semibold text-app-text"
          >
            {{
              request.user?.fullName ||
              request.userId
            }}
          </p>

          <p
            class="truncate text-sm text-app-text-muted"
          >
            {{
              request.user?.email ||
              'Không có thông tin email'
            }}
          </p>

          <p
            class="mt-1 text-xs text-app-text-muted"
          >
            Gửi yêu cầu
            {{ formatDateTime(request.createdAt) }}
          </p>
        </div>

        <div class="flex gap-2">
          <BaseButton
            variant="secondary"
            :loading="processingId === request.id"
            :disabled="Boolean(processingId) && processingId === request.id"
            @click="handleReject(request)"
          >
            <template #leading>
              <UserRoundX :size="16" />
            </template>

            Từ chối
          </BaseButton>

          <BaseButton
            :loading="processingId === request.id"
            :disabled="Boolean(processingId) && processingId === request.id"
            @click="handleApprove(request)"
          >
            <template #leading>
              <Check :size="16" />
            </template>

            Chấp nhận
          </BaseButton>
        </div>
      </li>
    </ul>

    <div
      v-if="
        !loading &&
        requestsPage.pageable.totalElements > 0
      "
      class="border-t border-app-border px-5 py-4"
    >
      <BasePagination
        :page="currentPage"
        :total-pages="totalPages"
        :total-elements="
          requestsPage.pageable.totalElements
        "
        @previous="goToPage(currentPage - 1)"
        @next="goToPage(currentPage + 1)"
      />
    </div>
  </section>
</template>