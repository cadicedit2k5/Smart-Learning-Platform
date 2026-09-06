<template>
  <section
    class="overflow-hidden rounded-card border border-app-border bg-app-surface shadow-card"
  >
    <header
      v-if="$slots.header"
      class="border-b border-app-border px-6 py-5"
    >
      <slot name="header" />
    </header>

    <div class="p-6">
      <slot />
    </div>

    <footer
      v-if="$slots.footer"
      class="border-t border-app-border bg-app-surface-muted px-6 py-4"
    >
      <slot name="footer" />
    </footer>
  </section>
</template>