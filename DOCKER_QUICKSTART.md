# 🚀 Novel2Comic Docker 快速开始指南

## 3 分钟快速部署

### 第 1 步：准备环境

确保已安装 Docker 和 Docker Compose：

```bash
# 检查 Docker
docker --version
# 输出示例：Docker version 24.0.0

# 检查 Docker Compose
docker-compose --version
# 输出示例：Docker Compose version v2.20.0
```

如果未安装，请访问：
- Docker：https://docs.docker.com/get-docker/
- Docker Compose：https://docs.docker.com/compose/install/

### 第 2 步：配置 API Key

```bash
# 1. 复制环境变量配置文件
cp env.example .env

# 2. 编辑 .env 文件（Windows 用户可使用记事本）
vim .env  # 或 nano .env 或 code .env
```

**必须配置的 API Key：**

```env
# 文本生成（至少配置一个）
ZHIPU_API_KEY=your-zhipu-api-key        # 智谱AI（推荐）
DEEPSEEK_API_KEY=your-deepseek-api-key  # 或 DeepSeek

# 图片生成
SILICONFLOW_API_KEY=your-siliconflow-api-key
```

**获取 API Key：**

| 服务 | 注册地址 | 免费额度 | 速度 |
|------|---------|---------|------|
| 智谱AI | https://open.bigmodel.cn/ | 有 | 快⚡ |
| DeepSeek | https://platform.deepseek.com/ | 有 | 快⚡ |
| 硅基流动 | https://siliconflow.cn/ | 有 | 快⚡ |

### 第 3 步：一键启动

```bash
# 方式 1：使用自动化脚本（推荐）
bash docker-deploy.sh

# 方式 2：手动启动
docker-compose up -d
```

等待约 1-2 分钟，服务启动完成。

### 第 4 步：访问应用

🎉 部署完成！打开浏览器访问：

- **前端应用**：http://localhost
- **API 文档**：http://localhost:8123/api/doc.html

## 📋 管理命令速查

```bash
# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 重启服务
docker-compose restart

# 停止服务
docker-compose stop

# 完全删除（包括数据）
docker-compose down -v
```

## 🔍 验证部署

```bash
# 1. 检查所有容器是否运行
docker-compose ps

# 应该看到 4 个容器都是 Up 状态：
# - novel2comic-frontend
# - novel2comic-backend
# - novel2comic-mysql
# - novel2comic-redis

# 2. 检查后端健康状态
curl http://localhost:8123/api/actuator/health

# 应该返回：{"status":"UP"}

# 3. 访问前端
# 在浏览器打开：http://localhost
```

## ⚠️ 常见问题

### 问题 1：端口被占用

**错误信息**：`Bind for 0.0.0.0:80 failed: port is already allocated`

**解决方法**：修改 `.env` 文件中的端口

```env
FRONTEND_PORT=8080
BACKEND_PORT=8124
```

然后重启：
```bash
docker-compose down
docker-compose up -d
```

### 问题 2：API Key 未配置

**错误信息**：后端日志显示 API 调用失败

**解决方法**：
1. 编辑 `.env` 文件，配置正确的 API Key
2. 重启后端：`docker-compose restart backend`

### 问题 3：MySQL 初始化失败

**解决方法**：
```bash
# 删除数据卷并重新初始化
docker-compose down -v
docker-compose up -d
```

### 问题 4：内存不足

**错误信息**：容器频繁重启

**解决方法**：
- 确保至少有 4GB 可用内存
- 关闭其他不必要的应用
- 或修改 `docker-compose.yml` 限制资源使用

## 🎯 下一步

- 📖 阅读详细文档：[DOCKER_README.md](./DOCKER_README.md)
- 🔧 生产环境部署：[docs/PRODUCTION_DEPLOYMENT_GUIDE.md](./docs/PRODUCTION_DEPLOYMENT_GUIDE.md)
- 💡 功能使用指南：[README.md](./README.md)

## 🆘 获取帮助

遇到问题？

1. 查看日志：`docker-compose logs -f backend`
2. 查看完整文档：`DOCKER_README.md`
3. 提交 Issue：附上错误日志和环境信息

---

**祝您使用愉快！🎉**

