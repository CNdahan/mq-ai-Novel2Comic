#!/bin/bash
# =====================================================
# Novel2Comic 生产环境配置检查脚本
# =====================================================
# 功能：检查生产环境配置是否正确
# 使用：./check-config.sh
# =====================================================

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 配置文件路径
CONFIG_FILE="src/main/resources/application-prod.yml"
if [ -f "/opt/novel2comic/application-prod.yml" ]; then
    CONFIG_FILE="/opt/novel2comic/application-prod.yml"
fi

# 打印函数
print_header() {
    echo -e "\n${BLUE}==================== $1 ====================${NC}\n"
}

print_check() {
    echo -n "  检查 $1 ... "
}

print_ok() {
    echo -e "${GREEN}✓ OK${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ WARNING${NC} - $1"
}

print_error() {
    echo -e "${RED}✗ ERROR${NC} - $1"
}

print_info() {
    echo -e "${BLUE}ℹ INFO${NC} - $1"
}

# 检查计数器
PASS_COUNT=0
WARN_COUNT=0
ERROR_COUNT=0

# 检查配置文件
check_config_file() {
    print_header "配置文件检查"
    
    print_check "配置文件是否存在"
    if [ -f "$CONFIG_FILE" ]; then
        print_ok
        print_info "配置文件: $CONFIG_FILE"
        ((PASS_COUNT++))
    else
        print_error "配置文件不存在: $CONFIG_FILE"
        ((ERROR_COUNT++))
        exit 1
    fi
}

# 检查数据库配置
check_database_config() {
    print_header "数据库配置检查"
    
    # 检查数据库URL
    print_check "数据库URL"
    DB_URL=$(grep -A 2 "datasource:" "$CONFIG_FILE" | grep "url:" | awk '{print $2}')
    if [[ "$DB_URL" == *"your-db-host"* ]] || [[ "$DB_URL" == *"localhost:3306"* ]]; then
        if [[ "$DB_URL" == *"your-db-host"* ]]; then
            print_warning "包含默认值，请修改为真实数据库地址"
            ((WARN_COUNT++))
        else
            print_ok
            ((PASS_COUNT++))
        fi
    else
        print_ok
        ((PASS_COUNT++))
    fi
    print_info "URL: $DB_URL"
    
    # 检查数据库用户名
    print_check "数据库用户名"
    DB_USER=$(grep -A 3 "datasource:" "$CONFIG_FILE" | grep "username:" | awk '{print $2}')
    if [[ "$DB_USER" == *"your_username"* ]] || [[ "$DB_USER" == "root" ]]; then
        if [[ "$DB_USER" == *"your_username"* ]]; then
            print_warning "包含默认值，请修改"
            ((WARN_COUNT++))
        elif [[ "$DB_USER" == "root" ]]; then
            print_warning "不建议使用root用户"
            ((WARN_COUNT++))
        fi
    else
        print_ok
        ((PASS_COUNT++))
    fi
    print_info "用户名: $DB_USER"
    
    # 检查数据库密码
    print_check "数据库密码"
    DB_PASS=$(grep -A 4 "datasource:" "$CONFIG_FILE" | grep "password:" | awk '{print $2}')
    if [[ "$DB_PASS" == *"your_password"* ]] || [ -z "$DB_PASS" ]; then
        print_warning "包含默认值或为空，请修改"
        ((WARN_COUNT++))
    else
        print_ok
        ((PASS_COUNT++))
    fi
}

# 检查Redis配置
check_redis_config() {
    print_header "Redis配置检查"
    
    # 检查Redis主机
    print_check "Redis主机"
    REDIS_HOST=$(grep -A 2 "redis:" "$CONFIG_FILE" | grep "host:" | awk '{print $2}')
    if [[ "$REDIS_HOST" == *"your-redis-host"* ]]; then
        print_warning "包含默认值，请修改"
        ((WARN_COUNT++))
    else
        print_ok
        ((PASS_COUNT++))
    fi
    print_info "主机: $REDIS_HOST"
    
    # 检查Redis密码
    print_check "Redis密码"
    REDIS_PASS=$(grep -A 4 "redis:" "$CONFIG_FILE" | grep "password:" | awk '{print $2}')
    if [[ "$REDIS_PASS" == *"your_redis_password"* ]]; then
        print_warning "包含默认值，请修改（如果Redis设置了密码）"
        ((WARN_COUNT++))
    else
        print_ok
        ((PASS_COUNT++))
    fi
}

