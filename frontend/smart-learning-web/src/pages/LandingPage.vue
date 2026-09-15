<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, BookOpen, Bot, Check, ChevronDown, ClipboardList, FileText, GraduationCap, LogOut, UsersRound } from 'lucide-vue-next'

import { useAuthStore } from '@/features/auth/stores'
import { portalRegistry } from '@/portals/registry'

const router = useRouter()
const authStore = useAuthStore()

const capabilities = [
  { title: 'Học theo cấu trúc khóa học', description: 'Chương, bài học và tiến độ được tổ chức trong cùng một luồng để người học luôn biết mình đang ở đâu.', icon: BookOpen },
  { title: 'Nội dung học tập không bị phân mảnh', description: 'Tài liệu, bài tập, thông báo và thảo luận nằm ngay trong khóa học thay vì bị chia ra nhiều công cụ khác nhau.', icon: ClipboardList },
  { title: 'AI Tutor hiểu ngữ cảnh khóa học', description: 'Người học có thể đặt câu hỏi dựa trên tài liệu của khóa học khi cần làm rõ khái niệm hoặc nội dung khó.', icon: Bot },
]

const steps = [
  { number: '01', title: 'Tham gia khóa học', description: 'Khám phá khóa học phù hợp hoặc tham gia khóa học được giảng viên cung cấp.' },
  { number: '02', title: 'Học trong một workspace', description: 'Theo dõi bài học, tài liệu, bài tập, thông báo và tiến độ tại cùng một nơi.' },
  { number: '03', title: 'Đào sâu khi cần', description: 'Trao đổi, đặt câu hỏi và sử dụng AI Tutor khi cần thêm hỗ trợ trong quá trình học.' },
]

const headerSections = [
  { id: 'platform', label: 'Nền tảng' },
  { id: 'ai', label: 'AI Tutor' },
  { id: 'roles', label: 'Dành cho ai' },
  { id: 'how', label: 'Cách hoạt động' },
]

const scrolled = ref(false)
const activeSection = ref('')
const userMenuOpen = ref(false)
const userMenuRef = ref<HTMLElement | null>(null)
const avatarFailed = ref(false)
const observers = new Map<HTMLElement, IntersectionObserver>()
let sectionObserver: IntersectionObserver | null = null

const userNavigation = computed(() => {
  const user = authStore.user
  return user ? portalRegistry[user.role.code].navigation : []
})

const initials = computed(() => {
  const name = authStore.user?.fullName?.trim()
  if (!name) return 'U'
  return name.split(/\s+/).slice(-2).map((part) => part[0]).join('').toUpperCase()
})

const displayedAvatar = computed(() => !avatarFailed.value ? authStore.user?.avatar : null)

const vReveal = {
  mounted(el: HTMLElement) {
    if (typeof IntersectionObserver === 'undefined') {
      el.classList.add('is-visible')
      return
    }

    const observer = new IntersectionObserver(([entry]) => {
      if (!entry?.isIntersecting) return
      el.classList.add('is-visible')
      observer.unobserve(el)
    }, { threshold: 0.12, rootMargin: '0px 0px -60px 0px' })

    observers.set(el, observer)
    observer.observe(el)
  },
  unmounted(el: HTMLElement) {
    observers.get(el)?.disconnect()
    observers.delete(el)
  },
}

const onScroll = () => {
  scrolled.value = window.scrollY > 12
}

const scrollToSection = (id: string) => {
  const element = document.getElementById(id)
  if (!element) return

  activeSection.value = id
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  const top = element.getBoundingClientRect().top + window.scrollY - 76
  window.scrollTo({ top, behavior: reducedMotion ? 'auto' : 'smooth' })
}

const onDocumentClick = (event: MouseEvent) => {
  if (userMenuRef.value && !userMenuRef.value.contains(event.target as Node)) userMenuOpen.value = false
}

const onKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') userMenuOpen.value = false
}

const closeUserMenu = () => {
  userMenuOpen.value = false
}

const handleLogout = async () => {
  authStore.logout()
  userMenuOpen.value = false
  await router.replace({ name: 'home' })
}

