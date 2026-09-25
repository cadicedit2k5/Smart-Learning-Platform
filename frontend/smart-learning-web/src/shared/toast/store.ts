import { defineStore } from 'pinia'
import { ref } from 'vue'

export type ToastType = 'success' | 'error' | 'info' | 'warning'

export interface Toast {
  id: number
  type: ToastType
  message: string
}

const TOAST_DURATION = 4000

export const useToastStore = defineStore('toast', () => {
  const items = ref<Toast[]>([])
  let nextId = 0

  const remove = (id: number) => {
    items.value = items.value.filter(item => item.id !== id)
  }

  const add = (type: ToastType, message: string) => {
    const id = ++nextId
    items.value.push({ id, type, message })
    setTimeout(() => remove(id), TOAST_DURATION)
  }

  const success = (message: string) => add('success', message)
  const error = (message: string) => add('error', message)
  const info = (message: string) => add('info', message)
  const warning = (message: string) => add('warning', message)

  return { items, success, error, info, warning, remove }
})