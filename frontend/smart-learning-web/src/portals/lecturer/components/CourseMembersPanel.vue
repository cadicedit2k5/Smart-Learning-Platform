<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Search, Trash2, UserPlus, UsersRound, X } from 'lucide-vue-next'

import type { PaginatedData } from '@/shared/api'
import type { CourseStatus, CourseVisibility } from '@/shared/course'
import { BaseAlert, BaseButton, BasePagination} from '@/shared/components'
import { formatDate } from '@/shared/utils'

import {
  addStudent,
  getCourseMembers,
  getStudents,
  removeCourseMember,
  type CourseMemberDetail,
  type UserLookup,
} from '../api/memberApi'
import { useLecturerApiError } from '../composables/useLecturerApiError'

const props = defineProps<{
  courseId: string
  visibility: CourseVisibility
  status: CourseStatus
}>()

const { handleApiError } = useLecturerApiError()

const membersPage = ref<PaginatedData<CourseMemberDetail>>({
  content: [],
  pageable: {
    page: 1,
    size: 0,
    totalElements: 0,
    totalPages: 0,
  },
})

const studentsPage = ref<PaginatedData<UserLookup>>({
  content: [],
  pageable: {
    page: 1,
    size: 0,
    totalElements: 0,
    totalPages: 0,
  },
})

const memberPage = ref(1)
const studentPage = ref(1)

const keyword = ref('')
const appliedKeyword = ref('')

const loadingMembers = ref(false)
const loadingStudents = ref(false)
const addingUserId = ref('')
const removingMemberId = ref('')

const memberMessage = ref('')
const studentMessage = ref('')

const canInviteStudent = computed(() => props.status === 'PUBLISHED')
const isInviteOnly = computed(() => props.visibility === 'INVITE_ONLY')

const members = computed(() => membersPage.value.content)
const memberTotalPages = computed(() => membersPage.value.pageable.totalPages)
const studentTotalPages = computed(() => studentsPage.value.pageable.totalPages)

const loadMembers = async () => {
  loadingMembers.value = true
  memberMessage.value = ''

  try {
    membersPage.value = await getCourseMembers(props.courseId, memberPage.value)
  } catch (error) {
    memberMessage.value = handleApiError(
      error,
      'Không thể tải danh sách thành viên.',
    ).message
  } finally {
    loadingMembers.value = false
  }
}

const loadStudents = async () => {
  if (!canInviteStudent.value) return

  loadingStudents.value = true
  studentMessage.value = ''

  try {
    studentsPage.value = await getStudents({
      page: studentPage.value,
      keyword: appliedKeyword.value || undefined,
    })
  } catch (error) {
    studentMessage.value = handleApiError(
      error,
      'Không thể tải danh sách học viên.',
    ).message
  } finally {
    loadingStudents.value = false
  }
}

const applySearch = () => {
  appliedKeyword.value = keyword.value.trim()
  studentPage.value = 1
  void loadStudents()
}

const resetSearch = () => {
  keyword.value = ''
  appliedKeyword.value = ''
  studentPage.value = 1
  void loadStudents()
}

const goToMemberPage = (page: number) => {
  if (
    page < 1 ||
    page > memberTotalPages.value ||
    page === memberPage.value
  ) return

  memberPage.value = page
  void loadMembers()
}

const goToStudentPage = (page: number) => {
  if (
    page < 1 ||
    page > studentTotalPages.value ||
    page === studentPage.value
  ) return

  studentPage.value = page
  void loadStudents()
}

const handleAdd = async (user: UserLookup) => {
  addingUserId.value = user.id
  studentMessage.value = ''

  try {
    await addStudent(props.courseId, user.id)

    memberPage.value = 1
    await loadMembers()
  } catch (error) {
    studentMessage.value = handleApiError(
      error,
      'Không thể thêm học viên.',
    ).message
  } finally {
    addingUserId.value = ''
  }
}

