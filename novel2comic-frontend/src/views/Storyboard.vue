<template>
  <div class="storyboard-container">
    <el-page-header @back="router.back()" content="分镜预览" />
    
    <div class="storyboard-content">
      <!-- 统计卡片区域 -->
      <div v-if="!loading && storyboardList && storyboardList.length > 0" class="stats-section">
        <div class="stat-card">
          <div class="stat-card-header">
            <div class="stat-icon primary">
              <span style="color: white;">📊</span>
            </div>
            <div class="stat-info">
              <h4>分镜总数</h4>
              <p>{{ storyboardList.length }}</p>
            </div>
          </div>
        </div>
        
        <div class="stat-card">
          <div class="stat-card-header">
            <div class="stat-icon success">
              <span style="color: white;">✅</span>
            </div>
            <div class="stat-info">
              <h4>已完成</h4>
              <p>{{ storyboardList.length }}</p>
            </div>
          </div>
        </div>
        
        <div class="stat-card">
          <div class="stat-card-header">
            <div class="stat-icon warning">
              <span style="color: white;">🎨</span>
            </div>
            <div class="stat-info">
              <h4>选择风格</h4>
              <p style="font-size: 16px;">{{ getStyleName(comicStyle) }}</p>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 头部卡片 -->
      <el-card class="header-card">
        <template #header>
          <div class="header-content">
            <div>
              <h2>🎬 分镜脚本预览</h2>
              <p>AI已自动生成分镜脚本，您可以进行调整</p>
            </div>
            <div class="header-actions">
              <el-button type="success" :icon="Plus" @click="handleAddPanel">
                添加分镜
              </el-button>
            </div>
          </div>
        </template>
        
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-state">
          <el-skeleton :rows="3" animated />
        </div>
        
        <!-- 空状态 -->
        <el-empty 
          v-else-if="!storyboardList || storyboardList.length === 0" 
          description="暂无分镜数据，请先生成分镜脚本"
        >
          <el-button type="primary" @click="handleGenerateStoryboard" :loading="generating">
            生成分镜脚本
          </el-button>
          <el-button @click="loadStoryboard" style="margin-left: 10px;">
            重新加载
          </el-button>
        </el-empty>
        
        <!-- 分镜列表 -->
        <div v-else class="storyboard-list">
          <div 
            v-for="(panel, index) in storyboardList" 
            :key="panel.id" 
            class="panel-item"
          >
            <div class="panel-header">
              <div class="panel-number">第 {{ index + 1 }} 格</div>
              <div class="panel-actions">
                <el-button 
                  type="primary" 
                  size="small" 
                  :icon="Edit"
                  @click="handleEditPanel(panel)"
                >
                  编辑
                </el-button>
                <el-button 
                  type="danger" 
                  size="small" 
                  :icon="Delete"
                  @click="handleDeletePanel(panel.id)"
                >
                  删除
                </el-button>
              </div>
            </div>
            
            <div class="panel-content">
              <div class="panel-field">
                <label>场景描述：</label>
                <div class="field-value">{{ panel.descriptionCn || '暂无' }}</div>
              </div>
              
              <div class="panel-field">
                <label>对话内容：</label>
                <div class="field-value">{{ panel.dialogueText || '暂无对话' }}</div>
              </div>
              
              <div class="panel-field">
                <label>环境描述：</label>
                <div class="field-value">{{ panel.environment || '暂无' }}</div>
              </div>
              
              <div class="panel-field">
                <label>镜头类型：</label>
                <div class="field-value">{{ panel.shotTypeDesc || panel.shotType || '暂无' }}</div>
              </div>
              
              <div class="panel-field">
                <label>场景类型：</label>
                <div class="field-value">{{ panel.sceneTypeDesc || panel.sceneType || '暂无' }}</div>
              </div>
              
              <div class="panel-field">
                <label>情绪氛围：</label>
                <div class="field-value">{{ panel.mood || '暂无' }}</div>
              </div>
            </div>
          </div>
        </div>
      </el-card>
      
      <!-- 生成设置卡片 -->
      <el-card v-if="storyboardList && storyboardList.length > 0" class="generate-card">
        <template #header>
          <h3>⚙️ 生成设置</h3>
        </template>
        
        <div class="generate-settings">
          <div class="setting-item">
            <label class="setting-label">漫画风格：</label>
            <el-radio-group v-model="comicStyle" class="style-group">
              <el-radio-button label="japanese">
                🎌 日系漫画
              </el-radio-button>
              <el-radio-button label="chinese">
                🏮 国风漫画
              </el-radio-button>
              <el-radio-button label="realistic">
                📷 写实风格
              </el-radio-button>
            </el-radio-group>
          </div>
          
          <div class="setting-item">
            <label class="setting-label">分镜数量：</label>
            <el-tag type="info" size="large">
              共 {{ storyboardList.length }} 个分镜
            </el-tag>
          </div>
        </div>
        
        <div class="generate-actions">
          <el-button 
            type="primary" 
            size="large"
            :icon="VideoPlay"
            :loading="generating"
            @click="handleGenerate"
          >
            确认生成漫画
          </el-button>
        </div>
      </el-card>
    </div>
    
    <!-- 编辑/添加分镜对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      :title="isNewPanel ? '✨ 添加新分镜' : '✏️ 编辑分镜'"
      width="700px"
      @close="handleDialogClose"
      :close-on-click-modal="false"
      class="storyboard-dialog"
    >
      <div class="dialog-tips">
        <el-icon><InfoFilled /></el-icon>
        <span>{{ isNewPanel ? '请填写新分镜的详细信息，帮助AI更准确地生成画面' : '修改分镜内容将影响最终生成的漫画画面' }}</span>
      </div>
      
      <el-form :model="editForm" label-width="120px" class="storyboard-form">
        <el-form-item class="form-item-with-icon">
          <template #label>
            <div class="label-with-icon">
              <el-icon color="#3b82f6"><Picture /></el-icon>
              <span>场景描述</span>
            </div>
          </template>
          <el-input
            v-model="editForm.descriptionCn"
            type="textarea"
            :rows="4"
            placeholder="请描述场景环境、时间、地点等，例如：清晨的学校教室，阳光透过窗户洒进来"
            maxlength="500"
            show-word-limit
          />
          <div class="field-hint">💡 详细的场景描述有助于生成更准确的画面</div>
        </el-form-item>
        
        <el-form-item class="form-item-with-icon">
          <template #label>
            <div class="label-with-icon">
              <el-icon color="#10b981"><ChatDotRound /></el-icon>
              <span>对话内容</span>
            </div>
          </template>
          <el-input
            v-model="editForm.dialogueText"
            type="textarea"
            :rows="3"
            placeholder="请输入角色对话内容，例如：小明：早上好啊！"
            maxlength="300"
            show-word-limit
          />
          <div class="field-hint">💬 对话会以气泡形式显示在漫画中</div>
        </el-form-item>
        
        <el-form-item class="form-item-with-icon">
          <template #label>
            <div class="label-with-icon">
              <el-icon color="#f59e0b"><Location /></el-icon>
              <span>环境描述</span>
            </div>
          </template>
          <el-input
            v-model="editForm.environment"
            type="textarea"
            :rows="2"
            placeholder="请描述具体环境，例如：教室、操场、图书馆等"
            maxlength="200"
            show-word-limit
          />
          <div class="field-hint">🌍 环境信息帮助构建更真实的场景</div>
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item class="form-item-with-icon">
              <template #label>
                <div class="label-with-icon">
                  <el-icon color="#8b5cf6"><VideoCamera /></el-icon>
                  <span>镜头类型</span>
                </div>
              </template>
              <el-select 
                v-model="editForm.shotType" 
                placeholder="请选择镜头类型"
                style="width: 100%"
              >
                <el-option label="特写 (近景)" value="close-up" />
                <el-option label="中景" value="medium" />
                <el-option label="全景 (远景)" value="full" />
                <el-option label="仰视角度" value="low-angle" />
                <el-option label="俯视角度" value="high-angle" />
              </el-select>
            </el-form-item>
          </el-col>
          
          <el-col :span="12">
            <el-form-item class="form-item-with-icon">
              <template #label>
                <div class="label-with-icon">
                  <el-icon color="#ec4899"><Sunny /></el-icon>
                  <span>情绪氛围</span>
                </div>
              </template>
              <el-select 
                v-model="editForm.mood" 
                placeholder="请选择情绪氛围"
                style="width: 100%"
              >
                <el-option label="明亮愉悦" value="bright" />
                <el-option label="阴暗压抑" value="dark" />
                <el-option label="紧张刺激" value="tense" />
                <el-option label="温馨温暖" value="warm" />
                <el-option label="平静中性" value="neutral" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item class="form-item-with-icon">
          <template #label>
            <div class="label-with-icon">
              <el-icon color="#06b6d4"><Film /></el-icon>
              <span>场景类型</span>
            </div>
          </template>
          <el-select 
            v-model="editForm.sceneType" 
            placeholder="请选择场景类型"
            style="width: 100%"
          >
            <el-option label="对话场景" value="dialogue" />
            <el-option label="动作场景" value="action" />
            <el-option label="环境描写" value="environment" />
            <el-option label="高潮场景" value="climax" />
          </el-select>
          <div class="field-hint">🎬 不同场景类型会影响画面的表现方式</div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button size="large" @click="editDialogVisible = false">
            <el-icon><Close /></el-icon>
            取消
          </el-button>
          <el-button 
            type="primary" 
            size="large"
            :loading="saving" 
            @click="handleSavePanel"
          >
            <el-icon><Check /></el-icon>
            {{ isNewPanel ? '添加分镜' : '保存修改' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Plus, Edit, Delete, VideoPlay, 
  InfoFilled, Picture, ChatDotRound, Location, 
  VideoCamera, Sunny, Film, Close, Check 
} from '@element-plus/icons-vue'
import { getStoryboardList, updateStoryboardPanel, deleteStoryboardPanel } from '@/api/storyboard'
import { generateComic } from '@/api/comic'

const router = useRouter()
const route = useRoute()

// 获取小说ID
const novelId = ref(route.params.novelId)

// 数据状态
const loading = ref(false)
const generating = ref(false)
const saving = ref(false)
const storyboardList = ref([])
const comicStyle = ref('japanese')

// 编辑对话框
const editDialogVisible = ref(false)
const isNewPanel = ref(false)
const editForm = ref({
  id: null,
  descriptionCn: '',
  dialogueText: '',
  environment: '',
  shotType: 'medium',
  sceneType: 'dialogue',
  mood: 'neutral'
})

// 加载分镜列表
const loadStoryboard = async () => {
  loading.value = true
  try {
    const response = await getStoryboardList(novelId.value)
    if (response.code === 200) {
      // 后端返回的是 StoryboardResponse 对象，数组在 panels 字段中
      const panels = response.data?.panels || []
      // 过滤掉可能的 null 值，确保数据有效性
      storyboardList.value = panels.filter(item => item !== null && item !== undefined)
      console.log('加载分镜列表成功:', storyboardList.value.length, '个分镜')
    } else {
      ElMessage.error(response.message || '加载分镜失败')
    }
  } catch (error) {
    console.error('加载分镜失败:', error)
    ElMessage.error('加载分镜失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 生成分镜脚本
const handleGenerateStoryboard = async () => {
  try {
    await ElMessageBox.confirm(
      '即将为该小说生成分镜脚本，是否继续？',
      '确认生成',
      {
        confirmButtonText: '确认生成',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    generating.value = true
    
    try {
      const { generateStoryboard } = await import('@/api/storyboard')
      const response = await generateStoryboard(novelId.value)
      
      if (response.code === 200) {
        ElMessage.success('分镜脚本生成成功！')
        // 重新加载分镜列表
        await loadStoryboard()
      } else {
        ElMessage.error(response.message || '分镜生成失败')
      }
    } catch (error) {
      console.error('生成分镜失败:', error)
      ElMessage.error('生成分镜失败，请稍后重试')
    } finally {
      generating.value = false
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
    }
  }
}

// 编辑分镜
const handleEditPanel = (panel) => {
  isNewPanel.value = false
  editForm.value = {
    id: panel.id,
    descriptionCn: panel.descriptionCn || '',
    dialogueText: panel.dialogueText || '',
    environment: panel.environment || '',
    shotType: panel.shotType || 'medium',
    sceneType: panel.sceneType || 'dialogue',
    mood: panel.mood || 'neutral'
  }
  editDialogVisible.value = true
}

// 添加分镜
const handleAddPanel = () => {
  isNewPanel.value = true
  editForm.value = {
    id: null,
    descriptionCn: '',
    dialogueText: '',
    environment: '',
    shotType: 'medium',
    sceneType: 'dialogue',
    mood: 'neutral'
  }
  editDialogVisible.value = true
}

// 保存分镜
const handleSavePanel = async () => {
  if (isNewPanel.value) {
    // 添加新分镜（暂时添加到本地列表）
    storyboardList.value.push({
      id: Date.now(), // 临时ID
      ...editForm.value,
      panelIndex: storyboardList.value.length
    })
    ElMessage.success('分镜添加成功')
    editDialogVisible.value = false
    return
  }
  
  // 更新现有分镜
  saving.value = true
  try {
    const response = await updateStoryboardPanel(editForm.value.id, editForm.value)
    if (response.code === 200) {
      ElMessage.success('分镜更新成功')
      editDialogVisible.value = false
      // 重新加载列表
      await loadStoryboard()
    } else {
      ElMessage.error(response.message || '更新失败')
    }
  } catch (error) {
    console.error('更新分镜失败:', error)
    ElMessage.error('更新失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

// 删除分镜
const handleDeletePanel = async (panelId) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这个分镜吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const response = await deleteStoryboardPanel(panelId)
    if (response.code === 200) {
      ElMessage.success('分镜删除成功')
      // 重新加载列表
      await loadStoryboard()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除分镜失败:', error)
      ElMessage.error('删除失败，请稍后重试')
    }
  }
}

// 对话框关闭
const handleDialogClose = () => {
  editForm.value = {
    id: null,
    descriptionCn: '',
    dialogueText: '',
    environment: '',
    shotType: 'medium',
    sceneType: 'dialogue',
    mood: 'neutral'
  }
}

// 生成漫画
const handleGenerate = async () => {
  if (!storyboardList.value || storyboardList.value.length === 0) {
    ElMessage.warning('请先添加分镜')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `即将生成 ${storyboardList.value.length} 个分镜的漫画，风格为${getStyleName(comicStyle.value)}，确认继续吗？`,
      '确认生成',
      {
        confirmButtonText: '确认生成',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    generating.value = true
    const response = await generateComic({
      novelId: novelId.value,
      style: comicStyle.value,
      panelCount: storyboardList.value.length
    })
    
    if (response.code === 200) {
      ElMessage.success('漫画生成任务已创建')
      // 跳转到进度页面
      router.push(`/progress/${response.data.taskId}`)
    } else {
      ElMessage.error(response.message || '生成失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('生成漫画失败:', error)
      ElMessage.error('生成失败，请稍后重试')
    }
  } finally {
    generating.value = false
  }
}

// 获取风格名称
const getStyleName = (style) => {
  const styleMap = {
    japanese: '日系漫画',
    chinese: '国风漫画',
    realistic: '写实风格'
  }
  return styleMap[style] || style
}

// 页面挂载时加载数据
onMounted(() => {
  loadStoryboard()
})
</script>

<style scoped>
.storyboard-container {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 24px;
}

.storyboard-content {
  max-width: 1400px;
  margin: 0 auto;
}

/* 头部卡片 */
.header-card {
  margin-bottom: 24px;
  border-radius: 16px;
  border: 1px solid #e8ecf0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  background: #ffffff;
}

.header-card :deep(.el-card__header) {
  border-bottom: 1px solid #f0f2f5;
  padding: 24px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content h2 {
  margin: 0 0 8px;
  color: #1f2937;
  font-size: 22px;
  font-weight: 600;
}

.header-content p {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.header-actions .el-button {
  border-radius: 8px;
  padding: 10px 20px;
  font-weight: 500;
}

/* 加载状态 */
.loading-state {
  padding: 40px 20px;
}

/* 分镜列表 */
.storyboard-list {
  display: grid;
  gap: 16px;
}

.panel-item {
  background: #ffffff;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
  border: 1px solid #e8ecf0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04);
}

.panel-item:hover {
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
  transform: translateY(-2px);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.panel-number {
  color: #ffffff;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-number::before {
  content: '📋';
  font-size: 20px;
}

.panel-actions {
  display: flex;
  gap: 8px;
}

.panel-actions .el-button {
  border-radius: 6px;
  font-size: 13px;
}

.panel-content {
  padding: 24px;
  background: #fafbfc;
}

.panel-field {
  margin-bottom: 20px;
  background: #ffffff;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e8ecf0;
  transition: all 0.2s ease;
}

.panel-field:hover {
  border-color: #d1d5db;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.panel-field:last-child {
  margin-bottom: 0;
}

.panel-field label {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #374151;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.panel-field label::before {
  content: '';
  width: 3px;
  height: 14px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border-radius: 2px;
}

.field-value {
  color: #1f2937;
  font-size: 14px;
  line-height: 1.8;
  padding: 0;
  background: transparent;
  border: none;
  white-space: pre-wrap;
  min-height: 24px;
}

/* 生成设置卡片 */
.generate-card {
  margin-top: 24px;
  border-radius: 16px;
  border: 1px solid #e8ecf0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  background: #ffffff;
}

.generate-card :deep(.el-card__header) {
  border-bottom: 1px solid #f0f2f5;
  padding: 24px;
  background: linear-gradient(135deg, #fafbfc 0%, #f3f4f6 100%);
}

.generate-card h3 {
  margin: 0;
  color: #1f2937;
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.generate-settings {
  padding: 24px;
}

.setting-item {
  margin-bottom: 28px;
}

.setting-item:last-child {
  margin-bottom: 0;
}

.setting-label {
  display: block;
  color: #374151;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 16px;
}

.style-group {
  width: 100%;
  display: flex;
  gap: 12px;
}

.style-group :deep(.el-radio-button) {
  flex: 1;
}

.style-group :deep(.el-radio-button__inner) {
  width: 100%;
  padding: 16px 24px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 10px;
  border: 2px solid #e5e7eb;
  background: #ffffff;
  color: #6b7280;
  transition: all 0.3s ease;
}

.style-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border-color: #3b82f6;
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.style-group :deep(.el-radio-button__inner:hover) {
  border-color: #3b82f6;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.generate-actions {
  margin-top: 32px;
  text-align: center;
  padding-top: 24px;
  border-top: 1px solid #e8ecf0;
}

.generate-actions .el-button {
  min-width: 240px;
  font-size: 16px;
  font-weight: 600;
  padding: 16px 32px;
  border-radius: 12px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
  transition: all 0.3s ease;
}

.generate-actions .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(16, 185, 129, 0.4);
}

.generate-actions .el-button:active {
  transform: translateY(0);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .storyboard-container {
    padding: 16px;
  }
  
  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .panel-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .style-group {
    flex-direction: column;
    gap: 8px;
  }
  
  .style-group :deep(.el-radio-button__inner) {
    padding: 14px 20px;
  }
  
  .generate-actions .el-button {
    width: 100%;
    min-width: auto;
  }
}

/* 新增：统计卡片样式 */
.stats-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #e8ecf0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.stat-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-icon.primary {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
}

.stat-icon.success {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.stat-icon.warning {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

.stat-info h4 {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
  font-weight: 500;
}

.stat-info p {
  margin: 4px 0 0;
  color: #1f2937;
  font-size: 24px;
  font-weight: 700;
}

/* 对话框样式优化 */
.storyboard-dialog :deep(.el-dialog) {
  border-radius: 16px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.15);
}

.storyboard-dialog :deep(.el-dialog__header) {
  padding: 24px 24px 16px;
  border-bottom: 1px solid #f0f2f5;
  background: linear-gradient(135deg, #fafbfc 0%, #f3f4f6 100%);
}

.storyboard-dialog :deep(.el-dialog__title) {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
}

.storyboard-dialog :deep(.el-dialog__body) {
  padding: 24px;
  max-height: 600px;
  overflow-y: auto;
}

.storyboard-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px 24px;
  border-top: 1px solid #f0f2f5;
}

/* 对话框提示信息 */
.dialog-tips {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border-left: 4px solid #3b82f6;
  border-radius: 8px;
  margin-bottom: 24px;
  color: #1e40af;
  font-size: 14px;
}

.dialog-tips .el-icon {
  font-size: 18px;
  color: #3b82f6;
}

/* 表单样式 */
.storyboard-form {
  padding: 0;
}

.form-item-with-icon :deep(.el-form-item__label) {
  padding: 0;
}

.label-with-icon {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.label-with-icon .el-icon {
  font-size: 16px;
}

.storyboard-form :deep(.el-textarea__inner) {
  border-radius: 8px;
  border: 2px solid #e5e7eb;
  padding: 12px;
  font-size: 14px;
  line-height: 1.6;
  transition: all 0.3s ease;
}

.storyboard-form :deep(.el-textarea__inner:focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.storyboard-form :deep(.el-select) {
  width: 100%;
}

.storyboard-form :deep(.el-input__inner) {
  border-radius: 8px;
  border: 2px solid #e5e7eb;
  padding: 12px;
  font-size: 14px;
  transition: all 0.3s ease;
}

.storyboard-form :deep(.el-input__inner:focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.storyboard-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.field-hint {
  margin-top: 8px;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
}

/* 对话框底部 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.dialog-footer .el-button {
  min-width: 120px;
  border-radius: 8px;
  font-weight: 500;
  padding: 12px 24px;
}

.dialog-footer .el-button--primary {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.dialog-footer .el-button--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.4);
}

/* 字数限制提示 */
.storyboard-form :deep(.el-input__count) {
  background: transparent;
  color: #9ca3af;
  font-size: 12px;
}
</style>

