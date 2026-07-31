<template>
  <main class="grid min-h-screen place-items-center bg-slate-50 px-6">
    <div class="text-center">
      <p class="text-sm font-semibold text-violet-600">404</p>
      <h1 class="mt-2 text-3xl font-bold text-slate-900">Page not found</h1>
      <p class="mt-3 text-slate-600">The page you are looking for does not exist.</p>
      <RouterLink
        to="/"
        class="mt-6 inline-flex rounded-lg bg-violet-600 px-4 py-2 text-sm font-semibold text-white hover:bg-violet-700"
      >
        Back to home
      </RouterLink>
    </div>
  </main>
</template>
