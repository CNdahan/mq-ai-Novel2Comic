<template>
  <div class="preview-container">
    <el-page-header @back="handleBack" :content="comicTitle || '漫画预览'" />
    
    <div class="preview-content" v-loading="loading">
      <el-card v-if="!loading && comic">
        <template #header>
          <div class="preview-header">
            <div class="title-section">
              <h2>{{ comicTitle }}</h2>
              <div class="meta-tags">
                <el-tag :type="getStyleType(comic.style)" size="large" effect="light">
                  {{ getStyleName(comic.style) }}
                </el-tag>
                <el-tag type="success" size="large" effect="light">
                  {{ comic.panelCount || panels.length }} 幅
                </el-tag>
              </div>
            </div>
          </div>
        </template>
        
        <!-- 统计信息卡片 -->
        <div class="comic-stats" v-if="panels && panels.length > 0">
          <div class="stat-item">
            <div class="stat-icon-wrapper purple">
              <el-icon><PictureFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ panels.length }}</div>
              <div class="stat-text">总分镜数</div>
            </div>
          </div>
          
          <div class="stat-item">
            <div class="stat-icon-wrapper green">
              <el-icon><SuccessFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ getCachedCount() }}</div>
              <div class="stat-text">缓存命中</div>
            </div>
          </div>
          
          <div class="stat-item">
            <div class="stat-icon-wrapper orange">
              <el-icon><Timer /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ getAvgTime() }}ms</div>
              <div class="stat-text">平均耗时</div>
            </div>
          </div>
        </div>
        
        <!-- 漫画面板展示 -->
        <div class="comic-panels" v-if="panels && panels.length > 0">
          <div 
            class="panel-item" 
            v-for="(panel, index) in panels" 
            :key="panel.id"
          >
            <div class="panel-header">
              <span class="panel-index">第 {{ index + 1 }} 幅</span>
              <el-tag v-if="panel.isCached" type="success" size="small">缓存命中</el-tag>
            </div>
            <div class="panel-image">
              <div class="image-debug-info">
                <el-tag size="small" type="info">URL: {{ panel.imageUrl ? '✅' : '❌' }}</el-tag>
              </div>
              <el-image
                :src="panel.imageUrl"
                :alt="`分镜${index + 1}`"
                fit="contain"
                :preview-src-list="allImageUrls"
                :initial-index="index"
                :lazy="true"
                @load="handleImageLoad(index)"
                @error="handleImageError(index, panel.imageUrl)"
              >
                <template #placeholder>
                  <div class="image-loading">
                    <el-icon class="is-loading"><Loading /></el-icon>
                    <p>加载中...</p>
                  </div>
                </template>
                <template #error>
                  <div class="image-error">
                    <el-icon><Picture /></el-icon>
                    <p>图片加载失败</p>
                    <el-button size="small" @click="showImageUrl(panel.imageUrl)">
                      查看URL
                    </el-button>
                  </div>
                </template>
              </el-image>
            </div>
            <div class="panel-info">
              <el-collapse>
                <el-collapse-item title="📋 查看详情" name="1">
                  <div class="detail-section">
                    <div class="detail-item">
                      <span class="detail-label">🖼️ 图片链接:</span>
                      <el-link 
                        :href="panel.imageUrl" 
                        target="_blank" 
                        type="primary"
                        style="margin-left: 10px;"
                      >
                        在新窗口打开
                      </el-link>
                      <el-button 
                        size="small" 
                        style="margin-left: 10px;"
                        @click="copyUrl(panel.imageUrl)"
                      >
                        复制链接
                      </el-button>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">⏱️ 生成耗时:</span>
                      <span>{{ panel.generateTimeMs }}ms</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">💾 缓存状态:</span>
                      <el-tag :type="panel.isCached ? 'success' : 'info'" size="small">
                        {{ panel.isCached ? '缓存命中' : '新生成' }}
                      </el-tag>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">🎨 Prompt:</span>
                    </div>
                    <p class="prompt-text">{{ panel.promptText }}</p>
                  </div>
                </el-collapse-item>
              </el-collapse>
            </div>
          </div>
        </div>
        
        <!-- 无数据提示 -->
        <el-empty v-else description="暂无漫画内容" />
        
      </el-card>
      
      <!-- 错误提示 -->
      <el-card v-if="!loading && error">
        <el-result
          icon="error"
          title="😢 加载失败"
          :sub-title="error"
        >
          <template #extra>
            <div class="error-actions">
              <el-button type="primary" size="large" @click="loadComic">
                🔄 重新加载
              </el-button>
              <el-button size="large" @click="handleBack">
                ← 返回首页
              </el-button>
            </div>
            
            <!-- 调试信息 -->
            <div class="debug-info">
              <el-collapse>
                <el-collapse-item title="🔍 调试信息" name="1">
                  <div class="debug-content">
                    <p><strong>ComicId:</strong> {{ comicId || '未获取到' }}</p>
                    <p><strong>路由参数:</strong> {{ JSON.stringify(route.params) }}</p>
                    <p><strong>错误信息:</strong> {{ error }}</p>
                    <p><strong>提示:</strong> 请检查浏览器控制台(F12)查看详细日志</p>
                  </div>
                </el-collapse-item>
              </el-collapse>
            </div>
          </template>
        </el-result>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Loading, PictureFilled, SuccessFilled, Timer } from '@element-plus/icons-vue'
