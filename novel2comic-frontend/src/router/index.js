import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/upload',
    name: 'Upload',
    component: () => import('@/views/Upload.vue'),
    meta: { title: '上传小说', requiresAuth: true }
  },
  {
    path: '/character/:novelId',
    name: 'Character',
    component: () => import('@/views/Character.vue'),
    meta: { title: '角色确认', requiresAuth: true }
  },
  {
    path: '/storyboard/:novelId',
    name: 'Storyboard',
    component: () => import('@/views/Storyboard.vue'),
    meta: { title: '分镜预览', requiresAuth: true }
  },
  {
    path: '/progress/:taskId',
    name: 'Progress',
    component: () => import('@/views/Progress.vue'),
    meta: { title: '生成进度', requiresAuth: true }
  },
  {
    path: '/preview/:comicId',
    name: 'Preview',
    component: () => import('@/views/Preview.vue'),
    meta: { title: '漫画预览', requiresAuth: true }
  },
  {
    path: '/history',
    name: 'History',
    component: () => import('@/views/History.vue'),
    meta: { title: '历史记录', requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '个人中心', requiresAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - Novel2Comic` : 'Novel2Comic'
  
  // 检查是否需要登录
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    // 需要登录但未登录，跳转到登录页
    next({
      path: '/login',
      query: { redirect: to.fullPath } // 保存目标路由，登录后跳转
    })
  } else if ((to.path === '/login' || to.path === '/register') && userStore.isLoggedIn) {
    // 已登录用户访问登录/注册页，跳转到首页
    next('/home')
  } else {
    next()
  }
})

export default router

