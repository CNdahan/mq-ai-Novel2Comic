# Novel2Comic Frontend - 快速开始指南

## 📦 项目初始化完成

恭喜！Novel2Comic 前端项目已成功初始化。以下是项目的详细信息和后续开发步骤。

## ✅ 已完成的工作

### 1. 项目结构创建

```
✅ 基于 Vue 3 + Vite 创建项目
✅ 安装所有必要依赖
✅ 创建完整的目录结构
✅ 配置环境变量
```

### 2. 核心配置完成

- ✅ Vite配置（路径别名、代理、端口）
- ✅ Vue Router配置（路由守卫、页面路由）
- ✅ Pinia状态管理（用户状态、漫画生成状态）
- ✅ Axios请求封装（拦截器、错误处理）
- ✅ WebSocket封装（STOMP.js集成）

### 3. API接口封装

- ✅ 认证接口（auth.js）
- ✅ 小说管理接口（novel.js）
- ✅ 角色管理接口（character.js）
- ✅ 分镜设计接口（storyboard.js）
- ✅ 漫画生成接口（comic.js）

### 4. 页面组件创建

- ✅ 首页（Home.vue）- 完整实现
- ✅ 登录页（Login.vue）- 完整实现
- ✅ 注册页（Register.vue）- 完整实现
- ✅ 上传页（Upload.vue）- 完整实现
- ✅ 角色确认页（Character.vue）- 基础框架
- ✅ 分镜预览页（Storyboard.vue）- 基础框架
- ✅ 生成进度页（Progress.vue）- 基础框架
- ✅ 漫画预览页（Preview.vue）- 基础框架
- ✅ 历史记录页（History.vue）- 基础框架
- ✅ 个人中心页（Profile.vue）- 基础框架
- ✅ 404页面（NotFound.vue）- 完整实现

## 🚀 立即启动项目

### 1. 启动后端服务

确保后端服务已启动：

```bash
# 在后端项目目录
cd D:\qi\novel2comic
mvn spring-boot:run
```

后端服务将运行在: http://localhost:8123

### 2. 启动前端开发服务器

```bash
# 在前端项目目录
cd D:\qi\novel2comic\novel2comic-frontend
npm run dev
```

前端服务将运行在: http://localhost:5173

### 3. 访问应用

在浏览器中打开: http://localhost:5173

## 🎯 测试功能

### 测试账号

可以使用以下测试数据：

```
邮箱: test@novel2comic.com
密码: test123456
```

或者注册新账号进行测试。

### 测试小说文本

可以使用以下测试文本（约560字）：

```
李明是一个25岁的年轻人，有着黑色的短发和深邃的黑眸。他身高180厘米，身材修长，经常穿着白色衬衫和黑色西裤，左眼下方有一道细长的疤痕，给人一种冷峻高贵的感觉。

今天，李明来到市中心的一家咖啡厅，准备和他的老朋友王芳见面。王芳是一个23岁的女孩，有着一头乌黑的长发和温柔的笑容，她穿着一条淡蓝色的连衣裙，显得格外清新可人。

两人在咖啡厅里找了个靠窗的位置坐了下来。"好久不见，你最近怎么样？"王芳微笑着问道。

李明看着窗外，神情有些复杂，"还好吧，工作上遇到了一些麻烦，不过应该能解决。"

"如果需要帮忙的话，记得跟我说。"王芳关切地说。

李明转过头，看着王芳，眼神中闪过一丝感动，"谢谢你，一直以来都这么关心我。"

窗外，夕阳西下，金色的阳光洒在两人的脸上，整个咖啡厅都被染成了温暖的颜色。
```

## 📋 功能测试流程

### 1. 用户注册/登录

1. 访问首页
2. 点击"注册"按钮
3. 填写注册信息
4. 注册成功后自动登录

### 2. 上传小说

1. 登录后点击"开始创作"
2. 输入或粘贴小说文本
3. 选择风格（日式/国风/写实）
4. 点击"开始生成"

### 3. 查看流程（目前为演示版本）

- 角色确认页：显示"功能开发中"提示
- 分镜预览页：显示"功能开发中"提示
- 生成进度页：显示模拟进度
- 漫画预览页：显示基础信息

## 🔧 后续开发任务

### 优先级1：核心功能完善

1. **角色确认页（Character.vue）**
   - [ ] 展示AI识别的角色列表
   - [ ] 角色信息编辑功能
   - [ ] 角色删除/添加功能
   - [ ] 角色卡片UI设计

