-- 创建库
create database if not exists mq_novel2comic;

-- 切换库
use mq_novel2comic;

-- 用户表
CREATE TABLE user (
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
    INDEX idx_userEmail (userEmail),
    INDEX idx_createTime (createTime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 小说表
create table if not exists novel
(
    id            bigint auto_increment comment '小说ID' primary key,
    userId        bigint                            not null comment '用户ID',
    novelTitle    varchar(200)                      null comment '小说标题',
    novelContent  text                              not null comment '小说内容',
    contentLength int                               not null comment '字数',
    sourceType    varchar(20) default 'direct'      not null comment '来源类型：direct/file/url',
    sourceUrl     varchar(500)                      null comment '源URL',
    status        varchar(20) default 'pending'     not null comment '状态：pending/processing/completed/failed',
    errorMessage  text                              null comment '错误信息',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint      default 0                 not null comment '是否删除',
    index idx_userId (userId),
    index idx_status (status),
    foreign key (userId) references user(id) on delete cascade
) comment '小说表' collate = utf8mb4_unicode_ci;

-- 角色档案表
create table if not exists character_profile
(
    id                 bigint auto_increment comment '角色ID' primary key,
    novelId            bigint                              not null comment '小说ID',
    characterName      varchar(100)                        not null comment '角色名称',
    descriptionCn      text                                null comment '中文描述',
    descriptionEn      text                                not null comment '英文描述',
    appearanceData     json                                null comment '外貌特征数据',
    referenceImageUrl  varchar(500)                        null comment '参考图URL',
    embeddingVector    json                                null comment '特征向量',
    vectorId           varchar(100)                        null comment '向量数据库ID',
    useCount           int          default 0              not null comment '使用次数',
    createTime         datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime         datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      	   tinyint      default 0                 not null comment '是否删除',
    unique key uk_novel_character (novelId, characterName),
    index idx_novelId (novelId),
    foreign key (novelId) references novel(id) on delete cascade
) comment '角色档案表' collate = utf8mb4_unicode_ci;

-- 分镜脚本表
create table if not exists storyboard_panel
(
    id             bigint auto_increment comment '分镜ID' primary key,
    novelId        bigint                              not null comment '小说ID',
    panelIndex     int                                 not null comment '分镜序号',
    sceneType      varchar(50)                         not null comment '场景类型',
    shotType       varchar(50)                         not null comment '镜头类型',
    descriptionCn  text                                not null comment '场景描述（中文）',
    descriptionEn  text                                not null comment '场景描述（英文）',
    characterList  json                                null comment '角色列表',
    environment    varchar(200)                        null comment '环境描述',
    mood           varchar(50)                         null comment '情绪氛围',
    dialogueText   text                                null comment '对话文本',
    createTime     datetime default CURRENT_TIMESTAMP  not null comment '创建时间',
    isDelete       tinyint      default 0                 not null comment '是否删除',
    unique key uk_novel_panel (novelId, panelIndex),
    index idx_novelId (novelId),
    foreign key (novelId) references novel(id) on delete cascade
) comment '分镜脚本表' collate = utf8mb4_unicode_ci;

-- 漫画表
create table if not exists comic
(
    id            bigint auto_increment comment '漫画ID' primary key,
    userId        bigint                              not null comment '用户ID',
    novelId       bigint                              not null comment '小说ID',
    comicTitle    varchar(200)                        not null comment '漫画标题',
    coverUrl      varchar(500)                        null comment '封面URL',
    panelCount    int                                 not null comment '分镜数量',
    style         varchar(50)                         not null comment '风格',
    totalCost     decimal(10,4) default 0             not null comment '总成本',
    totalTimeMs   int                                 null comment '总耗时',
    cacheHitRate  double                              null comment '缓存命中率',
    status        varchar(20) default 'generating'    not null comment '状态',
    viewCount     int          default 0              not null comment '浏览次数',
    likeCount     int          default 0              not null comment '点赞次数',
    shareCount    int          default 0              not null comment '分享次数',
    isPublic      tinyint      default 0              not null comment '是否公开',
    createTime    datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint      default 0                 not null comment '是否删除',
    index idx_userId (userId),
    foreign key (userId) references user(id) on delete cascade,
    foreign key (novelId) references novel(id) on delete cascade
) comment '漫画作品表' collate = utf8mb4_unicode_ci;

-- 漫画面板表
create table if not exists comic_panel
(
    id              bigint auto_increment comment '漫画面板ID' primary key,
    comicId         bigint                              not null comment '漫画ID',
    novelId         bigint                              not null comment '小说ID',
    storyboardId    bigint                              not null comment '分镜ID',
    panelIndex      int                                 not null comment '面板序号',
    imageUrl        varchar(500)                        not null comment '图片URL',
    imageWidth      int                                 null comment '宽度',
    imageHeight     int                                 null comment '高度',
    imageSize       bigint                              null comment '图片大小',
    style           varchar(50)                         not null comment '风格',
    promptText      text                                not null comment 'Prompt',
    negativePrompt  text                                null comment '负面Prompt',
    isCached        tinyint      default 0              not null comment '是否缓存命中',
    cacheSimilarity double                              null comment '缓存相似度',
    generateTimeMs  int                                 null comment '生成耗时（ms）',
    apiCost         decimal(10,4)                       null comment 'API成本',
    createTime      datetime default CURRENT_TIMESTAMP  not null comment '创建时间',
    isDelete        tinyint      default 0                 not null comment '是否删除',
    unique key uk_comic_panel (comicId, panelIndex),
    index idx_comicId (comicId),
    index idx_novelId (novelId),
    index idx_storyboardId (storyboardId),
    foreign key (comicId) references comic(id) on delete cascade,
    foreign key (novelId) references novel(id) on delete cascade,
    foreign key (storyboardId) references storyboard_panel(id) on delete cascade
) comment '漫画面板表' collate = utf8mb4_unicode_ci;

-- 生成任务表
create table if not exists generate_task
(
    id                bigint auto_increment comment '任务ID' primary key,
    taskUuid          varchar(100)                        not null comment '任务UUID',
    userId            bigint                              not null comment '用户ID',
    novelId           bigint                              not null comment '小说ID',
    taskType          varchar(50)                         not null comment '任务类型',
    status            varchar(20) default 'pending'       not null comment '状态：pending/processing/completed/failed',
    progressPercent   int         default 0               not null comment '进度百分比',
    currentStep       varchar(100)                        null comment '当前步骤',
    totalPanels       int                                 null comment '总分镜数',
    completedPanels   int         default 0               null comment '已完成分镜数',
    errorMessage      text                                null comment '错误信息',
    startTime         datetime                            null comment '开始时间',
    completeTime      datetime                            null comment '完成时间',
    createTime        datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime        datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete          tinyint     default 0                 not null comment '是否删除',
    unique key uk_taskUuid (taskUuid),
    index idx_userId (userId),
    index idx_status (status),
    foreign key (userId) references user(id) on delete cascade,
    foreign key (novelId) references novel(id) on delete cascade
) comment '生成任务表' collate = utf8mb4_unicode_ci;

-- 用户操作日志表
create table if not exists user_action_log
(
    id            bigint auto_increment comment '日志ID' primary key,
    userId        bigint                              not null comment '用户ID',
    actionType    varchar(50)                         not null comment '操作类型',
    resourceType  varchar(50)                         null comment '资源类型',
    resourceId    bigint                              null comment '资源ID',
    ipAddress     varchar(50)                         null comment 'IP地址',
    userAgent     varchar(500)                        null comment 'User Agent',
    createTime    datetime default CURRENT_TIMESTAMP  not null comment '创建时间',
    isDelete      tinyint  default 0                 not null comment '是否删除',
    index idx_userId (userId),
    index idx_actionType (actionType),
    index idx_createTime (createTime),
    foreign key (userId) references user(id) on delete cascade
) comment '用户操作日志表' collate = utf8mb4_unicode_ci;

-- API调用统计表
create table if not exists api_call_stat
(
    id              bigint auto_increment comment '统计ID' primary key,
    userId          bigint                              not null comment '用户ID',
    apiType         varchar(50)                         not null comment 'API类型',
    modelName       varchar(100)                        null comment '模型名称',
    requestTokens   int                                 null comment '请求Token数',
    responseTokens  int                                 null comment '响应Token数',
    imageCount      int         default 0               null comment '图片数量',
    costAmount      decimal(10,4)                       null comment '成本金额',
    responseTimeMs  int                                 null comment '响应时间(ms)',
    isSuccess       tinyint     default 1               not null comment '是否成功',
    errorMessage    text                                null comment '错误信息',
    createTime      datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    isDelete        tinyint     default 0                 not null comment '是否删除',
    index idx_userId (userId),
    index idx_apiType (apiType),
    index idx_createTime (createTime),
    foreign key (userId) references user(id) on delete cascade
) comment 'API调用统计表' collate = utf8mb4_unicode_ci;