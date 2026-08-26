<template>
  <div class="character-container">
    <el-page-header @back="router.back()" content="角色确认" />

    <div class="character-content">
      <el-card v-loading="loading" class="main-card">
        <template #header>
          <div class="card-header">
            <div>
              <h2>👥 确认角色信息</h2>
              <p>AI已为您识别出以下角色，请确认或编辑</p>
            </div>
            <el-tag type="success">共 {{ characters.length }} 个角色</el-tag>
          </div>
        </template>

        <!-- 角色列表 -->
        <div v-if="characters.length > 0" class="character-list">
          <el-row :gutter="20">
            <el-col
              v-for="(character, index) in characters"
              :key="character.characterId"
              :xs="24"
              :sm="12"
              :md="8"
              :lg="6"
            >
              <el-card shadow="hover" class="character-card">
                <!-- 卡片头部 -->
                <div class="card-header-section">
                  <div class="character-avatar">
                    <el-icon :size="40" color="#409EFF">
                      <User />
                    </el-icon>
                  </div>
                  <div class="character-title">
                    <div class="character-badge">{{ index + 1 }}</div>
                    <h3 class="character-name">{{ character.characterName }}</h3>
                  </div>
                </div>
                <el-divider style="margin: 16px 0" />
                <!-- 角色信息 -->
                <div class="character-body">
                  <!-- 角色设定 -->
                  <div class="info-section">
                    <div class="info-label">
                      <el-icon :size="14"><Document /></el-icon>
                      <span>角色设定</span>
                    </div>
                    <div class="info-content" :class="{ expanded: expandedDesc[character.characterId] }">
                      {{ character.descriptionCn || '暂无描述' }}
                    </div>
                    <el-button
                      v-if="(character.descriptionCn || '').length > 80"
                      text
                      size="small"
                      @click="toggleDesc(character.characterId)"
                    >
                      {{ expandedDesc[character.characterId] ? '收起' : '展开' }}
                    </el-button>
                  </div>
                  <!-- AI提示词 -->
                  <div class="info-section">
                    <div class="info-label">
                      <el-icon :size="14"><ChatDotRound /></el-icon>
                      <span>AI提示词</span>
                    </div>
                    <div class="info-content mono" :class="{ expanded: expandedDescEn[character.characterId] }">
                      {{ character.descriptionEn || '暂无描述' }}
                    </div>
                    <el-button
                      v-if="(character.descriptionEn || '').length > 80"
                      text
                      size="small"
                      @click="toggleDescEn(character.characterId)"
                    >
                      {{ expandedDescEn[character.characterId] ? '收起' : '展开' }}
                    </el-button>
                  </div>
                  <!-- 外貌特征 -->
                  <div v-if="character.appearanceData && character.appearanceData.length > 0" class="info-section">
                    <div class="info-label">
                      <el-icon :size="14"><View /></el-icon>
                      <span>外貌特征</span>
                    </div>
                    <div class="tags-wrapper">
                      <el-tag
                        v-for="(item, idx) in character.appearanceData"
                        :key="idx"
                        size="small"
                        class="feature-tag"
                      >
                        {{ item }}
                      </el-tag>
                    </div>
                  </div>
                </div>
                <!-- 操作按钮 -->
                <div class="card-footer-section">
                  <el-button
                    size="small"
                    :icon="Edit"
                    @click="handleEdit(character)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    size="small"
                    :icon="Delete"
                    @click="handleDelete(character)"
                    type="danger"
                    text
                  >
                    删除
                  </el-button>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>

        <!-- 空状态 -->
        <el-empty
          v-else
          :description="extractFailed ? '角色提取失败，请检查 AI 配置后重试' : '暂无角色信息'"
          :image-size="200"
        >
          <el-button
            v-if="extractFailed"
            type="primary"
            :icon="Refresh"
            :loading="retrying"
            @click="handleRetryExtraction"
          >
            重新提取角色
          </el-button>
        </el-empty>

        <!-- 底部操作 -->
        <div class="footer-actions">
          <el-button @click="router.back()">
            返回上一步
          </el-button>
          <el-button
            type="primary"
            :disabled="characters.length === 0"
            @click="handleNext"
          >
            确认并继续生成分镜
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑角色信息"
      width="600px"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        label-width="100px"
        :rules="editRules"
      >
        <el-form-item label="角色名称">
          <el-input v-model="editForm.characterName" disabled />
        </el-form-item>

        <el-form-item label="中文描述" prop="descriptionCn">
          <el-input
            v-model="editForm.descriptionCn"
            type="textarea"
            :rows="3"
            placeholder="请输入中文描述"
          />
        </el-form-item>

        <el-form-item label="英文描述" prop="descriptionEn">
          <el-input
            v-model="editForm.descriptionEn"
            type="textarea"
            :rows="3"
            placeholder="请输入英文描述（用于图片生成）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getCharacterList, updateCharacter, deleteCharacter } from '@/api/character'
