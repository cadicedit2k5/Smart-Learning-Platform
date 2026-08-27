import { useRouter } from 'vue-router'

import { useAuthStore } from '@/features/auth/stores'
import { parseApiError } from '@/shared/api'

export interface StudentApiError {
  message: string
  fieldErrors: Record<string, string>
}

export const useStudentApiError = () => {
  const authStore = useAuthStore()
  const router = useRouter()

  const handleApiError = (error: unknown, fallbackMessage: string): StudentApiError => {
    const parsed = parseApiError(error)
    const fieldErrors = parsed.errors.reduce<Record<string, string>>((errors, detail) => {
      if (detail.field) errors[detail.field] = detail.message
      return errors
    }, {})

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
        message: 'Bạn không có quyền truy cập nội dung này.',
        fieldErrors,
      }
    }

    return {
      message: parsed.statusCode ? parsed.message : fallbackMessage,
      fieldErrors,
    }
  }

  return { handleApiError }
}
