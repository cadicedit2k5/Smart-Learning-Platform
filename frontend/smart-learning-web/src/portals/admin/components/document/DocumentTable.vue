<script setup lang="ts">
import {  Eye,   FileText, Trash2,} from 'lucide-vue-next'

import type { CourseDocument } from '@/shared/document'

import {documentStatusLabel,  documentStatusTone, formatFileSize,} from '@/shared/document'

import {  BaseBadge, BaseEmptyState,} from '@/shared/components'

import { formatDateTime } from '@/shared/utils/date'

defineProps<{
    documents: CourseDocument[]
    loading: boolean
    total: number
    previewingId?: string
}>()

defineEmits<{
    view: [document: CourseDocument]
    delete: [document: CourseDocument]
}>()

const canDelete = (document: CourseDocument,) => {
    return [ 'INDEXED', 'FAILED'].includes(document.version.processingStatus)
}
</script>

<template>
    <section class="overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card">
        <header class="border-b border-app-border px-5 py-4">
            <h2 class="font-heading font-bold text-app-text">
                Danh sách tài liệu
            </h2>

            <p class="mt-1 text-xs text-app-text-muted">
                {{ total }} tài liệu
            </p>
        </header>

        <div v-if="loading" class="space-y-3 p-5">
            <div v-for="index in 5" :key="index" class="h-20 animate-pulse rounded-control bg-app-surface-muted" />
        </div>

        <div v-else-if="
            documents.length === 0
        " class="p-5">
            <BaseEmptyState title="Không tìm thấy tài liệu" description="Thử thay đổi bộ lọc hoặc từ khóa tìm kiếm." />
        </div>

        <div v-else class="overflow-x-auto">
            <table class="w-full min-w-[1000px] text-left text-sm">
                <thead class="bg-app-surface-muted text-xs uppercase tracking-wide text-app-text-muted">
                    <tr>
                        <th class="px-5 py-3">
                            Tài liệu
                        </th>

                        <th class="px-5 py-3">
                            Môn học
                        </th>

                        <th class="px-5 py-3">
                            Trạng thái
                        </th>

                        <th class="px-5 py-3">
                            Kích thước
                        </th>

                        <th class="px-5 py-3">
                            Ngày tải
                        </th>

                        <th class="px-5 py-3 text-right">
                            Thao tác
                        </th>
                    </tr>
                </thead>

                <tbody class="divide-y divide-app-border">
                    <tr v-for=" document in documents" :key="document.id" class="hover:bg-app-surface-muted/50">
                        <td class="px-5 py-4">
                            <div class="flex items-center gap-3">
                                <span
                                    class="flex h-10 w-10 shrink-0 items-center justify-center rounded-control bg-app-surface-muted text-app-text-muted">
                                    <FileText :size="19" />
                                </span>

                                <div class="min-w-0">
                                    <p class="max-w-[280px] truncate font-semibold text-app-text">
                                        {{ document.title }}
                                    </p>

                                    <p class="mt-1 max-w-[280px] truncate text-xs text-app-text-muted">
                                        {{
                                            document.version
                                                .fileName
                                        }}
                                    </p>
                                </div>
                            </div>
                        </td>

                        <td class="px-5 py-4">
                            <p class="max-w-[220px] truncate font-medium text-app-text">
                                {{
                                    document.courseTitle
                                }}
                            </p>
                        </td>

                        <td class="px-5 py-4">
                            <BaseBadge :tone="documentStatusTone[
                                document.version
                                    .processingStatus
                                ]
                                ">
                                {{
                                    documentStatusLabel[
                                    document.version
                                        .processingStatus
                                ]
                                }}
                            </BaseBadge>
                        </td>

                        <td class="px-5 py-4 text-app-text-muted">
                            {{
                                formatFileSize(
                                    document.version
                                        .fileSize,
                            )
                            }}
                        </td>

                        <td class="whitespace-nowrap px-5 py-4 text-app-text-muted">
                            {{
                                formatDateTime(
                                    document.createdAt,
                            )
                            }}
                        </td>

                        <td class="px-5 py-4">
                            <div class="flex justify-end gap-1">
                                <button
                                type="button"
                                class="flex h-9 w-9 items-center justify-center rounded-control text-app-text-muted hover:bg-secondary-soft hover:text-secondary"
                                :disabled="previewingId === document.id"
                                :aria-label="`Xem tài liệu ${document.title}`"
                                @click="$emit('view',document)">
                                <Eye :size="17" />
                                </button>

                                <button type="button"
                                    class="flex h-9 w-9 items-center justify-center rounded-control text-danger hover:bg-danger-soft disabled:cursor-not-allowed disabled:opacity-40"
                                    :disabled="!canDelete(
                                        document,
                                    )
                                        " :title="canDelete(document)
                        ? 'Xóa tài liệu'
                        : 'Tài liệu đang được xử lý'
                    " @click="
                    $emit(
                        'delete',
                        document,
                    )
                    ">
                                    <Trash2 :size="17" />
                                </button>
                            </div>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </section>
</template>