onMounted(() => {
  onScroll()
  window.addEventListener('scroll', onScroll, { passive: true })
  document.addEventListener('click', onDocumentClick)
  document.addEventListener('keydown', onKeydown)

  if (typeof IntersectionObserver !== 'undefined') {
    sectionObserver = new IntersectionObserver((entries) => {
      const visible = entries.filter((entry) => entry.isIntersecting).sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0]
      if (visible) activeSection.value = visible.target.id
    }, { rootMargin: '-18% 0px -68% 0px', threshold: [0, 0.15, 0.35, 0.6] })

    headerSections.forEach(({ id }) => {
      const element = document.getElementById(id)
      if (element) sectionObserver?.observe(element)
    })
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  document.removeEventListener('click', onDocumentClick)
  document.removeEventListener('keydown', onKeydown)
  observers.forEach((observer) => observer.disconnect())
  observers.clear()
  sectionObserver?.disconnect()
})
</script>

<template>
  <main class="min-h-screen bg-app-surface text-app-text">
    <header
      class="sticky top-0 z-50 border-b border-app-border bg-app-surface/95 backdrop-blur-sm transition-shadow duration-300"
      :class="{ 'shadow-card': scrolled }"
    >
      <div class="mx-auto flex h-16 max-w-app items-center justify-between px-5 sm:px-8">
        <RouterLink to="/" class="flex items-center gap-2.5 font-heading font-bold">
          <span class="flex h-9 w-9 items-center justify-center rounded-control bg-primary text-on-primary">
            <GraduationCap :size="18" />
          </span>
          <span>Smart Learning</span>
        </RouterLink>

        <nav class="hidden h-full items-center gap-7 text-sm font-medium md:flex" aria-label="Điều hướng trang chủ">
          <button
            v-for="section in headerSections"
            :key="section.id"
            type="button"
            class="group relative flex h-full items-center text-app-text-muted transition-colors hover:text-app-text"
            :class="{ 'text-app-text': activeSection === section.id }"
            @click="scrollToSection(section.id)"
          >
            {{ section.label }}
            <span
              class="absolute bottom-0 left-0 h-0.5 w-full origin-left rounded-full bg-secondary transition-transform duration-300 ease-out"
              :class="activeSection === section.id ? 'scale-x-100' : 'scale-x-0'"
            />
          </button>
        </nav>

        <!-- Guest -->
        <div v-if="!authStore.isAuthenticated" class="flex items-center gap-2">
          <RouterLink to="/login" class="inline-flex h-10 items-center justify-center rounded-control px-4 text-sm font-semibold transition-colors hover:bg-app-surface-muted">
            Đăng nhập
          </RouterLink>

          <RouterLink to="/register" class="hidden h-10 items-center justify-center rounded-control bg-primary px-4 text-sm font-semibold text-on-primary transition-colors hover:bg-primary-hover sm:inline-flex">
            Tạo tài khoản
          </RouterLink>
        </div>

        <!-- Logged in user -->
        <div v-else ref="userMenuRef" class="relative">
          <button
            type="button"
            class="group flex h-11 items-center gap-2 rounded-control px-2 transition-colors hover:bg-app-surface-muted sm:gap-3 sm:px-3"
            :aria-expanded="userMenuOpen"
            aria-haspopup="menu"
            @click.stop="userMenuOpen = !userMenuOpen"
          >
            <div class="flex h-9 w-9 shrink-0 items-center justify-center overflow-hidden rounded-full bg-secondary-soft text-xs font-bold text-secondary ring-1 ring-app-border">
              <img
                v-if="displayedAvatar"
                :src="displayedAvatar"
                :alt="`Ảnh đại diện của ${authStore.user?.fullName}`"
                class="h-full w-full object-cover"
                @error="avatarFailed = true"
              />
              <span v-else>{{ initials }}</span>
            </div>

            <div class="hidden max-w-40 text-left lg:block">
              <p class="truncate text-sm font-semibold leading-5 text-app-text">{{ authStore.user?.fullName }}</p>
              <p class="truncate text-xs leading-4 text-app-text-muted">{{ authStore.user?.role.name }}</p>
            </div>

            <ChevronDown
              :size="16"
              class="hidden text-app-text-muted transition-transform duration-200 sm:block"
              :class="{ 'rotate-180': userMenuOpen }"
            />
          </button>

          <Transition name="user-menu">
            <div
              v-if="userMenuOpen"
              role="menu"
              class="absolute right-0 top-[calc(100%+0.6rem)] w-[19rem] overflow-hidden rounded-card border border-app-border bg-app-surface shadow-overlay"
            >
              <div class="border-b border-app-border px-4 py-4">
                <div class="flex items-center gap-3">
                  <div class="flex h-10 w-10 shrink-0 items-center justify-center overflow-hidden rounded-full bg-secondary-soft text-xs font-bold text-secondary">
                    <img
                      v-if="displayedAvatar"
                      :src="displayedAvatar"
                      alt=""
                      class="h-full w-full object-cover"
                      @error="avatarFailed = true"
                    />
                    <span v-else>{{ initials }}</span>
                  </div>

                  <div class="min-w-0">
                    <p class="truncate text-sm font-semibold text-app-text">{{ authStore.user?.fullName }}</p>
                    <p class="mt-0.5 truncate text-xs text-app-text-muted">{{ authStore.user?.email }}</p>
                  </div>
                </div>
              </div>

              <div class="px-2 py-2">
                <p class="px-2 py-1.5 text-[11px] font-semibold uppercase tracking-[0.1em] text-app-text-muted">
                  Không gian làm việc
                </p>

                <RouterLink
                  v-for="item in userNavigation"
                  :key="item.routeName"
                  :to="{ name: item.routeName }"
                  role="menuitem"
                  class="flex items-center gap-3 rounded-control px-3 py-2.5 text-sm font-medium text-app-text transition-colors hover:bg-app-surface-muted"
                  @click="closeUserMenu"
                >
                  <component :is="item.icon" v-if="item.icon" :size="17" class="shrink-0 text-app-text-muted" />
                  <span>{{ item.label }}</span>
                </RouterLink>
              </div>

              <div class="border-t border-app-border p-2">
                <button
                  type="button"
                  role="menuitem"
                  class="flex w-full items-center gap-3 rounded-control px-3 py-2.5 text-left text-sm font-medium text-danger transition-colors hover:bg-danger-soft"
                  @click="handleLogout"
                >
                  <LogOut :size="17" />
                  Đăng xuất
                </button>
              </div>
            </div>
          </Transition>
        </div>
      </div>
    </header>

    <!-- HERO -->
    <section class="overflow-hidden border-b border-app-border">
      <div class="mx-auto grid max-w-app gap-12 px-5 py-16 sm:px-8 sm:py-20 lg:grid-cols-[minmax(0,0.9fr)_minmax(32rem,1.1fr)] lg:items-center lg:gap-16 lg:py-24">
        <div class="hero-copy max-w-2xl">
          <p class="text-sm font-semibold uppercase tracking-[0.12em] text-secondary">Learning Management System</p>

          <h1 class="mt-4 font-heading text-4xl font-bold leading-[1.08] tracking-tight text-app-text sm:text-5xl lg:text-[3.5rem]">
            Một nơi để học, giảng dạy và theo dõi toàn bộ khóa học.
          </h1>

          <p class="mt-6 max-w-xl text-base leading-7 text-app-text-muted sm:text-lg">
            Smart Learning kết nối nội dung khóa học, tài liệu, bài tập, trao đổi và AI Tutor trong một trải nghiệm học tập thống nhất.
          </p>

          <div class="hero-actions mt-8 flex flex-col gap-3 sm:flex-row">
            <RouterLink to="/register" class="group inline-flex h-11 items-center justify-center gap-2 rounded-control bg-primary px-5 text-sm font-semibold text-on-primary transition-colors hover:bg-primary-hover">
              Bắt đầu với Smart Learning
              <ArrowRight :size="17" class="transition-transform duration-200 group-hover:translate-x-1" />
            </RouterLink>

            <RouterLink to="/login" class="inline-flex h-11 items-center justify-center rounded-control border border-app-border px-5 text-sm font-semibold transition-colors hover:bg-app-surface-muted">
              Tôi đã có tài khoản
            </RouterLink>
          </div>

          <div class="hero-points mt-9 flex flex-wrap gap-x-6 gap-y-3 text-sm text-app-text-muted">
            <span class="flex items-center gap-2"><Check :size="15" class="text-ai" />Khóa học có cấu trúc</span>
            <span class="flex items-center gap-2"><Check :size="15" class="text-ai" />Theo dõi tiến độ</span>
            <span class="flex items-center gap-2"><Check :size="15" class="text-ai" />AI theo ngữ cảnh</span>
          </div>
        </div>

        <div class="hero-product overflow-hidden rounded-panel border border-app-border bg-app-surface shadow-card" aria-label="Mô phỏng không gian học tập">
          <div class="flex items-center justify-between border-b border-app-border px-5 py-4">
            <div>
              <p class="text-xs font-medium text-app-text-muted">Khóa học</p>
              <p class="mt-0.5 text-sm font-semibold text-app-text">Spring Boot Fundamentals</p>
            </div>

            <div class="text-right">
              <p class="text-xs font-medium text-app-text-muted">Tiến độ</p>
              <p class="mt-0.5 text-sm font-bold text-secondary">62%</p>
            </div>
          </div>

          <div class="grid sm:grid-cols-[11rem_minmax(0,1fr)]">
            <aside class="hidden border-r border-app-border bg-app-bg p-4 sm:block">
              <p class="mb-3 text-xs font-semibold uppercase tracking-wide text-app-text-muted">Nội dung</p>

              <div class="space-y-1 text-sm">
                <div class="preview-row rounded-control px-3 py-2 text-app-text-muted">01 · Giới thiệu</div>
                <div class="preview-row active rounded-control bg-secondary-soft px-3 py-2 font-semibold text-secondary">02 · Spring IoC</div>
                <div class="preview-row rounded-control px-3 py-2 text-app-text-muted">03 · REST API</div>
                <div class="preview-row rounded-control px-3 py-2 text-app-text-muted">04 · Database</div>
              </div>
            </aside>

            <div class="min-w-0 p-5 sm:p-6">
              <div class="flex items-center gap-2 text-xs font-medium text-app-text-muted">
                <span>Chương 2</span><span>·</span><span>12 phút</span>
              </div>

              <h2 class="mt-2 font-heading text-xl font-bold text-app-text">Inversion of Control trong Spring</h2>

              <p class="mt-3 max-w-xl text-sm leading-6 text-app-text-muted">
                Tìm hiểu cách Spring Container quản lý việc khởi tạo và cung cấp dependency cho các thành phần trong ứng dụng.
              </p>

              <div class="mt-6">
                <div class="flex items-center justify-between text-xs">
                  <span class="font-medium text-app-text-muted">Tiến độ chương</span>
                  <span class="font-semibold text-app-text">3 / 5 bài</span>
                </div>

                <div class="mt-2 h-2 overflow-hidden rounded-pill bg-app-surface-muted">
                  <div class="hero-progress h-full w-3/5 origin-left rounded-pill bg-secondary" />
                </div>
              </div>

              <div class="mt-6 border-t border-app-border pt-5">
                <div class="flex items-start gap-3">
                  <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-control bg-ai-soft text-ai">
                    <Bot :size="17" />
                  </div>

                  <div>
                    <p class="text-sm font-semibold text-app-text">Bạn chưa rõ phần này?</p>
                    <p class="mt-1 text-sm leading-6 text-app-text-muted">Hỏi AI Tutor dựa trên tài liệu của chính khóa học này.</p>
                    <span class="group mt-2 inline-flex items-center gap-1.5 text-sm font-semibold text-ai">
                      Mở AI Tutor
                      <ArrowRight :size="14" class="transition-transform duration-200 group-hover:translate-x-1" />
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- PLATFORM -->
    <section id="platform" class="scroll-mt-20 bg-app-bg">
      <div class="mx-auto grid max-w-app gap-10 px-5 py-16 sm:px-8 lg:grid-cols-[0.8fr_1.2fr] lg:gap-20 lg:py-24">
        <div v-reveal class="reveal max-w-md">
          <p class="text-sm font-semibold text-secondary">Một learning workflow thống nhất</p>
          <h2 class="mt-3 font-heading text-3xl font-bold leading-tight tracking-tight text-app-text">
            Từ nội dung khóa học đến tiến độ học tập, không cần chuyển qua nhiều công cụ.
          </h2>
          <p class="mt-4 text-base leading-7 text-app-text-muted">
            Mỗi thành phần của Smart Learning được đặt trong ngữ cảnh của khóa học để người học tập trung vào việc học thay vì tìm kiếm thông tin.
          </p>
        </div>

        <div class="border-t border-app-border">
          <article
            v-for="(feature, index) in capabilities"
            :key="feature.title"
            v-reveal
            class="reveal feature-row grid gap-3 border-b border-app-border py-6 sm:grid-cols-[3rem_minmax(0,1fr)] sm:gap-5"
            :style="{ transitionDelay: `${index * 90}ms` }"
          >
            <div class="flex h-10 w-10 items-center justify-center rounded-control bg-app-surface text-secondary ring-1 ring-app-border">
              <component :is="feature.icon" :size="18" />
            </div>

            <div class="max-w-2xl">
              <h3 class="font-heading text-lg font-bold text-app-text">{{ feature.title }}</h3>
              <p class="mt-2 text-sm leading-6 text-app-text-muted">{{ feature.description }}</p>
            </div>
          </article>
        </div>
      </div>
    </section>

    <!-- AI -->
    <section id="ai" class="scroll-mt-20 border-y border-app-border bg-app-surface">
      <div class="mx-auto grid max-w-app gap-12 px-5 py-16 sm:px-8 lg:grid-cols-[minmax(0,0.85fr)_minmax(30rem,1.15fr)] lg:items-center lg:gap-20 lg:py-24">
        <div v-reveal class="reveal reveal-left max-w-xl">
          <p class="text-sm font-semibold text-ai">AI Tutor</p>

          <h2 class="mt-3 font-heading text-3xl font-bold leading-tight tracking-tight text-app-text sm:text-4xl">
            AI xuất hiện khi người học cần hỗ trợ, không thay thế quá trình học.
          </h2>

          <p class="mt-5 text-base leading-7 text-app-text-muted">
            AI Tutor sử dụng nội dung tài liệu của khóa học làm ngữ cảnh để hỗ trợ giải thích, làm rõ khái niệm và kết nối kiến thức liên quan.
          </p>

          <div class="mt-7 space-y-3 text-sm">
            <div class="flex items-start gap-3">
              <Check :size="16" class="mt-0.5 shrink-0 text-ai" />
              <p class="leading-6 text-app-text-muted"><strong class="font-semibold text-app-text">Theo ngữ cảnh.</strong> Câu trả lời gắn với tài liệu đang học.</p>
            </div>

            <div class="flex items-start gap-3">
              <Check :size="16" class="mt-0.5 shrink-0 text-ai" />
              <p class="leading-6 text-app-text-muted"><strong class="font-semibold text-app-text">Đúng thời điểm.</strong> Người học chủ động hỏi khi gặp điểm chưa hiểu.</p>
            </div>

            <div class="flex items-start gap-3">
              <Check :size="16" class="mt-0.5 shrink-0 text-ai" />
              <p class="leading-6 text-app-text-muted"><strong class="font-semibold text-app-text">Gắn với khóa học.</strong> Không tách AI thành một công cụ đứng ngoài learning flow.</p>
            </div>
          </div>
        </div>

        <div v-reveal class="reveal reveal-right ai-demo overflow-hidden rounded-panel border border-app-border bg-app-bg">
          <div class="flex items-center gap-3 border-b border-app-border bg-app-surface px-5 py-4">
            <div class="flex h-9 w-9 items-center justify-center rounded-control bg-ai text-on-ai">
              <Bot :size="17" />
            </div>

            <div>
              <p class="text-sm font-semibold text-app-text">AI Tutor</p>
              <p class="text-xs text-app-text-muted">Spring Boot Fundamentals</p>
            </div>
          </div>

          <div class="space-y-5 p-5 sm:p-6">
            <div class="chat-user flex justify-end">
              <div class="max-w-sm rounded-card bg-primary px-4 py-3 text-sm leading-6 text-on-primary">
                IoC và Dependency Injection khác nhau như thế nào?
              </div>
            </div>

            <div class="chat-ai max-w-lg">
              <div class="rounded-card border border-app-border bg-app-surface px-4 py-3 text-sm leading-6 text-app-text">
                IoC là nguyên tắc tổng quát: quyền kiểm soát việc tạo và quản lý dependency được chuyển cho framework. Dependency Injection là một cách triển khai IoC, trong đó dependency được cung cấp từ bên ngoài thay vì đối tượng tự tạo chúng.
              </div>

              <div class="mt-2 flex items-center gap-2 text-xs font-medium text-ai">
                <FileText :size="13" />
                Dựa trên tài liệu của khóa học
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ROLES -->
    <section id="roles" class="scroll-mt-20">
      <div class="mx-auto max-w-app px-5 py-16 sm:px-8 lg:py-24">
        <div v-reveal class="reveal max-w-2xl">
          <p class="text-sm font-semibold text-secondary">Một hệ thống, hai góc nhìn</p>
          <h2 class="mt-3 font-heading text-3xl font-bold tracking-tight text-app-text">
            Được thiết kế cho cả người học và người tổ chức việc học.
          </h2>
        </div>

        <div v-reveal class="reveal mt-10 grid border-y border-app-border md:grid-cols-2">
          <article class="py-8 md:pr-10 lg:pr-16">
            <div class="flex items-center gap-3">
              <BookOpen :size="20" class="text-secondary" />
              <h3 class="font-heading text-xl font-bold text-app-text">Học viên</h3>
            </div>

            <p class="mt-4 max-w-lg text-sm leading-6 text-app-text-muted">
              Một workspace giúp người học biết cần học gì, đang học đến đâu và tìm hỗ trợ ở đâu.
            </p>

            <ul class="mt-6 space-y-3 text-sm text-app-text">
              <li class="flex gap-3"><Check :size="16" class="mt-0.5 shrink-0 text-secondary" />Khám phá và tham gia khóa học</li>
              <li class="flex gap-3"><Check :size="16" class="mt-0.5 shrink-0 text-secondary" />Theo dõi bài học và tiến độ cá nhân</li>
              <li class="flex gap-3"><Check :size="16" class="mt-0.5 shrink-0 text-secondary" />Nhận thông báo, làm bài tập và trao đổi</li>
              <li class="flex gap-3"><Check :size="16" class="mt-0.5 shrink-0 text-secondary" />Hỏi AI Tutor khi cần làm rõ kiến thức</li>
            </ul>
          </article>

          <article class="border-t border-app-border py-8 md:border-l md:border-t-0 md:pl-10 lg:pl-16">
            <div class="flex items-center gap-3">
              <UsersRound :size="20" class="text-ai" />
              <h3 class="font-heading text-xl font-bold text-app-text">Giảng viên</h3>
            </div>

            <p class="mt-4 max-w-lg text-sm leading-6 text-app-text-muted">
              Quản lý khóa học và quá trình học tập mà không phải ghép nhiều công cụ quản trị rời rạc.
            </p>

            <ul class="mt-6 space-y-3 text-sm text-app-text">
              <li class="flex gap-3"><Check :size="16" class="mt-0.5 shrink-0 text-ai" />Tạo và tổ chức nội dung khóa học</li>
              <li class="flex gap-3"><Check :size="16" class="mt-0.5 shrink-0 text-ai" />Quản lý thành viên và yêu cầu tham gia</li>
              <li class="flex gap-3"><Check :size="16" class="mt-0.5 shrink-0 text-ai" />Giao bài, chấm điểm và theo dõi tiến độ</li>
              <li class="flex gap-3"><Check :size="16" class="mt-0.5 shrink-0 text-ai" />Đăng thông báo, tài liệu và hỗ trợ thảo luận</li>
            </ul>
          </article>
        </div>
      </div>
    </section>

    <!-- HOW -->
    <section id="how" class="scroll-mt-20 border-y border-app-border bg-app-bg">
      <div class="mx-auto max-w-app px-5 py-16 sm:px-8 lg:py-24">
        <div class="grid gap-8 lg:grid-cols-[0.65fr_1.35fr] lg:gap-20">
          <div v-reveal class="reveal reveal-left">
            <p class="text-sm font-semibold text-secondary">Cách hoạt động</p>
            <h2 class="mt-3 font-heading text-3xl font-bold tracking-tight text-app-text">
              Một learning flow đơn giản từ lúc tham gia đến khi hoàn thành.
            </h2>
          </div>

          <ol class="border-t border-app-border">
            <li
              v-for="(step, index) in steps"
              :key="step.number"
              v-reveal
              class="reveal workflow-row grid gap-3 border-b border-app-border py-6 sm:grid-cols-[4rem_13rem_minmax(0,1fr)] sm:gap-5"
              :style="{ transitionDelay: `${index * 100}ms` }"
            >
              <span class="font-heading text-sm font-bold text-secondary">{{ step.number }}</span>
              <h3 class="font-heading font-bold text-app-text">{{ step.title }}</h3>
              <p class="text-sm leading-6 text-app-text-muted">{{ step.description }}</p>
            </li>
          </ol>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="overflow-hidden bg-primary text-on-primary">
      <div v-reveal class="reveal mx-auto flex max-w-app flex-col gap-7 px-5 py-14 sm:px-8 md:flex-row md:items-center md:justify-between lg:py-16">
        <div class="max-w-2xl">
          <p class="text-sm font-semibold text-slate-300">Smart Learning</p>
          <h2 class="mt-2 font-heading text-3xl font-bold tracking-tight">
            Bắt đầu học tập trong một hệ thống được tổ chức rõ ràng hơn.
          </h2>
          <p class="mt-3 text-sm leading-6 text-slate-300">
            Tạo tài khoản để tham gia khóa học hoặc bắt đầu xây dựng khóa học của bạn.
          </p>
        </div>

        <RouterLink to="/register" class="group inline-flex h-11 shrink-0 items-center justify-center gap-2 rounded-control bg-app-surface px-5 text-sm font-semibold text-primary transition hover:bg-app-surface-muted">
          Tạo tài khoản
          <ArrowRight :size="17" class="transition-transform duration-200 group-hover:translate-x-1" />
        </RouterLink>
      </div>
    </section>

    <footer class="border-t border-app-border bg-app-surface">
      <div class="mx-auto flex max-w-app flex-col gap-4 px-5 py-7 text-sm text-app-text-muted sm:flex-row sm:items-center sm:justify-between sm:px-8">
        <RouterLink to="/" class="flex items-center gap-2 font-semibold text-app-text">
          <GraduationCap :size="18" />
          Smart Learning
        </RouterLink>
        <p>© 2026 Smart Learning</p>
      </div>
    </footer>
  </main>
