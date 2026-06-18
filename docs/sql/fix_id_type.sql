-- ============================================================================
-- SQL Migration: INT -> BIGINT for ID and Foreign Key Columns
-- Purpose: Align database column types with Java entity classes (all IDs use Long)
-- Database: qingsi
-- Date: 2026-06-15
--
-- Prerequisites:
--   1. Run fix_is_del.sql first (if not already applied)
--   2. Backup the database before running this script
--   3. Execute during a maintenance window - ALTER TABLE LOCK=NONE where possible
--
-- Processing order:
--   1. Parent tables (user)
--   2. Core child tables (article, family, web_info, etc.)
--   3. IM chat tables (im_chat_*)
-- ============================================================================

USE `qingsi`;

-- ============================================================================
-- 1. PARENT TABLES (no foreign keys to other custom tables)
-- ============================================================================

-- 1.1 user - primary key id: INT -> BIGINT
ALTER TABLE `user`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id';

-- 1.2 tag (stored as `label` in DB) - primary key id: INT -> BIGINT
ALTER TABLE `label`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id';

-- ============================================================================
-- 2. CORE TABLES
-- ============================================================================

-- 2.1 article - id(INT), user_id(INT->Long), sort_id(INT->Long), label_id(INT->Long)
-- Note: article extends BaseDO which uses IdType.ASSIGN_ID (snowflake),
--   but the DB column is AUTO_INCREMENT. We keep AUTO_INCREMENT to match the DB,
--   but widen to BIGINT to match Long in Java entity.
ALTER TABLE `article`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  MODIFY COLUMN `user_id` BIGINT NOT NULL COMMENT '用户ID',
  MODIFY COLUMN `sort_id` BIGINT NOT NULL COMMENT '分类ID',
  MODIFY COLUMN `label_id` BIGINT NOT NULL COMMENT '标签ID';

-- 2.2 web_info - id(INT->Long)
ALTER TABLE `web_info`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id';

-- 2.3 family - id(INT->Long), user_id(INT->Long)
ALTER TABLE `family`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  MODIFY COLUMN `user_id` BIGINT NOT NULL COMMENT '用户ID';

-- 2.4 history_info - id(INT->Long), user_id(INT->Long)
ALTER TABLE `history_info`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  MODIFY COLUMN `user_id` BIGINT DEFAULT NULL COMMENT '用户ID';

-- 2.5 sys_config - id(INT->Long)
ALTER TABLE `sys_config`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id';

-- ============================================================================
-- 3. IM CHAT TABLES
-- ============================================================================

-- 3.1 im_chat_group - id(INT->Long), master_user_id(INT->Long)
ALTER TABLE `im_chat_group`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  MODIFY COLUMN `master_user_id` BIGINT NOT NULL COMMENT '群主用户ID';

-- 3.2 im_chat_group_user - id(INT->Long), group_id(INT->Long), user_id(INT->Long), verify_user_id(INT->Long)
ALTER TABLE `im_chat_group_user`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  MODIFY COLUMN `group_id` BIGINT NOT NULL COMMENT '群ID',
  MODIFY COLUMN `user_id` BIGINT NOT NULL COMMENT '用户ID',
  MODIFY COLUMN `verify_user_id` BIGINT DEFAULT NULL COMMENT '审核用户ID';

-- 3.3 im_chat_user_friend - id(INT->Long), user_id(INT->Long), friend_id(INT->Long)
ALTER TABLE `im_chat_user_friend`
  MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  MODIFY COLUMN `user_id` BIGINT NOT NULL COMMENT '用户ID',
  MODIFY COLUMN `friend_id` BIGINT NOT NULL COMMENT '好友ID';

-- 3.4 im_chat_user_message - from_id(INT->Long), to_id(INT->Long)
-- Note: id is already BIGINT in the schema, skip id column
ALTER TABLE `im_chat_user_message`
  MODIFY COLUMN `from_id` BIGINT NOT NULL COMMENT '发送ID',
  MODIFY COLUMN `to_id` BIGINT NOT NULL COMMENT '接收ID';

-- 3.5 im_chat_user_group_message - group_id(INT->Long), from_id(INT->Long), to_id(INT->Long)
-- Note: id is already BIGINT in the schema, skip id column
ALTER TABLE `im_chat_user_group_message`
  MODIFY COLUMN `group_id` BIGINT NOT NULL COMMENT '群ID',
  MODIFY COLUMN `from_id` BIGINT NOT NULL COMMENT '发送ID',
  MODIFY COLUMN `to_id` BIGINT DEFAULT NULL COMMENT '接收ID';

-- ============================================================================
-- 4. INDEX CREATION
-- ============================================================================

-- 4.1 article: indexes on foreign key columns
-- Check if indexes already exist before creating
ALTER TABLE `article` ADD INDEX `idx_user_id` (`user_id`);
ALTER TABLE `article` ADD INDEX `idx_sort_id` (`sort_id`);
ALTER TABLE `article` ADD INDEX `idx_label_id` (`label_id`);

-- 4.2 im_chat_group_user: indexes on foreign key columns
ALTER TABLE `im_chat_group_user` ADD INDEX `idx_group_id` (`group_id`);
ALTER TABLE `im_chat_group_user` ADD INDEX `idx_user_id` (`user_id`);

-- 4.3 im_chat_user_friend: indexes on foreign key columns
ALTER TABLE `im_chat_user_friend` ADD INDEX `idx_user_id` (`user_id`);
ALTER TABLE `im_chat_user_friend` ADD INDEX `idx_friend_id` (`friend_id`);

-- 4.4 im_chat_user_message: to_id already has index (union_index)
-- No additional index needed for to_id.

-- ============================================================================
-- Migration complete.
-- Verify with: SHOW CREATE TABLE <table_name>;
-- ============================================================================
