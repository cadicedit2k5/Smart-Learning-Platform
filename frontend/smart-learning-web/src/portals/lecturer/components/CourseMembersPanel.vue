<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ChevronLeft, ChevronRight, Search, Trash2, UserPlus, UsersRound } from 'lucide-vue-next'

import BaseAlert from '@/shared/components/BaseAlert.vue'
import BaseButton from '@/shared/components/BaseButton.vue'
import BaseInput from '@/shared/components/BaseInput.vue'
import type { PaginatedData } from '@/shared/api'

import {
  addStudent,
  getCourseMembers,
  removeCourseMember,
  searchStudents,
  type CourseMemberDetail,
  type UserLookup,
} from '../api/memberApi'
import { useLecturerApiError } from '../composables/useLecturerApiError'
import type { CourseStatus, CourseVisibility } from '../api/courseApi'

const props = defineProps<{ 
  courseId: string
  visibility: CourseVisibility
  status: CourseStatus
 }>()
const { handleApiError } = useLecturerApiError()

const members = ref<CourseMemberDetail[]>([])
const loading = ref(true)
const message = ref('')
const keyword = ref('')
const searchPage = ref(1)
const searching = ref(false)
const addingUserId = ref('')
const removingMemberId = ref('')
const results = ref<PaginatedData<UserLookup>>({
  content: [],
  pageable: { page: 1, size: 10, totalElements: 0, totalPages: 0 },
})

const canInviteStudent = computed(() =>
    props.visibility === 'INVITE_ONLY' && props.status === 'PUBLISHED'
)
const memberUserIds = computed(() => new Set(members.value.map((member) => member.userId)))
const availableStudents = computed(() =>
  results.value.content.filter((user) => !memberUserIds.value.has(user.id)),
)

const loadMembers = async () => {
  loading.value = true
  message.value = ''

  try {
    members.value = await getCourseMembers(props.courseId);
  } catch (error) {
    message.value = handleApiError(error, 'Không thể tải danh sách thành viên.').message
  } finally {
    loading.value = false
  }
}

const runSearch = async (page = 1) => {
  const value = keyword.value.trim()
  if (!value) {
    results.value = {
      content: [],
      pageable: { page: 1, size: 10, totalElements: 0, totalPages: 0 },
    }
    return
  }

  searching.value = true
  message.value = ''
  try {
    searchPage.value = page
    results.value = await searchStudents(value, page)
  } catch (error) {
    message.value = handleApiError(error, 'Không thể tìm học viên.').message
  } finally {
    searching.value = false
  }
}

const handleAdd = async (user: UserLookup) => {
  addingUserId.value = user.id
  message.value = ''
  try {
    await addStudent(props.courseId, user.id)
    await loadMembers()
    await runSearch(searchPage.value)
  } catch (error) {
    message.value = handleApiError(error, 'Không thể thêm học viên.').message
  } finally {
    addingUserId.value = ''
  }
}

const handleRemove = async (member: CourseMemberDetail) => {
  const displayName = member.user?.fullName || member.user?.email || member.userId
  if (!window.confirm(`Xóa ${displayName} khỏi khóa học?`))
    return

  removingMemberId.value = member.id
  message.value = ''
  try {
    await removeCourseMember(props.courseId, member.id)
    members.value = members.value.filter((item) => item.id !== member.id)
  } catch (error) {
    message.value = handleApiError(error, 'Không thể xóa thành viên.').message
  } finally {
    removingMemberId.value = ''
  }
}

const formatDate = (value: string | null) => {
  if (!value) return '—'
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium' }).format(new Date(value))
}

onMounted(() => void loadMembers())
</script>

