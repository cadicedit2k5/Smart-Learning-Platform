export interface ApiStatus {
  timestamp: string
  statusCode: number
  message: string
}

export interface ApiResponse<T> {
  status: ApiStatus
  data: T
}

export interface ApiErrorDetail {
  code: string
  field?: string
  message: string
}

export interface ApiErrorStatus extends ApiStatus {
  errors?: ApiErrorDetail[]
}

export interface ApiErrorResponse {
  status: ApiErrorStatus
}