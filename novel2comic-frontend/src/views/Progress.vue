<template>
  <div class="progress-container">
    <el-card class="progress-card">
      <template #header>
        <div class="header-content">
          <h2>
            <span v-if="taskStatus === 'completed'">✅ 漫画生成完成</span>
            <span v-else-if="taskStatus === 'failed'">❌ 生成失败</span>
            <span v-else>⏱️ 正在生成漫画...</span>
          </h2>
          <div class="task-info">
            <el-tag size="small" :type="getStatusType(taskStatus)">
              {{ getStatusText(taskStatus) }}
            </el-tag>
            <el-tag size="small" type="info" style="margin-left: 10px;">
              任务ID: {{ taskUuid.slice(0, 8) }}...
            </el-tag>
          </div>
        </div>
      </template>
      
      <div class="progress-content">
        <!-- 进度条 -->
        <el-progress
          :percentage="progress"
          :status="getProgressStatus()"
          :stroke-width="32"
          :text-inside="true"
        />
        
        <!-- 进度信息 -->
        <div class="progress-info">
          <div class="current-step">
            <el-icon v-if="taskStatus === 'processing'" class="is-loading">
              <Loading />
            </el-icon>
            <el-icon v-else-if="taskStatus === 'completed'" class="success-icon">
              <SuccessFilled />
            </el-icon>
            <span>{{ currentStep }}</span>
          </div>
          
          <!-- 统计卡片 -->
          <div class="stats-grid" v-if="totalPanels > 0">
            <div class="stat-card">
              <div class="stat-icon" :class="taskStatus === 'completed' ? 'success' : 'primary'">
                <el-icon><Picture /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-label">生成进度</div>
                <div class="stat-value">
                  <span v-if="taskStatus === 'completed'" class="completed-number">{{ totalPanels }}</span>
                  <span v-else class="current-number">{{ completedPanels }}</span>
                  <span class="total-number">/ {{ totalPanels }}</span>
                </div>
              </div>
            </div>
            
            <div class="stat-card">
              <div class="stat-icon clock">
                <el-icon><Clock /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-label">{{ completeTime ? '总耗时' : '已用时' }}</div>
                <div class="stat-value time-value-large">
                  <span v-if="completeTime">{{ calculateDuration() }}</span>
                  <span v-else>{{ elapsedTime }}</span>
                </div>
              </div>
            </div>
            
            <div class="stat-card">
              <div class="stat-icon info">
                <el-icon><InfoFilled /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-label">任务状态</div>
                <div class="stat-value">
                  <el-tag :type="getStatusType(taskStatus)" size="large">
                    {{ getStatusText(taskStatus) }}
                  </el-tag>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 时间详情 -->
          <div class="time-details" v-if="startTime">
            <div class="time-detail-item">
              <span class="label">开始时间</span>
              <span class="value">{{ formatTime(startTime) }}</span>
            </div>
            <div class="time-detail-item" v-if="completeTime">
              <span class="label">完成时间</span>
              <span class="value">{{ formatTime(completeTime) }}</span>
            </div>
          </div>
        </div>
        
        <!-- WebSocket连接状态 -->
        <el-alert
          v-if="wsConnected"
          title="实时推送已连接"
          type="success"
          :closable="false"
          show-icon
          style="margin-top: 20px;"
        />
        <el-alert
          v-else-if="taskStatus === 'processing'"
          title="正在连接WebSocket..."
          type="info"
          :closable="false"
          show-icon
          style="margin-top: 20px;"
        />
        
        <!-- 错误信息 -->
        <el-alert
          v-if="errorMessage"
          :title="errorMessage"
          type="error"
          :closable="false"
          show-icon
          style="margin-top: 20px;"
        />
        
        <!-- 操作按钮 -->
        <div class="action-buttons">
          <!-- 完成状态 -->
          <template v-if="taskStatus === 'completed'">
            <el-button
              v-if="comicId"
              type="primary"
              size="large"
              :icon="SuccessFilled"
              @click="handleViewResult"
            >
              🎨 查看漫画
            </el-button>
            <el-button
              v-else
              type="primary"
              size="large"
              :loading="true"
            >
              正在获取漫画ID...
            </el-button>
            <el-button
              size="large"
              @click="handleBack"
            >
              返回首页
            </el-button>
          </template>
          
          <!-- 生成中状态 -->
          <el-button
            v-if="taskStatus === 'processing'"
            size="large"
            @click="handleCancel"
          >
            取消生成
          </el-button>
          
          <!-- 失败状态 -->
          <template v-if="taskStatus === 'failed'">
            <el-button
              type="primary"
              size="large"
              @click="handleBack"
            >
              返回首页
            </el-button>
          </template>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, SuccessFilled, Picture, Clock, InfoFilled } from '@element-plus/icons-vue'
