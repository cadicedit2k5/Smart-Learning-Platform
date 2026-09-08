<script setup lang="ts">
import {
  ArrowRight,
  BookOpen,
  Bot,
  Check,
  FileText,
  GraduationCap,
  Sparkles,
  UsersRound,
} from 'lucide-vue-next'

import authBrain from '@/assets/images/auth-brain.png'

const features = [
  {
    title: 'Khóa học',
    description:
      'Tham gia khóa học và tiếp cận nội dung học tập trong một không gian thống nhất.',
    icon: BookOpen,
    tone: 'primary',
  },
  {
    title: 'Tài liệu học tập',
    description:
      'Truy cập tài liệu của khóa học và sử dụng chúng xuyên suốt quá trình học.',
    icon: FileText,
    tone: 'secondary',
  },
  {
    title: 'AI Tutor',
    description:
      'Đặt câu hỏi và nhận giải thích dựa trên nội dung tài liệu của khóa học.',
    icon: Sparkles,
    tone: 'ai',
  },
]

const steps = [
  {
    number: '01',
    title: 'Tham gia khóa học',
    description:
      'Khám phá và tham gia khóa học phù hợp với mục tiêu học tập.',
  },
  {
    number: '02',
    title: 'Học từ tài liệu',
    description:
      'Tiếp cận tài liệu và nội dung được giảng viên cung cấp.',
  },
  {
    number: '03',
    title: 'Hỏi AI Tutor',
    description:
      'Đặt câu hỏi khi gặp nội dung khó và nhận hỗ trợ theo ngữ cảnh.',
  },
]
</script>