import { retryCharacterExtraction } from '@/api/novel'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import {
  User,
  Edit,
  Delete,
  ArrowRight,
  StarFilled,
  Document,
  ChatDotRound,
  View,
  CollectionTag,
  ArrowDown,
  ArrowUp,
  Refresh
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

// 状态
const loading = ref(false)
const characters = ref([])
const editDialogVisible = ref(false)
const saving = ref(false)
const retrying = ref(false)
const extractFailed = ref(false)
const editFormRef = ref(null)
const editForm = ref({
  characterId: null,
  characterName: '',
  descriptionCn: '',
  descriptionEn: '',
  appearanceData: null
})

// 展开/折叠状态
const expandedDesc = reactive({})
const expandedDescEn = reactive({})

// 切换描述展开状态
const toggleDesc = (characterId) => {
  expandedDesc[characterId] = !expandedDesc[characterId]
}

const toggleDescEn = (characterId) => {
  expandedDescEn[characterId] = !expandedDescEn[characterId]
}

// 获取标签类型（用于外貌特征的不同颜色）
const getTagType = (key) => {
  const types = ['', 'success', 'info', 'warning', 'danger']
  // 将 key 转换为字符串（key 可能是数字索引）
  const keyStr = String(key)
  const hash = keyStr.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0)
  return types[hash % types.length]
}

// 表单验证规则
const editRules = {
  descriptionCn: [
    { required: true, message: '请输入中文描述', trigger: 'blur' }
  ],
  descriptionEn: [
    { required: true, message: '请输入英文描述', trigger: 'blur' }
  ]
}

// 获取角色列表
const loadCharacters = async () => {
  const novelId = route.params.novelId
  if (!novelId) {
    ElMessage.error('缺少小说ID')
    router.back()
    return
  }

  loading.value = true
  try {
    console.log('🔍 开始获取角色列表，novelId:', novelId)
    const response = await getCharacterList(novelId)
    console.log('📦 API响应 - code:', response?.code, 'message:', response?.message, 'dataLength:', response?.data?.length)

    // 确保响应数据结构正确
    if (response && response.data) {
      characters.value = Array.isArray(response.data) ? response.data : []
      extractFailed.value = false
      console.log('✅ 角色列表加载成功，数量:', characters.value.length)

      // 打印角色名称列表（不打印完整对象）
      if (characters.value.length > 0) {
        const names = characters.value.map(c => c.characterName).join(', ')
        console.log('👥 角色名称:', names)
        ElMessage.success(`成功加载${characters.value.length}个角色`)
      } else {
        ElMessage.warning('未识别到角色信息')
      }
    } else {
      console.error('❌ 响应数据格式错误，response:', typeof response)
      ElMessage.error('数据格式错误')
      characters.value = []
    }
  } catch (error) {
    console.error('获取角色列表失败:', error)
    console.error('错误详情:', error.response || error.message)
    // 显示更详细的错误信息
    const errorMessage = error.response?.data?.message || error.response?.statusText || error.message || ''
    extractFailed.value = errorMessage.includes('角色提取失败')
    if (error.response) {
      ElMessage.error(`获取角色列表失败: ${errorMessage}`)
    } else if (error.request) {
      ElMessage.error('网络请求失败，请检查网络连接')
    } else {
      ElMessage.error(`获取角色列表失败: ${error.message}`)
    }
    characters.value = []
  } finally {
    loading.value = false
  }
}

// 角色提取失败后，直接使用已保存的小说内容重试。
const handleRetryExtraction = async () => {
  const novelId = route.params.novelId
  retrying.value = true
  try {
    await retryCharacterExtraction(novelId)
    ElMessage.success('角色重新提取成功')
    await loadCharacters()
  } catch (error) {
    const message = error.response?.data?.message || error.message || '角色重新提取失败，请重试'
    ElMessage.error(message)
  } finally {
    retrying.value = false
  }
}

// 编辑角色
const handleEdit = (character) => {
  editForm.value = {
    characterId: character.characterId,
    characterName: character.characterName,
    descriptionCn: character.descriptionCn || '',
    descriptionEn: character.descriptionEn || '',
    appearanceData: character.appearanceData
  }
  editDialogVisible.value = true
}

