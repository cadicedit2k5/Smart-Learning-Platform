import { useRouter } from 'vue-router'

import { useAuthStore } from '@/features/auth/stores'
import { parseApiError } from '@/shared/api'

export interface LecturerApiError {
  message: string
  fieldErrors: Record<string, string>
}

export const useLecturerApiError = () => {
  const authStore = useAuthStore()
  const router = useRouter()

  const handleApiError = (error: unknown, fallbackMessage: string): LecturerApiError => {
    const parsed = parseApiError(error)
    const fieldErrors = parsed.errors.reduce<Record<string, string>>((errors, detail) => {
      if (detail.field) {
        errors[detail.field] = detail.message
      }

      return errors
    }, {})
    const message = parsed.statusCode ? parsed.message : fallbackMessage

    if (parsed.statusCode === 401) {
      authStore.logout()
      void router.replace({ name: 'login' })

      return {
        message: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
        fieldErrors,
      }
    }

    if (parsed.statusCode === 403) {
      return {
        message: 'Bạn không có đủ quyền để thực hiện thao tác này.',
        fieldErrors,
      }
    }

    if (parsed.statusCode === 409) {
      return {
        message: parsed.message || 'Dữ liệu đang xung đột với trạng thái hiện tại.',
        fieldErrors,
      }
    }

    return {
      message,
      fieldErrors,
    }
  }

  return { handleApiError }
}
