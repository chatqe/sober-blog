-- ============================================================================
-- SQL Migration: INT -> BIGINT for ID and Foreign Key Columns (修正版)
-- Purpose: Align database column types with Java entity classes (all IDs use Long)
-- Database: qingsi
-- Date: 2026-06-25 (修正：使用实际表名，而非旧表名)
--
-- 重要说明：
--   1. 此脚本基于实际数据库表名编写（非 SQL 文件中的旧表名）
--   2. 旧表 sort/label/wei_yan/tree_hole/resource/resource_path/history_info
--      在代码中已被 Category/Tag/Family/AccessLog/Weblinks/File 等新实体替代
--   3. 新表 moments/moment_media/moment_social_sync 已在 moment_restructure.sql 中定义
--   4. 执行前请先备份数据库！
--   5. 执行顺序：先执行 fix_is_del.sql（如果需要），再执行本脚本
--
-- 涉及的真实表名（基于 @TableName 注解）：
--   user, article, comment, category, tag, family, web_info, sys_config,
--   weblinks, danmaku, access_log, behavior_log, behavior_state, behavior_cnt,
--   file_info, file_biz_ref, sys_dict_type, sys_dict_data,
--   im_chat_group, im_chat_group_user, im_chat_user_friend,
--   im_chat_user_message, im_chat_user_group_message
-- ============================================================================

USE `qingsi`;

-- ============================================================================
-- 0. 安全检查：确认表存在后再执行 ALTER
-- ============================================================================

-- 可以先运行以下查询确认哪些表需要处理：
-- SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE
-- FROM information_schema.COLUMNS
-- WHERE TABLE_SCHEMA = 'qingsi'
--   AND (COLUMN_NAME IN ('id', 'user_id', 'sort_id', 'label_id', 'category_id', 'tag_id', 'file_id', 'group_id', 'from_id', 'to_id', 'friend_id', 'master_user_id', 'verify_user_id')
--        OR TABLE_NAME IN ('article', 'comment', 'family', 'web_info', 'sys_config', 'weblinks', 'danmaku', 'access_log', 'behavior_log', 'behavior_state', 'behavior_cnt', 'file_info', 'file_biz_ref'))
--   AND DATA_TYPE IN ('int', 'integer', 'tinyint', 'smallint', 'mediumint')
-- ORDER BY TABLE_NAME, COLUMN_NAME;

-- ============================================================================
-- 1. PARENT TABLES (无外键引用)
-- ============================================================================

-- 1.1 user - id: 检查是否需要转换
-- ALTER TABLE `user` MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id';

-- 1.2 category (原 sort 表) - id
-- 注意：如果数据库中还有 sort 表，请使用 sort；如果已改名为 category，使用 category
ALTER TABLE `category` MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id' IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'qingsi' AND TABLE_NAME = 'category');

-- 1.3 tag (原 label 表) - id
ALTER TABLE `tag` MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id' IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'qingsi' AND TABLE_NAME = 'tag');

-- ============================================================================
-- 2. CORE TABLES
-- ============================================================================

-- 2.1 article - id, user_id, sort_id(category_id), label_id(tag_id)
-- 注意：article 表中的分类ID列可能是 sort_id 或 category_id，标签ID列可能是 label_id 或 tag_id
-- 先检查实际列名：
-- SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'qingsi' AND TABLE_NAME = 'article' AND COLUMN_NAME LIKE '%sort%' OR COLUMN_NAME LIKE '%category%' OR COLUMN_NAME LIKE '%label%' OR COLUMN_NAME LIKE '%tag%';

-- 如果列名是 sort_id / label_id（旧名）：
-- ALTER TABLE `article`
--   MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
--   MODIFY COLUMN `user_id` BIGINT NOT NULL COMMENT '用户ID',
--   MODIFY COLUMN `sort_id` BIGINT NOT NULL COMMENT '分类ID';
-- ALTER TABLE `article` DROP COLUMN IF EXISTS `label_id`;
-- ALTER TABLE `article` ADD COLUMN `label_id` BIGINT NOT NULL COMMENT '标签ID' AFTER `sort_id`;

