#!/bin/bash
# =====================================================
# Novel2Comic 生产环境一键部署脚本
# =====================================================
# 功能：自动化部署应用到生产环境
# 使用：./deploy-prod.sh
# =====================================================

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置变量
APP_NAME="novel2comic"
APP_DIR="/opt/novel2comic"
LOG_DIR="/var/log/novel2comic"
IMAGE_DIR="/data/novel2comic/images"
BACKUP_DIR="/data/backups/mysql"

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查是否为root用户
check_root() {
    if [ "$EUID" -eq 0 ]; then
        print_warning "建议不要使用root用户运行此脚本"
        read -p "是否继续？(y/n): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
}

# 检查Java版本
check_java() {
    print_info "检查Java环境..."
    if ! command -v java &> /dev/null; then
        print_error "未找到Java，请先安装JDK 17+"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 17 ]; then
        print_error "Java版本过低，需要JDK 17+，当前版本: $JAVA_VERSION"
        exit 1
    fi
    
    print_success "Java环境检查通过 (version: $JAVA_VERSION)"
}

# 检查MySQL
check_mysql() {
    print_info "检查MySQL..."
    if ! command -v mysql &> /dev/null; then
        print_error "未找到MySQL，请先安装MySQL 8.0+"
        exit 1
    fi
    print_success "MySQL已安装"
}

# 检查Redis
check_redis() {
    print_info "检查Redis..."
    if ! command -v redis-cli &> /dev/null; then
        print_error "未找到Redis，请先安装Redis 6.0+"
        exit 1
    fi
    
    if ! redis-cli ping &> /dev/null; then
        print_warning "Redis服务未运行，请启动Redis"
        read -p "是否继续？(y/n): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    else
        print_success "Redis服务正常"
    fi
}

# 创建必要的目录
create_directories() {
    print_info "创建应用目录..."
    
    sudo mkdir -p "$APP_DIR"
    sudo mkdir -p "$LOG_DIR"
    sudo mkdir -p "$IMAGE_DIR"
    sudo mkdir -p "$BACKUP_DIR"
    
    sudo chown -R $USER:$USER "$APP_DIR"
    sudo chown -R $USER:$USER "$LOG_DIR"
    sudo chown -R $USER:$USER "$IMAGE_DIR"
    sudo chown -R $USER:$USER "$BACKUP_DIR"
    
    print_success "目录创建完成"
}

# 编译打包
build_app() {
    print_info "开始编译打包..."
    
    if [ ! -f "pom.xml" ]; then
        print_error "未找到pom.xml，请在项目根目录运行此脚本"
        exit 1
    fi
    
    ./mvnw clean package -DskipTests
    
    if [ $? -ne 0 ]; then
        print_error "编译打包失败"
        exit 1
    fi
    
    print_success "编译打包完成"
}

# 部署应用
deploy_app() {
    print_info "部署应用..."
    
    # 查找JAR文件
    JAR_FILE=$(find target -name "*.jar" ! -name "*-sources.jar" | head -n 1)
    if [ -z "$JAR_FILE" ]; then
        print_error "未找到JAR文件"
        exit 1
    fi
    
    # 停止旧服务
    if [ -f "$APP_DIR/app.pid" ]; then
        OLD_PID=$(cat "$APP_DIR/app.pid")
        if ps -p $OLD_PID > /dev/null 2>&1; then
            print_info "停止旧服务 (PID: $OLD_PID)..."
            kill $OLD_PID
            sleep 3
            
            # 强制结束（如果还在运行）
            if ps -p $OLD_PID > /dev/null 2>&1; then
                kill -9 $OLD_PID
            fi
        fi
        rm -f "$APP_DIR/app.pid"
    fi
    
    # 备份旧版本
    if [ -f "$APP_DIR/novel2comic.jar" ]; then
        print_info "备份旧版本..."
        mv "$APP_DIR/novel2comic.jar" "$APP_DIR/novel2comic.jar.bak.$(date +%Y%m%d_%H%M%S)"
    fi
    
    # 复制新版本
    cp "$JAR_FILE" "$APP_DIR/novel2comic.jar"
    
    # 复制配置文件（如果不存在）
    if [ ! -f "$APP_DIR/application-prod.yml" ]; then
        print_info "复制配置文件..."
        cp src/main/resources/application-prod.yml "$APP_DIR/"
        print_warning "请编辑 $APP_DIR/application-prod.yml 配置数据库、Redis和API Key"
    fi
    
    print_success "应用部署完成"
}

