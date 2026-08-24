# Novel2Comic Frontend

> 基于 Vue 3 + Vite + Element Plus 的小说转漫画前端应用

## 📋 项目简介

Novel2Comic 是一个基于AIGC技术的小说转漫画自动化生成平台的前端项目。用户上传小说文本后，系统通过AI自动提取角色、设计分镜、生成图片，最终合成完整的漫画作品。

## 🛠️ 技术栈

- **框架**: Vue 3.5+ (Composition API)
- **构建工具**: Vite 7.1+
- **UI组件库**: Element Plus 2.11+
- **状态管理**: Pinia 3.0+
- **路由**: Vue Router 4.6+
- **HTTP客户端**: Axios 1.12+
- **WebSocket**: @stomp/stompjs 7.2+
- **数据可视化**: Echarts 6.0+

## 📂 项目结构

```
novel2comic-frontend/
├── public/                  # 静态资源
│   └── vite.svg
├── src/
│   ├── api/                # API接口
│   │   ├── auth.js        # 认证接口
│   │   ├── novel.js       # 小说管理接口
│   │   ├── character.js   # 角色管理接口
│   │   ├── storyboard.js  # 分镜设计接口
│   │   └── comic.js       # 漫画生成接口
│   ├── assets/            # 资源文件
│   ├── components/        # 公共组件
│   ├── composables/       # 组合式函数
│   ├── layouts/           # 布局组件
│   ├── router/            # 路由配置
│   │   └── index.js
│   ├── store/             # 状态管理
│   │   ├── index.js       # Pinia实例
│   │   ├── user.js        # 用户状态
│   │   └── comic.js       # 漫画生成状态
│   ├── utils/             # 工具函数
│   │   ├── request.js     # Axios封装
│   │   └── websocket.js   # WebSocket封装
│   ├── views/             # 页面组件
│   │   ├── Home.vue       # 首页
│   │   ├── Login.vue      # 登录页
│   │   ├── Register.vue   # 注册页
│   │   ├── Upload.vue     # 上传小说页
│   │   ├── Character.vue  # 角色确认页
│   │   ├── Storyboard.vue # 分镜预览页
│   │   ├── Progress.vue   # 生成进度页
│   │   ├── Preview.vue    # 漫画预览页
│   │   ├── History.vue    # 历史记录页
│   │   ├── Profile.vue    # 个人中心页
│   │   └── NotFound.vue   # 404页面
│   ├── App.vue            # 根组件
│   ├── main.js            # 入口文件
│   └── style.css          # 全局样式
├── .env.development       # 开发环境配置
├── .env.production        # 生产环境配置
├── .gitignore
├── index.html
├── package.json
├── README.md
└── vite.config.js         # Vite配置

```

## 🚀 快速开始

### 环境要求

- Node.js >= 20.17.0
- npm >= 10.0.0

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:5173

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

## 🔧 环境配置

### 开发环境 (.env.development)

```env
VITE_API_BASE_URL=http://localhost:8123/api
VITE_WS_BASE_URL=ws://localhost:8123/api/ws
VITE_CDN_URL=https://cdn.example.com
VITE_APP_TITLE=Novel2Comic - 小说转漫画平台
VITE_APP_ENV=development
```

### 生产环境 (.env.production)

```env
VITE_API_BASE_URL=https://api.novel2comic.com/v1
VITE_WS_BASE_URL=wss://api.novel2comic.com/v1/ws
VITE_CDN_URL=https://cdn.novel2comic.com
VITE_APP_TITLE=Novel2Comic - 小说转漫画平台
VITE_APP_ENV=production
```

## 📱 页面路由

