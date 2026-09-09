import type { TopicContent } from '@/shared/course-content'

export interface DiscussionAuthor {
  id: string
  fullName: string
  email: string
}

export interface DiscussionReply {
  id: string
  authorId: string
  author: DiscussionAuthor | null
  content: TopicContent
  createdAt: string
}

export interface Discussion {
  id: string
  courseId: string
  topicId: string | null
  topicTitle: string | null
  authorId: string
  author: DiscussionAuthor | null
  title: string
  content: TopicContent
  replies: DiscussionReply[]
  createdAt: string
  updatedAt: string
}

export interface DiscussionCreateInput {
  topicId?: string | null
  title: string
  content: TopicContent
}