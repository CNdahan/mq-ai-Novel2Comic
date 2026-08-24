<template>
  <div class="upload-container">
    <el-page-header @back="router.push('/home')" content="上传小说" />

    <div class="upload-content">
      <el-card>
        <template #header>
          <h2>📝 上传小说，开始创作</h2>
        </template>

        <el-form
          ref="uploadFormRef"
          :model="uploadForm"
          :rules="rules"
          label-width="120px"
          size="large"
        >
          <el-form-item label="小说标题" prop="novelTitle">
            <el-input
              v-model="uploadForm.novelTitle"
              placeholder="请输入小说标题"
              clearable
            />
          </el-form-item>

          <el-form-item label="小说内容" prop="novelContent">
            <el-input
              v-model="uploadForm.novelContent"
              type="textarea"
              :rows="10"
              placeholder="请输入小说文本（300-3000字）"
              show-word-limit
              maxlength="3000"
            />
          </el-form-item>

          <el-form-item>
            <div class="word-count">
              字数统计: {{ uploadForm.novelContent.length }} / 3000
            </div>
          </el-form-item>

          <el-divider>或者</el-divider>

          <el-form-item label="每日推荐小说">
            <div class="recommend-container">
              <div v-if="recommendLoading" class="loading-container">
                <el-icon class="is-loading"><i-ep-loading /></el-icon>
                <span>加载推荐小说中...</span>
              </div>
              <div v-else class="recommend-list">
                <div
                  v-for="(novel, index) in recommendations"
                  :key="index"
                  :class="['novel-card', { 'selected': selectedNovelIndex === index }]"
                  @click="selectNovel(index)"
                >
                  <div class="novel-header">
                    <h3>{{ novel.title }}</h3>
                    <el-tag type="info" size="small">{{ novel.sourceType }}</el-tag>
                  </div>
                  <div class="novel-content">
                    {{ novel.content.substring(0, 100) }}...
                  </div>
                  <div class="novel-footer">
                    <span class="word-count">{{ novel.characterCount }}字</span>
                    <span class="recommendation">{{ novel.recommendation }}</span>
                  </div>
                </div>
              </div>
            </div>
          </el-form-item>

          <el-form-item label="风格选择" prop="style">
            <el-radio-group v-model="uploadForm.style">
              <el-radio label="japanese">日式漫画</el-radio>
              <el-radio label="chinese">国风漫画</el-radio>
              <el-radio label="realistic">写实风格</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              @click="handleSubmit"
            >
              🚀 开始生成
            </el-button>
            <el-button size="large" @click="handleReset">
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useComicStore } from '@/store/comic'
import { uploadNovel, getDailyRecommendations } from '@/api/novel'
import { ElMessage } from 'element-plus'

const router = useRouter()
const comicStore = useComicStore()

const uploadFormRef = ref(null)
const loading = ref(false)
const recommendLoading = ref(false)
const recommendations = ref([])
const selectedNovelIndex = ref(null)

const uploadForm = reactive({
  novelTitle: '',
  novelContent: '',
  sourceType: 'direct',
  style: 'japanese'
})

const rules = {
  novelContent: [
    { required: true, message: '请输入小说内容', trigger: 'blur' },
    { min: 300, max: 3000, message: '小说内容长度为300-3000字', trigger: 'blur' }
  ],
  style: [
    { required: true, message: '请选择风格', trigger: 'change' }
  ]
}

// 组件挂载时加载推荐小说
onMounted(() => {
  loadRecommendations()
})

// 加载推荐小说
const loadRecommendations = async () => {
  recommendLoading.value = true
  try {
    const response = await getDailyRecommendations()
    recommendations.value = response.data
  } catch (error) {
    console.error('加载推荐小说失败:', error)
    ElMessage.error('加载推荐小说失败')
  } finally {
    recommendLoading.value = false
  }
}

// 选择推荐小说
const selectNovel = (index) => {
  selectedNovelIndex.value = index
  const novel = recommendations.value[index]
  uploadForm.novelTitle = novel.title
  uploadForm.novelContent = novel.content
  uploadForm.sourceType = 'recommend'
  ElMessage.success(`已选择小说：${novel.title}`)
}

// 提交表单
const handleSubmit = async () => {
  if (!uploadFormRef.value) return

  await uploadFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const response = await uploadNovel({
          title: uploadForm.novelTitle,
          content: uploadForm.novelContent,
          sourceType: uploadForm.sourceType
        })

        ElMessage.success('小说上传成功')

        // 保存小说信息到store
        comicStore.setCurrentNovel(response.data)

        // 跳转到角色确认页面
        router.push(`/character/${response.data.novelId}`)
      } catch (error) {
        console.error('上传失败:', error)
      } finally {
        loading.value = false
      }
    }
  })
}

// 重置表单
const handleReset = () => {
  uploadFormRef.value?.resetFields()
  uploadForm.novelContent = ''
}
</script>

<style scoped>
.upload-container {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 40px;
}

.upload-content {
  max-width: 1000px;
  margin: 20px auto;
}

.el-card :deep(.el-card__header) h2 {
  margin: 0;
  text-align: center;
  color: #303133;
}

.word-count {
  color: #909399;
  font-size: 14px;
}

.upload-demo {
  width: 100%;
}

.recommend-container {
  width: 100%;
  min-height: 200px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #909399;
}

.loading-container .el-icon {
  font-size: 32px;
  margin-bottom: 10px;
}

.recommend-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  width: 100%;
}

.novel-card {
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s;
  background-color: #fff;
}

.novel-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.2);
  transform: translateY(-2px);
}

.novel-card.selected {
  border-color: #409eff;
  background-color: #ecf5ff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.3);
}

.novel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.novel-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  margin-right: 8px;
}

.novel-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 12px;
  min-height: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.novel-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
}

.novel-footer .word-count {
  font-weight: 500;
}

.novel-footer .recommendation {
  color: #409eff;
  font-weight: 500;
}

@media (max-width: 1200px) {
  .recommend-list {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .recommend-list {
    grid-template-columns: 1fr;
  }
}
</style>

