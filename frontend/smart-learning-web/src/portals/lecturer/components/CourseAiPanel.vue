<script setup lang="ts">

import CourseAiChat from '@/features/ai/components/CourseAiChat.vue';
import { useLecturerApiError }
  from '../composables/useLecturerApiError'

defineProps<{
  courseId: string
}>()

const { handleApiError } =
  useLecturerApiError()
</script>

<template>
  <CourseAiChat
    :course-id="courseId"
    eyebrow="Trợ lý theo ngữ cảnh khóa học"
    title="Hỏi đáp với AI"
    description="Câu trả lời ưu tiên nội dung và tài liệu đã được lập chỉ mục trong khóa học."
    :handle-error="handleApiError"
  />
</template>