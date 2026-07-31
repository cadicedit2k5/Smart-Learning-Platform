import type { Component } from 'vue'
import type { RouteRecordRaw } from 'vue-router'

import type { UserRole } from '@/features/auth/role'

export interface NavigationItem {
  label: string
  routeName: string
  icon?: Component
}

export interface PortalDefinition {
  role: UserRole
  basePath: string
  routes: RouteRecordRaw[]
  navigation: NavigationItem[]
}