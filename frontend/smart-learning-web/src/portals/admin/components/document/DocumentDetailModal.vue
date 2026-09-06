<script setup lang="ts">
import type { CourseDocument,} from '@/shared/document'

import { documentStatusLabel, documentStatusTone, formatFileSize } from '@/shared/document'

import { BaseAlert, BaseBadge, BaseModal } from '@/shared/components'

import { formatDateTime } from '@/shared/utils/date'

defineProps<{
  open: boolean
  document: CourseDocument | null
  loading: boolean
  error: string
}>()

defineEmits<{
  close: []
}>()
</script>

<template>
  <BaseModal
    :open="open"
    title="Chi tiết tài liệu"
    :loading="loading"
    max-width="max-w-2xl"
    @close="$emit('close')"
  >
    <BaseAlert
      v-if="error"
      variant="error"
    >
      {{ error }}
    </BaseAlert>

    <div
      v-else-if="loading"
      class="space-y-3"
    >
      <div
        v-for="index in 5"
        :key="index"
        class="h-12 animate-pulse rounded-control bg-app-surface-muted"
      />
    </div>

    <div
      v-else-if="document"
      class="space-y-5"
    >
      <div>
        <p
          class="text-xs font-medium uppercase tracking-wide text-app-text-muted"
        >
          Tài liệu
        </p>

        <h3
          class="mt-1 text-lg font-bold text-app-text"
        >
          {{ document.title }}
        </h3>

        <p
          v-if="document.description"
          class="mt-2 text-sm leading-6 text-app-text-muted"
        >
          {{ document.description }}
        </p>
      </div>

      <div
        class="grid gap-4 sm:grid-cols-2"
      >
        <div>
          <p class="text-xs text-app-text-muted">
            Môn học
          </p>

          <p class="mt-1 font-medium text-app-text">
            {{ document.courseTitle }}
          </p>
        </div>

        <div>
          <p class="text-xs text-app-text-muted">
            Trạng thái
          </p>

          <BaseBadge
            class="mt-1"
            :tone="
              documentStatusTone[
                document.version
                  .processingStatus
              ]
            "
          >
            {{
              documentStatusLabel[
                document.version
                  .processingStatus
              ]
            }}
          </BaseBadge>
        </div>

        <div>
          <p class="text-xs text-app-text-muted">
            Tệp
          </p>

          <p class="mt-1 font-medium text-app-text">
            {{ document.version.fileName }}
          </p>
        </div>

        <div>
          <p class="text-xs text-app-text-muted">
            Kích thước
          </p>

          <p class="mt-1 font-medium text-app-text">
            {{
              formatFileSize(
                document.version.fileSize,
              )
            }}
          </p>
        </div>

        <div>
          <p class="text-xs text-app-text-muted">
            MIME type
          </p>

          <p class="mt-1 font-medium text-app-text">
            {{ document.version.mimeType }}
          </p>
        </div>

        <div>
          <p class="text-xs text-app-text-muted">
            Phiên bản
          </p>

          <p class="mt-1 font-medium text-app-text">
            v{{
              document.version.versionNumber
            }}
          </p>
        </div>

        <div>
          <p class="text-xs text-app-text-muted">
            Người tải lên
          </p>

          <p
            class="mt-1 break-all font-mono text-xs text-app-text"
          >
            {{ document.uploadedBy }}
          </p>
        </div>

        <div>
          <p class="text-xs text-app-text-muted">
            Ngày tải
          </p>

          <p class="mt-1 font-medium text-app-text">
            {{
              formatDateTime(
                document.createdAt,
              )
            }}
          </p>
        </div>
      </div>
    </div>
  </BaseModal>
</template>