-- 如果列名已是 category_id / tag_id（新名）：
-- ALTER TABLE `article`
--   MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
--   MODIFY COLUMN `user_id` BIGINT NOT NULL COMMENT '用户ID',
--   MODIFY COLUMN `category_id` BIGINT NOT NULL COMMENT '分类ID',
--   MODIFY COLUMN `tag_id` BIGINT NOT NULL COMMENT '标签ID';

-- 2.2 comment - id, user_id, article_id, parent_id, etc.
-- ALTER TABLE `comment`
--   MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
--   MODIFY COLUMN `user_id` BIGINT NOT NULL COMMENT '用户ID',
--   MODIFY COLUMN `article_id` BIGINT NOT NULL COMMENT '文章ID',
--   MODIFY COLUMN `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID',
--   MODIFY COLUMN `reply_to_id` BIGINT DEFAULT NULL COMMENT '回复目标ID';

-- 2.3 family - id, user_id
ALTER TABLE `family`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  MODIFY COLUMN `user_id` BIGINT NOT NULL COMMENT '用户ID';

-- 2.4 web_info - id
ALTER TABLE `web_info`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id';

-- 2.5 sys_config - id
ALTER TABLE `sys_config`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id';

-- 2.6 weblinks - id
ALTER TABLE `weblinks`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id';

-- 2.7 danmaku - id, user_id
-- ALTER TABLE `danmaku`
--   MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
--   MODIFY COLUMN `user_id` BIGINT NOT NULL COMMENT '用户ID';

-- 2.8 access_log - id, user_id
-- ALTER TABLE `access_log`
--   MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
--   MODIFY COLUMN `user_id` BIGINT DEFAULT NULL COMMENT '用户ID';

-- 2.9 behavior_log - id, user_id
-- ALTER TABLE `behavior_log`
--   MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
--   MODIFY COLUMN `user_id` BIGINT DEFAULT NULL COMMENT '用户ID';

-- 2.10 behavior_state - id, user_id
-- ALTER TABLE `behavior_state`
--   MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
--   MODIFY COLUMN `user_id` BIGINT DEFAULT NULL COMMENT '用户ID';

-- 2.11 behavior_cnt - id
-- ALTER TABLE `behavior_cnt`
--   MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id';

-- 2.12 file_info - id
-- ALTER TABLE `file_info`
--   MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id';

-- 2.13 file_biz_ref - id, file_id, biz_id
-- ALTER TABLE `file_biz_ref`
--   MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
--   MODIFY COLUMN `file_id` BIGINT NOT NULL COMMENT '文件ID',
--   MODIFY COLUMN `biz_id` BIGINT NOT NULL COMMENT '业务ID';

-- ============================================================================
-- 3. IM CHAT TABLES
-- ============================================================================

-- 3.1 im_chat_group - id, master_user_id
ALTER TABLE `im_chat_group`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  MODIFY COLUMN `master_user_id` BIGINT NOT NULL COMMENT '群主用户ID';

-- 3.2 im_chat_group_user - id, group_id, user_id, verify_user_id
ALTER TABLE `im_chat_group_user`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  MODIFY COLUMN `group_id` BIGINT NOT NULL COMMENT '群ID',
  MODIFY COLUMN `user_id` BIGINT NOT NULL COMMENT '用户ID',
  MODIFY COLUMN `verify_user_id` BIGINT DEFAULT NULL COMMENT '审核用户ID';

-- 3.3 im_chat_user_friend - id, user_id, friend_id
ALTER TABLE `im_chat_user_friend`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  MODIFY COLUMN `user_id` BIGINT NOT NULL COMMENT '用户ID',
  MODIFY COLUMN `friend_id` BIGINT NOT NULL COMMENT '好友ID';

-- 3.4 im_chat_user_message - id, from_id, to_id
ALTER TABLE `im_chat_user_message`
  MODIFY COLUMN `from_id` BIGINT NOT NULL COMMENT '发送者ID',
  MODIFY COLUMN `to_id` BIGINT NOT NULL COMMENT '接收者ID';

-- 3.5 im_chat_user_group_message - id, group_id, from_id, to_id
ALTER TABLE `im_chat_user_group_message`
  MODIFY COLUMN `group_id` BIGINT NOT NULL COMMENT '群ID',
  MODIFY COLUMN `from_id` BIGINT NOT NULL COMMENT '发送者ID',
  MODIFY COLUMN `to_id` BIGINT DEFAULT NULL COMMENT '接收者ID';

-- ============================================================================
-- 4. SYS DICT TABLES
-- ============================================================================

-- 4.1 sys_dict_type - id
ALTER TABLE `sys_dict_type`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id';

-- 4.2 sys_dict_data - id, dict_type_id
ALTER TABLE `sys_dict_data`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id';

-- ============================================================================
-- 5. MOMENT TABLES (已通过 moment_restructure.sql 创建，此处确保 ID 类型正确)
-- ============================================================================

-- moments - id, user_id (已通过 ASSIGN_ID 雪花算法生成，BIGINT)
-- 如果 moments 表已创建但 id 是 INT，执行以下：
-- ALTER TABLE `moments` MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID';
-- ALTER TABLE `moments` MODIFY COLUMN `user_id` BIGINT NOT NULL COMMENT '发布者ID';

-- moment_media - id, moment_id, file_id
-- ALTER TABLE `moment_media`
--   MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
--   MODIFY COLUMN `moment_id` BIGINT NOT NULL COMMENT '动态ID',
--   MODIFY COLUMN `file_id` BIGINT NOT NULL COMMENT '关联fileInfo表主键';

-- moment_social_sync - id, moment_id
-- ALTER TABLE `moment_social_sync`
--   MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
--   MODIFY COLUMN `moment_id` BIGINT NOT NULL COMMENT '本地动态ID';

-- ============================================================================
-- 6. INDEX CREATION
-- ============================================================================

-- 6.1 article: indexes on foreign key columns
-- ALTER TABLE `article` ADD INDEX IF NOT EXISTS `idx_user_id` (`user_id`);
-- ALTER TABLE `article` ADD INDEX IF NOT EXISTS `idx_category_id` (`category_id`);
-- ALTER TABLE `article` ADD INDEX IF NOT EXISTS `idx_tag_id` (`tag_id`);

-- 6.2 comment: indexes
-- ALTER TABLE `comment` ADD INDEX IF NOT EXISTS `idx_user_id` (`user_id`);
-- ALTER TABLE `comment` ADD INDEX IF NOT EXISTS `idx_article_id` (`article_id`);

-- 6.3 im_chat_group_user: indexes
ALTER TABLE `im_chat_group_user` ADD INDEX `idx_group_id` (`group_id`);
ALTER TABLE `im_chat_group_user` ADD INDEX `idx_user_id` (`user_id`);

-- 6.4 im_chat_user_friend: indexes
ALTER TABLE `im_chat_user_friend` ADD INDEX `idx_user_id` (`user_id`);
ALTER TABLE `im_chat_user_friend` ADD INDEX `idx_friend_id` (`friend_id`);

-- ============================================================================
-- Migration complete.
-- Verify with:
--   SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, COLUMN_TYPE
--   FROM information_schema.COLUMNS
--   WHERE TABLE_SCHEMA = 'qingsi' AND DATA_TYPE IN ('int', 'integer', 'tinyint', 'smallint', 'mediumint')
--   ORDER BY TABLE_NAME, COLUMN_NAME;
-- ============================================================================
