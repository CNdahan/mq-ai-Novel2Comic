# Novel2Comic Docker 部署指南

## 📦 项目简介

Novel2Comic 是一个基于 AI 的小说转漫画自动化生成平台，本文档介绍如何使用 Docker 一键部署整个应用。

## 🏗️ 架构说明

本项目采用 Docker Compose 编排以下服务：

- **Frontend**：Vue 3 + Nginx（端口 80）
- **Backend**：Spring Boot 3 + Java 21（端口 8123）
- **MySQL**：8.0（端口 3306）
- **Redis**：7（端口 6379）

## 🚀 快速开始

### 前置要求

- Docker 20.10+
- Docker Compose 2.0+
- 至少 4GB 可用内存
- 至少 10GB 可用磁盘空间

### 一键部署

```bash
# 1. 克隆项目（如果还没有）
git clone <repository-url>
cd novel2comic

# 2. 复制环境变量配置
cp .env.example .env

# 3. 编辑 .env 文件，配置 API Key
vim .env  # 或使用其他编辑器

# 4. 执行一键部署脚本
bash docker-deploy.sh
```

### 手动部署

如果不使用脚本，可以手动执行：

```bash
# 1. 创建 .env 文件
cp .env.example .env

# 2. 构建镜像
docker-compose build

# 3. 启动服务
docker-compose up -d

# 4. 查看日志
docker-compose logs -f
```

## ⚙️ 配置说明

### 必须配置的项

在 `.env` 文件中，以下配置项必须修改：

```env
# LLM API Key（至少配置一个）
ZHIPU_API_KEY=your-zhipu-api-key          # 智谱AI（推荐）
DEEPSEEK_API_KEY=your-deepseek-api-key    # DeepSeek（备选）

# AIGC API Key（图片生成）
SILICONFLOW_API_KEY=your-siliconflow-api-key  # 硅基流动（推荐）

# 生产环境安全配置
JWT_SECRET=your-complex-random-secret-key-at-least-64-characters
MYSQL_ROOT_PASSWORD=your-strong-mysql-password
MYSQL_PASSWORD=your-strong-mysql-password
```

### API Key 获取方式

| 服务 | 获取地址 | 用途 | 推荐度 |
|------|---------|------|--------|
| 智谱AI | https://open.bigmodel.cn/ | 文本生成 | ⭐⭐⭐⭐⭐ |
| DeepSeek | https://platform.deepseek.com/ | 文本生成 | ⭐⭐⭐⭐⭐ |
| 硅基流动 | https://siliconflow.cn/ | 图片生成 | ⭐⭐⭐⭐⭐ |
| 阿里云通义 | https://dashscope.console.aliyun.com/ | 文本/图片 | ⭐⭐⭐ |

### 可选配置

```env
# 端口配置
MYSQL_PORT=3306
REDIS_PORT=6379
BACKEND_PORT=8123
FRONTEND_PORT=80

# Redis 密码（可选）
REDIS_PASSWORD=

# LLM 提供商选择
LLM_PROVIDER=zhipu  # zhipu / deepseek / dashscope

# AIGC 提供商选择
AIGC_PROVIDER=siliconflow  # siliconflow / wanx / mock
```

## 📋 常用命令

### 服务管理

```bash
# 启动所有服务
docker-compose up -d

# 停止所有服务
docker-compose stop

# 重启所有服务
docker-compose restart

# 删除所有服务（保留数据）
docker-compose down

# 删除所有服务和数据
docker-compose down -v

# 重新构建并启动
docker-compose up -d --build
```

### 查看状态

```bash
# 查看所有容器状态
docker-compose ps

# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f mysql
docker-compose logs -f redis
```

### 进入容器

```bash
# 进入后端容器
docker exec -it novel2comic-backend bash

# 进入 MySQL
docker exec -it novel2comic-mysql mysql -unovel2comic -p

# 进入 Redis
docker exec -it novel2comic-redis redis-cli
```