<template>
  <main class="min-h-screen bg-app-bg text-app-text">

    <!-- Header -->
    <header
      class="
        sticky top-0 z-50
        border-b border-app-border
        bg-app-surface
      "
    >
      <div
        class="
          mx-auto flex h-16 max-w-app
          items-center justify-between
          px-5 sm:px-8
        "
      >
        <RouterLink
          to="/"
          class="flex items-center gap-2 font-heading font-bold"
        >
          <span
            class="
              flex h-9 w-9 items-center justify-center
              rounded-control
              bg-primary text-on-primary
            "
          >
            <GraduationCap :size="18" />
          </span>

          <span>Smart Learning</span>
        </RouterLink>

        <nav
          class="
            hidden items-center gap-7
            text-sm font-medium
            md:flex
          "
        >
          <a
            href="#features"
            class="text-app-text-muted hover:text-app-text"
          >
            Tính năng
          </a>

          <a
            href="#ai"
            class="text-app-text-muted hover:text-app-text"
          >
            AI Tutor
          </a>

          <a
            href="#how"
            class="text-app-text-muted hover:text-app-text"
          >
            Cách hoạt động
          </a>
        </nav>

        <div class="flex items-center gap-2">
          <RouterLink
            to="/login"
            class="
              flex h-10 items-center justify-center
              rounded-control px-4
              text-sm font-semibold
              hover:bg-app-surface-muted
            "
          >
            Đăng nhập
          </RouterLink>

          <RouterLink
            to="/register"
            class="
              hidden h-10 items-center justify-center
              rounded-control
              bg-primary px-4
              text-sm font-semibold
              text-on-primary
              hover:bg-primary-hover
              sm:flex
            "
          >
            Bắt đầu học
          </RouterLink>
        </div>
      </div>
    </header>

    <!-- Hero -->
    <section
      class="
        border-b border-app-border
        bg-app-surface
      "
    >
      <div
        class="
          mx-auto grid max-w-app
          items-center gap-12
          px-5 py-16
          sm:px-8 sm:py-20
          lg:grid-cols-2 lg:py-24
        "
      >
        <div class="max-w-2xl">
          <div
            class="
              inline-flex items-center gap-2
              rounded-pill
              bg-ai-soft
              px-3 py-1.5
              text-sm font-semibold
              text-ai
            "
          >
            <Sparkles :size="16" />
            Học tập cùng AI
          </div>

          <h1
            class="
              mt-6 font-heading
              text-4xl font-bold
              leading-tight tracking-tight
              sm:text-5xl
              lg:text-6xl
            "
          >
            Học thông minh hơn với

            <span class="text-secondary">
              trợ lý AI
            </span>

            của bạn
          </h1>

          <p
            class="
              mt-6 max-w-xl
              text-base leading-7
              text-app-text-muted
              sm:text-lg
            "
          >
            Khám phá khóa học, học từ tài liệu
            và nhận hỗ trợ từ AI Tutor dựa trên
            chính nội dung bạn đang học.
          </p>

          <div
            class="
              mt-8 flex flex-col gap-3
              sm:flex-row
            "
          >
            <RouterLink
              to="/register"
              class="
                inline-flex h-11 items-center
                justify-center gap-2
                rounded-control
                bg-primary px-5
                text-sm font-semibold
                text-on-primary
                hover:bg-primary-hover
              "
            >
              Bắt đầu học
              <ArrowRight :size="17" />
            </RouterLink>

            <RouterLink
              to="/login"
              class="
                inline-flex h-11 items-center
                justify-center
                rounded-control
                border border-app-border
                bg-app-surface px-5
                text-sm font-semibold
                hover:bg-app-surface-muted
              "
            >
              Đăng nhập
            </RouterLink>
          </div>

          <div
            class="
              mt-8 flex flex-wrap gap-x-6 gap-y-3
              text-sm text-app-text-muted
            "
          >
            <span class="flex items-center gap-2">
              <Check
                :size="16"
                class="text-ai"
              />
              Học theo khóa học
            </span>

            <span class="flex items-center gap-2">
              <Check
                :size="16"
                class="text-ai"
              />
              Tài liệu tập trung
            </span>

            <span class="flex items-center gap-2">
              <Check
                :size="16"
                class="text-ai"
              />
              AI theo ngữ cảnh
            </span>
          </div>
        </div>

        <!-- Visual -->
        <div class="flex justify-center">
          <div
            class="
              w-full max-w-xl
              rounded-panel
              border border-app-border
              bg-app-bg
              p-6 shadow-card
              sm:p-8
            "
          >
            <div class="flex items-center gap-3">
              <div
                class="
                  flex h-10 w-10
                  items-center justify-center
                  rounded-card
                  bg-ai text-on-ai
                "
              >
                <Bot :size="19" />
              </div>

              <div>
                <p class="text-sm font-semibold">
                  AI Tutor
                </p>

                <p class="text-xs text-app-text-muted">
                  Trợ lý học tập của bạn
                </p>
              </div>
            </div>

            <div
              class="
                mt-6 flex min-h-64
                items-center justify-center
                rounded-panel
                bg-app-surface
                p-5
              "
            >
              <img
                :src="authBrain"
                alt=""
                class="
                  max-h-64 w-full
                  object-contain
                "
              />
            </div>

            <div
              class="
                mt-5 rounded-card
                border border-app-border
                bg-app-surface
                p-4
              "
            >
              <p class="text-xs text-app-text-muted">
                Gợi ý câu hỏi
              </p>

              <p class="mt-1 text-sm font-semibold">
                Dependency Injection trong Spring hoạt động như thế nào?
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Features -->
    <section
      id="features"
      class="
        mx-auto max-w-app
        px-5 py-16
        sm:px-8
        lg:py-20
      "
    >
      <div
        class="
          mx-auto max-w-2xl
          text-center
        "
      >
        <p class="text-sm font-semibold text-secondary">
          Nền tảng học tập
        </p>

        <h2
          class="
            mt-3 mb-5 font-heading
            text-3xl font-bold
          "
        >
          Mọi thứ bạn cần để học tốt hơn
        </h2>
      </div>

      <div
        class="
          mt-10 grid gap-5
          md:grid-cols-3
        "
      >
        <article
          v-for="feature in features"
          :key="feature.title"
          class="
            rounded-panel
            border border-app-border
            bg-app-surface
            p-6 shadow-card
          "
        >
          <div
            v-if="feature.tone === 'primary'"
            class="
              flex h-11 w-11
              items-center justify-center
              rounded-card
              bg-primary text-on-primary
            "
          >
            <component
              :is="feature.icon"
              :size="20"
            />
          </div>

          <div
            v-else-if="feature.tone === 'secondary'"
            class="
              flex h-11 w-11
              items-center justify-center
              rounded-card
              bg-secondary-soft text-secondary
            "
          >
            <component
              :is="feature.icon"
              :size="20"
            />
          </div>

          <div
            v-else
            class="
              flex h-11 w-11
              items-center justify-center
              rounded-card
              bg-ai-soft text-ai
            "
          >
            <component
              :is="feature.icon"
              :size="20"
            />
          </div>

          <h3
            class="
              mt-5 font-heading
              text-lg font-bold
            "
          >
            {{ feature.title }}
          </h3>

          <p
            class="
              mt-2 text-sm leading-6
              text-app-text-muted
            "
          >
            {{ feature.description }}
          </p>
        </article>
      </div>
    </section>

    <!-- AI -->
   <section
        id="ai"
        class="border-y border-app-border bg-app-surface"
        >
        <div
            class="
            mx-auto grid max-w-app
            gap-12 px-5 py-16
            sm:px-8
            lg:grid-cols-2
            lg:items-center
            lg:gap-16
            lg:py-20
            "
        >
            <!-- Chat preview -->
            <div class="min-w-0">
            <div
                class="
                mx-auto w-full max-w-xl
                rounded-panel
                border border-app-border
                bg-app-bg
                p-5 shadow-card
                sm:p-6
                lg:mx-0
                "
            >
                <!-- Header -->
                <div
                class="
                    flex items-center gap-3
                    border-b border-app-border
                    pb-4
                "
                >
                <div
                    class="
                    flex h-10 w-10 shrink-0
                    items-center justify-center
                    rounded-card
                    bg-ai text-on-ai
                    "
                >
                    <Bot :size="19" />
                </div>

                <div class="min-w-0">
                    <p class="text-sm font-semibold">
                    AI Tutor
                    </p>

                    <p class="text-xs text-app-text-muted">
                    Hỗ trợ dựa trên tài liệu khóa học
                    </p>
                </div>
                </div>

                <!-- Messages -->
                <div class="mt-6 space-y-4">
                <!-- User -->
                <div class="flex justify-end">
                    <div
                    class="
                        max-w-sm
                        rounded-card
                        bg-primary
                        px-4 py-3
                        text-sm leading-6
                        text-on-primary
                    "
                    >
                    IoC trong Spring là gì?
                    </div>
                </div>

                <!-- AI -->
                <div class="flex justify-start">
                    <div
                    class="
                        max-w-md
                        rounded-card
                        border border-app-border
                        bg-app-surface
                        px-4 py-3
                        text-sm leading-6
                        text-app-text
                    "
                    >
                    <p>
                        IoC là nguyên tắc chuyển quyền kiểm soát
                        việc tạo và quản lý dependency cho
                        Spring Container.
                    </p>

                    <div
                        class="
                        mt-3 flex items-center gap-2
                        rounded-control
                        bg-ai-soft
                        px-3 py-2
                        text-xs font-medium
                        text-ai
                        "
                    >
                        <FileText :size="14" />

                        Dựa trên tài liệu khóa học
                    </div>
                    </div>
                </div>
                </div>
            </div>
            </div>

            <!-- Content -->
            <div class="min-w-0">
            <div class="max-w-xl">
                <div
                class="
                    inline-flex items-center gap-2
                    rounded-pill
                    bg-ai-soft
                    px-3 py-1.5
                    text-sm font-semibold
                    text-ai
                "
                >
                <Sparkles :size="16" />

                AI Tutor
                </div>

                <h2
                class="
                    mt-5
                    font-heading
                    text-3xl font-bold
                    leading-tight
                    text-app-text
                "
                >
                AI hỗ trợ dựa trên chính nội dung
                bạn đang học
                </h2>

                <p
                class="
                    mt-4
                    text-base leading-7
                    text-app-text-muted
                "
                >
                AI Tutor sử dụng tài liệu trong khóa học
                để hỗ trợ giải thích kiến thức, làm rõ
                nội dung khó và kết nối các khái niệm
                liên quan.
                </p>

                <div class="mt-7 space-y-4">
                <div class="flex items-start gap-3">
                    <div
                    class="
                        flex h-6 w-6 shrink-0
                        items-center justify-center
                        rounded-pill
                        bg-ai-soft text-ai
                    "
                    >
                    <Check :size="14" />
                    </div>

                    <p class="text-sm leading-6">
                    Giải thích khái niệm và nội dung khó
                    </p>
                </div>

                <div class="flex items-start gap-3">
                    <div
                    class="
                        flex h-6 w-6 shrink-0
                        items-center justify-center
                        rounded-pill
                        bg-ai-soft text-ai
                    "
                    >
                    <Check :size="14" />
                    </div>

                    <p class="text-sm leading-6">
                    Trả lời theo ngữ cảnh tài liệu
                    </p>
                </div>

                <div class="flex items-start gap-3">
                    <div
                    class="
                        flex h-6 w-6 shrink-0
                        items-center justify-center
                        rounded-pill
                        bg-ai-soft text-ai
                    "
                    >
                    <Check :size="14" />
                    </div>

                    <p class="text-sm leading-6">
                    Hỗ trợ quá trình tự học
                    </p>
                </div>
                </div>
            </div>
            </div>
        </div>
        </section>      

    <!-- Steps -->
    <section
      id="how"
      class="
        mx-auto max-w-app
        px-5 py-16
        sm:px-8
        lg:py-20
      "
    >
      <div class="text-center">
        <h2
          class="
            font-heading text-3xl
            font-bold
          "
        >
          Bắt đầu chỉ với 3 bước
        </h2>

        <p class="mt-4 text-app-text-muted">
          Một quy trình học tập đơn giản và liền mạch.
        </p>
      </div>

      <div
        class="
          mt-10 grid gap-5
          md:grid-cols-3
        "
      >
        <article
          v-for="step in steps"
          :key="step.number"
          class="
            rounded-panel
            border border-app-border
            bg-app-surface
            p-6 shadow-card
          "
        >
          <div
            class="
              flex h-9 w-9
              items-center justify-center
              rounded-control
              bg-secondary-soft
              text-sm font-bold
              text-secondary
            "
          >
            {{ step.number }}
          </div>

          <h3
            class="
              mt-5 font-heading
              text-lg font-bold
            "
          >
            {{ step.title }}
          </h3>

          <p
            class="
              mt-2 text-sm leading-6
              text-app-text-muted
            "
          >
            {{ step.description }}
          </p>
        </article>
      </div>
    </section>

    <!-- Roles -->
    <section
      class="
        border-y border-app-border
        bg-app-surface
      "
    >
      <div
        class="
          mx-auto max-w-app
          px-5 py-16
          sm:px-8
          lg:py-20
        "
      >
        <div class="text-center mb-4">
          <h2
            class="
              font-heading text-3xl
              font-bold
            "
          >
            Dành cho người học và giảng viên
          </h2>
        </div>

        <div
          class="
            mx-auto mt-10 grid
            max-w-5xl gap-5
            md:grid-cols-2
          "
        >
          <article
            class="
              rounded-panel
              border border-app-border
              bg-app-bg p-6
            "
          >
            <div
              class="
                flex h-11 w-11
                items-center justify-center
                rounded-card
                bg-secondary-soft
                text-secondary
              "
            >
              <BookOpen :size="20" />
            </div>

            <h3
              class="
                mt-5 font-heading
                text-xl font-bold
              "
            >
              Học viên
            </h3>

            <ul
              class="
                mt-5 space-y-3
                text-sm text-app-text-muted
              "
            >
              <li class="flex gap-2">
                <Check :size="16" class="text-secondary" />
                Khám phá và tham gia khóa học
              </li>

              <li class="flex gap-2">
                <Check :size="16" class="text-secondary" />
                Truy cập tài liệu học tập
              </li>

              <li class="flex gap-2">
                <Check :size="16" class="text-secondary" />
                Học cùng AI Tutor
              </li>
            </ul>
          </article>

          <article
            class="
              rounded-panel
              border border-app-border
              bg-app-bg p-6
            "
          >
            <div
              class="
                flex h-11 w-11
                items-center justify-center
                rounded-card
                bg-ai-soft text-ai
              "
            >
              <UsersRound :size="20" />
            </div>

            <h3
              class="
                mt-5 font-heading
                text-xl font-bold
              "
            >
              Giảng viên
            </h3>

            <ul
              class="
                mt-5 space-y-3
                text-sm text-app-text-muted
              "
            >
              <li class="flex gap-2">
                <Check :size="16" class="text-ai" />
                Tạo và quản lý khóa học
              </li>

              <li class="flex gap-2">
                <Check :size="16" class="text-ai" />
                Quản lý tài liệu
              </li>

              <li class="flex gap-2">
                <Check :size="16" class="text-ai" />
                Quản lý thành viên
              </li>
            </ul>
          </article>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section
      class="
        mx-auto max-w-app
        px-5 py-16
        sm:px-8
        lg:py-20
      "
    >
      <div
        class="
          rounded-panel
          bg-primary
          px-6 py-12
          text-center
          text-on-primary
          sm:px-10
        "
      >
        <h2
          class="
            font-heading text-3xl
            font-bold
          "
        >
          Sẵn sàng bắt đầu học tập?
        </h2>

        <p
          class="
            mx-auto mt-4 max-w-xl
            text-sm leading-6
          "
        >
          Tạo tài khoản và bắt đầu trải nghiệm
          Smart Learning ngay hôm nay.
        </p>

        <RouterLink
          to="/register"
          class="
            mt-7 inline-flex h-11
            items-center justify-center gap-2
            rounded-control
            bg-app-surface px-5
            text-sm font-semibold
            text-primary
            hover:bg-app-surface-muted
          "
        >
          Bắt đầu ngay
          <ArrowRight :size="17" />
        </RouterLink>
      </div>
    </section>

    <!-- Footer -->
    <footer
      class="
        border-t border-app-border
        bg-app-surface
        p-5
      "
    >
      <div
        class="
          mx-auto flex max-w-app
          flex-col gap-3
          px-5 py-7
          text-sm text-app-text-muted
          sm:flex-row
          sm:items-center
          sm:justify-between
          sm:px-8
        "
      >
        <div
          class="
            flex items-center gap-2
            font-semibold text-app-text
          "
        >
          <GraduationCap :size="18" />
          Smart Learning
        </div>

        <p>© 2026 Smart Learning</p>
      </div>
    </footer>
  </main>
</template>