import { createWebSocketClient, disconnectWebSocket } from '@/utils/websocket'
import { getTaskProgress, cancelTask, getLatestComicByNovelId } from '@/api/comic'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 任务信息
const taskUuid = ref(route.params.taskId || '')
const taskStatus = ref('processing') // processing, completed, failed
const progress = ref(0)
const currentStep = ref('初始化任务...')
const completedPanels = ref(0)
const totalPanels = ref(0)
const comicId = ref(null)
const errorMessage = ref('')
const startTime = ref(null)
const completeTime = ref(null)

// WebSocket
let wsClient = null
const wsConnected = ref(false)

// 轮询定时器
let pollingTimer = null
const currentTime = ref(Date.now())
let timeTimer = null

// 计算已用时
const elapsedTime = computed(() => {
  if (!startTime.value) return '-'
  const elapsed = Math.floor((currentTime.value - new Date(startTime.value).getTime()) / 1000)
  const minutes = Math.floor(elapsed / 60)
  const seconds = elapsed % 60
  return `${minutes}分${seconds}秒`
})

onMounted(() => {
  if (!taskUuid.value) {
    ElMessage.error('任务ID不存在')
    router.push('/storyboard')
    return
  }
  
  // 启动时间更新
  timeTimer = setInterval(() => {
    currentTime.value = Date.now()
  }, 1000)
  
  // 初始查询任务状态
  fetchTaskProgress()
  
  // 启动WebSocket连接
  initWebSocket()
  
  // 启动轮询作为备用方案（5秒一次）
  pollingTimer = setInterval(fetchTaskProgress, 5000)
})

onUnmounted(() => {
  // 清理资源
  if (wsClient) {
    disconnectWebSocket(wsClient)
  }
  if (pollingTimer) {
    clearInterval(pollingTimer)
  }
  if (timeTimer) {
    clearInterval(timeTimer)
  }
})

// 初始化WebSocket
const initWebSocket = () => {
  try {
    wsClient = createWebSocketClient(
      taskUuid.value,
      handleProgressUpdate,
      handleTaskComplete,
      handleTaskError
    )
  } catch (error) {
    console.error('WebSocket初始化失败:', error)
    ElMessage.warning('实时推送连接失败，将使用轮询方式获取进度')
  }
}

// 处理进度更新
const handleProgressUpdate = (data) => {
  console.log('进度更新:', data)
  wsConnected.value = true
  
  if (data.progress !== undefined) {
    progress.value = data.progress
  }
  if (data.currentStep) {
    currentStep.value = data.currentStep
  }
  if (data.completedPanels !== undefined) {
    completedPanels.value = data.completedPanels
  }
  if (data.totalPanels !== undefined) {
    totalPanels.value = data.totalPanels
  }
}