## 🔍 访问地址

部署成功后，可以通过以下地址访问：

- **前端应用**：http://localhost
- **后端 API**：http://localhost:8123/api
- **API 文档**：http://localhost:8123/api/doc.html

如果修改了端口，请使用配置的端口访问。

## 🐛 故障排除

### 1. 端口被占用

```bash
# 检查端口占用
netstat -tuln | grep -E '80|3306|6379|8123'

# 修改 .env 文件中的端口配置
FRONTEND_PORT=8080
BACKEND_PORT=8124
MYSQL_PORT=3307
REDIS_PORT=6380

# 重启服务
docker-compose down
docker-compose up -d
```

### 2. MySQL 初始化失败

```bash
# 查看 MySQL 日志
docker-compose logs mysql

# 删除数据卷重新初始化
docker-compose down -v
docker-compose up -d
```

### 3. 后端启动失败

```bash
# 查看后端日志
docker-compose logs backend

# 常见问题：
# - API Key 未配置：检查 .env 文件
# - 数据库连接失败：确认 MySQL 已启动
# - Redis 连接失败：确认 Redis 已启动
```

### 4. 前端无法访问后端

```bash
# 检查 nginx 配置
docker exec -it novel2comic-frontend cat /etc/nginx/conf.d/default.conf

# 检查后端健康状态
curl http://localhost:8123/api/actuator/health

# 查看网络连接
docker-compose exec frontend ping backend
```

### 5. 内存不足

```bash
# 查看容器资源使用
docker stats

# 限制容器内存（在 docker-compose.yml 中添加）
services:
  backend:
    mem_limit: 2g
  mysql:
    mem_limit: 512m
```

## 📊 性能优化

### 生产环境建议

1. **增加资源限制**：
```yaml
services:
  backend:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
```

2. **启用日志轮转**：
```yaml
services:
  backend:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

3. **使用生产级数据库配置**：
```bash
# 修改 docker/mysql.cnf
innodb_buffer_pool_size=2G
max_connections=500
```

## 🔒 安全建议

1. **修改所有默认密码**：
   - MySQL root 密码
   - MySQL 应用用户密码
   - JWT 密钥
   - Redis 密码（如果需要）

2. **使用 HTTPS**：
   - 配置 Nginx SSL 证书
   - 或在前面部署反向代理（如 Traefik）

3. **限制端口暴露**：
   - 生产环境不要暴露 MySQL、Redis 端口
   - 只暴露前端端口（80/443）

4. **定期备份**：
```bash
# 备份数据库
docker exec novel2comic-mysql mysqldump -uroot -p mq_novel2comic > backup.sql

# 备份数据卷
docker run --rm -v novel2comic_mysql_data:/data -v $(pwd):/backup alpine tar czf /backup/mysql-backup.tar.gz /data
```

## 📈 监控

### 查看容器健康状态

```bash
# 查看所有容器健康检查结果
docker ps --format "table {{.Names}}\t{{.Status}}"

# 查看特定容器健康检查详情
docker inspect --format='{{json .State.Health}}' novel2comic-backend | jq
```

### 查看资源使用

```bash
# 实时监控
docker stats

# 导出统计信息
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}"
```

## 🆙 更新升级

```bash
# 1. 拉取最新代码
git pull

# 2. 备份数据
docker exec novel2comic-mysql mysqldump -unovel2comic -p mq_novel2comic > backup.sql

# 3. 重新构建并启动
docker-compose up -d --build

# 4. 查看日志确认正常
docker-compose logs -f backend
```

## 📞 技术支持

如果遇到问题，请：

1. 查看日志：`docker-compose logs -f`
2. 检查配置：确认 `.env` 文件配置正确
3. 查看文档：`docs/` 目录下的相关文档
4. 提交 Issue：附上完整的错误日志

## 📄 许可证

本项目采用 MIT 许可证。

---

**Happy Coding! 🚀**

