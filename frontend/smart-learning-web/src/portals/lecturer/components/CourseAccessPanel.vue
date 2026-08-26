<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Check, Clipboard, KeyRound, Link2Off, UserPlus } from 'lucide-vue-next'

import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import BaseInput from '@/shared/components/BaseInput.vue'

import {
  addStudent,
  createAccessCode,
  revokeAccessCode,
  type AccessCode,
  type CourseMember,
} from '../api/courseApi'
import { useLecturerApiError } from '../composables/useLecturerApiError'

const props = defineProps<{
  courseId: string
}>()

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

const { handleApiError } = useLecturerApiError()

const studentUserId = ref('')
const addingStudent = ref(false)
const studentMessage = ref('')
const studentSuccess = ref<CourseMember | null>(null)
const studentErrors = reactive<Record<string, string>>({})

const expiresAt = ref('')
const creatingCode = ref(false)
const revokingCode = ref(false)
const codeMessage = ref('')
const accessCode = ref<AccessCode | null>(null)
const codeErrors = reactive<Record<string, string>>({})
const copied = ref(false)

const minExpiry = computed(() => {
  const value = new Date(Date.now() + 60 * 1000)
  return toLocalDateTime(value)
})

function toLocalDateTime(value: Date) {
  const local = new Date(value.getTime() - value.getTimezoneOffset() * 60_000)
  return local.toISOString().slice(0, 16)
}

const clearErrors = (errors: Record<string, string>) => {
  Object.keys(errors).forEach((field) => {
    delete errors[field]
  })
}

const submitStudent = async () => {
  clearErrors(studentErrors)
  studentMessage.value = ''
  studentSuccess.value = null

  const userId = studentUserId.value.trim()

  if (!userId) {
    studentErrors.userId = 'Vui lòng nhập ID học viên.'
  } else if (!UUID_PATTERN.test(userId)) {
    studentErrors.userId = 'ID học viên phải đúng định dạng UUID.'
  }

  if (Object.keys(studentErrors).length > 0) {
    return
  }

  addingStudent.value = true

  try {
    studentSuccess.value = await addStudent(props.courseId, userId)
    studentUserId.value = ''
  } catch (error) {
    const parsed = handleApiError(error, 'Không thể thêm học viên.')
    studentMessage.value = parsed.message
    Object.assign(studentErrors, parsed.fieldErrors)
  } finally {
    addingStudent.value = false
  }
}

const submitAccessCode = async () => {
  clearErrors(codeErrors)
  codeMessage.value = ''

  if (!expiresAt.value) {
    codeErrors.expiresAt = 'Vui lòng chọn thời điểm hết hạn.'
  } else if (new Date(expiresAt.value).getTime() <= Date.now()) {
    codeErrors.expiresAt = 'Thời điểm hết hạn phải ở tương lai.'
  }

  if (Object.keys(codeErrors).length > 0) {
    return
  }

  creatingCode.value = true

  try {
    accessCode.value = await createAccessCode(
      props.courseId,
      new Date(expiresAt.value).toISOString(),
    )
    copied.value = false
  } catch (error) {
    const parsed = handleApiError(error, 'Không thể tạo mã tham gia.')
    codeMessage.value = parsed.message
    Object.assign(codeErrors, parsed.fieldErrors)
  } finally {
    creatingCode.value = false
  }
}

const copyCode = async () => {
  if (!accessCode.value) {
    return
  }

  try {
    await navigator.clipboard.writeText(accessCode.value.code)
    copied.value = true
  } catch {
    copied.value = false
  }
}

const revokeCode = async () => {
  if (!accessCode.value) {
    return
  }

  revokingCode.value = true
  codeMessage.value = ''

  try {
    await revokeAccessCode(props.courseId, accessCode.value.id)
    accessCode.value = null
    codeMessage.value = 'Mã tham gia đã được thu hồi.'
  } catch (error) {
    codeMessage.value = handleApiError(error, 'Không thể thu hồi mã tham gia.').message
  } finally {
    revokingCode.value = false
  }
}

