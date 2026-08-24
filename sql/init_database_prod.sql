-- =====================================================
-- Novel2Comic 生产环境数据库初始化脚本
-- =====================================================
-- 功能：一键创建完整的数据库表结构
-- 版本：v1.2.0
-- 作者：MQ
-- 日期：2025-10-26
-- =====================================================
-- 使用说明：
-- 1. 登录MySQL：mysql -u root -p
-- 2. 执行脚本：source /path/to/init_database_prod.sql
-- 3. 或者：mysql -u root -p < init_database_prod.sql
-- =====================================================

-- ==================== 创建数据库 ====================
CREATE DATABASE IF NOT EXISTS mq_novel2comic 
  DEFAULT CHARACTER SET utf8mb4 
  DEFAULT COLLATE utf8mb4_unicode_ci;

-- 切换到目标数据库
USE mq_novel2comic;

-- ==================== 用户表 ====================
CREATE TABLE IF NOT EXISTS `user` (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    userName        VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    userEmail       VARCHAR(100) UNIQUE COMMENT '邮箱',
    userPassword    VARCHAR(255) NOT NULL COMMENT '密码哈希',
    userAvatar      VARCHAR(500) NULL COMMENT '头像URL',
    quotaRemain     INT          DEFAULT 10 COMMENT '剩余生成次数',
    quotaTotal      INT          DEFAULT 10 COMMENT '总配额',
    vipLevel        TINYINT      DEFAULT 0 COMMENT 'VIP等级：0-免费，1-月费，2-年费',
    vipExpireAt     DATETIME     NULL COMMENT 'VIP过期时间',
    createTime      DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime      DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete        TINYINT      DEFAULT 0                 NOT NULL COMMENT '是否删除：0-正常，1-已删除',
    INDEX idx_userName (userName),
    INDEX idx_userEmail (userEmail),
    INDEX idx_createTime (createTime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ==================== 小说表 ====================
CREATE TABLE IF NOT EXISTS `novel` (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '小说ID',
    userId        BIGINT                             NOT NULL COMMENT '用户ID',
    novelTitle    VARCHAR(200)                       NULL COMMENT '小说标题',
    novelContent  TEXT                               NOT NULL COMMENT '小说内容',
    contentLength INT                                NOT NULL COMMENT '字数',
    sourceType    VARCHAR(20) DEFAULT 'direct'       NOT NULL COMMENT '来源类型：direct/file/url',
    sourceUrl     VARCHAR(500)                       NULL COMMENT '源URL',
    status        VARCHAR(20) DEFAULT 'pending'      NOT NULL COMMENT '状态：pending/processing/completed/failed',
    errorMessage  TEXT                               NULL COMMENT '错误信息',
    createTime    DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime    DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete      TINYINT     DEFAULT 0                  NOT NULL COMMENT '是否删除',
    INDEX idx_userId (userId),
    INDEX idx_status (status),
    INDEX idx_createTime (createTime),
    FOREIGN KEY (userId) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小说表';

-- ==================== 角色档案表 ====================
CREATE TABLE IF NOT EXISTS `character_profile` (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    novelId            BIGINT                              NOT NULL COMMENT '小说ID',
    characterName      VARCHAR(100)                        NOT NULL COMMENT '角色名称',
    descriptionCn      TEXT                                NULL COMMENT '中文描述',
    descriptionEn      TEXT                                NOT NULL COMMENT '英文描述',
    appearanceData     JSON                                NULL COMMENT '外貌特征数据',
    referenceImageUrl  VARCHAR(500)                        NULL COMMENT '参考图URL',
    embeddingVector    JSON                                NULL COMMENT '特征向量',
    vectorId           VARCHAR(100)                        NULL COMMENT '向量数据库ID',
    useCount           INT          DEFAULT 0              NOT NULL COMMENT '使用次数',
    createTime         DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime         DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete           TINYINT      DEFAULT 0                  NOT NULL COMMENT '是否删除',
    UNIQUE KEY uk_novel_character (novelId, characterName),
    INDEX idx_novelId (novelId),
    INDEX idx_characterName (characterName),
    FOREIGN KEY (novelId) REFERENCES `novel`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色档案表';

-- ==================== 分镜脚本表（支持多版本）====================
CREATE TABLE IF NOT EXISTS `storyboard_panel` (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分镜ID',
    novelId        BIGINT                              NOT NULL COMMENT '小说ID',
    version        INT                                 NOT NULL DEFAULT 1 COMMENT '分镜版本号',
    isCurrent      TINYINT                             NOT NULL DEFAULT 1 COMMENT '是否为当前版本：1-是，0-否',
    versionNote    VARCHAR(200)                        NULL COMMENT '版本说明',
    panelIndex     INT                                 NOT NULL COMMENT '分镜序号',
    sceneType      VARCHAR(50)                         NOT NULL COMMENT '场景类型',
    shotType       VARCHAR(50)                         NOT NULL COMMENT '镜头类型',
    descriptionCn  TEXT                                NOT NULL COMMENT '场景描述（中文）',
    descriptionEn  TEXT                                NOT NULL COMMENT '场景描述（英文）',
    characterList  JSON                                NULL COMMENT '角色列表',
    environment    VARCHAR(200)                        NULL COMMENT '环境描述',
    mood           VARCHAR(50)                         NULL COMMENT '情绪氛围',
    dialogueText   TEXT                                NULL COMMENT '对话文本',
    createTime     DATETIME DEFAULT CURRENT_TIMESTAMP  NOT NULL COMMENT '创建时间',
    isDelete       TINYINT  DEFAULT 0                  NOT NULL COMMENT '是否删除',
    UNIQUE KEY uk_novel_version_panel (novelId, version, panelIndex),
    INDEX idx_novelId (novelId),
    INDEX idx_novel_version (novelId, version),
    INDEX idx_novel_current (novelId, isCurrent),
    FOREIGN KEY (novelId) REFERENCES `novel`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分镜脚本表（支持多版本）';

-- ==================== 漫画作品表 ====================
CREATE TABLE IF NOT EXISTS `comic` (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '漫画ID',
    userId        BIGINT                              NOT NULL COMMENT '用户ID',
    novelId       BIGINT                              NOT NULL COMMENT '小说ID',
    comicTitle    VARCHAR(200)                        NOT NULL COMMENT '漫画标题',
    coverUrl      VARCHAR(500)                        NULL COMMENT '封面URL',
    panelCount    INT                                 NOT NULL COMMENT '分镜数量',
    style         VARCHAR(50)                         NOT NULL COMMENT '风格',
    totalCost     DECIMAL(10,4) DEFAULT 0             NOT NULL COMMENT '总成本',
    totalTimeMs   INT                                 NULL COMMENT '总耗时',
    cacheHitRate  DOUBLE                              NULL COMMENT '缓存命中率',
    status        VARCHAR(20) DEFAULT 'generating'    NOT NULL COMMENT '状态：generating/completed/failed',
    viewCount     INT          DEFAULT 0              NOT NULL COMMENT '浏览次数',
    likeCount     INT          DEFAULT 0              NOT NULL COMMENT '点赞次数',
    shareCount    INT          DEFAULT 0              NOT NULL COMMENT '分享次数',
    isPublic      TINYINT      DEFAULT 0              NOT NULL COMMENT '是否公开',
    createTime    DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime    DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete      TINYINT      DEFAULT 0                  NOT NULL COMMENT '是否删除',
    INDEX idx_userId (userId),
    INDEX idx_novelId (novelId),
    INDEX idx_status (status),
    INDEX idx_createTime (createTime),
    FOREIGN KEY (userId) REFERENCES `user`(id) ON DELETE CASCADE,
    FOREIGN KEY (novelId) REFERENCES `novel`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='漫画作品表';

-- ==================== 漫画面板表 ====================
CREATE TABLE IF NOT EXISTS `comic_panel` (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '漫画面板ID',
    comicId         BIGINT                              NOT NULL COMMENT '漫画ID',
    novelId         BIGINT                              NOT NULL COMMENT '小说ID',
    storyboardId    BIGINT                              NOT NULL COMMENT '分镜ID',
    panelIndex      INT                                 NOT NULL COMMENT '面板序号',
    imageUrl        VARCHAR(500)                        NOT NULL COMMENT '图片URL',
    imageWidth      INT                                 NULL COMMENT '宽度',
    imageHeight     INT                                 NULL COMMENT '高度',
    imageSize       BIGINT                              NULL COMMENT '图片大小',
    style           VARCHAR(50)                         NOT NULL COMMENT '风格',
    promptText      TEXT                                NOT NULL COMMENT 'Prompt',
    negativePrompt  TEXT                                NULL COMMENT '负面Prompt',
    isCached        TINYINT      DEFAULT 0              NOT NULL COMMENT '是否缓存命中',
    cacheSimilarity DOUBLE                              NULL COMMENT '缓存相似度',
    generateTimeMs  INT                                 NULL COMMENT '生成耗时（ms）',
    apiCost         DECIMAL(10,4)                       NULL COMMENT 'API成本',
    createTime      DATETIME DEFAULT CURRENT_TIMESTAMP  NOT NULL COMMENT '创建时间',
    isDelete        TINYINT  DEFAULT 0                  NOT NULL COMMENT '是否删除',
    UNIQUE KEY uk_comic_panel (comicId, panelIndex),
    INDEX idx_comicId (comicId),
    INDEX idx_novelId (novelId),
    INDEX idx_storyboardId (storyboardId),
    FOREIGN KEY (comicId) REFERENCES `comic`(id) ON DELETE CASCADE,
    FOREIGN KEY (novelId) REFERENCES `novel`(id) ON DELETE CASCADE,
    FOREIGN KEY (storyboardId) REFERENCES `storyboard_panel`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='漫画面板表';

-- ==================== 生成任务表 ====================
CREATE TABLE IF NOT EXISTS `generate_task` (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '任务ID',
    taskUuid          VARCHAR(100)                        NOT NULL COMMENT '任务UUID',
    userId            BIGINT                              NOT NULL COMMENT '用户ID',
    novelId           BIGINT                              NOT NULL COMMENT '小说ID',
    taskType          VARCHAR(50)                         NOT NULL COMMENT '任务类型',
    status            VARCHAR(20) DEFAULT 'pending'       NOT NULL COMMENT '状态：pending/processing/completed/failed',
    progressPercent   INT         DEFAULT 0               NOT NULL COMMENT '进度百分比',
    currentStep       VARCHAR(100)                        NULL COMMENT '当前步骤',
    totalPanels       INT                                 NULL COMMENT '总分镜数',
    completedPanels   INT         DEFAULT 0               NULL COMMENT '已完成分镜数',
    errorMessage      TEXT                                NULL COMMENT '错误信息',
    startTime         DATETIME                            NULL COMMENT '开始时间',
    completeTime      DATETIME                            NULL COMMENT '完成时间',
    createTime        DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime        DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete          TINYINT     DEFAULT 0                  NOT NULL COMMENT '是否删除',
    UNIQUE KEY uk_taskUuid (taskUuid),
    INDEX idx_userId (userId),
    INDEX idx_novelId (novelId),
    INDEX idx_status (status),
    INDEX idx_createTime (createTime),
    FOREIGN KEY (userId) REFERENCES `user`(id) ON DELETE CASCADE,
    FOREIGN KEY (novelId) REFERENCES `novel`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生成任务表';

-- ==================== 用户操作日志表 ====================
CREATE TABLE IF NOT EXISTS `user_action_log` (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    userId        BIGINT                              NOT NULL COMMENT '用户ID',
    actionType    VARCHAR(50)                         NOT NULL COMMENT '操作类型',
    resourceType  VARCHAR(50)                         NULL COMMENT '资源类型',
    resourceId    BIGINT                              NULL COMMENT '资源ID',
    ipAddress     VARCHAR(50)                         NULL COMMENT 'IP地址',
    userAgent     VARCHAR(500)                        NULL COMMENT 'User Agent',
    createTime    DATETIME DEFAULT CURRENT_TIMESTAMP  NOT NULL COMMENT '创建时间',
    isDelete      TINYINT  DEFAULT 0                  NOT NULL COMMENT '是否删除',
    INDEX idx_userId (userId),
    INDEX idx_actionType (actionType),
    INDEX idx_createTime (createTime),
    FOREIGN KEY (userId) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户操作日志表';

-- ==================== API调用统计表 ====================
CREATE TABLE IF NOT EXISTS `api_call_stat` (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '统计ID',
    userId          BIGINT                              NOT NULL COMMENT '用户ID',
    apiType         VARCHAR(50)                         NOT NULL COMMENT 'API类型',
    modelName       VARCHAR(100)                        NULL COMMENT '模型名称',
    requestTokens   INT                                 NULL COMMENT '请求Token数',
    responseTokens  INT                                 NULL COMMENT '响应Token数',
    imageCount      INT         DEFAULT 0               NULL COMMENT '图片数量',
    costAmount      DECIMAL(10,4)                       NULL COMMENT '成本金额',
    responseTimeMs  INT                                 NULL COMMENT '响应时间(ms)',
    isSuccess       TINYINT     DEFAULT 1               NOT NULL COMMENT '是否成功',
    errorMessage    TEXT                                NULL COMMENT '错误信息',
    createTime      DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    isDelete        TINYINT     DEFAULT 0                  NOT NULL COMMENT '是否删除',
    INDEX idx_userId (userId),
    INDEX idx_apiType (apiType),
    INDEX idx_createTime (createTime),
    FOREIGN KEY (userId) REFERENCES `user`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API调用统计表';

-- =====================================================
-- 初始化完成提示
-- =====================================================
SELECT '✅ 数据库初始化完成！' AS '状态';
SELECT '📊 数据库名称：mq_novel2comic' AS '信息';
SELECT '📋 共创建10张表：user, novel, character_profile, storyboard_panel, comic, comic_panel, generate_task, user_action_log, api_call_stat' AS '表信息';

-- ==================== 验证脚本 ====================
-- 查看所有表
SHOW TABLES;

-- 查看表结构（可选）
-- DESC user;
-- DESC novel;
-- DESC character_profile;
-- DESC storyboard_panel;
-- DESC comic;
-- DESC comic_panel;
-- DESC generate_task;
-- DESC user_action_log;
-- DESC api_call_stat;

-- =====================================================
-- 数据库维护建议
-- =====================================================
-- 1. 定期备份：
--    mysqldump -u root -p mq_novel2comic > backup_$(date +%Y%m%d).sql
-- 
-- 2. 定期清理日志表（可选）：
--    DELETE FROM user_action_log WHERE createTime < DATE_SUB(NOW(), INTERVAL 30 DAY);
--    DELETE FROM api_call_stat WHERE createTime < DATE_SUB(NOW(), INTERVAL 30 DAY);
-- 
-- 3. 优化表（可选）：
--    OPTIMIZE TABLE user;
--    OPTIMIZE TABLE novel;
--    OPTIMIZE TABLE comic;
--    
-- 4. 查看表大小：
--    SELECT 
--      table_name AS '表名',
--      table_rows AS '记录数',
--      CONCAT(ROUND(data_length / 1024 / 1024, 2), 'MB') AS '数据大小',
--      CONCAT(ROUND(index_length / 1024 / 1024, 2), 'MB') AS '索引大小'
--    FROM information_schema.tables
--    WHERE table_schema = 'mq_novel2comic'
--    ORDER BY data_length DESC;
-- =====================================================

