-- 修复 comic_panel 表的唯一索引问题
-- 问题：原来的唯一索引 uk_novel_panel(novelId, panelIndex) 导致同一个小说无法生成多次漫画
-- 解决：改为 uk_comic_panel(comicId, panelIndex)，允许同一个小说生成多次漫画

USE mq_novel2comic;

-- 1. 删除旧的唯一索引
ALTER TABLE comic_panel DROP INDEX uk_novel_panel;

-- 2. 添加新的唯一索引（comicId + panelIndex）
ALTER TABLE comic_panel ADD UNIQUE KEY uk_comic_panel (comicId, panelIndex);

-- 3. 保留 novelId 的普通索引（用于查询）
ALTER TABLE comic_panel ADD INDEX idx_novelId (novelId);

-- 验证索引
SHOW INDEX FROM comic_panel;