const formatDate = (value: string) => {
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

onMounted(() => {
  expiresAt.value = toLocalDateTime(new Date(Date.now() + 7 * 24 * 60 * 60 * 1000))
})
</script>

<template>
  <section class="grid gap-5 xl:grid-cols-2">
    <article class="rounded-card border border-app-border bg-app-surface p-5 shadow-card sm:p-6">
      <header class="flex gap-3">
        <span
          class="flex h-11 w-11 shrink-0 items-center justify-center rounded-control bg-secondary-soft text-secondary"
        >
          <UserPlus :size="21" />
        </span>
        <div>
          <h2 class="font-heading text-lg font-bold text-app-text">Thêm học viên</h2>
          <p class="mt-1 text-sm text-app-text-muted">Gán quyền STUDENT bằng ID tài khoản.</p>
        </div>
      </header>

      <form class="mt-6 space-y-4" @submit.prevent="submitStudent">
        <BaseAlert v-if="studentMessage">{{ studentMessage }}</BaseAlert>

        <BaseAlert v-if="studentSuccess" variant="info" title="Đã thêm học viên">
          Thành viên {{ studentSuccess.userId }} hiện có trạng thái {{ studentSuccess.status }}.
        </BaseAlert>

        <BaseInput
          v-model="studentUserId"
          label="ID học viên"
          placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
          required
          :disabled="addingStudent"
          :error="studentErrors.userId"
        />

        <BaseButton type="submit" :loading="addingStudent">
          <template #leading><UserPlus :size="18" /></template>
          Thêm vào khóa học
        </BaseButton>
      </form>
    </article>

    <article class="rounded-card border border-app-border bg-app-surface p-5 shadow-card sm:p-6">
      <header class="flex gap-3">
        <span
          class="flex h-11 w-11 shrink-0 items-center justify-center rounded-control bg-ai-soft text-ai"
        >
          <KeyRound :size="21" />
        </span>
        <div>
          <h2 class="font-heading text-lg font-bold text-app-text">Mã tham gia</h2>
          <p class="mt-1 text-sm text-app-text-muted">
            Tạo mã có thời hạn để chia sẻ với học viên.
          </p>
        </div>
      </header>

      <div v-if="accessCode" class="mt-6 space-y-4">
        <BaseAlert variant="ai" title="Mã đã được tạo">
          Hãy sao chép và gửi mã cho học viên trước khi rời trang này.
        </BaseAlert>

        <div class="rounded-card border border-ai/20 bg-ai-soft p-4">
          <p class="text-xs font-semibold uppercase tracking-wide text-ai">Mã tham gia</p>
          <div class="mt-2 flex items-center justify-between gap-3">
            <code class="break-all font-mono text-2xl font-bold tracking-[0.18em] text-ai">{{
              accessCode.code
            }}</code>
            <button
              type="button"
              class="flex h-10 w-10 shrink-0 items-center justify-center rounded-control bg-app-surface text-ai shadow-sm"
              :aria-label="copied ? 'Đã sao chép' : 'Sao chép mã'"
              @click="copyCode"
            >
              <Check v-if="copied" :size="18" />
              <Clipboard v-else :size="18" />
            </button>
          </div>
          <p class="mt-3 text-xs text-ai/80">
            Hết hạn {{ formatDate(accessCode.expiresAt ?? expiresAt) }}
          </p>
        </div>

        <button
          type="button"
          class="inline-flex h-11 items-center justify-center gap-2 rounded-control border border-danger/30 px-4 text-sm font-semibold text-danger hover:bg-danger-soft disabled:opacity-60"
          :disabled="revokingCode"
          @click="revokeCode"
        >
          <span
            v-if="revokingCode"
            class="h-4 w-4 animate-spin rounded-full border-2 border-current border-r-transparent"
          />
          <Link2Off v-else :size="18" />
          Thu hồi mã
        </button>
      </div>

      <form v-else class="mt-6 space-y-4" @submit.prevent="submitAccessCode">
        <BaseAlert v-if="codeMessage" :variant="codeMessage.includes('đã được') ? 'info' : 'error'">
          {{ codeMessage }}
        </BaseAlert>

        <div class="space-y-2">
          <label for="access-code-expiry" class="block text-sm font-semibold text-app-text">
            Hết hạn lúc <span class="text-danger">*</span>
          </label>
          <input
            id="access-code-expiry"
            v-model="expiresAt"
            type="datetime-local"
            :min="minExpiry"
            class="h-11 w-full rounded-control border bg-app-surface px-3 text-sm outline-none focus:border-secondary focus:ring-2 focus:ring-secondary/20"
            :class="codeErrors.expiresAt ? 'border-danger' : 'border-app-border'"
            :disabled="creatingCode"
            required
          />
          <p v-if="codeErrors.expiresAt" role="alert" class="text-sm text-danger">
            {{ codeErrors.expiresAt }}
          </p>
        </div>

        <BaseButton type="submit" :loading="creatingCode">
          <template #leading><KeyRound :size="18" /></template>
          Tạo mã tham gia
        </BaseButton>
      </form>
    </article>
  </section>
</template>
