import type { Component } from 'vue'
import type { RouteRecordRaw } from 'vue-router'

import type { UserRole } from '@/features/auth/role'

export interface NavigationItem {
  label: string
  routeName: string
  icon?: Component
}

export interface TopbarAction {
  label: string
  routeName: string
  icon?: Component
}

export interface TopbarConfig {
  search?: {
    placeholder: string
  }

  primaryAction?: TopbarAction
}

export interface PortalDefinition {
  role: UserRole
  basePath: string
  homeRouteName: string
  routes: RouteRecordRaw[]
  navigation: NavigationItem[]
  topbar: TopbarConfig
}