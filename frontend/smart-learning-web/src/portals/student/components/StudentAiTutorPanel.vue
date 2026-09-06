<script setup lang="ts">

import CourseAiChat from '@/features/ai/components/CourseAiChat.vue';
import { useStudentApiError }
  from '../composables/useStudentApiError'

defineProps<{
  courseId: string
}>()

const { handleApiError } =
  useStudentApiError()
</script>

<template>
  <CourseAiChat
    :course-id="courseId"
    eyebrow="AI Tutor"
    title="Học cùng trợ lý AI"
    description="Hỏi về bài học, tài liệu hoặc yêu cầu AI hỗ trợ giải thích và ôn tập."
    :handle-error="handleApiError"
  />
</template>