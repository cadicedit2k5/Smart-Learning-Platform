import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import HomePage from '../HomePage.vue'

describe('HomePage', () => {
  it('renders the application heading', () => {
    const wrapper = mount(HomePage)

    expect(wrapper.get('h1').text()).toBe('Smart Learning Platform')
  })
})
