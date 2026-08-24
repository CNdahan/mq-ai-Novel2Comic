# 🚀 使用 Docker 快速启动 Novel2Comic

> 仅需 3 分钟，无需安装 Java、MySQL、Redis！

## 步骤 1：安装 Docker

### Windows 用户
下载并安装 [Docker Desktop for Windows](https://docs.docker.com/desktop/install/windows-install/)

### macOS 用户
下载并安装 [Docker Desktop for Mac](https://docs.docker.com/desktop/install/mac-install/)

### Linux 用户
```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 安装 Docker Compose
sudo apt-get install docker-compose-plugin
```

验证安装：
```bash
docker --version
docker-compose --version
```

---

## 步骤 2：配置 API Key

### 2.1 复制配置文件

```bash
cp env.example .env
```

### 2.2 获取 API Key

| 服务 | 用途 | 注册地址 | 免费额度 |
|------|------|---------|---------|
| 🤖 智谱AI | 文本生成 | https://open.bigmodel.cn/ | ✅ 有 |
| 🎨 硅基流动 | 图片生成 | https://siliconflow.cn/ | ✅ 有 |

### 2.3 编辑 .env 文件

使用任意文本编辑器打开 `.env` 文件，填入你的 API Key：

```env
# 文本生成（必填）
ZHIPU_API_KEY=你的智谱AI的API密钥

# 图片生成（必填）
SILICONFLOW_API_KEY=你的硅基流动的API密钥
```

保存文件即可。

---

## 步骤 3：启动应用

### 方式 1：自动脚本（推荐）

#### Windows (PowerShell)
```powershell
# 如果提示权限错误，先执行：
# Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# 启动（使用 docker-compose 命令）
docker-compose up -d
```

#### macOS / Linux (Bash)
```bash
bash docker-deploy.sh
```

### 方式 2：手动启动

```bash
docker-compose up -d
```

---

## 步骤 4：访问应用

等待约 1-2 分钟，然后在浏览器中打开：

- 🌐 **前端应用**: http://localhost
- 📖 **API 文档**: http://localhost:8123/api/doc.html

---

## 🎉 完成！

现在你可以：
1. 注册一个新账号
2. 上传小说文本
3. 生成漫画分镜
4. 查看生成的漫画

---

## 📋 常用命令

```bash
# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose stop

# 启动服务
docker-compose start

# 重启服务
docker-compose restart

# 删除服务（保留数据）
docker-compose down

# 删除服务和数据
docker-compose down -v
```

---

## ❓ 遇到问题？

### 端口被占用

**现象**: 启动失败，提示端口被占用

**解决**: 修改 `.env` 文件中的端口
```env
FRONTEND_PORT=8080
BACKEND_PORT=8124
```

### API Key 错误

**现象**: 生成失败，提示 API 调用错误

**解决**: 
1. 检查 `.env` 文件中的 API Key 是否正确
2. 确认 API Key 是否有剩余额度
3. 重启后端服务：`docker-compose restart backend`

### 容器启动失败

**解决**:
```bash
# 查看详细错误
docker-compose logs <服务名>

# 例如：
docker-compose logs backend
docker-compose logs mysql
```

---

## 📚 详细文档

需要更多帮助？查看详细文档：

- 🚀 [快速开始指南](./DOCKER_QUICKSTART.md) - 详细的 3 分钟部署指南
- 📖 [完整部署文档](./DOCKER_README.md) - 包含故障排除和优化建议
- ✅ [部署验证清单](./DOCKER_CHECKLIST.md) - 逐项验证部署是否成功
- 📁 [文件清单](docs/DOCKER_FILES_SUMMARY.md) - 所有 Docker 文件说明

---

## 🛑 停止和清理

### 停止服务（保留数据）
```bash
docker-compose stop
```

### 删除服务（保留数据）
```bash
docker-compose down
```

### 完全清理（包括数据）
```bash
docker-compose down -v
```

---

**需要帮助？** 查看 [完整文档](./DOCKER_README.md) 或提交 Issue。