// 处理任务完成
const handleTaskComplete = async (data) => {
  console.log('任务完成:', data)
  wsConnected.value = false
  taskStatus.value = 'completed'
  progress.value = 100
  currentStep.value = '✅ 生成完成'
  
  if (data.comicId) {
    comicId.value = data.comicId
    console.log('获取到comicId:', comicId.value)
  }
  
  // 更新完成的分镜数量
  if (totalPanels.value > 0) {
    completedPanels.value = totalPanels.value
  }
  
  // 停止轮询
  if (pollingTimer) {
    clearInterval(pollingTimer)
  }
  
  // 刷新用户剩余次数
  await userStore.refreshUserInfo()
  
  ElMessage.success({
    message: '🎉 漫画生成完成！',
    type: 'success',
    duration: 3000
  })
}

// 处理任务错误
const handleTaskError = (data) => {
  console.error('任务失败:', data)
  wsConnected.value = false
  taskStatus.value = 'failed'
  errorMessage.value = data.errorMessage || '生成失败'
  
  // 停止轮询
  if (pollingTimer) {
    clearInterval(pollingTimer)
  }
  
  ElMessage.error('漫画生成失败')
}

// 轮询获取任务进度
const fetchTaskProgress = async () => {
  try {
    const response = await getTaskProgress(taskUuid.value)
    
    if (response.code === 200 && response.data) {
      const task = response.data
      
      // 更新任务信息
      taskStatus.value = task.status
      progress.value = task.progressPercent || 0
      currentStep.value = task.currentStep || '处理中'
      completedPanels.value = task.completedPanels || 0
      totalPanels.value = task.totalPanels || 0
      startTime.value = task.startTime
      completeTime.value = task.completeTime
      
      // 处理任务完成
      if (task.status === 'completed') {
        taskStatus.value = 'completed'
        currentStep.value = '✅ 生成完成'
        
        // 更新完成的分镜数量
        if (totalPanels.value > 0 && completedPanels.value === 0) {
          completedPanels.value = totalPanels.value
        }
        
        // 如果还没有comicId，尝试从novelId查询
        if (!comicId.value) {
          await fetchComicId()
        }
        
        // 停止轮询
        if (pollingTimer) {
          clearInterval(pollingTimer)
        }
        
        if (!comicId.value) {
          console.warn('任务完成但未获取到comicId')
        }
      }
      
      // 处理任务失败
      if (task.status === 'failed') {
        errorMessage.value = task.errorMessage || '生成失败'
        
        // 停止轮询
        if (pollingTimer) {
          clearInterval(pollingTimer)
        }
        
        ElMessage.error('漫画生成失败')
      }
    }
  } catch (error) {
    console.error('获取任务进度失败:', error)
  }
}

// 获取漫画ID（从任务完成后的comic表查询）
const fetchComicId = async () => {
  try {
    // 通过taskUuid从generate_task表获取novelId，然后查询最新的comic
    const taskResponse = await getTaskProgress(taskUuid.value)
    if (taskResponse.code === 200 && taskResponse.data) {
      const novelId = taskResponse.data.novelId
      
      // 等待一会儿，让漫画数据完全保存
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      // 查询该小说最新的漫画
      try {
        const comicResponse = await getLatestComicByNovelId(novelId)
        if (comicResponse.code === 200 && comicResponse.data) {
          comicId.value = comicResponse.data.id || comicResponse.data.comicId
          console.log('✅ 获取到comicId:', comicId.value)
        }
      } catch (err) {
        console.warn('获取最新漫画失败，使用备用方案:', err)
        // 备用方案：如果API不存在，等待WebSocket推送
      }
    }
  } catch (error) {
    console.error('获取comicId失败:', error)
  }
}

// 查看结果
const handleViewResult = () => {
  console.log('🎨 准备查看漫画, comicId:', comicId.value)
  
  if (comicId.value) {
    const targetPath = `/preview/${comicId.value}`
    console.log('🔀 跳转到:', targetPath)
    router.push(targetPath)
  } else {
    console.error('❌ comicId未找到')
    ElMessage.warning({
      message: '漫画ID未找到，请稍后重试',
      duration: 3000
    })
  }
}

