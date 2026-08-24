import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

/**
 * 创建WebSocket连接
 * @param {string} taskId - 任务ID
 * @param {function} onProgress - 进度更新回调
 * @param {function} onComplete - 完成回调
 * @param {function} onError - 错误回调
 * @returns {Client} WebSocket客户端实例
 */
export function createWebSocketClient(taskId, onProgress, onComplete, onError) {
  const wsBaseUrl = import.meta.env.VITE_WS_BASE_URL || 'http://localhost:8123'
  
  const client = new Client({
    // 使用SockJS作为WebSocket的传输层
    webSocketFactory: () => new SockJS(`${wsBaseUrl}/ws`),
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    debug: (str) => {
      console.log('WebSocket Debug:', str)
    }
  })
  
  // 连接成功
  client.onConnect = () => {
    console.log('✅ WebSocket连接成功')
    
    // 订阅进度更新
    client.subscribe(`/topic/progress/${taskId}`, (message) => {
      try {
        const progress = JSON.parse(message.body)
        console.log('📨 收到进度更新:', progress)
        
        // 根据消息类型处理
        if (progress.type === 'progress') {
          // 进度更新
          onProgress && onProgress(progress)
        } else if (progress.type === 'completed') {
          // 任务完成
          onComplete && onComplete(progress)
          // 断开连接
          setTimeout(() => client.deactivate(), 1000)
        } else if (progress.type === 'failed') {
          // 任务失败
          onError && onError(progress)
          // 断开连接
          setTimeout(() => client.deactivate(), 1000)
        }
      } catch (error) {
        console.error('❌ 解析WebSocket消息失败:', error)
      }
    })
  }
  
  // 连接失败
  client.onStompError = (frame) => {
    console.error('❌ WebSocket连接失败:', frame)
    onError && onError({ errorMessage: 'WebSocket连接失败' })
  }
  
  // 激活连接
  client.activate()
  
  return client
}

/**
 * 断开WebSocket连接
 * @param {Client} client - WebSocket客户端实例
 */
export function disconnectWebSocket(client) {
  if (client && client.active) {
    client.deactivate()
    console.log('WebSocket连接已断开')
  }
}

