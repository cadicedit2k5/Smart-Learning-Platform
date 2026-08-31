<script setup lang="ts">
import {
  computed,
  reactive,
  ref,
  watch,
} from 'vue'

import type { UserRole } from '@/features/auth/role'
import BaseButton from '@/shared/components/BaseButton.vue'
import BaseInput from '@/shared/components/BaseInput.vue'

import type { AuthUser } from '@/features/auth/types'
import type {
  AdminCreateUserRequest,
  AdminUpdateUserRequest,
} from '../../api/userApi'
import { BaseAlert, BaseModal } from '@/shared/components'

const props = defineProps<{
  open: boolean
  user: AuthUser | null
  loading: boolean
  serverErrors?: Record<string, string>
  serverMessage?: string
}>();

const emit = defineEmits<{
  close: []
  create: [request: AdminCreateUserRequest]
  update: [
    id: string,
    request: AdminUpdateUserRequest,
  ]
}>()

const form = reactive({
  email: '',
  password: '',
  fullName: '',
  roleCode: 'STUDENT' as UserRole,
})

const avatar = ref<File>()
const localErrors = reactive<Record<string, string>>({})

const isEditing = computed(() => {
  return props.user !== null;
});

const clearErrors = () => {
  Object.keys(localErrors).forEach((field) => {
    delete localErrors[field];
  })
};

const resetForm = () => {
  form.email = props.user?.email ?? '';
  form.password = '';
  form.fullName = props.user?.fullName ?? '';
  form.roleCode = props.user?.role.code ?? 'STUDENT';

  avatar.value = undefined;
  clearErrors();
};

watch(
  () => [props.open, props.user],
  ([open]) => {
    if (open) {
      resetForm()
    }
  },
);

const getError = (field: string) => {
  return (
    localErrors[field] ?? props.serverErrors?.[field]
  );
};

const validate = () => {
  clearErrors();

  const email = form.email.trim();
  const fullName = form.fullName.trim();

  if (!fullName) {
    localErrors.fullName = 'Vui lòng nhập họ và tên.';
  } else if (fullName.length > 100) {
    localErrors.fullName = 'Họ tên không được quá 100 ký tự.';
  }

  if (!email) {
    localErrors.email = 'Vui lòng nhập email.';
  }

  if (!isEditing.value && !form.password) {
    localErrors.password = 'Vui lòng nhập mật khẩu.';
  }

  if (form.password && (
      form.password.length < 8 ||
      form.password.length > 36
    )) {
    localErrors.password = 'Mật khẩu phải có từ 8 đến 36 ký tự.';
  }

//   if (avatar.value) {
//     localErrors.avatar =
//       'Ảnh đại diện không được vượt quá 50 MB.'
//   }

  return Object.keys(localErrors).length === 0;
}

const selectAvatar = (event: Event) => {
  const input = event.target as HTMLInputElement;

  avatar.value = input.files?.[0];
}

const submit = () => {
  if (!validate()) {
    return;
  }

  const email = form.email.trim();
  const fullName = form.fullName.trim();

  if (!props.user) {
    emit('create', {
      email,
      fullName,
      password: form.password,
      roleCode: form.roleCode,
      avatar: avatar.value,
    })

    return
  }

  const request: AdminUpdateUserRequest = {};

  if (email !== props.user.email) {
    request.email = email;
  }

  if (fullName !== props.user.fullName) {
    request.fullName = fullName;
  }

  if (form.roleCode !== props.user.role.code) {
    request.roleCode = form.roleCode;
  }

  if (form.password) {
    request.password = form.password;
  }

  if (avatar.value) {
    request.avatar = avatar.value;
  }

  emit('update', props.user.id, request);
}

const closeModal = () => {
  if (!props.loading) {
    emit('close')
  }
}
</script>

