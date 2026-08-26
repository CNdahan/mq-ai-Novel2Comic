<template>
  <div class="home-container">
    <!-- 导航栏 -->
    <el-header class="header">
      <div class="header-content">
        <div class="logo">
          <img src="/favicon.ico" alt="Logo" />
          <span>Novel2Comic</span>
        </div>
        <el-menu
          mode="horizontal"
          :default-active="activeMenu"
          class="nav-menu"
        >
          <el-menu-item index="1" @click="router.push('/home')">首页</el-menu-item>
          <el-menu-item index="2" @click="router.push('/history')">我的作品</el-menu-item>
          <el-menu-item index="3" @click="router.push('/profile')">个人中心</el-menu-item>
        </el-menu>
        <div class="user-info">
          <!-- 主题切换按钮 -->
          <el-tooltip :content="themeStore.theme === 'light' ? '切换到夜间模式' : '切换到日间模式'" placement="bottom">
            <el-button 
              circle 
              @click="themeStore.toggleTheme"
              class="theme-toggle"
            >
              <el-icon :size="18">
                <Sunny v-if="themeStore.theme === 'light'" />
                <Moon v-else />
              </el-icon>
            </el-button>
          </el-tooltip>
          
          <template v-if="userStore.isLoggedIn">
            <span class="username">{{ userStore.username }}</span>
            <el-tag type="success">剩余次数: {{ userStore.quotaRemaining }}</el-tag>
            <el-button text @click="handleLogout">退出</el-button>
          </template>
          <template v-else>
            <el-button @click="router.push('/login')">登录</el-button>
            <el-button type="primary" @click="router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>

    <!-- 主要内容 -->
    <div class="main-content">
      <!-- Hero区域 -->
      <div class="hero-section">
        <h1 class="title">Novel2Comic</h1>
        <p class="subtitle">基于AI的小说漫画生成平台</p>
        <p class="description">
          将你的小说文本转换为精美漫画，只需2-3分钟
        </p>
        <el-button
          type="primary"
          size="large"
          @click="handleStartCreate"
          class="start-button"
        >
          <el-icon><EditPen /></el-icon>
          <span>开始创作</span>
        </el-button>
      </div>

      <!-- 特色功能 -->
      <div class="features-section">
        <h2>核心特色</h2>
        <el-row :gutter="20" class="features-grid">
          <el-col :xs="24" :sm="12" :md="6">
            <el-card shadow="hover" class="feature-card">
              <el-icon :size="40" color="#409EFF"><MagicStick /></el-icon>
              <h3>AI智能识别</h3>
              <p>自动提取角色和场景，智能设计分镜</p>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-card shadow="hover" class="feature-card">
              <el-icon :size="40" color="#67C23A"><Lightning /></el-icon>
              <h3>极速生成</h3>
              <p>10张图片仅需2-3分钟，效率提升960倍</p>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-card shadow="hover" class="feature-card">
              <el-icon :size="40" color="#E6A23C"><User /></el-icon>
              <h3>角色一致性</h3>
              <p>确保同一角色在不同场景保持外观统一</p>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-card shadow="hover" class="feature-card">
              <el-icon :size="40" color="#F56C6C"><Money /></el-icon>
              <h3>成本优化</h3>
              <p>智能缓存技术，降低99%制作成本</p>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 使用流程 -->
      <div class="process-section">
        <h2>使用流程</h2>
        <el-steps :active="4" align-center class="steps">
          <el-step title="上传小说" description="输入300-10000字小说文本" />
          <el-step title="确认角色" description="AI识别角色，可手动调整" />
          <el-step title="设计分镜" description="AI自动生成分镜脚本" />
          <el-step title="生成漫画" description="2-3分钟生成精美漫画" />
        </el-steps>
      </div>
    </div>

    <!-- 页脚 -->
    <el-footer class="footer">
      <p>&copy; 2024 Novel2Comic. All rights reserved.</p>
    </el-footer>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useThemeStore } from '@/store/theme'
import { ElMessage } from 'element-plus'
import {
  EditPen,
  MagicStick,
  Lightning,
  User,
  Money,
  Sunny,
  Moon
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const activeMenu = ref('1')

// 开始创作
const handleStartCreate = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  router.push('/upload')
}

