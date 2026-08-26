import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 创建axios实例
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 300000, // 请求超时时间：5分钟（小说解析等长任务需要更长时间）
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 调试信息
console.log('API BaseURL:', import.meta.env.VITE_API_BASE_URL || '/api')
console.log('Environment:', import.meta.env.MODE)

// 请求拦截器
request.interceptors.request.use(
  config => {
    console.log('=== 请求拦截器 ===')
    console.log('请求URL:', config.url)
    console.log('完整URL:', config.baseURL + config.url)
    console.log('请求方法:', config.method)
    
    // 从localStorage获取token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
      console.log('Token已添加')
    } else {
      console.log('⚠️ 未找到token')
    }
    return config
  },
  error => {
    console.error('请求错误：', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    console.log('=== 响应拦截器 ===')
    console.log('响应状态:', response.status)

    if (response.config.responseType === 'blob') {
      return response
    }
    
    const res = response.data
    console.log('res.code:', res.code)
    console.log('res.message:', res.message)
    console.log('res.data类型:', Array.isArray(res.data) ? 'Array' : typeof res.data)
    console.log('res.data长度:', res.data?.length)
    
    // 不要打印完整的response和response.data，可能导致浏览器卡死
    if (res.data && Array.isArray(res.data)) {
      console.log('数据示例（第一项）:', res.data[0] ? JSON.stringify(res.data[0]).substring(0, 200) + '...' : 'empty')
    }
    
    // 如果返回的状态码为200，说明接口请求成功，可以正常拿到数据
    if (res.code === 200) {
      console.log('✅ 响应成功，返回数据')
      return res
    }
    
    // 其他状态码都作为错误处理
    console.error('❌ 响应失败，code:', res.code)
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  error => {
    console.error('响应错误：', error)
    
    if (error.response) {
      const { status, data } = error.response
      
      switch (status) {
        case 400:
          ElMessage.error(data.message || '参数错误')
          break
        case 401:
          ElMessage.error('请先登录')
          // 清除token
          localStorage.removeItem('token')
          localStorage.removeItem('userId')
          localStorage.removeItem('username')
          // 跳转到登录页
          router.push('/login')
          break
        case 403:
          ElMessage.error('配额不足，请升级VIP')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error(data.message || '服务器错误，请稍后重试')
          break
        default:
          ElMessage.error(data.message || '未知错误')
      }
    } else if (error.request) {
      ElMessage.error('网络错误，请检查网络连接')
    } else {
      ElMessage.error('请求配置错误')
    }
    
    return Promise.reject(error)
  }
)

export default request

