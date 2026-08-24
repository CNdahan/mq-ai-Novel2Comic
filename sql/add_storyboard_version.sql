-- =====================================================
-- 分镜多版本支持 - 数据库优化脚本
-- =====================================================
-- 功能：支持同一小说生成多个版本的分镜，用户可以对比选择
-- 作者：MQ
-- 日期：2025-10-25

USE mq_novel2comic;

-- Step 1: 删除旧的唯一约束（不再需要）
ALTER TABLE storyboard_panel 
DROP INDEX uk_novel_panel;

-- Step 2: 添加版本号字段
ALTER TABLE storyboard_panel 
ADD COLUMN version INT NOT NULL DEFAULT 1 COMMENT '分镜版本号' AFTER novelId;

-- Step 3: 添加新的唯一约束（小说ID + 版本号 + 分镜序号）
ALTER TABLE storyboard_panel 
ADD UNIQUE KEY uk_novel_version_panel (novelId, version, panelIndex);

-- Step 4: 添加是否为当前版本标识
ALTER TABLE storyboard_panel 
ADD COLUMN isCurrent TINYINT DEFAULT 1 NOT NULL COMMENT '是否为当前版本：1-是，0-否' AFTER version;

-- Step 5: 添加版本描述字段（可选）
ALTER TABLE storyboard_panel 
ADD COLUMN versionNote VARCHAR(200) NULL COMMENT '版本说明' AFTER isCurrent;

-- Step 6: 添加索引优化查询
ALTER TABLE storyboard_panel 
ADD INDEX idx_novel_version (novelId, version);

ALTER TABLE storyboard_panel 
ADD INDEX idx_novel_current (novelId, isCurrent);

-- Step 7: 更新现有数据（设置为版本1）
UPDATE storyboard_panel 
SET version = 1, isCurrent = 1 
WHERE version IS NULL OR version = 0;

-- =====================================================
-- 说明：
-- 1. version: 版本号，每次生成新版本时自动递增
-- 2. isCurrent: 标识当前使用的版本，生成漫画时使用
-- 3. versionNote: 用户可以为每个版本添加备注
-- 4. 新的唯一约束确保：同一小说的同一版本中，分镜序号不重复
-- 5. 支持同一小说有多个版本的分镜共存
-- =====================================================

-- 验证脚本
-- 查看表结构
DESC storyboard_panel;

-- 查看索引
SHOW INDEX FROM storyboard_panel;

-- 查看现有数据
SELECT novelId, version, panelIndex, isCurrent, 
       LEFT(descriptionCn, 50) as description 
FROM storyboard_panel 
WHERE isDelete = 0 
ORDER BY novelId, version, panelIndex 
LIMIT 10;