// 退出登录
const handleLogout = () => {
  userStore.logout()
  router.push('/home')
}
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  transition: background-color 0.3s ease;
}

/* 头部 */
.header {
  background: var(--header-bg);
  box-shadow: var(--shadow-sm);
  padding: 0;
  height: 60px;
  line-height: 60px;
  transition: background-color 0.3s ease, box-shadow 0.3s ease;
}

.header-content {
  max-width: 1600px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: bold;
  color: #409EFF;
}

.logo img {
  width: 32px;
  height: 32px;
}

.nav-menu {
  flex: 1;
  margin: 0 40px;
  border: none;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.theme-toggle {
  transition: all 0.3s ease;
}

.theme-toggle:hover {
  transform: rotate(20deg);
  background-color: rgba(64, 158, 255, 0.1);
}

.username {
  color: var(--text-secondary);
}

/* 主要内容 */
.main-content {
  flex: 1;
  max-width: 1600px;
  width: 100%;
  margin: 0 auto;
  padding: 40px 40px;
}

/* Hero区域 */
.hero-section {
  text-align: center;
  padding: 80px 0;
  background: var(--bg-hero);
  margin: -40px -40px 60px -40px;
  border-radius: 0;
  position: relative;
  transition: background 0.3s ease;
}

.hero-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml,<svg width="100" height="100" xmlns="http://www.w3.org/2000/svg"><rect width="100" height="100" fill="none"/><circle cx="50" cy="50" r="1" fill="%23409EFF" opacity="0.1"/></svg>');
  opacity: 0.3;
  pointer-events: none;
}

.title {
  font-size: 56px;
  font-weight: bold;
  background: linear-gradient(135deg, #409EFF 0%, #67C23A 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 20px;
  position: relative;
}

.subtitle {
  font-size: 24px;
  color: var(--text-secondary);
  margin-bottom: 15px;
  font-weight: 500;
}

.description {
  font-size: 16px;
  color: var(--text-tertiary);
  margin-bottom: 40px;
}

.start-button {
  font-size: 18px;
  padding: 15px 40px;
  height: auto;
  background: linear-gradient(135deg, #409EFF 0%, #66b1ff 100%);
  border: none;
  box-shadow: 0 4px 15px rgba(64, 158, 255, 0.3);
  transition: all 0.3s ease;
}

.start-button:hover {
  background: linear-gradient(135deg, #66b1ff 0%, #409EFF 100%);
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.4);
}

/* 特色功能 */
.features-section {
  margin-top: 0px;
  padding: 50px 40px;
}

.features-section h2 {
  text-align: center;
  font-size: 32px;
  margin-bottom: 40px;
  color: var(--text-primary);
}

.features-grid {
  margin-top: 30px;
}

.feature-card {
  text-align: center;
  padding: 30px 20px;
  transition: all 0.3s ease;
  border-radius: 16px;
  border: none;
  background: var(--card-bg);
  box-shadow: var(--shadow-sm);
  height: 100%;
}

.feature-card:hover {
  transform: translateY(-8px);
  box-shadow: var(--shadow-lg);
}

.feature-card h3 {
  margin: 20px 0 10px;
  font-size: 18px;
  color: var(--text-primary);
}

.feature-card p {
  color: var(--text-tertiary);
  line-height: 1.6;
}

/* 使用流程 */
.process-section {
  margin-top: 60px;
  padding: 50px 40px;
  background: var(--card-bg);
  border-radius: 16px;
  box-shadow: var(--shadow-sm);
}

.process-section h2 {
  text-align: center;
  font-size: 32px;
  margin-bottom: 40px;
  color: var(--text-primary);
}

.steps {
  margin-top: 40px;
  padding: 0 20px;
}

/* 页脚 */
.footer {
  background: var(--header-bg);
  text-align: center;
  padding: 30px 0;
  border-top: 1px solid var(--border-color);
  transition: background-color 0.3s ease;
}

.footer p {
  color: var(--text-tertiary);
  margin: 5px 0;
}
</style>

