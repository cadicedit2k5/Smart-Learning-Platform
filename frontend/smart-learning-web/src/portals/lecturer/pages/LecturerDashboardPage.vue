<template>
  <section class="mx-auto max-w-app">
    <div class="rounded-panel bg-primary p-6 text-white shadow-card sm:p-8">
      <p class="text-sm font-semibold text-secondary-soft">Không gian giảng viên</p>
      <h1 class="mt-2 font-heading text-3xl font-bold">Quản lý hoạt động giảng dạy</h1>
      <p class="mt-3 max-w-2xl text-sm leading-6 text-white/75">
        Tạo khóa học, quản lý tài liệu và sử dụng trợ lý AI theo ngữ cảnh môn học trong cùng một
        không gian.
      </p>

      <RouterLink
        :to="{ name: 'lecturer-courses' }"
        class="mt-6 inline-flex h-11 items-center rounded-control bg-white px-4 text-sm font-semibold text-primary transition hover:bg-secondary-soft"
      >
        Mở danh sách khóa học
      </RouterLink>
    </div>
  </section>
</template>
