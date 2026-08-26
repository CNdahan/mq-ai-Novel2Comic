<template>
  <div class="history-container">
    <el-page-header @back="router.push('/home')" content="我的作品" />
    
    <div class="history-content">
      <!-- 筛选器 -->
      <el-card class="filter-card">
        <div class="filter-section">
          <div class="filter-item">
            <span class="filter-label">状态筛选：</span>
            <el-radio-group v-model="queryParams.status" @change="handleFilterChange">
              <el-radio-button label="all">全部</el-radio-button>
              <el-radio-button label="uploaded">已上传小说</el-radio-button>
              <el-radio-button label="storyboard_pending">待生成分镜</el-radio-button>
              <el-radio-button label="storyboard_generating">生成分镜中</el-radio-button>
              <el-radio-button label="storyboard_completed">已生成分镜</el-radio-button>
              <el-radio-button label="image_pending">待生成图片</el-radio-button>
              <el-radio-button label="image_generating">生成图片中</el-radio-button>
              <el-radio-button label="completed">生成成功</el-radio-button>
            </el-radio-group>
          </div>
          
          <div class="filter-item">
            <span class="filter-label">风格筛选：</span>
            <el-radio-group v-model="queryParams.style" @change="handleFilterChange">
              <el-radio-button label="all">全部</el-radio-button>
              <el-radio-button label="japanese">日系漫画</el-radio-button>
              <el-radio-button label="chinese">国风漫画</el-radio-button>
              <el-radio-button label="realistic">写实风格</el-radio-button>
            </el-radio-group>
          </div>
          <div v-if="comicList.length > 0" class="batch-actions">
            <el-checkbox
              :model-value="allCurrentPageSelected"
              :indeterminate="isIndeterminate"
              @change="handleSelectAll"
            >全选本页</el-checkbox>
            <span v-if="selectedComicIds.length > 0" class="selected-count">已选择 {{ selectedComicIds.length }} 个</span>
            <el-button
              type="danger"
              :icon="Delete"
              :disabled="selectedComicIds.length === 0"
              @click="handleBatchDelete"
            >批量删除</el-button>
          </div>
        </div>
      </el-card>
      
      <!-- 漫画列表 -->
      <div class="comic-list" v-loading="loading">
        <el-empty v-if="!loading && comicList.length === 0" description="暂无作品">
          <el-button type="primary" @click="router.push('/upload')">
            📝 去创作
          </el-button>
        </el-empty>
        
        <div class="comic-grid" v-else>
          <div 
            class="comic-card" 
            v-for="comic in comicList" 
            :key="comic.novelId"
          >
            <el-checkbox
              class="comic-selector"
              :model-value="selectedComicIds.includes(comic.novelId)"
              @click.stop
              @change="handleSelectComic(comic.novelId, $event)"
            />
            <!-- 删除按钮 -->
            <div class="card-actions">
              <el-button 
                type="danger" 
                size="small" 
                circle
                :icon="Delete"
                @click.stop="handleDeleteComic(comic, comic.title)"
                title="删除作品"
              />
            </div>
            
            <!-- 封面图 -->
            <div class="comic-cover" @click="handleComicClick(comic.novelId)">
              <el-image
                v-if="comic.coverImage"
                :src="comic.coverImage"
                fit="cover"
                :lazy="true"
              >
                <template #placeholder>
                  <div class="image-placeholder">
                    <el-icon class="is-loading"><Loading /></el-icon>
                  </div>
                </template>
                <template #error>
                  <div class="image-error">
                    <el-icon><PictureFilled /></el-icon>
                  </div>
                </template>
              </el-image>
              <div v-else class="no-cover">
                <el-icon><PictureFilled /></el-icon>
                <p>暂无封面</p>
              </div>
              
              <!-- 状态标签 -->
              <div class="status-badge" :class="getStatusClass(comic.status)">
                {{ getStatusText(comic.status) }}
              </div>
            </div>
            
            <!-- 漫画信息 -->
            <div class="comic-info" @click="handleComicClick(comic.novelId)">
              <h3 class="comic-title" :title="comic.title">{{ comic.title }}</h3>
              <p class="novel-title" :title="comic.novelTitle">📖 {{ comic.novelTitle }}</p>
              
              <div class="comic-meta">
                <el-tag 
                  v-if="comic.style"
                  :type="getStyleType(comic.style)" 
                  size="small" 
                  effect="plain"
                >
                  {{ getStyleName(comic.style) }}
                </el-tag>
                <span class="panel-count">
                  <el-icon><PictureFilled /></el-icon>
                  {{ comic.panelCount }} 幅
                </span>
              </div>
              
              <div class="comic-time">
                <el-icon><Clock /></el-icon>
                {{ formatTime(comic.createdAt) }}
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="queryParams.current"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 30, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, PictureFilled, Clock, Delete } from '@element-plus/icons-vue'
import { getWorkflowList } from '@/api/novel'
import { deleteNovel } from '@/api/novel'

