#!/bin/bash
# =====================================================
# Novel2Comic Docker 一键部署脚本
# =====================================================
# 功能：自动检查环境、构建镜像、启动服务
# 使用：bash docker-deploy.sh
# =====================================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=====================================================${NC}"
echo -e "${GREEN}   Novel2Comic Docker 一键部署脚本${NC}"
echo -e "${GREEN}=====================================================${NC}"
echo ""

# ==================== 检查依赖 ====================
echo -e "${YELLOW}[1/7] 检查依赖...${NC}"

if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker 未安装！请先安装 Docker${NC}"
    echo "安装文档：https://docs.docker.com/get-docker/"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose 未安装！请先安装 Docker Compose${NC}"
    echo "安装文档：https://docs.docker.com/compose/install/"
    exit 1
fi

echo -e "${GREEN}✅ Docker 已安装: $(docker --version)${NC}"
echo -e "${GREEN}✅ Docker Compose 已安装: $(docker-compose --version)${NC}"
echo ""

# ==================== 检查配置文件 ====================
echo -e "${YELLOW}[2/7] 检查配置文件...${NC}"

if [ ! -f .env ]; then
    echo -e "${YELLOW}⚠️  .env 文件不存在，从 .env.example 创建...${NC}"
    cp .env.example .env
    echo -e "${GREEN}✅ 已创建 .env 文件${NC}"
    echo -e "${YELLOW}⚠️  请编辑 .env 文件，配置 API Key 等信息！${NC}"
    echo ""
    read -p "是否现在编辑 .env 文件？(y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        ${EDITOR:-vi} .env
    fi
else
    echo -e "${GREEN}✅ .env 文件已存在${NC}"
fi

# 检查必要的配置
source .env

if [ "$ZHIPU_API_KEY" = "your-zhipu-api-key" ] && [ "$SILICONFLOW_API_KEY" = "your-siliconflow-api-key" ]; then
    echo -e "${YELLOW}⚠️  警告：API Key 尚未配置！${NC}"
    echo "请编辑 .env 文件，配置以下项："
    echo "  - ZHIPU_API_KEY (智谱AI)"
    echo "  - SILICONFLOW_API_KEY (硅基流动)"
    echo ""
fi

echo ""

# ==================== 创建必要的目录 ====================
echo -e "${YELLOW}[3/7] 创建必要的目录...${NC}"

mkdir -p docker
mkdir -p images
mkdir -p logs

echo -e "${GREEN}✅ 目录创建完成${NC}"
echo ""

# ==================== 停止旧容器 ====================
echo -e "${YELLOW}[4/7] 停止旧容器...${NC}"

if [ "$(docker ps -q -f name=novel2comic)" ]; then
    echo "发现运行中的容器，正在停止..."
    docker-compose down
    echo -e "${GREEN}✅ 旧容器已停止${NC}"
else
    echo -e "${GREEN}✅ 没有运行中的容器${NC}"
fi

echo ""

# ==================== 构建镜像 ====================
echo -e "${YELLOW}[5/7] 构建 Docker 镜像...${NC}"

docker-compose build --no-cache

echo -e "${GREEN}✅ 镜像构建完成${NC}"
echo ""

# ==================== 启动服务 ====================
echo -e "${YELLOW}[6/7] 启动服务...${NC}"

docker-compose up -d

echo -e "${GREEN}✅ 服务启动完成${NC}"
echo ""

# ==================== 等待服务就绪 ====================
echo -e "${YELLOW}[7/7] 等待服务就绪...${NC}"

echo "等待 MySQL 启动..."
until docker exec novel2comic-mysql mysqladmin ping -h localhost --silent &> /dev/null; do
    printf '.'
    sleep 2
done
echo -e "${GREEN}✅ MySQL 已就绪${NC}"

echo "等待 Redis 启动..."
until docker exec novel2comic-redis redis-cli ping &> /dev/null; do
    printf '.'
    sleep 1
done
echo -e "${GREEN}✅ Redis 已就绪${NC}"

echo "等待后端服务启动（约60秒）..."
sleep 10
for i in {1..12}; do
    if curl -s http://localhost:${BACKEND_PORT:-8123}/api/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ 后端服务已就绪${NC}"
        break
    fi
    printf '.'
    sleep 5
done

echo ""
echo -e "${GREEN}=====================================================${NC}"
echo -e "${GREEN}   🎉 部署完成！${NC}"
echo -e "${GREEN}=====================================================${NC}"
echo ""
echo -e "📝 服务访问地址："
echo -e "   前端：${GREEN}http://localhost:${FRONTEND_PORT:-80}${NC}"
echo -e "   后端API：${GREEN}http://localhost:${BACKEND_PORT:-8123}/api${NC}"
echo -e "   API文档：${GREEN}http://localhost:${BACKEND_PORT:-8123}/api/doc.html${NC}"
echo ""
echo -e "🔧 管理命令："
echo -e "   查看日志：${YELLOW}docker-compose logs -f${NC}"
echo -e "   停止服务：${YELLOW}docker-compose stop${NC}"
echo -e "   启动服务：${YELLOW}docker-compose start${NC}"
echo -e "   重启服务：${YELLOW}docker-compose restart${NC}"
echo -e "   删除服务：${YELLOW}docker-compose down${NC}"
echo ""
echo -e "📊 查看容器状态："
docker-compose ps
echo ""

# ==================== 健康检查 ====================
echo -e "${YELLOW}执行健康检查...${NC}"
sleep 5

HEALTH_STATUS=$(docker inspect --format='{{.State.Health.Status}}' novel2comic-backend 2>/dev/null || echo "unknown")
if [ "$HEALTH_STATUS" = "healthy" ]; then
    echo -e "${GREEN}✅ 后端服务健康检查通过${NC}"
else
    echo -e "${YELLOW}⚠️  后端服务健康检查：$HEALTH_STATUS${NC}"
    echo -e "${YELLOW}   如果服务未就绪，请等待或查看日志：docker-compose logs backend${NC}"
fi

echo ""
echo -e "${GREEN}=====================================================${NC}"
echo -e "${GREEN}   部署完成！访问前端开始使用吧 🚀${NC}"
echo -e "${GREEN}=====================================================${NC}"

