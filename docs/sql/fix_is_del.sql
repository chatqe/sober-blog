-- 修复脚本：为所有继承 BaseDO 的表添加 is_del 列（MyBatis-Plus 逻辑删除）
-- 执行前请先备份数据库
-- USE qingsi;

-- 1. user: 已有 deleted 列，添加 is_del（与 deleted 保持同步）
ALTER TABLE `user` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]' AFTER `update_by`;
UPDATE `user` SET `is_del` = `deleted` WHERE `deleted` = 1;

-- 2. article: 已有 deleted 列，添加 is_del
ALTER TABLE `article` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]' AFTER `update_by`;
UPDATE `article` SET `is_del` = `deleted` WHERE `deleted` = 1;

-- 3. comment: 无软删除列，添加 is_del
ALTER TABLE `comment` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 4. sort → category: 添加 is_del
ALTER TABLE `sort` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 5. label → tag: 添加 is_del
ALTER TABLE `label` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 6. tree_hole: 添加 is_del
ALTER TABLE `tree_hole` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 7. wei_yan → moment: 添加 is_del
ALTER TABLE `wei_yan` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 8. web_info: 添加 is_del
ALTER TABLE `web_info` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 9. resource_path → weblinks: 添加 is_del
ALTER TABLE `resource_path` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 10. resource: 添加 is_del
ALTER TABLE `resource` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 11. family: 添加 is_del
ALTER TABLE `family` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 12. history_info → access_log: 添加 is_del
ALTER TABLE `history_info` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';

-- 13. sys_config: 添加 is_del
ALTER TABLE `sys_config` ADD COLUMN `is_del` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除[0:未删除，1:已删除]';