# 创建启动脚本
create_scripts() {
    print_info "创建启动/停止脚本..."
    
    # 启动脚本
    cat > "$APP_DIR/start.sh" << 'EOF'
#!/bin/bash
APP_JAR="/opt/novel2comic/novel2comic.jar"
APP_LOG="/var/log/novel2comic/application.log"
PID_FILE="/opt/novel2comic/app.pid"
JVM_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p $PID > /dev/null 2>&1; then
        echo "应用已经在运行 (PID: $PID)"
        exit 1
    fi
fi

echo "启动 Novel2Comic..."
nohup java $JVM_OPTS \
    -jar "$APP_JAR" \
    --spring.profiles.active=prod \
    >> "$APP_LOG" 2>&1 &

echo $! > "$PID_FILE"
echo "应用已启动 (PID: $(cat $PID_FILE))"
echo "查看日志: tail -f $APP_LOG"
EOF

    # 停止脚本
    cat > "$APP_DIR/stop.sh" << 'EOF'
#!/bin/bash
PID_FILE="/opt/novel2comic/app.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "应用未运行"
    exit 1
fi

PID=$(cat "$PID_FILE")
if ! ps -p $PID > /dev/null 2>&1; then
    echo "应用未运行"
    rm -f "$PID_FILE"
    exit 1
fi

echo "停止应用 (PID: $PID)..."
kill $PID

for i in {1..30}; do
    if ! ps -p $PID > /dev/null 2>&1; then
        rm -f "$PID_FILE"
        echo "应用已停止"
        exit 0
    fi
    sleep 1
done

echo "强制结束应用..."
kill -9 $PID
rm -f "$PID_FILE"
echo "应用已强制停止"
EOF

    # 重启脚本
    cat > "$APP_DIR/restart.sh" << 'EOF'
#!/bin/bash
cd /opt/novel2comic
./stop.sh
sleep 2
./start.sh
EOF

    # 状态检查脚本
    cat > "$APP_DIR/status.sh" << 'EOF'
#!/bin/bash
PID_FILE="/opt/novel2comic/app.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "应用未运行"
    exit 1
fi

PID=$(cat "$PID_FILE")
if ! ps -p $PID > /dev/null 2>&1; then
    echo "应用未运行（PID文件存在但进程不存在）"
    exit 1
fi

echo "应用正在运行 (PID: $PID)"
ps -p $PID -o pid,user,%cpu,%mem,vsz,rss,tty,stat,start,time,command
EOF

    chmod +x "$APP_DIR/start.sh"
    chmod +x "$APP_DIR/stop.sh"
    chmod +x "$APP_DIR/restart.sh"
    chmod +x "$APP_DIR/status.sh"
    
    print_success "脚本创建完成"
}

# 初始化数据库
init_database() {
    print_info "数据库初始化..."
    
    read -p "是否需要初始化数据库？(y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        read -p "请输入MySQL用户名: " DB_USER
        read -sp "请输入MySQL密码: " DB_PASS
        echo
        
        print_info "执行数据库初始化脚本..."
        mysql -u "$DB_USER" -p"$DB_PASS" < sql/init_database_prod.sql
        
        if [ $? -eq 0 ]; then
            print_success "数据库初始化完成"
        else
            print_error "数据库初始化失败"
            exit 1
        fi
    fi
}

# 配置检查
check_config() {
    print_info "检查配置文件..."
    
    CONFIG_FILE="$APP_DIR/application-prod.yml"
    
    # 检查是否包含默认值
    if grep -q "your-db-host" "$CONFIG_FILE" 2>/dev/null; then
        print_warning "数据库配置包含默认值，请修改"
    fi
    
    if grep -q "your-zhipu-api-key" "$CONFIG_FILE" 2>/dev/null; then
        print_warning "智谱AI API Key未配置，请修改"
    fi
    
    if grep -q "your-siliconflow-api-key" "$CONFIG_FILE" 2>/dev/null; then
        print_warning "硅基流动 API Key未配置，请修改"
    fi
    
    if grep -q "PLEASE-CHANGE-THIS" "$CONFIG_FILE" 2>/dev/null; then
        print_warning "JWT密钥未修改，请修改为复杂随机字符串"
    fi
}

# 启动应用
start_app() {
    print_info "启动应用..."
    
    read -p "是否立即启动应用？(y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        cd "$APP_DIR"
        ./start.sh
        
        print_info "等待应用启动..."
        sleep 10
        
        # 检查是否启动成功
        if [ -f "$APP_DIR/app.pid" ]; then
            PID=$(cat "$APP_DIR/app.pid")
            if ps -p $PID > /dev/null 2>&1; then
                print_success "应用启动成功 (PID: $PID)"
                print_info "API地址: http://localhost:8123/api"
                print_info "API文档: http://localhost:8123/api/doc.html"
                print_info "查看日志: tail -f $LOG_DIR/application.log"
            else
                print_error "应用启动失败，请查看日志"
                tail -n 50 "$LOG_DIR/application.log"
            fi
        else
            print_error "应用启动失败"
        fi
    fi
}

# 显示部署后的提示
show_tips() {
    echo
    print_success "==================== 部署完成 ===================="
    echo
    echo "📁 应用目录: $APP_DIR"
    echo "📄 日志目录: $LOG_DIR"
    echo "🖼️  图片目录: $IMAGE_DIR"
    echo
    echo "🚀 常用命令:"
    echo "  启动应用: $APP_DIR/start.sh"
    echo "  停止应用: $APP_DIR/stop.sh"
    echo "  重启应用: $APP_DIR/restart.sh"
    echo "  查看状态: $APP_DIR/status.sh"
    echo "  查看日志: tail -f $LOG_DIR/application.log"
    echo
    echo "⚙️  配置文件: $APP_DIR/application-prod.yml"
    echo
    echo "📚 文档: docs/PRODUCTION_DEPLOYMENT_GUIDE.md"
    echo
    print_warning "下一步操作:"
    echo "1. 编辑配置文件: vim $APP_DIR/application-prod.yml"
    echo "2. 配置数据库连接、Redis、API Key等"
    echo "3. 启动应用: $APP_DIR/start.sh"
    echo "4. 配置Nginx反向代理（可选）"
    echo "5. 配置SSL证书（可选）"
    echo
    echo "=================================================="
}

# 主函数
main() {
    echo
    print_info "==================== Novel2Comic 生产环境部署 ===================="
    echo
    
    # 环境检查
    check_root
    check_java
    check_mysql
    check_redis
    
    # 创建目录
    create_directories
    
    # 编译打包
    build_app
    
    # 部署应用
    deploy_app
    
    # 创建脚本
    create_scripts
    
    # 数据库初始化
    init_database
    
    # 配置检查
    check_config
    
    # 启动应用
    start_app
    
    # 显示提示
    show_tips
}

# 执行主函数
main

