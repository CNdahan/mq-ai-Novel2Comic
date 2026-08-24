import request from '@/utils/request'

/**
 * 用户注册
 * @param {Object} data - 注册信息
 * @param {string} data.username - 用户名
 * @param {string} data.email - 邮箱（后端字段名）
 * @param {string} data.password - 密码（后端字段名）
 */
export function register(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

/**
 * 用户登录
 * @param {Object} data - 登录信息
 * @param {string} data.email - 邮箱（后端字段名）
 * @param {string} data.password - 密码（后端字段名）
 */
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

/**
 * 刷新Token
 */
export function refreshToken() {
  return request({
    url: '/auth/refresh',
    method: 'post'
  })
}

/**
 * 退出登录
 */
export function logout() {
  // 清除本地存储
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  localStorage.removeItem('quotaRemaining')
}

/**
 * 获取用户信息（包括剩余次数）
 */
export function getUserInfo() {
  return request({
    url: '/auth/info',
    method: 'get'
  })
}

/**
 * 更新个人信息
 * @param {Object} data - 个人信息
 * @param {string} data.username - 用户名（可选）
 * @param {string} data.email - 邮箱（可选）
 * @param {string} data.avatar - 头像URL（可选）
 */
export function updateProfile(data) {
  return request({
    url: '/auth/profile',
    method: 'put',
    data
  })
}

/**
 * 修改密码
 * @param {Object} data - 密码信息
 * @param {string} data.oldPassword - 旧密码
 * @param {string} data.newPassword - 新密码
 * @param {string} data.confirmPassword - 确认密码
 */
export function updatePassword(data) {
  return request({
    url: '/auth/password',
    method: 'put',
    data
  })
}

/**
 * VIP升级
 * @param {Object} data - VIP升级信息
 * @param {number} data.vipLevel - VIP等级：1-月费，2-年费
 * @param {number} data.duration - 购买时长（月）
 * @param {string} data.paymentMethod - 支付方式（可选）
 */
export function upgradeVip(data) {
  return request({
    url: '/auth/vip/upgrade',
    method: 'post',
    data
  })
}

