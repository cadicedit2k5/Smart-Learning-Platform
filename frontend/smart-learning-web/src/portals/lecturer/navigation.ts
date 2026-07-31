import {
  BarChart3,
  BrainCircuit,
  GraduationCap,
  LayoutDashboard,
  Settings,
} from 'lucide-vue-next'
import { markRaw } from 'vue'

import type { NavigationItem } from '@/portals/type'

export const lecturerNavigation: NavigationItem[] = [
  {
    label: 'Overview',
    routeName: 'lecturer-dashboard',
    icon: markRaw(LayoutDashboard),
  },
  {
    label: 'My Courses',
    routeName: 'lecturer-courses',
    icon: markRaw(GraduationCap),
  },
  {
    label: 'Learning Analytics',
    routeName: 'lecturer-analytics',
    icon: markRaw(BarChart3),
  },
  {
    label: 'AI Insights',
    routeName: 'lecturer-ai-insights',
    icon: markRaw(BrainCircuit),
  },
  {
    label: 'Settings',
    routeName: 'lecturer-settings',
    icon: markRaw(Settings),
  },
]