<template>
  <BaseModal
    :open="open"
    :title="
      isEditing
        ? 'Cập nhật người dùng'
        : 'Thêm người dùng'
    "
    :description="
      isEditing
        ? 'Cập nhật thông tin và vai trò của tài khoản.'
        : 'Tạo tài khoản mới trong hệ thống.'
    "
    :loading="loading"
    @close="closeModal"
  >
    <BaseAlert
      v-if="serverMessage"
      variant="error"
      class="mb-5"
    >
      {{ serverMessage }}
    </BaseAlert>

    <form
      id="user-form"
      class="space-y-5"
      @submit.prevent="submit"
    >
      <BaseInput
        v-model="form.fullName"
        label="Họ và tên"
        placeholder="Nguyễn Văn An"
        autocomplete="name"
        required
        :disabled="loading"
        :error="getError('fullName')"
      />

      <BaseInput
        v-model="form.email"
        label="Email"
        type="email"
        placeholder="user@example.com"
        autocomplete="email"
        required
        :disabled="loading"
        :error="getError('email')"
      />

      <div
        class="grid gap-5 sm:grid-cols-2"
      >
        <BaseInput
          v-model="form.password"
          label="Mật khẩu"
          type="password"
          autocomplete="new-password"
          :required="!isEditing"
          :disabled="loading"
          :placeholder="
            isEditing
              ? 'Để trống nếu không đổi'
              : '8–36 ký tự'
          "
          :error="getError('password')"
        />

        <div class="space-y-2">
          <label
            for="user-role"
            class="block text-sm font-semibold text-app-text"
          >
            Vai trò

            <span class="text-danger">
              *
            </span>
          </label>

          <select
            id="user-role"
            v-model="form.roleCode"
            class="h-11 w-full rounded-control border border-app-border bg-app-surface px-3 text-sm text-app-text outline-none transition focus:border-secondary focus:ring-2 focus:ring-secondary/20 disabled:opacity-60"
            :disabled="loading"
          >
            <option value="STUDENT">
              Học viên
            </option>

            <option value="LECTURER">
              Giảng viên
            </option>

            <option value="ADMIN">
              Quản trị viên
            </option>
          </select>
        </div>
      </div>

      <!-- Avatar -->
      <div class="space-y-2">
        <div>
          <p
            class="text-sm font-semibold text-app-text"
          >
            Ảnh đại diện
          </p>

          <p
            class="mt-1 text-xs text-app-text-muted"
          >
            {{
              isEditing
                ? 'Chọn ảnh mới nếu muốn thay ảnh hiện tại.'
                : 'Không bắt buộc.'
            }}
          </p>
        </div>

        <label
          for="user-avatar"
          class="flex cursor-pointer items-center gap-3 rounded-control border border-dashed border-app-border bg-app-surface-muted px-4 py-4 transition hover:border-secondary"
        >
          <span
            class="flex h-10 w-10 shrink-0 items-center justify-center rounded-control bg-secondary-soft text-secondary"
          >
            <Upload :size="19" />
          </span>

          <span
            class="min-w-0 text-sm"
          >
            <span
              class="block truncate font-medium text-app-text"
            >
              {{
                avatar?.name ??
                  (
                    isEditing
                      ? 'Chọn ảnh đại diện mới'
                      : 'Chọn tệp ảnh'
                  )
              }}
            </span>

            <span
              class="text-app-text-muted"
            >
              JPG, PNG hoặc WebP
            </span>
          </span>
        </label>

        <input
          id="user-avatar"
          type="file"
          accept="image/jpeg,image/png,image/webp"
          class="sr-only"
          :disabled="loading"
          @change="selectAvatar"
        />

        <p
          v-if="getError('avatar')"
          class="text-sm text-danger"
        >
          {{ getError('avatar') }}
        </p>
      </div>
    </form>

    <template #footer>
      <div
        class="flex justify-end gap-3"
      >
        <BaseButton
          variant="secondary"
          :disabled="loading"
          @click="closeModal"
        >
          Hủy
        </BaseButton>

        <BaseButton
          type="submit"
          form="user-form"
          :loading="loading"
        >
          {{
            isEditing
              ? 'Lưu thay đổi'
              : 'Tạo người dùng'
          }}
        </BaseButton>
      </div>
    </template>
  </BaseModal>
</template>