2. **分镜预览页（Storyboard.vue）**
   - [ ] 展示AI生成的分镜列表
   - [ ] 分镜拖拽排序
   - [ ] 分镜编辑功能
   - [ ] 分镜删除/添加功能

3. **生成进度页（Progress.vue）**
   - [ ] WebSocket连接实现
   - [ ] 实时进度更新
   - [ ] 已生成图片预览
   - [ ] 实时日志显示

4. **漫画预览页（Preview.vue）**
   - [ ] 漫画面板展示
   - [ ] 多种排版模式（单列/双列/网格）
   - [ ] 图片放大查看
   - [ ] 单张重新生成
   - [ ] 导出功能（ZIP/长图）

### 优先级2：用户体验优化

5. **历史记录页（History.vue）**
   - [ ] 作品列表展示
   - [ ] 搜索/筛选功能
   - [ ] 排序功能
   - [ ] 删除/分享功能

6. **个人中心页（Profile.vue）**
   - [ ] 用户信息展示
   - [ ] 信息编辑功能
   - [ ] VIP信息展示
   - [ ] 统计数据展示

### 优先级3：性能优化

7. **性能优化**
   - [ ] 图片懒加载
   - [ ] 虚拟滚动（大量数据）
   - [ ] 组件按需加载
   - [ ] 缓存策略优化

8. **响应式优化**
   - [ ] 移动端适配
   - [ ] 触摸手势支持
   - [ ] 断点优化

## 📝 开发建议

### 1. 组件开发规范

```vue
<template>
  <!-- 模板 -->
</template>

<script setup>
// 导入
import { ref, reactive, computed, onMounted } from 'vue'

// 状态
const state = reactive({
  // ...
})

// 计算属性
const computedValue = computed(() => {
  // ...
})

// 方法
const handleClick = () => {
  // ...
}

// 生命周期
onMounted(() => {
  // ...
})
</script>

<style scoped>
/* 样式 */
</style>
```

### 2. API调用示例

```javascript
import { getNovelDetail } from '@/api/novel'
import { ElMessage } from 'element-plus'

const loadNovelDetail = async (novelId) => {
  try {
    const response = await getNovelDetail(novelId)
    console.log('小说详情:', response.data)
  } catch (error) {
    console.error('加载失败:', error)
  }
}
```

### 3. WebSocket使用示例

```javascript
import { createWebSocketClient, disconnectWebSocket } from '@/utils/websocket'
import { onUnmounted } from 'vue'

let wsClient = null

const connectWebSocket = (taskId) => {
  wsClient = createWebSocketClient(
    taskId,
    (progress) => {
      // 进度更新
      console.log('进度:', progress.progressPercent)
    },
    (result) => {
      // 完成
      console.log('完成:', result.comicId)
    },
    (error) => {
      // 错误
      console.error('错误:', error.errorMessage)
    }
  )
}

// 组件卸载时断开连接
onUnmounted(() => {
  disconnectWebSocket(wsClient)
})
```

## 🐛 常见问题

### 1. 端口被占用

```bash
# 修改端口
# 在 vite.config.js 中修改 server.port
```

### 2. 跨域问题

```bash
# 已在 vite.config.js 中配置代理
# 如果仍有问题，检查后端CORS配置
```

### 3. 依赖安装失败

```bash
# 清除缓存重新安装
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

### 4. Token过期

```bash
# Token有效期24小时
# 过期后会自动跳转到登录页
# 可以使用refreshToken API刷新Token
```

## 📚 相关文档

- **后端对接文档**: `../前端对接-项目总结文档.md`
- **API接口文档**: 后端Swagger UI - http://localhost:8123/api/doc.html
- **项目README**: `./README.md`

## 💡 开发提示

1. **使用Vue DevTools**: 安装浏览器扩展调试Vue应用
2. **使用Element Plus组件**: 优先使用Element Plus提供的组件
3. **响应式数据**: 使用ref或reactive声明响应式数据
4. **错误处理**: 所有API调用都应包含错误处理
5. **加载状态**: 异步操作要显示loading状态
6. **用户反馈**: 操作结果要给用户明确的反馈（成功/失败提示）

## 🎉 开发愉快！

如有问题，请查阅文档或联系项目维护者。

---

**Created**: 2024-10-24  
**Version**: 1.0.0  
**Status**: 项目初始化完成，可以开始开发