import { getComicResult } from '@/api/comic'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const error = ref('')
const comic = ref(null)
const panels = ref([])

// 修复：路由参数是comicId，不是id
const comicId = computed(() => route.params.comicId)
const comicTitle = computed(() => comic.value?.title || comic.value?.comicTitle || '漫画预览')

// 所有图片URL（用于预览）
const allImageUrls = computed(() => {
  return panels.value.map(panel => panel.imageUrl)
})

onMounted(() => {
  loadComic()
})

// 加载漫画数据
const loadComic = async () => {
  console.log('🔍 开始加载漫画, comicId:', comicId.value)
  console.log('📍 当前路由参数:', route.params)
  
  if (!comicId.value) {
    error.value = `缺少漫画ID。当前路由参数: ${JSON.stringify(route.params)}`
    loading.value = false
    console.error('❌ 缺少comicId')
    return
  }
  
  loading.value = true
  error.value = ''
  
  try {
    console.log('📡 调用API: /comic/result/' + comicId.value)
    const response = await getComicResult(comicId.value)
    console.log('📥 API响应:', response)
    
    if (response.code === 200 && response.data) {
      comic.value = response.data
      panels.value = response.data.panels || []
      console.log('✅ 漫画加载成功, 面板数量:', panels.value.length)
      console.log('📊 漫画数据:', response.data)
      
      // 详细输出每个面板的图片URL
      panels.value.forEach((panel, index) => {
        console.log(`📷 面板${index + 1} URL:`, panel.imageUrl)
        console.log(`   - isCached:`, panel.isCached)
        console.log(`   - 生成耗时:`, panel.generateTimeMs, 'ms')
      })
      
      if (panels.value.length === 0) {
        ElMessage.warning('漫画没有面板内容')
      }
    } else {
      error.value = response.message || '加载漫画失败'
      console.error('❌ API返回错误:', error.value)
      ElMessage.error(error.value)
    }
  } catch (err) {
    console.error('❌ 加载漫画失败:', err)
    error.value = `网络错误: ${err.message || '请检查网络连接'}`
    ElMessage.error('加载漫画失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

// 返回
const handleBack = () => {
  router.push('/home')
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
    japanese: '日系漫画',
    chinese: '国风漫画',
    realistic: '写实风格'
  }
  return nameMap[style] || style
}

// 图片加载成功
const handleImageLoad = (index) => {
  console.log(`✅ 图片${index + 1}加载成功`)
}

// 图片加载失败
const handleImageError = (index, url) => {
  console.error(`❌ 图片${index + 1}加载失败`)
  console.error('图片URL:', url)
  console.error('URL长度:', url ? url.length : 0)
  console.error('URL前100字符:', url ? url.substring(0, 100) : 'null')
}

// 显示图片URL
const showImageUrl = (url) => {
  ElMessageBox.alert(url, '图片URL', {
    confirmButtonText: '复制',
    callback: () => {
      navigator.clipboard.writeText(url)
      ElMessage.success('已复制到剪贴板')
    }
  })
}

// 复制URL
const copyUrl = (url) => {
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('图片链接已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 获取缓存命中数量
const getCachedCount = () => {
  return panels.value.filter(p => p.isCached === 1 || p.isCached === true).length
}

// 获取平均生成时间
const getAvgTime = () => {
  if (panels.value.length === 0) return 0
  const total = panels.value.reduce((sum, p) => sum + (p.generateTimeMs || 0), 0)
  return Math.round(total / panels.value.length)
}
</script>

<style scoped>
.preview-container {
  min-height: 100vh;
  background: linear-gradient(to bottom, #f0f2f5 0%, #e6e8eb 100%);
  padding: 24px;
}

.preview-container :deep(.el-page-header) {
  background: #ffffff;
  padding: 18px 28px;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.preview-container :deep(.el-page-header__content) {
  font-size: 20px;
  font-weight: 700;
  color: #2c3e50;
}

.preview-content {
  margin-top: 24px;
  max-width: 1300px;
  margin-left: auto;
  margin-right: auto;
}

.preview-content :deep(.el-card) {
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.preview-content :deep(.el-card__header) {
  background: #fafbfc;
  border-bottom: 1px solid #e8e8e8;
  padding: 28px 36px;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-section {
  flex: 1;
}

.preview-header h2 {
  margin: 0 0 14px 0;
  color: #262626;
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.8px;
}

.meta-tags {
  display: flex;
  gap: 10px;
  align-items: center;
}

.meta-tags :deep(.el-tag) {
  border-radius: 8px;
  padding: 6px 16px;
  font-weight: 600;
  border: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

/* 统计信息卡片 */
.comic-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  padding: 28px 32px;
  background: #fafbfc;
  border-radius: 16px;
  margin-bottom: 28px;
  border: 1px solid #e8e8e8;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.stat-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(24, 144, 255, 0.12);
}

.stat-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #ffffff;
  flex-shrink: 0;
}

.stat-icon-wrapper.purple {
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.25);
}

.stat-icon-wrapper.green {
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
  box-shadow: 0 4px 12px rgba(82, 196, 26, 0.3);
}

.stat-icon-wrapper.orange {
  background: linear-gradient(135deg, #fa8c16 0%, #ffa940 100%);
  box-shadow: 0 4px 12px rgba(250, 140, 22, 0.3);
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 26px;
  font-weight: 700;
  color: #2c3e50;
  line-height: 1;
  margin-bottom: 4px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.stat-text {
  font-size: 13px;
  color: #8c8c8c;
  font-weight: 500;
}

/* 漫画面板样式 */
.comic-panels {
  display: grid;
  gap: 32px;
  margin-top: 24px;
  padding: 8px;
}

.panel-item {
  background: #ffffff;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.panel-item:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 36px rgba(24, 144, 255, 0.15);
  border-color: rgba(24, 144, 255, 0.2);
}

.panel-header {
  padding: 20px 28px;
  background: #fafbfc;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-index {
  font-size: 18px;
  font-weight: 700;
  color: #262626;
  display: flex;
  align-items: center;
  gap: 10px;
}

.panel-index::before {
  content: '';
  width: 4px;
  height: 20px;
  background: linear-gradient(135deg, #1890ff 0%, #52c41a 100%);
  border-radius: 2px;
}

.panel-image {
  padding: 32px;
  background: #fafbfc;
  min-height: 450px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.image-debug-info {
  margin-bottom: 12px;
  width: 100%;
  text-align: center;
  position: relative;
  z-index: 1;
}

.image-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 320px;
  color: #667eea;
}

.image-loading .el-icon {
  font-size: 56px;
  margin-bottom: 18px;
  animation: rotating 2s linear infinite;
  filter: drop-shadow(0 4px 8px rgba(102, 126, 234, 0.3));
}

.image-loading p {
  font-size: 15px;
  color: #8c8c8c;
  font-weight: 500;
}

.panel-image :deep(.el-image) {
  width: 100%;
  max-width: 820px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  transition: all 0.4s ease;
  position: relative;
  z-index: 1;
}

.panel-image :deep(.el-image):hover {
  transform: scale(1.02);
  box-shadow: 0 12px 48px rgba(24, 144, 255, 0.2);
}

.panel-image :deep(.el-image__inner) {
  cursor: pointer;
  transition: opacity 0.3s ease;
}

.panel-image :deep(.el-image__inner):hover {
  opacity: 0.95;
}

.image-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 320px;
  color: #909399;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  padding: 32px;
}

.image-error .el-icon {
  font-size: 72px;
  margin-bottom: 18px;
  opacity: 0.6;
}

.image-error p {
  margin: 12px 0;
  font-size: 15px;
  color: #666;
}

.image-error .el-button {
  margin-top: 16px;
  border-radius: 8px;
}

.panel-info {
  padding: 20px 24px 24px;
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
}

.panel-info :deep(.el-collapse) {
  border: none;
}

.panel-info :deep(.el-collapse-item__header) {
  font-size: 15px;
  font-weight: 600;
  color: #262626;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 10px;
  border: 1px solid #e8e8e8;
  margin-bottom: 0;
}

.panel-info :deep(.el-collapse-item__wrap) {
  border: none;
  background: transparent;
}

.panel-info :deep(.el-collapse-item__content) {
  padding: 16px;
}

.detail-section {
  padding: 12px 0;
}

.detail-item {
  margin: 14px 0;
  display: flex;
  align-items: center;
  padding: 10px 14px;
  background: rgba(248, 249, 250, 0.6);
  border-radius: 10px;
  transition: all 0.3s ease;
}

.detail-item:hover {
  background: #e6f7ff;
}

.detail-label {
  font-weight: 700;
  color: #495057;
  margin-right: 12px;
  min-width: 110px;
  font-size: 14px;
}

.prompt-text {
  font-size: 13px;
  color: #595959;
  line-height: 1.9;
  margin: 12px 0 0 0;
  padding: 18px 20px;
  background: #f5f7fa;
  border-radius: 12px;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 220px;
  overflow-y: auto;
  border-left: 4px solid #1890ff;
  box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.03);
  border: 1px solid #e8e8e8;
  border-left-width: 4px;
}

.meta-info {
  margin-top: 12px;
  font-size: 13px;
  color: #8c8c8c;
}

/* 错误提示样式 */
.error-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin-bottom: 20px;
}

.debug-info {
  margin-top: 20px;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}

.debug-content {
  text-align: left;
  background: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.8;
}

.debug-content p {
  margin: 8px 0;
}

.debug-content strong {
  color: #303133;
  font-weight: 600;
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

/* 滚动条美化 */
.prompt-text::-webkit-scrollbar {
  width: 6px;
}

.prompt-text::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 3px;
}

.prompt-text::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, #1890ff 0%, #52c41a 100%);
  border-radius: 3px;
}

.prompt-text::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg, #40a9ff 0%, #73d13d 100%);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .preview-container {
    padding: 12px;
  }
  
  .preview-content {
    margin-top: 16px;
  }
  
  .panel-image {
    min-height: 280px;
    padding: 20px;
  }
  
  .preview-header h2 {
    font-size: 22px;
  }
  
  .panel-header {
    padding: 14px 18px;
  }
  
  .panel-info {
    padding: 16px 18px 20px;
  }
  
  .error-actions {
    flex-direction: column;
  }
  
  .error-actions .el-button {
    width: 100%;
  }
  
  .comic-stats {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 20px;
  }
  
  .stat-item {
    padding: 14px;
  }
  
  .stat-icon-wrapper {
    width: 42px;
    height: 42px;
    font-size: 20px;
  }
  
  .stat-number {
    font-size: 22px;
  }
}
</style>

