import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi, logout as logoutApi, getUserInfo as getUserInfoApi } from '@/api/auth'
import { ElMessage } from 'element-plus'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const username = ref(localStorage.getItem('username') || '')
  const userEmail = ref(localStorage.getItem('userEmail') || '')
  const quotaRemaining = ref(Number(localStorage.getItem('quotaRemaining')) || 0)
  const avatar = ref(localStorage.getItem('avatar') || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png')

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)

  // 登录
  const login = async (loginForm) => {
    try {
      // 转换字段名：userEmail -> email, userPassword -> password
      const requestData = {
        email: loginForm.userEmail,
        password: loginForm.userPassword
      }
      const response = await loginApi(requestData)
      const { token: newToken, userId: newUserId, username: newUsername, quotaRemaining: newQuota, avatar: newAvatar } = response.data
      
      // 保存到状态
      token.value = newToken
      userId.value = newUserId
      username.value = newUsername
      quotaRemaining.value = newQuota
      if (newAvatar) {
        avatar.value = newAvatar
      }
      
      // 保存到localStorage
      localStorage.setItem('token', newToken)
      localStorage.setItem('userId', newUserId)
      localStorage.setItem('username', newUsername)
      localStorage.setItem('quotaRemaining', newQuota)
      if (newAvatar) {
        localStorage.setItem('avatar', newAvatar)
      }
      
      ElMessage.success('登录成功')
      return response
    } catch (error) {
      console.error('登录失败:', error)
      throw error
    }
  }

  // 注册
  const register = async (registerForm) => {
    try {
      // 转换字段名：userEmail -> email, userPassword -> password
      const requestData = {
        username: registerForm.username,
        email: registerForm.userEmail,
        password: registerForm.userPassword
      }
      const response = await registerApi(requestData)
      const { token: newToken, userId: newUserId, username: newUsername, quotaRemaining: newQuota, avatar: newAvatar } = response.data
      
      // 保存到状态
      token.value = newToken
      userId.value = newUserId
      username.value = newUsername
      quotaRemaining.value = newQuota
      if (newAvatar) {
        avatar.value = newAvatar
      }
      
      // 保存到localStorage
      localStorage.setItem('token', newToken)
      localStorage.setItem('userId', newUserId)
      localStorage.setItem('username', newUsername)
      localStorage.setItem('quotaRemaining', newQuota)
      if (newAvatar) {
        localStorage.setItem('avatar', newAvatar)
      }
      
      ElMessage.success('注册成功')
      return response
    } catch (error) {
      console.error('注册失败:', error)
      throw error
    }
  }

  // 退出登录
  const logout = () => {
    logoutApi()
    
    // 清除状态
    token.value = ''
    userId.value = ''
    username.value = ''
    userEmail.value = ''
    quotaRemaining.value = 0
    avatar.value = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
    
    ElMessage.success('已退出登录')
  }

  // 更新配额
  const updateQuota = (newQuota) => {
    quotaRemaining.value = newQuota
    localStorage.setItem('quotaRemaining', newQuota)
  }
  
  // 刷新用户信息（包括剩余次数）
  const refreshUserInfo = async () => {
    try {
      const response = await getUserInfoApi()
      if (response.code === 200 && response.data) {
        const { quotaRemaining: newQuota, username: newUsername, avatar: newAvatar } = response.data
        quotaRemaining.value = newQuota
        username.value = newUsername
        if (newAvatar) {
          avatar.value = newAvatar
          localStorage.setItem('avatar', newAvatar)
        }
        localStorage.setItem('quotaRemaining', newQuota)
        localStorage.setItem('username', newUsername)
        console.log('用户信息已刷新，剩余次数:', newQuota, '头像:', newAvatar)
      }
    } catch (error) {
      console.error('刷新用户信息失败:', error)
    }
  }

  return {
    token,
    userId,
    username,
    userEmail,
    quotaRemaining,
    avatar,
    isLoggedIn,
    login,
    register,
    logout,
    updateQuota,
    refreshUserInfo
  }
})