<template>
  <section
    class="grid gap-5"
    :class="canInviteStudent
        ? 'xl:grid-cols-[minmax(0,1fr)_minmax(22rem,0.8fr)]'
        : 'grid-cols-1'">
    <article
      class="overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card"
    >
      <header class="flex items-center justify-between border-b border-app-border px-5 py-4">
        <div>
          <h2 class="font-heading text-xl font-bold text-app-text">Thành viên khóa học</h2>
          <p class="mt-1 text-sm text-app-text-muted">
            {{ members.length }} thành viên đang hoạt động
          </p>
        </div>
        <UsersRound :size="22" class="text-secondary" />
      </header>

      <BaseAlert v-if="message" class="m-4">{{ message }}</BaseAlert>

      <div v-if="loading" class="space-y-3 p-5">
        <div
          v-for="index in 4"
          :key="index"
          class="h-16 animate-pulse rounded-control bg-app-surface-muted"
        />
      </div>
      <div v-else-if="members.length === 0" class="p-10 text-center text-sm text-app-text-muted">
        Chưa có thành viên.
      </div>
      <ul v-else class="divide-y divide-app-border">
        <li v-for="member in members" :key="member.id" class="flex items-center gap-3 px-5 py-4">
          <span
            class="flex h-10 w-10 shrink-0 items-center justify-center rounded-pill bg-secondary-soft font-semibold text-secondary"
          >
            {{
              (member.user?.fullName || member.user?.email || '?')
                .charAt(0)
                .toUpperCase()
            }}
          </span>
          <div class="min-w-0 flex-1">
            <p class="truncate font-semibold text-app-text">
              {{
                member.user?.fullName ||
                member.user?.email ||
                member.userId
              }}
            </p>
            <p class="truncate text-sm text-app-text-muted">
              {{
                member.user?.email ||
                `Tham gia ${formatDate(member.joinedAt)}`
              }}
            </p>
          </div>
          <span
            class="rounded-pill bg-app-surface-muted px-2.5 py-1 text-xs font-semibold text-app-text-muted"
            >{{ member.role }}</span
          >
          <button
            v-if="member.role !== 'OWNER'"
            type="button"
            class="rounded-control p-2 text-danger hover:bg-danger-soft disabled:opacity-50"
            :disabled="removingMemberId === member.id"
            aria-label="Xóa thành viên"
            @click="handleRemove(member)"
          >
            <Trash2 :size="17" />
          </button>
        </li>
      </ul>
    </article>

    <article
        v-if="canInviteStudent"
        class="rounded-card border border-app-border bg-app-surface p-5 shadow-card"
      >
      <h2 class="font-heading text-lg font-bold text-app-text">Thêm học viên</h2>
      <p class="mt-1 text-sm text-app-text-muted">Tìm tài khoản có system role STUDENT.</p>

      <form class="mt-5 flex gap-2" @submit.prevent="runSearch(1)">
        <BaseInput v-model="keyword" class="min-w-0 flex-1" placeholder="Tên hoặc email học viên">
          <template #leading><Search :size="17" /></template>
        </BaseInput>
        <BaseButton type="submit" :loading="searching">Tìm</BaseButton>
      </form>

      <div
        v-if="availableStudents.length === 0"
        class="mt-5 rounded-control bg-app-surface-muted p-5 text-center text-sm text-app-text-muted"
      >
        {{
          results.pageable.totalElements > 0
            ? 'Các kết quả đều đã là thành viên.'
            : 'Nhập từ khóa để tìm học viên.'
        }}
      </div>
      <ul v-else class="mt-5 divide-y divide-app-border rounded-control border border-app-border">
        <li v-for="user in availableStudents" :key="user.id" class="flex items-center gap-3 p-3">
          <div class="min-w-0 flex-1">
            <p class="truncate text-sm font-semibold text-app-text">{{ user.fullName }}</p>
            <p class="truncate text-xs text-app-text-muted">{{ user.email }}</p>
          </div>
          <BaseButton
            variant="secondary"
            :loading="addingUserId === user.id"
            @click="handleAdd(user)"
          >
            <template #leading><UserPlus :size="16" /></template>
            Thêm
          </BaseButton>
        </li>
      </ul>

      <div
        v-if="results.pageable.totalPages > 1"
        class="mt-4 flex items-center justify-between text-sm text-app-text-muted"
      >
        <button
          type="button"
          class="rounded-control p-2 disabled:opacity-40"
          :disabled="searchPage <= 1 || searching"
          @click="runSearch(searchPage - 1)"
        >
          <ChevronLeft :size="18" />
        </button>
        <span>Trang {{ searchPage }} / {{ results.pageable.totalPages }}</span>
        <button
          type="button"
          class="rounded-control p-2 disabled:opacity-40"
          :disabled="searchPage >= results.pageable.totalPages || searching"
          @click="runSearch(searchPage + 1)"
        >
          <ChevronRight :size="18" />
        </button>
      </div>
    </article>
  </section>
</template>
