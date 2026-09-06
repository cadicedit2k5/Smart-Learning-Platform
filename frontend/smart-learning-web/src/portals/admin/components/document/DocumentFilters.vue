<script setup lang="ts">
import { RefreshCw, Search,} from 'lucide-vue-next'

import type { DocumentProcessingStatus,} from '@/shared/document'

import { BaseButton,} from '@/shared/components'

export interface AdminDocumentFilterModel {
    keyword: string
    courseKeyword: string
    processingStatus: | DocumentProcessingStatus | ''
    createdAtOrder: 'ASC' | 'DESC'
}

const model = defineModel<AdminDocumentFilterModel>({required: true})

defineProps<{
    loading: boolean
}>()

defineEmits<{
    apply: []
    reset: []
}>()
</script>

<template>
    <section class="rounded-card border border-app-border bg-app-surface p-5 shadow-card">
        <form class="space-y-4" @submit.prevent="$emit('apply')">
            <div class="grid gap-3 lg:grid-cols-[minmax(250px,2fr)_minmax(220px,1.5fr)_1fr]">
                <div class="relative">
                    <Search :size="17"
                        class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-app-text-muted" />

                    <input v-model="model.keyword" type="search" placeholder="Tìm tài liệu..."
                        class="h-11 w-full rounded-control border border-app-border bg-app-surface pl-10 pr-3 text-sm outline-none focus:border-secondary" />
                </div>

                <input v-model="model.courseKeyword" type="search" placeholder="Tìm theo môn học..."
                    class="h-11 rounded-control border border-app-border bg-app-surface px-3 text-sm outline-none focus:border-secondary" />

                <select v-model="model.processingStatus
                    " class="h-11 rounded-control border border-app-border bg-app-surface px-3 text-sm">
                    <option value="">
                        Tất cả trạng thái
                    </option>

                    <option value="UPLOADED">
                        Đã tải lên
                    </option>

                    <option value="QUEUED">
                        Đang chờ xử lý
                    </option>

                    <option value="INDEXED">
                        Sẵn sàng cho AI
                    </option>

                    <option value="FAILED">
                        Xử lý thất bại
                    </option>
                </select>
            </div>

            <div class="flex flex-wrap items-end justify-between gap-3 border-t border-app-border pt-4">
                <div class="space-y-1.5">
                    <label class="text-xs font-medium text-app-text-muted">
                        Sắp xếp
                    </label>

                    <select v-model="model.createdAtOrder
                        " class="h-11 rounded-control border border-app-border bg-app-surface px-3 text-sm">
                        <option value="DESC">
                            Mới tải lên trước
                        </option>

                        <option value="ASC">
                            Cũ nhất trước
                        </option>
                    </select>
                </div>

                <div class="flex gap-2">
                    <BaseButton type="submit" :disabled="loading">
                        Lọc
                    </BaseButton>

                    <button type="button" aria-label="Đặt lại bộ lọc"
                        class="flex h-11 w-11 items-center justify-center rounded-control border border-app-border text-app-text-muted hover:bg-app-surface-muted"
                        :disabled="loading" @click="$emit('reset')">
                        <RefreshCw :size="17" />
                    </button>
                </div>
            </div>
        </form>
    </section>
</template>