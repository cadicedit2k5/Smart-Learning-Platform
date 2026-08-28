<script setup lang="ts">
import { onMounted, ref } from 'vue'
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

const props = defineProps<{
  courseId: string
}>()

const { handleApiError } = useLecturerApiError()

const requests = ref<CourseMemberDetail[]>([])
const loading = ref(true)
const message = ref('')

const approvingId = ref('')
const rejectingId = ref('')

const loadRequests = async () => {
  loading.value = true
  message.value = ''

  try {
    requests.value =
      await getJoinRequests(props.courseId)
  } catch (error) {
    message.value = handleApiError(
      error,
      'Không thể tải yêu cầu tham gia.',
    ).message
  } finally {
    loading.value = false
  }
}

const handleApprove = async (
  request: CourseMemberDetail,
) => {
  approvingId.value = request.id
  message.value = ''

  try {
    await approveJoinRequest(
      props.courseId,
      request.id,
    )

    requests.value = requests.value.filter(
      (item) => item.id !== request.id,
    )
  } catch (error) {
    message.value = handleApiError(
      error,
      'Không thể chấp nhận yêu cầu.',
    ).message
  } finally {
    approvingId.value = ''
  }
}

const handleReject = async (
  request: CourseMemberDetail,
) => {
  rejectingId.value = request.id
  message.value = ''

  try {
    await rejectJoinRequest(
      props.courseId,
      request.id,
    )

    requests.value = requests.value.filter(
      (item) => item.id !== request.id,
    )
  } catch (error) {
    message.value = handleApiError(
      error,
      'Không thể từ chối yêu cầu.',
    ).message
  } finally {
    rejectingId.value = ''
  }
}

const formatDate = (value: string) =>
  new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))

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
          {{ requests.length }}
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
      v-else-if="requests.length === 0"
      class="p-10 text-center text-sm text-app-text-muted"
    >
      Không có yêu cầu tham gia đang chờ duyệt.
    </div>

    <ul
      v-else
      class="divide-y divide-app-border"
    >
      <li
        v-for="request in requests"
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
            {{ formatDate(request.createdAt) }}
          </p>
        </div>

        <div class="flex gap-2">
          <BaseButton
            variant="secondary"
            :loading="rejectingId === request.id"
            :disabled="approvingId === request.id"
            @click="handleReject(request)"
          >
            <template #leading>
              <UserRoundX :size="16" />
            </template>

            Từ chối
          </BaseButton>

          <BaseButton
            :loading="approvingId === request.id"
            :disabled="rejectingId === request.id"
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
  </section>
</template>