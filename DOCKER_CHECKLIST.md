# Docker 部署验证清单

## 📋 部署前检查

### 环境检查

- [ ] Docker 已安装且版本 >= 20.10
  ```bash
  docker --version
  ```

- [ ] Docker Compose 已安装且版本 >= 2.0
  ```bash
  docker-compose --version
  ```

- [ ] 磁盘空间 >= 10GB
  ```bash
  df -h
  ```

- [ ] 可用内存 >= 4GB
  ```bash
  free -h
  ```

### 配置检查

- [ ] 已复制 `env.example` 为 `.env`
  ```bash
  ls -la .env
  ```

- [ ] 已配置 LLM API Key（智谱AI 或 DeepSeek）
  ```bash
  grep "ZHIPU_API_KEY\|DEEPSEEK_API_KEY" .env
  ```

- [ ] 已配置 AIGC API Key（硅基流动）
  ```bash
  grep "SILICONFLOW_API_KEY" .env
  ```

- [ ] 已修改默认密码（生产环境）
  ```bash
  grep "MYSQL_PASSWORD\|JWT_SECRET" .env
  ```

### 端口检查

- [ ] 80 端口未被占用（前端）
  ```bash
  netstat -tuln | grep :80 || echo "端口可用"
  ```

- [ ] 8123 端口未被占用（后端）
  ```bash
  netstat -tuln | grep :8123 || echo "端口可用"
  ```

- [ ] 3306 端口未被占用（MySQL）
  ```bash
  netstat -tuln | grep :3306 || echo "端口可用"
  ```

- [ ] 6379 端口未被占用（Redis）
  ```bash
  netstat -tuln | grep :6379 || echo "端口可用"
  ```

---

## 🚀 部署步骤

### 1. 启动服务

- [ ] 执行部署脚本
  ```bash
  bash docker-deploy.sh
  ```
  或手动启动：
  ```bash
  docker-compose up -d
  ```

### 2. 等待服务就绪（约 60-90 秒）

- [ ] 查看容器状态
  ```bash
  docker-compose ps
  ```
  所有容器应为 `Up` 状态

---

## ✅ 部署后验证

### 服务健康检查

- [ ] MySQL 健康检查
  ```bash
  docker exec novel2comic-mysql mysqladmin ping -h localhost --silent
  # 应无输出（成功）
  ```

- [ ] Redis 健康检查
  ```bash
  docker exec novel2comic-redis redis-cli ping
  # 应输出: PONG
  ```

- [ ] 后端健康检查
  ```bash
  curl http://localhost:8123/api/
  # 应返回: 200 OK 或 API 响应
  ```

- [ ] 前端健康检查
  ```bash
  curl http://localhost/health
  # 应返回: healthy
  ```

### 数据库验证

- [ ] 检查数据库是否创建
  ```bash
  docker exec novel2comic-mysql mysql -unovel2comic -pNovel2Comic@2025 -e "SHOW DATABASES;"
  # 应包含: mq_novel2comic
  ```

- [ ] 检查表是否创建
  ```bash
  docker exec novel2comic-mysql mysql -unovel2comic -pNovel2Comic@2025 mq_novel2comic -e "SHOW TABLES;"
  # 应包含: user, novel, character_profile, storyboard_panel, comic 等
  ```

### 功能验证

- [ ] 访问前端
  ```bash
  # 在浏览器打开: http://localhost
  # 应看到登录/注册页面
  ```

- [ ] 访问 API 文档
  ```bash
  # 在浏览器打开: http://localhost:8123/api/doc.html
  # 应看到 Swagger UI 文档
  ```

- [ ] 测试用户注册
  - [ ] 注册新用户成功
  - [ ] 自动登录成功

- [ ] 测试小说上传
  - [ ] 上传小说文本成功
  - [ ] 自动提取角色成功

- [ ] 测试分镜生成
  - [ ] 生成分镜成功
  - [ ] 可以编辑分镜