# 检查JWT配置
check_jwt_config() {
    print_header "JWT配置检查"
    
    print_check "JWT密钥"
    JWT_SECRET=$(grep -A 1 "jwt:" "$CONFIG_FILE" | grep "secret:" | awk '{print $2}')
    
    if [[ "$JWT_SECRET" == *"PLEASE-CHANGE-THIS"* ]]; then
        print_error "JWT密钥未修改，必须修改为复杂随机字符串！"
        ((ERROR_COUNT++))
    elif [ ${#JWT_SECRET} -lt 32 ]; then
        print_warning "JWT密钥长度过短（< 32字符），建议使用更长的密钥"
        ((WARN_COUNT++))
    else
        print_ok
        ((PASS_COUNT++))
    fi
    
    print_info "密钥长度: ${#JWT_SECRET} 字符"
}

# 检查LLM配置
check_llm_config() {
    print_header "LLM配置检查"
    
    # 检查提供商
    print_check "LLM提供商"
    LLM_PROVIDER=$(grep "provider:" "$CONFIG_FILE" | head -n 1 | awk '{print $2}')
    print_ok
    print_info "提供商: $LLM_PROVIDER"
    ((PASS_COUNT++))
    
    # 检查智谱AI配置
    if [[ "$LLM_PROVIDER" == "zhipu" ]]; then
        print_check "智谱AI API Key"
        ZHIPU_KEY=$(grep -A 3 "zhipu:" "$CONFIG_FILE" | grep "api-key:" | awk '{print $2}')
        
        if [[ "$ZHIPU_KEY" == *"your-zhipu-api-key"* ]] || [ -z "$ZHIPU_KEY" ]; then
            print_error "智谱AI API Key未配置"
            print_info "获取地址: https://open.bigmodel.cn/"
            ((ERROR_COUNT++))
        else
            print_ok
            print_info "API Key: ${ZHIPU_KEY:0:20}..."
            ((PASS_COUNT++))
        fi
    fi
    
    # 检查DeepSeek配置
    if [[ "$LLM_PROVIDER" == "deepseek" ]]; then
        print_check "DeepSeek API Key"
        DEEPSEEK_KEY=$(grep -A 3 "deepseek:" "$CONFIG_FILE" | grep "api-key:" | awk '{print $2}')
        
        if [[ "$DEEPSEEK_KEY" == *"your-deepseek-api-key"* ]] || [ -z "$DEEPSEEK_KEY" ]; then
            print_error "DeepSeek API Key未配置"
            print_info "获取地址: https://platform.deepseek.com/"
            ((ERROR_COUNT++))
        else
            print_ok
            ((PASS_COUNT++))
        fi
    fi
}

# 检查AIGC配置
check_aigc_config() {
    print_header "AIGC配置检查"
    
    # 检查提供商
    print_check "AIGC提供商"
    AIGC_PROVIDER=$(grep -A 1 "aigc:" "$CONFIG_FILE" | grep "provider:" | awk '{print $2}')
    print_ok
    print_info "提供商: $AIGC_PROVIDER"
    ((PASS_COUNT++))
    
    # 检查硅基流动配置
    if [[ "$AIGC_PROVIDER" == "siliconflow" ]]; then
        print_check "硅基流动 API Key"
        SILICON_KEY=$(grep -A 3 "siliconflow:" "$CONFIG_FILE" | grep "api-key:" | awk '{print $2}')
        
        if [[ "$SILICON_KEY" == *"your-siliconflow-api-key"* ]] || [ -z "$SILICON_KEY" ]; then
            print_error "硅基流动 API Key未配置"
            print_info "获取地址: https://siliconflow.cn/"
            ((ERROR_COUNT++))
        else
            print_ok
            print_info "API Key: ${SILICON_KEY:0:20}..."
            ((PASS_COUNT++))
        fi
        
        print_check "硅基流动模型"
        SILICON_MODEL=$(grep -A 6 "siliconflow:" "$CONFIG_FILE" | grep "model:" | awk '{print $2}')
        print_ok
        print_info "模型: $SILICON_MODEL"
        ((PASS_COUNT++))
    fi
}

# 检查目录权限
check_directories() {
    print_header "目录权限检查"
    
    # 检查图片目录
    print_check "图片存储目录"
    IMAGE_DIR=$(grep -A 2 "image:" "$CONFIG_FILE" | grep "path:" | awk '{print $2}')
    
    # 转换相对路径为绝对路径
    if [[ "$IMAGE_DIR" == ./* ]]; then
        IMAGE_DIR="/opt/novel2comic/${IMAGE_DIR#./}"
    fi
    
    if [ -d "$IMAGE_DIR" ]; then
        if [ -w "$IMAGE_DIR" ]; then
            print_ok
            ((PASS_COUNT++))
        else
            print_warning "目录存在但无写权限"
            ((WARN_COUNT++))
        fi
    else
        print_warning "目录不存在，需要创建"
        ((WARN_COUNT++))
    fi
    print_info "路径: $IMAGE_DIR"
    
    # 检查日志目录
    print_check "日志目录"
    LOG_DIR="/var/log/novel2comic"
    if [ -d "$LOG_DIR" ]; then
        if [ -w "$LOG_DIR" ]; then
            print_ok
            ((PASS_COUNT++))
        else
            print_warning "目录存在但无写权限"
            ((WARN_COUNT++))
        fi
    else
        print_warning "目录不存在，需要创建"
        ((WARN_COUNT++))
    fi
    print_info "路径: $LOG_DIR"
}

# 检查数据库连接
check_database_connection() {
    print_header "数据库连接测试"
    
    if ! command -v mysql &> /dev/null; then
        print_warning "MySQL客户端未安装，跳过连接测试"
        ((WARN_COUNT++))
        return
    fi
    
    # 提取数据库配置
    DB_HOST=$(echo "$DB_URL" | sed -n 's/.*:\/\/\([^:]*\):.*/\1/p')
    DB_PORT=$(echo "$DB_URL" | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
    DB_NAME=$(echo "$DB_URL" | sed -n 's/.*\/\([^?]*\).*/\1/p')
    
    print_check "连接到 $DB_HOST:$DB_PORT"
    
    if [[ "$DB_HOST" == *"your-db-host"* ]]; then
        print_warning "数据库地址未配置，跳过连接测试"
        ((WARN_COUNT++))
        return
    fi
    
    if mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASS" -e "USE $DB_NAME; SELECT 1;" &>/dev/null; then
        print_ok
        ((PASS_COUNT++))
        
        # 检查表是否存在
        print_check "检查数据库表"
        TABLE_COUNT=$(mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASS" -N -e "USE $DB_NAME; SHOW TABLES;" 2>/dev/null | wc -l)
        
        if [ "$TABLE_COUNT" -eq 9 ]; then
            print_ok
            print_info "找到 $TABLE_COUNT 张表"
            ((PASS_COUNT++))
        elif [ "$TABLE_COUNT" -eq 0 ]; then
            print_warning "数据库为空，需要执行初始化脚本"
            print_info "执行: mysql -u $DB_USER -p < sql/init_database_prod.sql"
            ((WARN_COUNT++))
        else
            print_warning "表数量不正确 (期望: 9, 实际: $TABLE_COUNT)"
            ((WARN_COUNT++))
        fi
    else
        print_error "无法连接到数据库"
        print_info "请检查数据库配置和网络连接"
        ((ERROR_COUNT++))
    fi
}

# 检查Redis连接
check_redis_connection() {
    print_header "Redis连接测试"
    
    if ! command -v redis-cli &> /dev/null; then
        print_warning "Redis客户端未安装，跳过连接测试"
        ((WARN_COUNT++))
        return
    fi
    
    print_check "连接到 $REDIS_HOST:6379"
    
    if [[ "$REDIS_HOST" == *"your-redis-host"* ]]; then
        print_warning "Redis地址未配置，跳过连接测试"
        ((WARN_COUNT++))
        return
    fi
    
    if [ -n "$REDIS_PASS" ] && [[ "$REDIS_PASS" != *"your_redis_password"* ]]; then
        PING_RESULT=$(redis-cli -h "$REDIS_HOST" -a "$REDIS_PASS" ping 2>/dev/null)
    else
        PING_RESULT=$(redis-cli -h "$REDIS_HOST" ping 2>/dev/null)
    fi
    
    if [ "$PING_RESULT" == "PONG" ]; then
        print_ok
        ((PASS_COUNT++))
    else
        print_error "无法连接到Redis"
        print_info "请检查Redis配置和服务状态"
        ((ERROR_COUNT++))
    fi
}

# 显示总结
show_summary() {
    print_header "检查总结"
    
    TOTAL=$((PASS_COUNT + WARN_COUNT + ERROR_COUNT))
    
    echo -e "  总检查项: ${BLUE}$TOTAL${NC}"
    echo -e "  通过: ${GREEN}$PASS_COUNT${NC}"
    echo -e "  警告: ${YELLOW}$WARN_COUNT${NC}"
    echo -e "  错误: ${RED}$ERROR_COUNT${NC}"
    echo
    
    if [ $ERROR_COUNT -eq 0 ] && [ $WARN_COUNT -eq 0 ]; then
        echo -e "${GREEN}✓ 所有检查通过！配置正确，可以部署。${NC}"
        echo
        return 0
    elif [ $ERROR_COUNT -eq 0 ]; then
        echo -e "${YELLOW}⚠ 有 $WARN_COUNT 个警告，建议修复后再部署。${NC}"
        echo
        return 1
    else
        echo -e "${RED}✗ 有 $ERROR_COUNT 个错误，必须修复后才能部署！${NC}"
        echo
        return 2
    fi
}

# 主函数
main() {
    echo
    echo -e "${BLUE}==================== Novel2Comic 配置检查 ====================${NC}"
    echo
    
    check_config_file
    check_database_config
    check_redis_config
    check_jwt_config
    check_llm_config
    check_aigc_config
    check_directories
    check_database_connection
    check_redis_connection
    
    show_summary
    
    echo -e "${BLUE}================================================================${NC}"
    echo
}

# 执行
main
exit $?