// 保存编辑
const handleSave = async () => {
  if (!editFormRef.value) return

  await editFormRef.value.validate(async (valid) => {
    if (!valid) return

    saving.value = true
    try {
      await updateCharacter(editForm.value.characterId, {
        descriptionCn: editForm.value.descriptionCn,
        descriptionEn: editForm.value.descriptionEn,
        appearanceData: editForm.value.appearanceData
      })

      ElMessage.success('角色信息更新成功')
      editDialogVisible.value = false

      // 重新加载角色列表
      await loadCharacters()
    } catch (error) {
      console.error('更新角色失败:', error)
      ElMessage.error('更新角色失败，请重试')
    } finally {
      saving.value = false
    }
  })
}

// 删除角色
const handleDelete = async (character) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除角色"${character.characterName}"吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    loading.value = true
    await deleteCharacter(character.characterId)
    ElMessage.success('角色删除成功')

    // 重新加载角色列表
    await loadCharacters()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除角色失败:', error)
      ElMessage.error('删除角色失败，请重试')
    }
  } finally {
    loading.value = false
  }
}

// 继续下一步
// 处理下一步 - 生成分镜
const handleNext = async () => {
  const novelId = route.params.novelId
  
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
    
    // 接口只负责创建后台任务，实际生成在进度页跟踪。
    const loadingInstance = ElLoading.service({
      lock: true,
      text: '正在创建分镜生成任务...',
      background: 'rgba(0, 0, 0, 0.7)'
    })
    
    try {
      // 调用生成分镜API
      const { generateStoryboard } = await import('@/api/storyboard')
      const response = await generateStoryboard(novelId)
      
      if (response.code === 200) {
        ElMessage.success('分镜生成任务已创建')
        router.push(`/progress/${response.data.taskId}`)
      } else {
        ElMessage.error(response.message || '分镜生成失败')
      }
    } catch (error) {
      console.error('生成分镜失败:', error)
      const message = error.response?.data?.message || error.message || '生成分镜失败，请稍后重试'
      ElMessage.error(message)
    } finally {
      loadingInstance.close()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
    }
  }
}

// 页面加载时获取数据
onMounted(() => {
  loadCharacters()
})
</script>

<style scoped>
.character-container {
  min-height: 100vh;
  background: var(--bg-primary);
  padding: 40px;
  transition: background-color 0.3s ease;
}

.character-content {
  max-width: 1600px;
  margin: 20px auto;
}

.main-card {
  background: var(--card-bg);
  transition: background-color 0.3s ease;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0 0 10px;
  color: var(--text-primary);
  font-size: 24px;
}

.card-header p {
  margin: 0;
  color: var(--text-tertiary);
  font-size: 14px;
}

/* 角色列表 */
.character-list {
  margin: 20px 0;
}

/* 角色卡片 */
.character-card {
  height: 100%;
  margin-bottom: 20px;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  transition: all 0.3s ease;
}

.character-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #409EFF;
}

[data-theme='dark'] .character-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

/* 卡片头部 */
.card-header-section {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0;
}

.character-avatar {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #E3F2FD 0%, #BBDEFB 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

[data-theme='dark'] .character-avatar {
  background: linear-gradient(135deg, #2d3748 0%, #3a4555 100%);
}

.character-title {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.character-badge {
  min-width: 24px;
  height: 24px;
  padding: 0 8px;
  background: #409EFF;
  color: #ffffff;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.character-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  flex: 1;
}

/* 角色信息体 */
.character-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

.info-content {
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-secondary);
  word-break: break-word;
  max-height: 60px;
  overflow: hidden;
  transition: max-height 0.3s ease;
}

.info-content.expanded {
  max-height: 600px;
}

.info-content.mono {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 12px;
  background: var(--bg-primary);
  padding: 8px 10px;
  border-radius: 4px;
  border: 1px solid var(--border-color);
}

.tags-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.feature-tag {
  border-radius: 4px;
  font-size: 12px;
}

/* 卡片底部 */
.card-footer-section {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  padding-top: 16px;
  margin-top: 16px;
  border-top: 1px solid var(--border-color);
}

/* 底部操作 */
.footer-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid var(--border-color);
}

/* 响应式 */
@media (max-width: 768px) {
  .character-container {
    padding: 20px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .footer-actions {
    flex-direction: column;
    gap: 10px;
  }

  .footer-actions button {
    width: 100%;
  }
}
</style>

