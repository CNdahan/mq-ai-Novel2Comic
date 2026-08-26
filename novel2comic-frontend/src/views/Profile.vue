<template>
  <div class="profile-container">
    <el-page-header @back="router.push('/home')" content="个人中心" />
    
    <div class="profile-content">
      <!-- 个人信息卡片 -->
      <el-card class="info-card">
        <template #header>
          <div class="card-header">
            <h2>👤 个人信息</h2>
            <el-button type="primary" :icon="Edit" @click="handleEditProfile">
              编辑资料
            </el-button>
          </div>
        </template>
        
        <div class="profile-info">
          <!-- 头像 -->
          <div class="avatar-section">
            <el-avatar 
              :size="100" 
              :src="userInfo.avatar" 
              class="user-avatar"
              @error="handleAvatarError"
            >
              <template #default>
                <el-icon :size="50"><User /></el-icon>
              </template>
            </el-avatar>
            <el-button size="small" @click="handleChangeAvatar" style="margin-top: 12px;">
              更换头像
            </el-button>
          </div>
          
          <!-- 信息列表 -->
          <div class="info-details">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="用户名" :span="2">
                <strong>{{ userInfo.username }}</strong>
              </el-descriptions-item>
              <el-descriptions-item label="邮箱" :span="2">
                {{ userInfo.email || '未设置' }}
              </el-descriptions-item>
              <el-descriptions-item label="用户ID" :span="2">
                <el-text type="info" size="small">{{ userInfo.userId }}</el-text>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
      </el-card>
      
      <!-- VIP信息卡片 -->
      <el-card class="vip-card">
        <template #header>
          <div class="card-header">
            <h2>👑 VIP会员</h2>
            <el-button 
              type="warning" 
              :icon="TrendCharts"
              @click="handleUpgradeVip"
              v-if="userInfo.vipLevel === 0"
            >
              立即升级
            </el-button>
            <el-button 
              type="success" 
              :icon="TrendCharts"
              @click="handleUpgradeVip"
              v-else
            >
              续费/升级
            </el-button>
          </div>
        </template>
        
        <div class="vip-info">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="当前等级">
              <el-tag :type="getVipTagType(userInfo.vipLevel)" size="large">
                {{ getVipLevelText(userInfo.vipLevel) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="到期时间">
              <template v-if="userInfo.vipLevel > 0">
                <span v-if="userInfo.vipExpireAt">
                  <span v-if="isVipActive" style="color: #67c23a; font-weight: 600;">
                    {{ formatDate(userInfo.vipExpireAt) }}
                  </span>
                  <el-tag v-else type="danger" size="small">
                    {{ formatDate(userInfo.vipExpireAt) }} (已过期)
                  </el-tag>
                </span>
                <el-text type="warning" v-else>
                  VIP等级已设置，但无到期时间（请联系客服）
                </el-text>
              </template>
              <el-text type="info" v-else>未开通</el-text>
            </el-descriptions-item>
            <el-descriptions-item label="剩余次数">
              <el-tag type="success" size="large">
                {{ userInfo.quotaRemaining }} 次
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="总配额">
              {{ userInfo.quotaTotal }} 次
            </el-descriptions-item>
          </el-descriptions>
          
          <!-- VIP特权说明 -->
          <div class="vip-benefits" v-if="userInfo.vipLevel === 0">
            <el-divider content-position="left">VIP特权</el-divider>
            <div class="benefits-grid">
              <div class="benefit-item">
                <el-icon color="#fbbf24"><Star /></el-icon>
                <span>每月50-100次生成配额</span>
              </div>
              <div class="benefit-item">
                <el-icon color="#fbbf24"><Lightning /></el-icon>
                <span>优先生成队列</span>
              </div>
              <div class="benefit-item">
                <el-icon color="#fbbf24"><Picture /></el-icon>
                <span>高清图片下载</span>
              </div>
              <div class="benefit-item">
                <el-icon color="#fbbf24"><Discount /></el-icon>
                <span>专属客服支持</span>
              </div>
            </div>
          </div>
        </div>
      </el-card>
      
      <!-- 安全设置卡片 -->
      <el-card class="security-card">
        <template #header>
          <div class="card-header">
            <h2>🔒 安全设置</h2>
            <el-button type="primary" :icon="Key" @click="handleChangePassword">
              修改密码
            </el-button>
          </div>
        </template>
        
        <el-alert
          title="账号安全提示"
          type="info"
          :closable="false"
          show-icon
        >
          <template #default>
            <p>为了您的账号安全，请定期修改密码</p>
            <p>密码长度至少6位，建议包含字母、数字和符号</p>
          </template>
        </el-alert>
      </el-card>

      <!-- AI配置卡片 -->
      <el-card class="ai-card">
        <template #header>
          <div class="card-header">
            <h2>LLM语言模型配置</h2>
            <el-button type="primary" :icon="Setting" @click="handleEditAiConfig">
              配置
            </el-button>
          </div>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="模型来源">
            <el-tag :type="aiConfig.provider === 'grok' ? 'warning' : 'primary'">
              {{ getAiProviderLabel(aiConfig.provider) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="API Key">
            <el-tag :type="aiConfig.apiKey ? 'success' : 'info'">
              {{ aiConfig.apiKey ? '已配置' : '未配置' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="模型">
            {{ aiConfig.model || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="Base URL">
            {{ aiConfig.baseUrl || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- AIGC配置卡片 -->
      <el-card class="aigc-card">
        <template #header>
          <div class="card-header">
            <h2>AIGC图片生成配置</h2>
            <el-button type="primary" :icon="Picture" @click="handleEditAigcConfig">
              配置
            </el-button>
          </div>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="图片来源">
            <el-tag :type="aigcConfig.provider === 'wanx' ? 'warning' : 'success'">
              {{ getAigcProviderLabel(aigcConfig.provider) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="API Key">
            <el-tag :type="aigcConfig.apiKey ? 'success' : 'info'">
              {{ aigcConfig.apiKey ? '已配置' : '未配置' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="模型">
            {{ aigcConfig.model || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="Base URL">
            {{ aigcConfig.baseUrl || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="图片分辨率">
            {{ aigcConfig.resolution === '2k' ? '2K（费用较高）' : '1K（默认）' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>
    
    <!-- 编辑个人信息对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑个人信息"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="editForm" label-width="80px" :rules="editRules" ref="editFormRef">
        <el-form-item label="用户名" prop="username">
          <el-input 
            v-model="editForm.username" 
            placeholder="请输入用户名"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="邮箱" prop="email">
          <el-input 
            v-model="editForm.email" 
            placeholder="请输入邮箱"
            type="email"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveProfile" :loading="saving">
          保存
        </el-button>
      </template>
    </el-dialog>
    
    <!-- 修改密码对话框 -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="450px"
      :close-on-click-modal="false"
    >
      <el-form :model="passwordForm" label-width="100px" :rules="passwordRules" ref="passwordFormRef">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input 
            v-model="passwordForm.oldPassword" 
            type="password"
            placeholder="请输入旧密码"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="新密码" prop="newPassword">
          <el-input 
            v-model="passwordForm.newPassword" 
            type="password"
            placeholder="请输入新密码（至少6位）"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input 
            v-model="passwordForm.confirmPassword" 
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePassword" :loading="saving">
          确定
        </el-button>
      </template>
    </el-dialog>
    
    <!-- 更换头像对话框 -->
    <el-dialog
      v-model="avatarDialogVisible"
      title="更换头像"
      width="500px"
    >
      <div class="avatar-options">
        <p style="margin-bottom: 16px;">选择一个头像或输入自定义URL：</p>
        
        <div class="avatar-grid">
          <div 
            v-for="(avatar, index) in avatarOptions" 
            :key="index"
            class="avatar-option"
            :class="{ 'selected': selectedAvatar === avatar }"
            @click="selectedAvatar = avatar"
          >
            <el-avatar :size="80" :src="avatar">
              <el-icon :size="40"><User /></el-icon>
            </el-avatar>
          </div>
        </div>
        
        <el-divider>或</el-divider>
        
        <el-input 
          v-model="customAvatarUrl"
          placeholder="输入自定义头像URL"
          clearable
        >
          <template #prepend>
            <el-icon><Link /></el-icon>
          </template>
        </el-input>
        
        <div class="avatar-preview" v-if="selectedAvatar || customAvatarUrl">
          <p>预览：</p>
          <el-avatar :size="80" :src="customAvatarUrl || selectedAvatar">
            <el-icon :size="40"><User /></el-icon>
          </el-avatar>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="avatarDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveAvatar" :loading="saving">
          保存
        </el-button>
      </template>
    </el-dialog>
    
    <!-- VIP升级对话框 -->
    <el-dialog
      v-model="vipDialogVisible"
      title="VIP会员升级"
      width="700px"
      :close-on-click-modal="false"
    >
      <div class="vip-upgrade-content">
        <!-- VIP套餐选择 -->
        <div class="vip-plans">
          <div 
            class="vip-plan"
            :class="{ 'selected': vipForm.vipLevel === 1 }"
            @click="vipForm.vipLevel = 1"
          >
            <div class="plan-badge">月费会员</div>
            <div class="plan-icon">⭐</div>
            <div class="plan-name">VIP月卡</div>
            <div class="plan-price">¥9.9<span>/月</span></div>
            <div class="plan-features">
              <div class="feature-item">✓ 每月50次生成配额</div>
              <div class="feature-item">✓ 优先生成队列</div>
              <div class="feature-item">✓ 高清图片下载</div>
            </div>
          </div>
          
          <div 
            class="vip-plan popular"
            :class="{ 'selected': vipForm.vipLevel === 2 }"
            @click="vipForm.vipLevel = 2"
          >
            <div class="popular-badge">🔥 最受欢迎</div>
            <div class="plan-badge">年费会员</div>
            <div class="plan-icon">👑</div>
            <div class="plan-name">VIP年卡</div>
            <div class="plan-price">¥19.9<span>/月</span></div>
            <div class="plan-features">
              <div class="feature-item">✓ 每月100次生成配额</div>
              <div class="feature-item">✓ 最高优先级</div>
              <div class="feature-item">✓ 专属客服支持</div>
              <div class="feature-item">✓ 高清无水印下载</div>
            </div>
          </div>
        </div>
        
        <!-- 购买时长 -->
        <div class="duration-selector">
          <label>购买时长：</label>
          <el-radio-group v-model="vipForm.duration">
            <el-radio-button :label="1">1个月</el-radio-button>
            <el-radio-button :label="3">3个月</el-radio-button>
            <el-radio-button :label="6">6个月</el-radio-button>
            <el-radio-button :label="12">12个月</el-radio-button>
          </el-radio-group>
        </div>
        
        <!-- 费用说明 -->
        <div class="cost-summary">
          <div class="summary-row">
            <span>套餐</span>
            <span>{{ getVipLevelText(vipForm.vipLevel) }}</span>
          </div>
          <div class="summary-row">
            <span>时长</span>
            <span>{{ vipForm.duration }} 个月</span>
          </div>
          <div class="summary-row">
            <span>新增配额</span>
            <span class="highlight">+{{ calculateQuota() }} 次</span>
          </div>
          <el-divider />
          <div class="summary-row total">
            <span>总计</span>
            <span class="price">¥{{ calculateTotalPrice() }}</span>
          </div>
        </div>
        
        <!-- 支付方式（Mock） -->
        <div class="payment-method">
          <label>支付方式：</label>
          <el-radio-group v-model="vipForm.paymentMethod">
            <el-radio label="mock">模拟支付（测试）</el-radio>
            <el-radio label="alipay" disabled>支付宝（开发中）</el-radio>
            <el-radio label="wechat" disabled>微信支付（开发中）</el-radio>
          </el-radio-group>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="vipDialogVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleConfirmUpgrade" 
          :loading="upgrading"
          size="large"
        >
          确认支付 ¥{{ calculateTotalPrice() }}
        </el-button>
      </template>
    </el-dialog>

    <!-- AI配置对话框 -->
    <el-dialog
      v-model="aiConfigDialogVisible"
      title="LLM语言模型配置"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form :model="aiConfigForm" label-width="100px">
        <el-form-item label="模型来源">
          <el-select
            v-model="aiConfigForm.provider"
            placeholder="请选择模型来源"
            style="width: 100%"
            @change="applyAiProviderPreset"
          >
            <el-option label="GPT (OpenAI)" value="openai" />
            <el-option label="Grok (xAI)" value="grok" />
          </el-select>
        </el-form-item>

        <el-form-item label="API Key">
          <el-input
            v-model="aiConfigForm.apiKey"
            type="password"
            placeholder="请输入 API Key"
            show-password
          />
        </el-form-item>

        <el-form-item label="模型">
          <el-select
            v-model="aiConfigForm.model"
            placeholder="请选择或输入模型名称"
            filterable
            allow-create
            default-first-option
            style="width: 100%"
            :loading="aiModelsLoading"
          >
            <el-option
              v-for="model in aiModelOptions"
              :key="model"
              :label="model"
              :value="model"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="Base URL">
          <el-input
            v-model="aiConfigForm.baseUrl"
            placeholder="请输入接口地址"
            @blur="loadAiModels"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button :icon="RefreshRight" @click="handleResetAiConfig" :loading="aiConfigSaving">
          恢复默认
        </el-button>
        <el-button @click="aiConfigDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveAiConfig" :loading="aiConfigSaving">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- AIGC配置对话框 -->
    <el-dialog
      v-model="aigcConfigDialogVisible"
      title="AIGC图片生成配置"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form :model="aigcConfigForm" label-width="100px">
        <el-form-item label="图片来源">
          <el-select
            v-model="aigcConfigForm.provider"
            placeholder="请选择图片生成来源"
            style="width: 100%"
            @change="handleAigcProviderChange"
          >
            <el-option label="GPT Image (OpenAI)" value="openai" />
            <el-option label="Grok Imagine (xAI)" value="grok" />
            <el-option label="硅基流动 (SiliconFlow)" value="siliconflow" />
            <el-option label="通义万相 (Wanx)" value="wanx" />
          </el-select>
        </el-form-item>

        <el-form-item label="API Key">
          <el-input
            v-model="aigcConfigForm.apiKey"
            type="password"
            placeholder="请输入 AIGC 图片生成 API Key"
            show-password
          />
        </el-form-item>

        <el-form-item label="模型">
          <el-select
            v-model="aigcConfigForm.model"
            placeholder="请选择或输入模型名称"
            filterable
            allow-create
            default-first-option
            style="width: 100%"
            :loading="aigcModelsLoading"
          >
            <el-option
              v-for="model in aigcModelOptions"
              :key="model"
              :label="model"
              :value="model"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="Base URL">
          <el-input
            v-model="aigcConfigForm.baseUrl"
            placeholder="请输入图片生成接口地址"
            @blur="loadAigcModels"
          />
        </el-form-item>

        <el-form-item label="图片分辨率">
          <el-select
            v-model="aigcConfigForm.resolution"
            style="width: 100%"
          >
            <el-option label="1K（默认，费用较低）" value="1k" />
            <el-option label="2K（更清晰，费用较高）" value="2k" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button :icon="RefreshRight" @click="handleResetAigcConfig" :loading="aigcConfigSaving">
          恢复默认
        </el-button>
        <el-button @click="aigcConfigDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveAigcConfig" :loading="aigcConfigSaving">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Key, User, TrendCharts, Star, Lightning, Picture, Discount, Link, Setting, RefreshRight } from '@element-plus/icons-vue'
import { getUserInfo, updateProfile, updatePassword, upgradeVip } from '@/api/auth'
import { getAiConfig, saveAiConfig, resetAiConfig } from '@/api/aiConfig'
import { getAiModels } from '@/api/aiModels'
import { getAigcConfig, saveAigcConfig, resetAigcConfig } from '@/api/aigcConfig'
import { getAigcModels } from '@/api/aigcModels'

const router = useRouter()
const userStore = useUserStore()

// 用户信息（初始化时从localStorage读取）
const userInfo = reactive({
  userId: localStorage.getItem('userId') || '',
  username: localStorage.getItem('username') || '',
  email: '',
  avatar: localStorage.getItem('avatar') || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
  quotaRemaining: Number(localStorage.getItem('quotaRemaining')) || 0,
  quotaTotal: 0,
  vipLevel: 0,
  vipExpireAt: null
})

// 对话框显示控制
const editDialogVisible = ref(false)
const passwordDialogVisible = ref(false)
const avatarDialogVisible = ref(false)
const vipDialogVisible = ref(false)
const aiConfigDialogVisible = ref(false)
const aigcConfigDialogVisible = ref(false)

// 保存状态
const saving = ref(false)
const upgrading = ref(false)
const aiConfigSaving = ref(false)
const aiModelsLoading = ref(false)
const aiModelOptions = ref([])
const aigcConfigSaving = ref(false)
const aigcModelsLoading = ref(false)
const aigcModelOptions = ref([])

// 编辑表单
const editForm = reactive({
  username: '',
  email: ''
})

const editFormRef = ref(null)
const editRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

// 密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordFormRef = ref(null)
const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 头像选项
const selectedAvatar = ref('')
const customAvatarUrl = ref('')
const avatarOptions = [
  'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
  'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
  'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png',
  'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png'
]

// VIP表单
const vipForm = reactive({
  vipLevel: 1,
  duration: 1,
  paymentMethod: 'mock'
})

const aiConfig = reactive({
  provider: 'openai',
  apiKey: '',
  model: 'gpt-4o-mini',
  baseUrl: 'https://api.openai.com/v1'
})

const aiConfigForm = reactive({
  provider: 'openai',
  apiKey: '',
  model: 'gpt-4o-mini',
  baseUrl: 'https://api.openai.com/v1'
})

const aigcConfig = reactive({
  provider: 'siliconflow',
  apiKey: '',
  model: 'black-forest-labs/FLUX.1-schnell',
  baseUrl: 'https://api.siliconflow.cn/v1/images/generations',
  resolution: '1k'
})

const aigcConfigForm = reactive({
  provider: 'siliconflow',
  apiKey: '',
  model: 'black-forest-labs/FLUX.1-schnell',
  baseUrl: 'https://api.siliconflow.cn/v1/images/generations',
  resolution: '1k'
})

// 计算VIP是否有效
const isVipActive = computed(() => {
  if (!userInfo.vipExpireAt) return false
  return new Date(userInfo.vipExpireAt) > new Date()
})

// 加载用户信息
const loadUserInfo = async () => {
  try {
    const response = await getUserInfo()
    if (response.code === 200 && response.data) {
      const data = response.data
      console.log('收到用户信息:', data)
      
      userInfo.userId = data.userId
      userInfo.username = data.username
      userInfo.email = data.email
      userInfo.quotaRemaining = data.quotaRemaining
      userInfo.quotaTotal = data.quotaTotal
      userInfo.vipLevel = data.vipLevel
      userInfo.vipExpireAt = data.vipExpireAt
      console.log('📅 VIP信息详情:')
      console.log('  - vipLevel:', data.vipLevel)
      console.log('  - vipExpireAt:', data.vipExpireAt)
      console.log('  - vipExpireAt类型:', typeof data.vipExpireAt)
      console.log('  - isVipActive:', isVipActive.value)
      
      // 头像处理：优先使用后端返回的，如果为空则使用localStorage，最后使用默认头像
      if (data.avatar) {
        userInfo.avatar = data.avatar
        // 同步到localStorage
        localStorage.setItem('avatar', data.avatar)
        console.log('✅ 使用后端头像:', data.avatar)
      } else {
        // 后端没有头像，使用localStorage或默认
        const cachedAvatar = localStorage.getItem('avatar')
        userInfo.avatar = cachedAvatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
        console.log('⚠️ 后端无头像，使用缓存或默认:', userInfo.avatar)
      }
      
      console.log('📷 用户信息加载成功, 最终头像URL:', userInfo.avatar)
    }
  } catch (error) {
    console.error('加载用户信息失败:', error)
    ElMessage.error('加载用户信息失败')
  }
}

const loadAiConfig = async () => {
  try {
    const response = await getAiConfig()
    if (response.code === 200 && response.data) {
      Object.assign(aiConfig, response.data)
    }
  } catch (error) {
    console.error('加载AI配置失败:', error)
  }
}

const loadAiModels = async () => {
  if (!aiConfigForm.baseUrl || !aiConfigForm.apiKey) {
    return
  }
  aiModelsLoading.value = true
  try {
    const response = await getAiModels({
      provider: aiConfigForm.provider,
      apiKey: aiConfigForm.apiKey,
      model: aiConfigForm.model,
      baseUrl: aiConfigForm.baseUrl
    })
    if (response.code === 200 && Array.isArray(response.data)) {
      aiModelOptions.value = response.data
      if (response.data.length > 0 && !response.data.includes(aiConfigForm.model)) {
        aiConfigForm.model = response.data[0]
      }
    }
  } catch (error) {
    console.error('获取AI模型列表失败:', error)
    aiModelOptions.value = []
  } finally {
    aiModelsLoading.value = false
  }
}

const loadAigcConfig = async () => {
  try {
    const response = await getAigcConfig()
    if (response.code === 200 && response.data) {
      Object.assign(aigcConfig, response.data, {
        resolution: response.data.resolution || '1k'
      })
      await loadAigcModels()
    }
  } catch (error) {
    console.error('加载AIGC配置失败:', error)
  }
}

const loadAigcModels = async () => {
  if (!aigcConfigForm.baseUrl || !aigcConfigForm.apiKey) {
    return
  }
  aigcModelsLoading.value = true
  try {
    const response = await getAigcModels({
      provider: aigcConfigForm.provider,
      apiKey: aigcConfigForm.apiKey,
      model: aigcConfigForm.model,
      baseUrl: aigcConfigForm.baseUrl
    })
    if (response.code === 200 && Array.isArray(response.data)) {
      aigcModelOptions.value = response.data
      if (response.data.length > 0 && !response.data.includes(aigcConfigForm.model)) {
        aigcConfigForm.model = response.data[0]
      }
    }
  } catch (error) {
    console.error('获取AIGC模型列表失败:', error)
    aigcModelOptions.value = []
  } finally {
    aigcModelsLoading.value = false
  }
}

// 编辑个人信息
const handleEditProfile = () => {
  editForm.username = userInfo.username
  editForm.email = userInfo.email
  editDialogVisible.value = true
}

// 保存个人信息
const handleSaveProfile = async () => {
  try {
    await editFormRef.value.validate()
    
    saving.value = true
    const response = await updateProfile({
      username: editForm.username,
      email: editForm.email
    })
    
    if (response.code === 200) {
      // 立即更新本地显示
      userInfo.username = editForm.username
      userInfo.email = editForm.email
      
      ElMessage.success('个人信息更新成功')
      editDialogVisible.value = false
      
      // 更新store
      userStore.username = editForm.username
      localStorage.setItem('username', editForm.username)
      
      // 重新加载确保同步
      await loadUserInfo()
    } else {
      ElMessage.error(response.message || '更新失败')
    }
  } catch (error) {
    if (error.errors) {
      // 表单验证错误
      return
    }
    console.error('更新个人信息失败:', error)
    ElMessage.error(error.response?.data?.message || '更新失败')
  } finally {
    saving.value = false
  }
}

// 修改密码
const handleChangePassword = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordDialogVisible.value = true
}

// 保存密码
const handleSavePassword = async () => {
  try {
    await passwordFormRef.value.validate()
    
    saving.value = true
    const response = await updatePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword
    })
    
    if (response.code === 200) {
      ElMessage.success('密码修改成功，请重新登录')
      passwordDialogVisible.value = false
      // 退出登录
      setTimeout(() => {
        userStore.logout()
        router.push('/login')
      }, 1500)
    } else {
      ElMessage.error(response.message || '修改失败')
    }
  } catch (error) {
    if (error.errors) {
      return
    }
    console.error('修改密码失败:', error)
    ElMessage.error(error.response?.data?.message || '修改失败')
  } finally {
    saving.value = false
  }
}

// 更换头像
const handleChangeAvatar = () => {
  selectedAvatar.value = userInfo.avatar || ''
  customAvatarUrl.value = ''
  avatarDialogVisible.value = true
  console.log('当前头像:', userInfo.avatar)
}

// 保存头像
const handleSaveAvatar = async () => {
  const newAvatar = customAvatarUrl.value || selectedAvatar.value
  if (!newAvatar) {
    ElMessage.warning('请选择头像或输入URL')
    return
  }
  
  saving.value = true
  try {
    console.log('更新头像为:', newAvatar)
    const response = await updateProfile({
      avatar: newAvatar
    })
    
    if (response.code === 200) {
      // 立即更新本地显示
      userInfo.avatar = newAvatar
      // 同步到store和localStorage
      userStore.avatar = newAvatar
      localStorage.setItem('avatar', newAvatar)
      
      ElMessage.success('头像更新成功')
      avatarDialogVisible.value = false
      // 重新加载用户信息确保同步
      await loadUserInfo()
    } else {
      ElMessage.error(response.message || '更新失败')
    }
  } catch (error) {
    console.error('更新头像失败:', error)
    ElMessage.error('更新失败')
  } finally {
    saving.value = false
  }
}

// VIP升级
const handleUpgradeVip = () => {
  vipForm.vipLevel = userInfo.vipLevel > 0 ? userInfo.vipLevel : 1
  vipForm.duration = 1
  vipForm.paymentMethod = 'mock'
  vipDialogVisible.value = true
}

const getAiProviderLabel = (provider) => {
  const map = {
    openai: 'GPT (OpenAI)',
    grok: 'Grok (xAI)'
  }
  return map[provider] || provider || '-'
}

const getAigcProviderLabel = (provider) => {
  const map = {
    openai: 'GPT Image (OpenAI)',
    grok: 'Grok Imagine (xAI)',
    siliconflow: '硅基流动 (SiliconFlow)',
    wanx: '通义万相 (Wanx)'
  }
  return map[provider] || provider || '-'
}

const applyAiProviderPreset = () => {
  if (aiConfigForm.provider === 'grok') {
    aiConfigForm.model = 'grok-2-latest'
    aiConfigForm.baseUrl = 'https://api.x.ai/v1'
  } else {
    aiConfigForm.model = 'gpt-4o-mini'
    aiConfigForm.baseUrl = 'https://api.openai.com/v1'
  }
  aiModelOptions.value = []
  loadAiModels()
}

const applyAigcProviderPreset = () => {
  if (aigcConfigForm.provider === 'grok') {
    aigcConfigForm.model = 'grok-imagine-image-2.0'
    aigcConfigForm.baseUrl = 'https://api.x.ai/v1/images/generations'
  } else if (aigcConfigForm.provider === 'openai') {
    aigcConfigForm.model = 'gpt-image-1'
    aigcConfigForm.baseUrl = 'https://api.openai.com/v1/images/generations'
  } else if (aigcConfigForm.provider === 'wanx') {
    aigcConfigForm.model = 'wanx-v1'
    aigcConfigForm.baseUrl = 'https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis'
  } else {
    aigcConfigForm.model = 'black-forest-labs/FLUX.1-schnell'
    aigcConfigForm.baseUrl = 'https://api.siliconflow.cn/v1/images/generations'
  }
  aigcModelOptions.value = []
}

const handleEditAiConfig = () => {
  Object.assign(aiConfigForm, aiConfig)
  aiConfigDialogVisible.value = true
  loadAiModels()
}

const handleSaveAiConfig = async () => {
  aiConfigSaving.value = true
  try {
    const response = await saveAiConfig({
      provider: aiConfigForm.provider,
      apiKey: aiConfigForm.apiKey,
      model: aiConfigForm.model,
      baseUrl: aiConfigForm.baseUrl
    })
    if (response.code === 200) {
      Object.assign(aiConfig, response.data)
      ElMessage.success('AI配置保存成功')
      aiConfigDialogVisible.value = false
    } else {
      ElMessage.error(response.message || '保存失败')
    }
  } catch (error) {
    console.error('保存AI配置失败:', error)
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    aiConfigSaving.value = false
  }
}

const handleResetAiConfig = async () => {
  aiConfigSaving.value = true
  try {
    const response = await resetAiConfig()
    if (response.code === 200) {
      const next = {
        provider: 'openai',
        apiKey: '',
        model: 'gpt-4o-mini',
        baseUrl: 'https://api.openai.com/v1'
      }
      Object.assign(aiConfig, next)
      Object.assign(aiConfigForm, next)
      ElMessage.success('已恢复默认配置')
    } else {
      ElMessage.error(response.message || '恢复失败')
    }
  } catch (error) {
    console.error('恢复AI配置失败:', error)
    ElMessage.error(error.response?.data?.message || '恢复失败')
  } finally {
    aiConfigSaving.value = false
  }
}

const handleEditAigcConfig = () => {
  Object.assign(aigcConfigForm, aigcConfig, {
    resolution: aigcConfig.resolution || '1k'
  })
  aigcConfigDialogVisible.value = true
  loadAigcModels()
}

const handleAigcProviderChange = () => {
  applyAigcProviderPreset()
  loadAigcModels()
}

const handleSaveAigcConfig = async () => {
  aigcConfigSaving.value = true
  try {
    const response = await saveAigcConfig({
      provider: aigcConfigForm.provider,
      apiKey: aigcConfigForm.apiKey,
      model: aigcConfigForm.model,
      baseUrl: aigcConfigForm.baseUrl,
      resolution: aigcConfigForm.resolution || '1k'
    })
    if (response.code === 200) {
      Object.assign(aigcConfig, response.data, {
        resolution: response.data.resolution || '1k'
      })
      ElMessage.success('AIGC配置保存成功')
      aigcConfigDialogVisible.value = false
    } else {
      ElMessage.error(response.message || '保存失败')
    }
  } catch (error) {
    console.error('保存AIGC配置失败:', error)
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    aigcConfigSaving.value = false
  }
}

const handleResetAigcConfig = async () => {
  aigcConfigSaving.value = true
  try {
    const response = await resetAigcConfig()
    if (response.code === 200) {
      const next = {
        provider: 'siliconflow',
        apiKey: '',
        model: 'black-forest-labs/FLUX.1-schnell',
        baseUrl: 'https://api.siliconflow.cn/v1/images/generations',
        resolution: '1k'
      }
      Object.assign(aigcConfig, next)
      Object.assign(aigcConfigForm, next)
      ElMessage.success('已恢复默认配置')
    } else {
      ElMessage.error(response.message || '恢复失败')
    }
  } catch (error) {
    console.error('恢复AIGC配置失败:', error)
    ElMessage.error(error.response?.data?.message || '恢复失败')
  } finally {
    aigcConfigSaving.value = false
  }
}

// 确认升级
const handleConfirmUpgrade = async () => {
  upgrading.value = true
  try {
    console.log('发送VIP升级请求:', vipForm)
    const response = await upgradeVip({
      vipLevel: vipForm.vipLevel,
      duration: vipForm.duration,
      paymentMethod: vipForm.paymentMethod
    })
    
    if (response.code === 200) {
      const data = response.data
      console.log('✅ VIP升级成功，返回数据:', data)
      
      // 立即更新本地显示
      userInfo.vipLevel = data.vipLevel
      userInfo.vipExpireAt = data.vipExpireAt
      userInfo.quotaRemaining = data.quotaRemaining
      userInfo.quotaTotal = data.quotaRemaining  // 临时显示，后面会刷新
      
      ElMessage.success({
        message: `VIP升级成功！新增配额 ${data.quotaAdded} 次，到期时间：${formatDate(data.vipExpireAt)}`,
        duration: 5000
      })
      vipDialogVisible.value = false
      
      // 刷新用户信息
      await loadUserInfo()
      // 更新store
      await userStore.refreshUserInfo()
    } else {
      ElMessage.error(response.message || '升级失败')
    }
  } catch (error) {
    console.error('VIP升级失败:', error)
    ElMessage.error(error.response?.data?.message || '升级失败')
  } finally {
    upgrading.value = false
  }
}

// 计算配额
const calculateQuota = () => {
  const monthlyQuota = vipForm.vipLevel === 1 ? 50 : 100
  return monthlyQuota * vipForm.duration
}

// 计算总价
const calculateTotalPrice = () => {
  const monthlyPrice = vipForm.vipLevel === 1 ? 9.9 : 19.9
  return (monthlyPrice * vipForm.duration).toFixed(2)
}

// 获取VIP等级文本
const getVipLevelText = (level) => {
  const textMap = {
    0: '普通用户',
    1: 'VIP月卡会员',
    2: 'VIP年卡会员'
  }
  return textMap[level] || '普通用户'
}

// 获取VIP标签类型
const getVipTagType = (level) => {
  const typeMap = {
    0: 'info',
    1: 'warning',
    2: 'danger'
  }
  return typeMap[level] || 'info'
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) {
    console.log('⚠️ 日期为空')
    return '-'
  }
  try {
    const date = new Date(dateString)
    if (isNaN(date.getTime())) {
      console.log('⚠️ 日期格式错误:', dateString)
      return '-'
    }
    const formatted = date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
    console.log('📅 格式化日期:', dateString, '->', formatted)
    return formatted
  } catch (e) {
    console.error('日期格式化失败:', e)
    return '-'
  }
}

// 头像加载失败处理
const handleAvatarError = () => {
  console.warn('头像加载失败，使用默认头像')
  // 设置为默认头像
  userInfo.avatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
  return true
}

// 页面加载时获取用户信息
onMounted(() => {
  loadUserInfo()
  loadAiConfig()
  loadAigcConfig()
})
</script>

<style scoped>
.profile-container {
  min-height: 100vh;
  background: linear-gradient(to bottom, #f0f2f5 0%, #e6e8eb 100%);
  padding: 24px;
}

.profile-container :deep(.el-page-header) {
  background: #ffffff;
  padding: 18px 28px;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.06);
  margin-bottom: 24px;
}

.profile-content {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  gap: 24px;
}

/* 卡片公共样式 */
.info-card,
.vip-card,
.security-card,
.ai-card,
.aigc-card {
  border-radius: 16px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
}

/* 个人信息卡片 */
.profile-info {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 32px;
  align-items: start;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.user-avatar {
  border: 3px solid #e8ecf0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.info-details {
  flex: 1;
}

/* VIP卡片 */
.vip-info :deep(.el-descriptions__label) {
  font-weight: 600;
}

.vip-benefits {
  margin-top: 24px;
}

.benefits-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.benefit-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #fef3c7;
  border-radius: 8px;
  font-size: 14px;
}

/* VIP升级对话框 */
.vip-upgrade-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.vip-plans {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.vip-plan {
  position: relative;
  padding: 24px;
  border: 2px solid #e8ecf0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: center;
}

.vip-plan:hover {
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
  transform: translateY(-4px);
}

.vip-plan.selected {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.vip-plan.popular {
  border-color: #f59e0b;
}

.vip-plan.popular.selected {
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
  border-color: #f59e0b;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
}

.popular-badge {
  position: absolute;
  top: -12px;
  right: 20px;
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.plan-badge {
  display: inline-block;
  background: #3b82f6;
  color: white;
  padding: 4px 12px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 12px;
}

.plan-icon {
  font-size: 48px;
  margin: 12px 0;
}

.plan-name {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 8px;
}

.plan-price {
  font-size: 32px;
  font-weight: 700;
  color: #3b82f6;
  margin-bottom: 16px;
}

.plan-price span {
  font-size: 14px;
  color: #6b7280;
}

.plan-features {
  text-align: left;
}

.feature-item {
  padding: 6px 0;
  color: #4b5563;
  font-size: 14px;
}

/* 时长选择器 */
.duration-selector {
  display: flex;
  align-items: center;
  gap: 16px;
}

.duration-selector label {
  font-weight: 600;
  color: #303133;
}

/* 费用说明 */
.cost-summary {
  background: #f9fafb;
  border: 1px solid #e8ecf0;
  border-radius: 12px;
  padding: 20px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
}

.summary-row.total {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.summary-row .highlight {
  color: #10b981;
  font-weight: 600;
}

.summary-row .price {
  color: #ef4444;
  font-size: 24px;
}

/* 支付方式 */
.payment-method {
  display: flex;
  align-items: center;
  gap: 16px;
}

.payment-method label {
  font-weight: 600;
  color: #303133;
}

/* 头像选择 */
.avatar-options {
  padding: 12px 0;
}

.avatar-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin: 16px 0;
}

.avatar-option {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 12px;
  border: 2px solid #e8ecf0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.avatar-option:hover {
  border-color: #3b82f6;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.2);
}

.avatar-option.selected {
  border-color: #3b82f6;
  background: #eff6ff;
}

.avatar-preview {
  margin-top: 16px;
  text-align: center;
}

.avatar-preview p {
  margin-bottom: 12px;
  font-weight: 600;
  color: #606266;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .profile-container {
    padding: 12px;
  }
  
  .profile-info {
    grid-template-columns: 1fr;
    text-align: center;
  }
  
  .vip-plans {
    grid-template-columns: 1fr;
  }
  
  .benefits-grid {
    grid-template-columns: 1fr;
  }
  
  .avatar-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
