import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { portals } from '@/portals/registry'
import { createRouter, createWebHistory } from 'vue-router'

const portalRoutes = portals.map((portal) => ({
  path: portal.basePath,
  component: DefaultLayout,
  meta: {
    portal: portal.role,
    requiresAuth: true,
    role: portal.role,
  },
  children: portal.routes,
}));

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [

    ...portalRoutes,
    {
      path: '/',
      redirect: {
        name: 'login',
      },
    },
    {
      path: '/login',
      name: 'login',
      component: () =>
        import('@/features/auth/pages/LoginPage.vue'),

      meta: {
        title: 'Sign in',
        guestOnly: true,
      },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/pages/errors/NotFoundPage.vue'),
      meta: {
        title: 'Page not found',
      },
    },
  ],
});

router.afterEach((to) => {
  const pageTitle = to.meta.title

  document.title = pageTitle
    ? `${pageTitle} | Smart Learning`
    : 'Smart Learning'
});

export default router;