// 取消生成
const handleCancel = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要取消生成吗？已生成的内容将会丢失。',
      '取消确认',
      {
        confirmButtonText: '确定取消',
        cancelButtonText: '继续生成',
        type: 'warning'
      }
    )
    
    const response = await cancelTask(taskUuid.value)
    
    if (response.code === 200) {
      ElMessage.success('已取消生成')
      taskStatus.value = 'failed'
      errorMessage.value = '用户取消任务'
      
      // 停止所有定时器
      if (pollingTimer) clearInterval(pollingTimer)
      if (timeTimer) clearInterval(timeTimer)
      if (wsClient) disconnectWebSocket(wsClient)
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消任务失败:', error)
      ElMessage.error('取消失败')
    }
  }
}

// 返回
const handleBack = () => {
  router.push('/home')
}

// 获取状态类型
const getStatusType = (status) => {
  const typeMap = {
    processing: 'primary',
    completed: 'success',
    failed: 'danger'
  }
  return typeMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const textMap = {
    processing: '生成中',
    completed: '已完成',
    failed: '失败'
  }
  return textMap[status] || status
}

// 获取进度条状态
const getProgressStatus = () => {
  if (taskStatus.value === 'completed') return 'success'
  if (taskStatus.value === 'failed') return 'exception'
  return undefined
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}:${date.getSeconds().toString().padStart(2, '0')}`
}

// 计算总耗时
const calculateDuration = () => {
  if (!startTime.value || !completeTime.value) return '-'
  const start = new Date(startTime.value).getTime()
  const end = new Date(completeTime.value).getTime()
  const duration = Math.floor((end - start) / 1000)
  const minutes = Math.floor(duration / 60)
  const seconds = duration % 60
  return `${minutes}分${seconds}秒`
}
</script>

<style scoped>
.progress-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(to bottom, #f0f2f5 0%, #e6e8eb 100%);
  padding: 20px;
  position: relative;
}

.progress-card {
  width: 100%;
  max-width: 1200px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.08);
  border-radius: 20px;
  background: #ffffff;
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.progress-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 5px;
  background: linear-gradient(90deg, #1890ff 0%, #52c41a 50%, #fa8c16 100%);
}

.progress-card :deep(.el-card__header) {
  background: #fafbfc;
  border-bottom: 1px solid #e8e8e8;
  padding: 36px 56px 32px;
}

.header-content {
  text-align: center;
}

.header-content h2 {
  margin: 0 0 14px 0;
  color: #262626;
  font-size: 36px;
  font-weight: 700;
  letter-spacing: -0.8px;
}

.task-info {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 14px;
  margin-top: 14px;
}

.progress-content {
  padding: 48px 64px 56px;
}

.progress-info {
  margin-top: 32px;
  text-align: center;
}

.current-step {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #262626;
  margin: 20px 0;
  font-weight: 600;
  padding: 16px 28px;
  background: #f5f7fa;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #e8e8e8;
}

.current-step .el-icon {
  margin-right: 10px;
  font-size: 22px;
}

.current-step .success-icon {
  color: #52c41a;
  filter: drop-shadow(0 2px 4px rgba(82, 196, 26, 0.3));
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  margin: 32px 0;
}

.stat-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 28px 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(24, 144, 255, 0.12);
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  color: #ffffff;
  flex-shrink: 0;
}

.stat-icon.primary {
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.25);
}

.stat-icon.success {
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
  box-shadow: 0 4px 12px rgba(82, 196, 26, 0.25);
}

.stat-icon.clock {
  background: linear-gradient(135deg, #fa8c16 0%, #ffa940 100%);
  box-shadow: 0 4px 12px rgba(250, 140, 22, 0.25);
}

.stat-icon.info {
  background: linear-gradient(135deg, #13c2c2 0%, #36cfc9 100%);
  box-shadow: 0 4px 12px rgba(19, 194, 194, 0.25);
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-label {
  font-size: 15px;
  color: #8c8c8c;
  margin-bottom: 8px;
  font-weight: 500;
  letter-spacing: 0.3px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #2c3e50;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.stat-value .current-number {
  color: #1890ff;
}

.stat-value .completed-number {
  color: #52c41a;
}

.stat-value .total-number {
  color: #8c8c8c;
  font-size: 22px;
  font-weight: 600;
}

.stat-value.time-value-large {
  font-size: 26px;
  color: #fa8c16;
}

/* 时间详情 */
.time-details {
  display: flex;
  justify-content: center;
  gap: 32px;
  margin-top: 20px;
  padding: 16px 24px;
  background: rgba(248, 249, 250, 0.5);
  border-radius: 12px;
}

.time-detail-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.time-detail-item .label {
  font-size: 12px;
  color: #8c8c8c;
  font-weight: 500;
}

.time-detail-item .value {
  font-size: 16px;
  color: #2c3e50;
  font-weight: 700;
  font-family: 'Courier New', monospace;
}

.action-buttons {
  margin-top: 36px;
  text-align: center;
  display: flex;
  justify-content: center;
  gap: 16px;
}

.action-buttons .el-button {
  min-width: 150px;
  height: 44px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.action-buttons .el-button--primary {
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
}

.action-buttons .el-button--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(24, 144, 255, 0.4);
}

.action-buttons .el-button:not(.el-button--primary) {
  background: #ffffff;
  border: 2px solid #e0e0e0;
  color: #666;
}

.action-buttons .el-button:not(.el-button--primary):hover {
  border-color: #1890ff;
  color: #1890ff;
  transform: translateY(-2px);
}

/* 进度条样式优化 */
.progress-content :deep(.el-progress) {
  margin: 0 8px;
}

.progress-content :deep(.el-progress-bar__outer) {
  background-color: #e9ecef;
  border-radius: 12px;
  overflow: hidden;
}

.progress-content :deep(.el-progress-bar__inner) {
  background: linear-gradient(90deg, #1890ff 0%, #52c41a 100%);
  border-radius: 12px;
  transition: width 0.6s ease;
}

.progress-content :deep(.el-progress__text) {
  font-size: 17px !important;
  font-weight: 700 !important;
  color: #1890ff !important;
}

/* 加载动画 */
.is-loading {
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* WebSocket连接状态样式优化 */
.progress-content :deep(.el-alert) {
  border-radius: 12px;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.progress-content :deep(.el-alert--success) {
  background: linear-gradient(135deg, #e8f5e9 0%, #f1f8e9 100%);
  border-left: 4px solid #52c41a;
}

.progress-content :deep(.el-alert--info) {
  background: #e6f7ff;
  border-left: 4px solid #1890ff;
}

.progress-content :deep(.el-alert__icon) {
  font-size: 20px;
}

.progress-content :deep(.el-alert__title) {
  font-weight: 600;
  font-size: 14px;
}

/* Tag样式优化 */
.task-info :deep(.el-tag) {
  border-radius: 8px;
  padding: 6px 14px;
  font-weight: 600;
  border: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .progress-container {
    padding: 12px;
  }
  
  .progress-card {
    max-width: 100%;
  }
  
  .progress-card :deep(.el-card__header) {
    padding: 20px 24px 16px;
  }
  
  .progress-content {
    padding: 24px 20px 28px;
  }
  
  .header-content h2 {
    font-size: 22px;
  }
  
  .panels-info {
    padding: 16px 20px;
  }
  
  .action-buttons {
    flex-direction: column;
    gap: 12px;
  }
  
  .action-buttons .el-button {
    width: 100%;
    min-width: auto;
  }
  
  .time-info :deep(.el-row) {
    margin: 0 !important;
  }
  
  .time-info :deep(.el-col) {
    padding: 0 6px !important;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .stat-card {
    padding: 16px;
  }
  
  .stat-icon {
    width: 44px;
    height: 44px;
    font-size: 20px;
  }
  
  .stat-value {
    font-size: 20px;
  }
  
  .time-details {
    flex-direction: column;
    gap: 12px;
  }
}
</style>

