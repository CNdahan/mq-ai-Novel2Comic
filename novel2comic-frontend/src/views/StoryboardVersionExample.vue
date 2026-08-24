<template>
  <div class="version-manager">
    <!-- 版本选择器 -->
    <el-card class="version-selector-card">
      <template #header>
        <div class="card-header">
          <h3>📚 分镜版本管理</h3>
          <el-button type="primary" @click="handleGenerateNewVersion">
            生成新版本
          </el-button>
        </div>
      </template>

      <div class="version-content">
        <!-- 当前版本信息 -->
        <el-descriptions :column="4" border>
          <el-descriptions-item label="当前版本">
            版本 {{ currentVersion }}
          </el-descriptions-item>
          <el-descriptions-item label="分镜数">
            {{ currentVersionInfo?.panelCount || 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDate(currentVersionInfo?.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="版本说明">
            {{ currentVersionInfo?.versionNote || '无' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 版本列表 -->
        <div class="version-list">
          <el-table :data="versionList" style="width: 100%" v-loading="loading">
            <el-table-column prop="version" label="版本号" width="100">
              <template #default="{ row }">
                <el-tag :type="row.isCurrent ? 'success' : 'info'">
                  版本 {{ row.version }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column prop="versionNote" label="版本说明" />

            <el-table-column prop="panelCount" label="分镜数" width="100" />

            <el-table-column prop="createTime" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.createTime) }}
              </template>
            </el-table-column>

            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.isCurrent" type="success">当前版本</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="280">
              <template #default="{ row }">
                <el-button-group>
                  <el-button
                    size="small"
                    @click="handleViewVersion(row.version)"
                  >
                    查看
                  </el-button>
                  <el-button
                    v-if="!row.isCurrent"
                    size="small"
                    type="primary"
                    @click="handleSetCurrent(row.version)"
                  >
                    设为当前
                  </el-button>
                  <el-button
                    size="small"
                    @click="handleCopyVersion(row.version)"
                  >
                    复制
                  </el-button>
                  <el-button
                    v-if="!row.isCurrent"
                    size="small"
                    type="danger"
                    @click="handleDeleteVersion(row.version)"
                  >
                    删除
                  </el-button>
                </el-button-group>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-card>

    <!-- 生成新版本对话框 -->
    <el-dialog
      v-model="generateDialogVisible"
      title="生成新版本分镜"
      width="500px"
    >
      <el-form :model="generateForm" label-width="100px">
        <el-form-item label="保留旧版本">
          <el-switch v-model="generateForm.keepOld" />
          <div class="form-hint">
            开启后会生成新版本，否则会覆盖所有旧版本
          </div>
        </el-form-item>

        <el-form-item label="版本说明">
          <el-input
            v-model="generateForm.versionNote"
            placeholder="例如：增加对话细节"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="设为当前">
          <el-switch v-model="generateForm.setCurrent" />
          <div class="form-hint">
            生成后自动设置为当前使用的版本
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          @click="confirmGenerateVersion"
          :loading="generating"
        >
          开始生成
        </el-button>
      </template>
    </el-dialog>

    <!-- 版本对比对话框（待实现） -->
    <el-dialog
      v-model="compareDialogVisible"
      title="版本对比"
      width="90%"
      fullscreen
    >
      <div class="compare-container">
        <div class="compare-panel">
          <h4>版本 {{ compareVersions[0] }}</h4>
          <!-- 分镜列表 -->
        </div>
        <div class="compare-panel">
          <h4>版本 {{ compareVersions[1] }}</h4>
          <!-- 分镜列表 -->
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAllVersions,
  setCurrentVersion,
  deleteVersion,
  copyVersion,
  generateStoryboard
} from '@/api/storyboard'

const route = useRoute()
const novelId = computed(() => route.params.novelId || route.query.novelId)

// 数据状态
const loading = ref(false)
const generating = ref(false)
const versionList = ref([])
const currentVersion = ref(1)

// 对话框状态
const generateDialogVisible = ref(false)
const compareDialogVisible = ref(false)

// 表单数据
const generateForm = ref({
  keepOld: true,
  setCurrent: true,
  versionNote: ''
})

const compareVersions = ref([1, 2])

// 计算当前版本信息
const currentVersionInfo = computed(() => {
  return versionList.value.find(v => v.isCurrent)
})

// 加载版本列表
const loadVersions = async () => {
  loading.value = true
  try {
    const response = await getAllVersions(novelId.value)
    if (response.code === 200) {
      versionList.value = response.data || []
      const current = versionList.value.find(v => v.isCurrent)
      if (current) {
        currentVersion.value = current.version
      }
    }
  } catch (error) {
    console.error('加载版本列表失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 生成新版本
const handleGenerateNewVersion = () => {
  generateForm.value = {
    keepOld: true,
    setCurrent: true,
    versionNote: ''
  }
  generateDialogVisible.value = true
}

// 确认生成版本
const confirmGenerateVersion = async () => {
  generating.value = true
  try {
    const response = await generateStoryboard(novelId.value, {
      keepOld: generateForm.value.keepOld,
      setCurrent: generateForm.value.setCurrent,
      versionNote: generateForm.value.versionNote
    })

    if (response.code === 200) {
      ElMessage.success(response.message || '生成成功')
      generateDialogVisible.value = false
      await loadVersions()
    } else {
      ElMessage.error(response.message || '生成失败')
    }
  } catch (error) {
    console.error('生成失败:', error)
    ElMessage.error('生成失败，请稍后重试')
  } finally {
    generating.value = false
  }
}

// 查看版本
const handleViewVersion = (version) => {
  // 跳转到分镜预览页面，带上version参数
  // router.push(`/storyboard?novelId=${novelId.value}&version=${version}`)
  ElMessage.info(`查看版本 ${version}`)
}

// 设置当前版本
const handleSetCurrent = async (version) => {
  try {
    await ElMessageBox.confirm(
      `确定将版本 ${version} 设置为当前版本吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await setCurrentVersion(novelId.value, version)
    if (response.code === 200) {
      ElMessage.success('设置成功')
      await loadVersions()
    } else {
      ElMessage.error(response.message || '设置失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('设置失败:', error)
      ElMessage.error('设置失败')
    }
  }
}

// 复制版本
const handleCopyVersion = async (sourceVersion) => {
  try {
    const { value: targetVersion } = await ElMessageBox.prompt(
      `请输入目标版本号（当前源版本：${sourceVersion}）`,
      '复制版本',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /^[1-9]\d*$/,
        inputErrorMessage: '请输入有效的版本号'
      }
    )

    const response = await copyVersion(novelId.value, {
      sourceVersion,
      targetVersion: parseInt(targetVersion)
    })

    if (response.code === 200) {
      ElMessage.success('复制成功')
      await loadVersions()
    } else {
      ElMessage.error(response.message || '复制失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('复制失败:', error)
      ElMessage.error('复制失败')
    }
  }
}

// 删除版本
const handleDeleteVersion = async (version) => {
  try {
    await ElMessageBox.confirm(
      `确定删除版本 ${version} 吗？删除后无法恢复。`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'error'
      }
    )

    const response = await deleteVersion(novelId.value, version)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      await loadVersions()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 页面加载时获取版本列表
onMounted(() => {
  if (novelId.value) {
    loadVersions()
  }
})
</script>

<style scoped>
.version-manager {
  padding: 20px;
}

.version-selector-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.version-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.version-list {
  margin-top: 20px;
}

.form-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.compare-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  height: 70vh;
}

.compare-panel {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 16px;
  overflow-y: auto;
}

.compare-panel h4 {
  margin: 0 0 16px 0;
  color: #333;
}
</style>

