import { useAuthStore } from '@/features/auth/stores';
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { portals } from '@/portals/registry'
import { getPortalHomeRoute } from '@/portals/routeHelpers';
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
      path: '/register',
      name: 'register',
      component: () =>
        import('@/features/auth/pages/RegisterPage.vue'),
      meta: {
        title: 'Đăng ký',
        guestOnly: true,
      },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/pages/errors/ErrorPage.vue'),
      meta: {
        title: 'Page not found',
      },
    },
  ],
});

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (!authStore.initialized) {
    await authStore.initialize()
  }

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return {
      name: 'login',
    }
  }

  if (authStore.user && to.meta.role && authStore.user.role.code !== to.meta.role) {
    return getPortalHomeRoute(authStore.user.role.code)
  }

  if (to.meta.guestOnly && authStore.user) {
    return getPortalHomeRoute(authStore.user.role.code)
  }

  return true
})

router.afterEach((to) => {
  const pageTitle = to.meta.title

  document.title = pageTitle
    ? `${pageTitle} | Smart Learning`
    : 'Smart Learning'
});

export default router;
