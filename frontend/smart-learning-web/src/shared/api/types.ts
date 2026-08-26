export interface ApiStatus {
  timestamp: string
  statusCode: number
  message: string
}

export interface ApiResponse<T> {
  status: ApiStatus
  data: T
}

export interface PageableData {
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface PaginatedData<T> {
  content: T[]
  pageable: PageableData
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
