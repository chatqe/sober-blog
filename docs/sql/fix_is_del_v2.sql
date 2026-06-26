-- ============================================================================
-- SQL Migration: 为所有表添加 is_del 列（MyBatis-Plus 逻辑删除）
-- 修正版 - 使用实际表名（2026-06-25）
--
-- 重要说明：
--   1. 此脚本使用实际数据库表名（基于 @TableName 注解）
--   2. 旧表 sort/label/wei_yan/tree_hole/resource/resource_path/history_info
--      在代码中已被 Category/Tag/Family/AccessLog/Weblinks/File 等新实体替代
--   3. 如果旧表仍然存在，可以并行执行旧脚本
--   4. 执行前请先备份数据库！
-- ============================================================================

USE `qingsi`;

-- 1. user - 已有 deleted 列，添加 is_del（与 deleted 保持同步）
ALTER TABLE `user` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]' AFTER `update_by`;
UPDATE `user` SET `is_del` = `deleted` WHERE `deleted` = 1;

-- 2. article - 已有 deleted 列，添加 is_del
ALTER TABLE `article` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]' AFTER `update_time`;
UPDATE `article` SET `is_del` = `deleted` WHERE `deleted` = 1;

-- 3. comment - 无软删除列，添加 is_del
ALTER TABLE `comment` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 4. category (原 sort 表) - 添加 is_del
ALTER TABLE `category` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 5. tag (原 label 表) - 添加 is_del
ALTER TABLE `tag` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 6. family - 添加 is_del
ALTER TABLE `family` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 7. web_info - 添加 is_del
ALTER TABLE `web_info` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 8. weblinks (原 resource_path) - 添加 is_del
ALTER TABLE `weblinks` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 9. sys_config - 添加 is_del
ALTER TABLE `sys_config` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 10. danmaku - 添加 is_del
ALTER TABLE `danmaku` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 11. access_log - 添加 is_del
ALTER TABLE `access_log` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 12. behavior_log - 添加 is_del
ALTER TABLE `behavior_log` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 13. behavior_state - 添加 is_del
ALTER TABLE `behavior_state` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 14. behavior_cnt - 添加 is_del
ALTER TABLE `behavior_cnt` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 15. file_info - 添加 is_del
ALTER TABLE `file_info` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 16. file_biz_ref - 添加 is_del
ALTER TABLE `file_biz_ref` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 17. sys_dict_type - 添加 is_del
ALTER TABLE `sys_dict_type` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 18. sys_dict_data - 添加 is_del
ALTER TABLE `sys_dict_data` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 19. moments - 添加 is_del（如果尚未添加）
-- moments 表在 moment_restructure.sql 中已包含 is_del 列，此处跳过

-- 20. moment_media - 添加 is_del
ALTER TABLE `moment_media` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 21. moment_social_sync - 添加 is_del
ALTER TABLE `moment_social_sync` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- ============================================================================
-- 旧表（如果仍然存在，并行处理）
-- ============================================================================

-- sort (已被 category 替代)
ALTER TABLE `sort` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除' IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'qingsi' AND TABLE_NAME = 'sort');

-- label (已被 tag 替代)
ALTER TABLE `label` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除' IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'qingsi' AND TABLE_NAME = 'label');

-- tree_hole (已废弃)
ALTER TABLE `tree_hole` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除' IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'qingsi' AND TABLE_NAME = 'tree_hole');

-- wei_yan (已被 moments 替代)
ALTER TABLE `wei_yan` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除' IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'qingsi' AND TABLE_NAME = 'wei_yan');

-- resource (已被 file_info 替代)
ALTER TABLE `resource` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除' IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'qingsi' AND TABLE_NAME = 'resource');

-- resource_path (已被 weblinks 替代)
ALTER TABLE `resource_path` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除' IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'qingsi' AND TABLE_NAME = 'resource_path');

-- history_info (已被 access_log 替代)
ALTER TABLE `history_info` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除' IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'qingsi' AND TABLE_NAME = 'history_info');

-- ============================================================================
-- 迁移完成
-- ============================================================================
