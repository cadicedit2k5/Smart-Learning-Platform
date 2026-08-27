<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Check, Clipboard, KeyRound, Link2Off } from 'lucide-vue-next'

import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'

import {
  createAccessCode,
  getAccessCodes,
  revokeAccessCode,
  type AccessCode,
  type AccessCodeItem,
} from '../api/accessCodeApi'
import { useLecturerApiError } from '../composables/useLecturerApiError'

const props = defineProps<{ courseId: string }>()
const { handleApiError } = useLecturerApiError()

const codes = ref<AccessCodeItem[]>([])
const rawCode = ref<AccessCode | null>(null)
const expiresAt = ref('')
const loading = ref(true)
const creating = ref(false)
const revokingId = ref('')
const copied = ref(false)
const message = ref('')

const minExpiry = computed(() => toLocalDateTime(new Date(Date.now() + 60_000)))

function toLocalDateTime(value: Date) {
  const local = new Date(value.getTime() - value.getTimezoneOffset() * 60_000)
  return local.toISOString().slice(0, 16)
}

const loadCodes = async () => {
  loading.value = true
  message.value = ''
  try {
    codes.value = await getAccessCodes(props.courseId)
  } catch (error) {
    message.value = handleApiError(error, 'Không thể tải danh sách mã tham gia.').message
  } finally {
    loading.value = false
  }
}

const handleCreate = async () => {
  if (!expiresAt.value || new Date(expiresAt.value).getTime() <= Date.now()) {
    message.value = 'Thời điểm hết hạn phải ở tương lai.'
    return
  }

  creating.value = true
  message.value = ''
  try {
    rawCode.value = await createAccessCode(props.courseId, new Date(expiresAt.value).toISOString())
    copied.value = false
    await loadCodes()
  } catch (error) {
    message.value = handleApiError(error, 'Không thể tạo mã tham gia.').message
  } finally {
    creating.value = false
  }
}

const copyCode = async () => {
  if (!rawCode.value) return
  try {
    await navigator.clipboard.writeText(rawCode.value.code)
    copied.value = true
  } catch {
    copied.value = false
  }
}

const handleRevoke = async (codeId: string) => {
  revokingId.value = codeId
  message.value = ''
  try {
    await revokeAccessCode(props.courseId, codeId)
    if (rawCode.value?.id === codeId) rawCode.value = null
    await loadCodes()
  } catch (error) {
    message.value = handleApiError(error, 'Không thể thu hồi mã tham gia.').message
  } finally {
    revokingId.value = ''
  }
}

const formatDate = (value: string | null) => {
  if (!value) return 'Không thời hạn'
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(
    new Date(value),
  )
}

onMounted(() => {
  expiresAt.value = toLocalDateTime(new Date(Date.now() + 7 * 24 * 60 * 60 * 1000))
  void loadCodes()
})
</script>

<template>
  <section class="grid gap-5 lg:grid-cols-[minmax(20rem,0.75fr)_minmax(0,1fr)]">
    <article class="rounded-card border border-app-border bg-app-surface p-5 shadow-card">
      <div class="flex items-center gap-3">
        <span class="flex h-11 w-11 items-center justify-center rounded-control bg-ai-soft text-ai"
          ><KeyRound :size="21"
        /></span>
        <div>
          <h2 class="font-heading text-lg font-bold text-app-text">Tạo mã tham gia</h2>
          <p class="text-sm text-app-text-muted">Raw code chỉ hiển thị sau khi tạo.</p>
        </div>
      </div>

      <BaseAlert v-if="message" class="mt-4">{{ message }}</BaseAlert>

      <div v-if="rawCode" class="mt-5 rounded-card border border-ai/20 bg-ai-soft p-4">
        <p class="text-xs font-semibold uppercase text-ai">Mã vừa tạo</p>
        <div class="mt-2 flex items-center justify-between gap-3">
          <code class="break-all text-xl font-bold tracking-widest text-ai">{{
            rawCode.code
          }}</code>
          <button
            type="button"
            class="rounded-control bg-app-surface p-2 text-ai"
            @click="copyCode"
          >
            <Check v-if="copied" :size="18" />
            <Clipboard v-else :size="18" />
          </button>
        </div>
      </div>

      <form class="mt-5 space-y-4" @submit.prevent="handleCreate">
        <div class="space-y-2">
          <label for="access-expiry" class="text-sm font-semibold text-app-text">Hết hạn lúc</label>
          <input
            id="access-expiry"
            v-model="expiresAt"
            type="datetime-local"
            :min="minExpiry"
            required
            class="h-11 w-full rounded-control border border-app-border bg-app-surface px-3 text-sm"
          />
        </div>
        <BaseButton type="submit" :loading="creating">Tạo mã</BaseButton>
      </form>
    </article>

    <article
      class="overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card"
    >
      <header class="border-b border-app-border px-5 py-4">
        <h2 class="font-heading text-lg font-bold text-app-text">Các mã đã tạo</h2>
      </header>
      <div v-if="loading" class="space-y-3 p-5">
        <div
          v-for="index in 3"
          :key="index"
          class="h-16 animate-pulse rounded-control bg-app-surface-muted"
        />
      </div>
      <div v-else-if="codes.length === 0" class="p-10 text-center text-sm text-app-text-muted">
        Chưa có mã tham gia.
      </div>
      <ul v-else class="divide-y divide-app-border">
        <li v-for="code in codes" :key="code.id" class="flex items-center gap-4 px-5 py-4">
          <div class="min-w-0 flex-1">
            <p class="font-mono font-semibold text-app-text">••••{{ code.codeHint }}</p>
            <p class="mt-1 text-xs text-app-text-muted">
              Hết hạn: {{ formatDate(code.expiresAt) }}
            </p>
          </div>
          <span
            class="rounded-pill px-2.5 py-1 text-xs font-semibold"
            :class="code.active ? 'bg-ai-soft text-ai' : 'bg-app-surface-muted text-app-text-muted'"
          >
            {{ code.active ? 'Hoạt động' : 'Đã thu hồi' }}
          </span>
          <button
            v-if="code.active"
            type="button"
            class="rounded-control p-2 text-danger hover:bg-danger-soft disabled:opacity-50"
            :disabled="revokingId === code.id"
            aria-label="Thu hồi mã"
            @click="handleRevoke(code.id)"
          >
            <Link2Off :size="18" />
          </button>
        </li>
      </ul>
    </article>
  </section>
</template>