</template>

<style scoped>
.hero-copy {
  animation: landing-rise 0.65s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.hero-actions {
  animation: landing-rise 0.65s 0.12s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.hero-points {
  animation: landing-rise 0.65s 0.2s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.hero-product {
  animation: landing-product-enter 0.8s 0.12s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.hero-progress {
  transform: scaleX(0);
  animation: landing-progress 0.9s 0.75s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}

.preview-row {
  transition: transform 180ms ease, background-color 180ms ease, color 180ms ease;
}

.preview-row:hover {
  transform: translateX(3px);
}

.preview-row.active {
  animation: landing-row-active 0.5s 0.55s ease both;
}

.reveal {
  opacity: 0;
  transform: translateY(22px);
  transition: opacity 0.6s cubic-bezier(0.22, 1, 0.36, 1), transform 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}

.reveal-left {
  transform: translateX(-24px);
}

.reveal-right {
  transform: translateX(24px);
}

.reveal.is-visible {
  opacity: 1;
  transform: translate(0);
}

.ai-demo .chat-user,
.ai-demo .chat-ai {
  opacity: 0;
  transform: translateY(10px);
}

.ai-demo.is-visible .chat-user {
  animation: landing-chat 0.45s 0.2s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}

.ai-demo.is-visible .chat-ai {
  animation: landing-chat 0.5s 0.48s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}

.user-menu-enter-active,
.user-menu-leave-active {
  transition: opacity 160ms ease, transform 160ms ease;
}

.user-menu-enter-from,
.user-menu-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@keyframes landing-rise {
  from { opacity: 0; transform: translateY(18px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes landing-product-enter {
  from { opacity: 0; transform: translateX(24px) translateY(8px); }
  to { opacity: 1; transform: translate(0); }
}

@keyframes landing-progress {
  from { transform: scaleX(0); }
  to { transform: scaleX(1); }
}

@keyframes landing-row-active {
  from { opacity: 0.55; transform: translateX(-6px); }
  to { opacity: 1; transform: translateX(0); }
}

@keyframes landing-chat {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (prefers-reduced-motion: reduce) {
  .hero-copy,
  .hero-actions,
  .hero-points,
  .hero-product,
  .hero-progress,
  .preview-row.active,
  .ai-demo.is-visible .chat-user,
  .ai-demo.is-visible .chat-ai {
    animation: none !important;
  }

  .hero-progress {
    transform: scaleX(1);
  }

  .reveal,
  .reveal-left,
  .reveal-right,
  .ai-demo .chat-user,
  .ai-demo .chat-ai {
    opacity: 1 !important;
    transform: none !important;
    transition: none !important;
  }

  .user-menu-enter-active,
  .user-menu-leave-active {
    transition: none;
  }
}
</style>