const handleRemove = async (member: CourseMemberDetail) => {
  const displayName =
    member.user?.fullName ||
    member.user?.email ||
    member.userId

  if (!window.confirm(`Xóa ${displayName} khỏi khóa học?`)) return

  removingMemberId.value = member.id
  memberMessage.value = ''

  try {
    await removeCourseMember(props.courseId, member.id)
    await loadMembers()

    if (
      membersPage.value.content.length === 0 &&
      memberPage.value > 1
    ) {
      memberPage.value -= 1
      await loadMembers()
    }
  } catch (error) {
    memberMessage.value = handleApiError(
      error,
      'Không thể xóa thành viên.',
    ).message
  } finally {
    removingMemberId.value = ''
  }
}

onMounted(async () => {
  await Promise.all([
    loadMembers(),
    loadStudents(),
  ])
})
</script>

<template>
  <section class="grid gap-5 xl:grid-cols-[minmax(0,1fr)_minmax(24rem,0.9fr)]">
    <!-- Thành viên khóa học -->
    <article
      class="overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card"
    >
      <header
        class="flex items-center justify-between border-b border-app-border px-5 py-4"
      >
        <div>
          <h2 class="font-heading text-xl font-bold text-app-text">
            Thành viên khóa học
          </h2>

          <p class="mt-1 text-sm text-app-text-muted">
            {{ membersPage.pageable.totalElements }} thành viên đang hoạt động
          </p>
        </div>

        <UsersRound :size="22" class="text-secondary" />
      </header>

      <BaseAlert
        v-if="memberMessage"
        class="m-4"
      >
        {{ memberMessage }}
      </BaseAlert>

      <div
        v-if="loadingMembers"
        class="space-y-3 p-5"
      >
        <div
          v-for="index in 4"
          :key="index"
          class="h-16 animate-pulse rounded-control bg-app-surface-muted"
        />
      </div>

      <div
        v-else-if="members.length === 0"
        class="p-10 text-center text-sm text-app-text-muted"
      >
        Chưa có thành viên.
      </div>

      <ul
        v-else
        class="divide-y divide-app-border"
      >
        <li
          v-for="member in members"
          :key="member.id"
          class="flex items-center gap-3 px-5 py-4"
        >
          <span
            class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-secondary-soft font-semibold text-secondary"
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
          >
            {{ member.role === 'OWNER' ? 'Chủ sở hữu' : 'Học viên' }}
          </span>

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

      <div
        v-if="membersPage.pageable.totalElements > 0"
        class="border-t border-app-border p-4"
      >
        <BasePagination
          :page="memberPage"
          :total-pages="memberTotalPages"
          :total-elements="membersPage.pageable.totalElements"
          @previous="goToMemberPage(memberPage - 1)"
          @next="goToMemberPage(memberPage + 1)"
        />
      </div>
    </article>

    <!-- Danh sách học viên có thể thêm -->
    <article
      v-if="canInviteStudent"
      class="overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card"
    >
      <header class="border-b border-app-border p-5">
        <div class="flex items-start gap-3">
          <div
            class="flex h-10 w-10 shrink-0 items-center justify-center rounded-card bg-secondary-soft text-secondary"
          >
            <UserPlus :size="17" />
          </div>

          <div>
            <h2 class="font-heading text-lg font-bold text-app-text">
              Mời học viên
            </h2>
          </div>
        </div>

        <form
          class="mt-5 flex gap-2"
          @submit.prevent="applySearch"
        >
          <div class="relative min-w-0 flex-1">
            <Search
              :size="17"
              class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-app-text-muted"
            />

            <input
              v-model="keyword"
              type="search"
              placeholder="Tìm tên hoặc email..."
              class="h-11 w-full rounded-control border border-app-border bg-app-surface pl-10 pr-10 text-sm outline-none transition focus:border-secondary focus:ring-2 focus:ring-secondary/20"
            />

            <button
              v-if="keyword"
              type="button"
              aria-label="Xóa từ khóa"
              class="absolute right-2 top-1/2 -translate-y-1/2 rounded-control p-1 text-app-text-muted hover:bg-app-surface-muted"
              @click="resetSearch"
            >
              <X :size="16" />
            </button>
          </div>

          <BaseButton
            type="submit"
            :loading="loadingStudents"
          >
            Tìm
          </BaseButton>
        </form>

        <div
          v-if="appliedKeyword"
          class="mt-3 flex items-center justify-between gap-3 text-xs text-app-text-muted"
        >
          <span>
            Kết quả cho “{{ appliedKeyword }}”
          </span>

          <button
            type="button"
            class="font-semibold text-secondary hover:underline"
            @click="resetSearch"
          >
            Xem tất cả
          </button>
        </div>
      </header>

      <BaseAlert
        v-if="studentMessage"
        class="m-4"
      >
        {{ studentMessage }}
      </BaseAlert>

      <div
        v-if="loadingStudents"
        class="space-y-2 p-4"
      >
        <div
          v-for="index in 5"
          :key="index"
          class="h-16 animate-pulse rounded-control bg-app-surface-muted"
        />
      </div>

      <div
        v-else-if="studentsPage.content.length === 0"
        class="p-10 text-center"
      >
        <p class="text-sm font-semibold text-app-text">
          {{
            appliedKeyword
              ? 'Không tìm thấy học viên'
              : 'Chưa có học viên'
          }}
        </p>

        <p class="mt-1 text-sm text-app-text-muted">
          {{
            appliedKeyword
              ? 'Hãy thử tìm bằng tên hoặc email khác.'
              : 'Hiện chưa có tài khoản học viên trong hệ thống.'
          }}
        </p>
      </div>

      <ul
        v-else
        class="divide-y divide-app-border"
      >
        <li
          v-for="user in studentsPage.content"
          :key="user.id"
          class="flex items-center gap-3 px-5 py-4"
        >
          <span
            class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-app-surface-muted text-sm font-semibold text-app-text"
          >
            {{ (user.fullName || user.email).charAt(0).toUpperCase() }}
          </span>

          <div class="min-w-0 flex-1">
            <p class="truncate text-sm font-semibold text-app-text">
              {{ user.fullName }}
            </p>

            <p class="truncate text-xs text-app-text-muted">
              {{ user.email }}
            </p>
          </div>

          <BaseButton
            variant="secondary"
            :loading="addingUserId === user.id"
            @click="handleAdd(user)"
          >
            <template #leading>
              <UserPlus :size="16" />
            </template>

            Thêm
          </BaseButton>
        </li>
      </ul>

      <div
        v-if="studentsPage.pageable.totalElements > 0"
        class="border-t border-app-border p-4"
      >
        <BasePagination
          :page="studentPage"
          :total-pages="studentTotalPages"
          :total-elements="studentsPage.pageable.totalElements"
          @previous="goToStudentPage(studentPage - 1)"
          @next="goToStudentPage(studentPage + 1)"
        />
      </div>
    </article>

    <!-- Chưa publish -->
    <article
      v-else-if="isInviteOnly"
      class="rounded-card border border-app-border bg-app-surface p-5 shadow-card"
    >
      <div class="flex items-start gap-3">
        <div
          class="flex h-10 w-10 shrink-0 items-center justify-center rounded-card bg-secondary-soft text-secondary"
        >
          <UserPlus :size="19" />
        </div>

        <div>
          <h2 class="font-heading text-lg font-bold text-app-text">
            Mời học viên
          </h2>

          <p class="mt-2 text-sm leading-6 text-app-text-muted">
            Hãy xuất bản khóa học trước khi thêm học viên vào khóa học.
          </p>
        </div>
      </div>
    </article>

    <article
      v-else
      class="rounded-card border border-app-border bg-app-surface p-5 shadow-card"
    >
      <div class="flex items-start gap-3">
        <div
          class="flex h-10 w-10 shrink-0 items-center justify-center rounded-card bg-secondary-soft text-secondary"
        >
          <UsersRound :size="19" />
        </div>

        <div>
          <h2 class="font-heading text-lg font-bold text-app-text">
            Khóa học công khai
          </h2>

          <p class="mt-2 text-sm leading-6 text-app-text-muted">
            Học viên có thể gửi yêu cầu tham gia khóa học. Bạn có thể xem và
            xử lý các yêu cầu trong mục “Yêu cầu tham gia”.
          </p>
        </div>
      </div>
    </article>
  </section>
</template>