| 路径 | 组件 | 说明 | 是否需要登录 |
|------|------|------|-------------|
| `/` | - | 重定向到首页 | ❌ |
| `/home` | Home.vue | 首页 | ❌ |
| `/login` | Login.vue | 登录页 | ❌ |
| `/register` | Register.vue | 注册页 | ❌ |
| `/upload` | Upload.vue | 上传小说 | ✅ |
| `/character/:novelId` | Character.vue | 角色确认 | ✅ |
| `/storyboard/:novelId` | Storyboard.vue | 分镜预览 | ✅ |
| `/progress/:taskId` | Progress.vue | 生成进度 | ✅ |
| `/preview/:comicId` | Preview.vue | 漫画预览 | ✅ |
| `/history` | History.vue | 历史记录 | ✅ |
| `/profile` | Profile.vue | 个人中心 | ✅ |
| `*` | NotFound.vue | 404页面 | ❌ |

## 🔌 API接口

所有API接口已封装在 `src/api/` 目录下：

- **auth.js**: 用户注册、登录、Token刷新
- **novel.js**: 小说上传、详情、列表、删除
- **character.js**: 角色列表、更新、删除
- **storyboard.js**: 分镜生成、列表、更新、删除
- **comic.js**: 漫画生成、进度、结果、列表

详细接口文档请参考后端项目的 `前端对接-项目总结文档.md`

## 💾 状态管理

使用 Pinia 进行状态管理，包含以下 Store：

### userStore (src/store/user.js)

- 用户登录状态
- 用户信息（ID、用户名、邮箱）
- 配额信息
- 登录、注册、退出操作

### comicStore (src/store/comic.js)

- 当前小说信息
- 角色列表
- 分镜列表
- 漫画信息
- 生成任务信息
- 生成进度

## 🔐 认证机制

- 使用 JWT Token 进行认证
- Token 存储在 localStorage
- Axios 请求拦截器自动添加 Authorization Header
- 响应拦截器处理 401 未认证错误
- 路由守卫保护需要登录的页面

## 📡 WebSocket连接

使用 STOMP.js 实现 WebSocket 连接，用于实时推送生成进度：

```javascript
import { createWebSocketClient } from '@/utils/websocket'

const client = createWebSocketClient(
  taskId,
  onProgress,   // 进度更新回调
  onComplete,   // 完成回调
  onError       // 错误回调
)
```

## 🎨 UI设计规范

- 使用 Element Plus 组件库
- 响应式设计，支持移动端
- 断点设置：
  - 手机：< 768px
  - 平板：768px - 1024px
  - 桌面：> 1024px

## 📝 开发规范

1. **代码风格**: 遵循 Vue 3 Composition API 规范
2. **组件命名**: 使用 PascalCase
3. **文件命名**: 使用 kebab-case
4. **提交规范**: 使用语义化提交信息

## 🐛 调试

### 开启调试模式

在浏览器控制台输入：

```javascript
localStorage.setItem('debug', 'true')
```

### 查看API请求

在 `src/utils/request.js` 中已配置请求/响应拦截器，所有API请求都会在控制台输出。

### WebSocket调试

在 `src/utils/websocket.js` 中已配置 debug 模式，WebSocket消息会在控制台输出。

## 📚 学习资源

- [Vue 3 官方文档](https://cn.vuejs.org/)
- [Vite 官方文档](https://cn.vitejs.dev/)
- [Element Plus 官方文档](https://element-plus.org/zh-CN/)
- [Pinia 官方文档](https://pinia.vuejs.org/zh/)
- [Vue Router 官方文档](https://router.vuejs.org/zh/)

## 🚧 待开发功能

- [ ] 角色确认页面完整功能
- [ ] 分镜预览页面完整功能
- [ ] WebSocket实时进度推送
- [ ] 漫画预览和导出功能
- [ ] 历史记录列表
- [ ] 个人中心功能完善
- [ ] 图片懒加载
- [ ] 虚拟滚动优化
- [ ] 响应式布局优化

## 📄 License

Copyright © 2024 Novel2Comic. All rights reserved.

## 👥 联系方式

如有问题或建议，请联系项目维护者。