- [ ] 测试漫画生成
  - [ ] 生成漫画成功
  - [ ] 图片正常显示
  - [ ] 可以预览和下载

### 日志检查

- [ ] 后端日志无严重错误
  ```bash
  docker-compose logs backend | grep -i "error\|exception" | tail -20
  ```

- [ ] MySQL 日志无异常
  ```bash
  docker-compose logs mysql | grep -i "error" | tail -20
  ```

- [ ] Redis 日志无异常
  ```bash
  docker-compose logs redis | grep -i "error" | tail -20
  ```

---

## 🔍 常见问题排查

### 问题 1: 容器启动失败

**检查**:
```bash
docker-compose ps
docker-compose logs <service-name>
```

**可能原因**:
- 端口被占用 → 修改 `.env` 中的端口
- 配置错误 → 检查 `.env` 文件
- 内存不足 → 释放内存或增加系统内存

### 问题 2: 后端无法连接数据库

**检查**:
```bash
docker-compose logs backend | grep -i "datasource\|mysql"
docker exec novel2comic-mysql mysqladmin ping
```

**可能原因**:
- MySQL 未就绪 → 等待 30 秒后重试
- 密码错误 → 检查 `.env` 中的 MYSQL_PASSWORD
- 数据库未创建 → 查看 MySQL 日志

### 问题 3: API Key 错误

**检查**:
```bash
docker-compose logs backend | grep -i "api\|key"
```

**可能原因**:
- API Key 未配置 → 编辑 `.env` 文件
- API Key 无效 → 检查是否过期或权限不足
- 服务未选择 → 检查 LLM_PROVIDER 和 AIGC_PROVIDER

### 问题 4: 前端无法访问后端

**检查**:
```bash
# 进入前端容器检查 nginx 配置
docker exec novel2comic-frontend cat /etc/nginx/conf.d/default.conf

# 测试后端连接
docker exec novel2comic-frontend ping backend
```

**可能原因**:
- Nginx 配置错误 → 检查 docker/nginx.conf
- 后端未启动 → 检查后端状态
- 网络问题 → 检查 Docker 网络

---

## 📊 性能验证

### 响应时间测试

- [ ] 前端首页加载时间 < 2 秒
  ```bash
  curl -w "@-" -o /dev/null -s http://localhost <<'EOF'
  time_total:  %{time_total}s\n
  EOF
  ```

- [ ] API 响应时间 < 500ms
  ```bash
  curl -w "@-" -o /dev/null -s http://localhost:8123/api/ <<'EOF'
  time_total:  %{time_total}s\n
  EOF
  ```

### 资源使用监控

- [ ] 查看容器资源使用
  ```bash
  docker stats --no-stream
  ```

- [ ] 内存使用 < 4GB
- [ ] CPU 使用 < 50%（空闲时）

---

## 🔒 安全检查（生产环境）

- [ ] 已修改所有默认密码
- [ ] JWT_SECRET 使用强随机字符串（>= 64 字符）
- [ ] 不暴露 MySQL 和 Redis 端口（生产环境）
- [ ] 配置了 HTTPS（生产环境）
- [ ] 设置了防火墙规则
- [ ] 配置了日志监控和告警

---

## 📝 验证完成

- [ ] 所有服务正常运行
- [ ] 所有功能测试通过
- [ ] 日志无异常
- [ ] 性能指标正常
- [ ] 安全配置完成（生产环境）

---

## 🎉 部署成功！

如果以上所有检查项都通过，恭喜您已成功部署 Novel2Comic！

**下一步**:
1. 阅读用户使用指南
2. 配置备份策略
3. 设置监控告警
4. 优化性能配置

**相关文档**:
- [Docker 快速开始](./DOCKER_QUICKSTART.md)
- [Docker 完整文档](./DOCKER_README.md)
- [项目 README](./README.md)

---

**验证日期**: _______________
**验证人员**: _______________
**部署环境**: □ 开发 □ 测试 □ 生产

