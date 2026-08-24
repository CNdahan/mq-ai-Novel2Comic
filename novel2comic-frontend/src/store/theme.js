import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  // 从localStorage读取主题设置，默认为light
  const theme = ref(localStorage.getItem('theme') || 'light')

  // 应用主题到document
  const applyTheme = (newTheme) => {
    document.documentElement.setAttribute('data-theme', newTheme)
    localStorage.setItem('theme', newTheme)
  }

  // 初始化时应用主题
  applyTheme(theme.value)

  // 监听主题变化
  watch(theme, (newTheme) => {
    applyTheme(newTheme)
  })

  // 切换主题
  const toggleTheme = () => {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
  }

  // 设置主题
  const setTheme = (newTheme) => {
    if (newTheme === 'light' || newTheme === 'dark') {
      theme.value = newTheme
    }
  }

  return {
    theme,
    toggleTheme,
    setTheme
  }
})

