import axios from 'axios'

import type {
  ApiErrorDetail,
  ApiErrorResponse,
} from './types'

export interface ParsedApiError {
  statusCode?: number
  message: string
  errors: ApiErrorDetail[]
}

export const parseApiError = (
  error: unknown,
): ParsedApiError => {
  if (!axios.isAxiosError<ApiErrorResponse>(error)) {
    return {
      message: 'An unexpected error occurred.',
      errors: [],
    }
  }

  const status = error.response?.data?.status

  return {
    statusCode: error.response?.status,
    message:
      status?.message ??
      error.message ??
      'Unable to complete the request.',
    errors: status?.errors ?? [],
  }
}