const router = useRouter()

// 查询参数
const queryParams = ref({
  current: 1,
  pageSize: 10,
  status: 'all',
  style: 'all'
})

const loading = ref(false)
const allWorkList = ref([])
const filteredWorkList = computed(() => allWorkList.value.filter(item => {
  const statusMatch = queryParams.value.status === 'all' || item.status === queryParams.value.status
  const styleMatch = queryParams.value.style === 'all' || item.style === queryParams.value.style
  return statusMatch && styleMatch
}))
const comicList = computed(() => {
  const start = (queryParams.value.current - 1) * queryParams.value.pageSize
  return filteredWorkList.value.slice(start, start + queryParams.value.pageSize)
})
const total = computed(() => filteredWorkList.value.length)
const selectedComicIds = ref([])

const allCurrentPageSelected = computed(() =>
  comicList.value.length > 0 && comicList.value.every(comic => selectedComicIds.value.includes(comic.novelId))
)

const isIndeterminate = computed(() =>
  selectedComicIds.value.length > 0 && !allCurrentPageSelected.value
)

onMounted(() => {
  loadComicList()
})

// 加载漫画列表
const loadComicList = async () => {
  loading.value = true
  try {
    const response = await getWorkflowList()
    if (response.code === 200 && response.data) {
      allWorkList.value = response.data || []
      const currentPageIds = new Set(comicList.value.map(comic => comic.novelId))
      selectedComicIds.value = selectedComicIds.value.filter(id => currentPageIds.has(id))
    } else {
      ElMessage.error(response.message || '加载作品列表失败')
    }
  } catch (error) {
    console.error('加载作品列表失败:', error)
    ElMessage.error('加载作品列表失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

// 筛选条件变化
const handleFilterChange = () => {
  queryParams.value.current = 1
  loadComicList()
}

// 分页大小变化
const handleSizeChange = () => {
  queryParams.value.current = 1
  loadComicList()
}

// 页码变化
const handlePageChange = () => {
  loadComicList()
}

// 点击漫画卡片
const handleComicClick = (novelId) => {
  const work = allWorkList.value.find(item => item.novelId === novelId)
  if (!work) return
  if (work.status === 'completed' && work.comicId) {
    router.push(`/preview/${work.comicId}`)
  } else if ((work.status === 'image_generating' || work.status === 'storyboard_generating') && work.taskId) {
    router.push(`/progress/${work.taskId}`)
  } else if (work.status === 'storyboard_completed' || work.status === 'image_pending') {
    router.push(`/storyboard/${work.novelId}`)
  } else {
    router.push(`/character/${work.novelId}`)
  }
}

// 删除漫画
const handleSelectComic = (novelId, selected) => {
  selectedComicIds.value = selected
    ? [...selectedComicIds.value, novelId]
    : selectedComicIds.value.filter(id => id !== novelId)
}

const handleSelectAll = (selected) => {
  selectedComicIds.value = selected ? comicList.value.map(comic => comic.novelId) : []
}

const handleBatchDelete = async () => {
  const novelIds = [...selectedComicIds.value]
  try {
    await ElMessageBox.confirm(
      '确定要删除选中的 ' + novelIds.length + ' 个作品吗？删除后无法恢复，本地图片也将被清理。',
      '批量删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        distinguishCancelAndClose: true
      }
    )

    loading.value = true
    const selectedWorks = allWorkList.value.filter(work => novelIds.includes(work.novelId))
    for (const work of selectedWorks) {
      await deleteNovel(work.novelId)
    }
    if (selectedWorks.length === novelIds.length) {
      selectedComicIds.value = []
      ElMessage.success('作品删除成功')
      if (comicList.value.length === novelIds.length && queryParams.value.current > 1) {
        queryParams.value.current--
      }
      await loadComicList()
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('批量删除作品失败:', error)
      ElMessage.error('批量删除失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

// 删除漫画
const handleDeleteComic = async (work, title) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除作品《${title}》吗？删除后无法恢复，本地图片也将被清理。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        distinguishCancelAndClose: true
      }
    )
    
    // 开始删除
    loading.value = true
    const response = await deleteNovel(work.novelId)
    
    if (response.code === 200) {
      ElMessage.success('作品删除成功')
      // 重新加载列表
      await loadComicList()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('删除作品失败:', error)
      ElMessage.error('删除失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

// 获取状态类型
const getStatusClass = (status) => {
  const classMap = {
    uploaded: 'status-uploaded',
    storyboard_pending: 'status-pending',
    storyboard_completed: 'status-storyboard',
    storyboard_generating: 'status-generating',
    image_pending: 'status-pending',
    image_generating: 'status-generating',
    completed: 'status-completed',
    generating: 'status-generating',
    failed: 'status-failed'
  }
  return classMap[status] || ''
}

// 获取状态文本
const getStatusText = (status) => {
  const textMap = {
    uploaded: '已上传小说',
    storyboard_pending: '待生成分镜',
    storyboard_completed: '已生成分镜',
    storyboard_generating: '生成分镜中',
    image_pending: '待生成图片',
    image_generating: '生成图片中',
    completed: '生成成功图片',
    generating: '生成中',
    failed: '失败'
  }
  return textMap[status] || status
}

// 获取风格类型
const getStyleType = (style) => {
  const typeMap = {
    japanese: 'primary',
    chinese: 'success',
    realistic: 'warning'
  }
  return typeMap[style] || 'info'
}

// 获取风格名称
const getStyleName = (style) => {
  const nameMap = {
    japanese: '日系',
    chinese: '国风',
    realistic: '写实'
  }
  return nameMap[style] || style
}

// 格式化时间
const formatTime = (dateTime) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  const now = new Date()
  const diff = now - date
  
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  
  if (diff < minute) {
    return '刚刚'
  } else if (diff < hour) {
    return `${Math.floor(diff / minute)} 分钟前`
  } else if (diff < day) {
    return `${Math.floor(diff / hour)} 小时前`
  } else if (diff < 7 * day) {
    return `${Math.floor(diff / day)} 天前`
  } else {
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    })
  }
}
</script>

<style scoped>
.history-container {
  min-height: 100vh;
  background: linear-gradient(to bottom, #f0f2f5 0%, #e6e8eb 100%);
  padding: 24px;
}

.history-container :deep(.el-page-header) {
  background: #ffffff;
  padding: 18px 28px;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.history-container :deep(.el-page-header__content) {
  font-size: 20px;
  font-weight: 700;
  color: #2c3e50;
}

.history-content {
  max-width: 1400px;
  margin: 24px auto 0;
}

/* 筛选器样式 */
.filter-card {
  margin-bottom: 24px;
  border-radius: 16px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.filter-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-label {
  font-weight: 600;
  color: #303133;
  font-size: 15px;
  min-width: 90px;
}

.batch-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-top: 4px;
}

.selected-count {
  color: #606266;
  font-size: 14px;
}

.filter-item :deep(.el-radio-button__inner) {
  border-radius: 8px;
  margin: 0 4px;
  transition: all 0.3s ease;
}

.filter-item :deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-radius: 8px;
}

.filter-item :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 8px;
}

/* 漫画列表样式 */
.comic-list {
  min-height: 400px;
}

.comic-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  margin-top: 8px;
}

.comic-card {
  background: #ffffff;
  border-radius: 16px;
  overflow: hidden;
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.comic-selector {
  position: absolute;
  top: 12px;
  left: 52px;
  z-index: 10;
  padding: 6px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.comic-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 32px rgba(24, 144, 255, 0.2);
  border-color: rgba(24, 144, 255, 0.3);
}

.comic-card:hover .card-actions {
  opacity: 1;
}

/* 卡片操作按钮 */
.card-actions {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 10;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.card-actions .el-button {
  backdrop-filter: blur(8px);
  background: rgba(255, 255, 255, 0.9);
  border: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.card-actions .el-button:hover {
  background: #ff4d4f;
  color: white;
  transform: scale(1.1);
}

/* 封面图样式 */
.comic-cover {
  position: relative;
  width: 100%;
  height: 200px;
  background: #f5f7fa;
  overflow: hidden;
}

.comic-cover :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.image-placeholder,
.image-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7ed 100%);
  color: #909399;
}

.image-placeholder .el-icon {
  font-size: 40px;
  animation: rotating 2s linear infinite;
}

.image-error .el-icon {
  font-size: 48px;
  opacity: 0.5;
}

.no-cover {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
}

.no-cover .el-icon {
  font-size: 48px;
  margin-bottom: 8px;
  opacity: 0.8;
}

.no-cover p {
  margin: 0;
  font-size: 14px;
  opacity: 0.9;
}

/* 状态标签 */
.status-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 12px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  backdrop-filter: blur(8px);
}

.status-completed {
  background: rgba(82, 196, 26, 0.9);
  color: #ffffff;
}

.status-generating {
  background: rgba(24, 144, 255, 0.9);
  color: #ffffff;
  animation: pulse 2s ease-in-out infinite;
}

.status-uploaded {
  background: rgba(114, 46, 209, 0.9);
  color: #ffffff;
}

.status-pending {
  background: rgba(230, 162, 60, 0.95);
  color: #ffffff;
}

.status-storyboard {
  background: rgba(19, 194, 194, 0.95);
  color: #ffffff;
}

.status-failed {
  background: rgba(245, 34, 45, 0.9);
  color: #ffffff;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}

/* 漫画信息样式 */
.comic-info {
  padding: 16px;
  cursor: pointer;
}

.comic-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 700;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.novel-title {
  margin: 0 0 12px 0;
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.comic-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.panel-count {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #606266;
}

.comic-time {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
}

/* 分页样式 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding: 20px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.pagination-wrapper :deep(.el-pagination) {
  font-weight: 500;
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

/* 响应式设计 */
@media (max-width: 768px) {
  .history-container {
    padding: 12px;
  }
  
  .filter-section {
    gap: 16px;
  }
  
  .filter-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .filter-label {
    min-width: auto;
  }

  .batch-actions {
    width: 100%;
    flex-wrap: wrap;
    gap: 12px;
  }
  
  .comic-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .pagination-wrapper {
    padding: 16px 12px;
  }
  
  .pagination-wrapper :deep(.el-pagination) {
    flex-wrap: wrap;
    justify-content: center;
  }
}

@media (min-width: 768px) and (max-width: 1200px) {
  .comic-grid {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  }
}
</style>

