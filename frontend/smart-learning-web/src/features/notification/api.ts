import { httpClient, type ApiResponse } from '@/shared/api'
import type { Notification, UnreadCount } from './types'

export const getNotifications = async (): Promise<Notification[]> => {
  const response = await httpClient.get<ApiResponse<Notification[]>>('/notifications')
  return response.data.data
}

export const getUnreadCount = async (): Promise<number> => {
  const response = await httpClient.get<ApiResponse<UnreadCount>>(
    '/notifications/unread-count',
  )

  return response.data.data.count
}

export const markNotificationAsRead = async (notificationId: string): Promise<void> => {
  await httpClient.patch(`/notifications/${notificationId}/read`)
}

export const markAllNotificationsAsRead = async (): Promise<void> => {
  await httpClient.patch('/notifications/read-all')
}