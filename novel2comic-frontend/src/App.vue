<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { useThemeStore } from '@/store/theme'

const userStore = useUserStore()
const themeStore = useThemeStore()

onMounted(() => {
  console.log('Novel2Comic 应用启动')
  console.log('当前环境:', import.meta.env.VITE_APP_ENV)
  console.log('API地址:', import.meta.env.VITE_API_BASE_URL)
  console.log('当前主题:', themeStore.theme)
  
  // 检查用户登录状态
  if (userStore.isLoggedIn) {
    console.log('用户已登录:', userStore.username)
  }
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  width: 100%;
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 
    'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
  transition: background-color 0.3s ease, color 0.3s ease;
}

#app {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* 主题变量定义 */
:root {
  /* 日间模式（默认） */
  --bg-primary: #f5f7fa;
  --bg-secondary: #ffffff;
  --bg-hero: linear-gradient(135deg, #f5f7fa 0%, #e3f2fd 100%);
  --text-primary: #303133;
  --text-secondary: #606266;
  --text-tertiary: #909399;
  --border-color: #EBEEF5;
  --shadow-sm: 0 2px 12px rgba(0, 0, 0, 0.05);
  --shadow-md: 0 2px 12px rgba(0, 0, 0, 0.08);
  --shadow-lg: 0 12px 28px rgba(0, 0, 0, 0.12);
  --card-bg: #ffffff;
  --header-bg: #ffffff;
}

/* 夜间模式 */
[data-theme='dark'] {
  --bg-primary: #1a1a1a;
  --bg-secondary: #2d2d2d;
  --bg-hero: linear-gradient(135deg, #1a1a1a 0%, #2d3748 100%);
  --text-primary: #e0e0e0;
  --text-secondary: #b0b0b0;
  --text-tertiary: #808080;
  --border-color: #404040;
  --shadow-sm: 0 2px 12px rgba(0, 0, 0, 0.3);
  --shadow-md: 0 2px 12px rgba(0, 0, 0, 0.4);
  --shadow-lg: 0 12px 28px rgba(0, 0, 0, 0.5);
  --card-bg: #2d2d2d;
  --header-bg: #2d2d2d;
}

/* 为Element Plus组件应用暗色主题 */
[data-theme='dark'] .el-card {
  background-color: var(--card-bg);
  border-color: var(--border-color);
  color: var(--text-primary);
}

[data-theme='dark'] .el-menu {
  background-color: transparent;
}

[data-theme='dark'] .el-menu-item {
  color: var(--text-secondary);
}

[data-theme='dark'] .el-menu-item:hover,
[data-theme='dark'] .el-menu-item.is-active {
  color: #409EFF;
}
</style>
