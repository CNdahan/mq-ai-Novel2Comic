import { createApp } from 'vue'
import './style.css'
import App from './App.vue'

// 引入路由
import router from './router'

// 引入状态管理
import pinia from './store'

// 引入Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

// 引入图标
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)

// 注册Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 使用插件
app.use(router)
app.use(pinia)
app.use(ElementPlus, {
  locale: zhCn,
})

app.mount('#app')
