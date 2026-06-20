-- ============================================================================
-- Moment 模块重构建表脚本
-- Purpose: 重构 moment 表，支持混合内容、多平台同步、软删除、预留审核
-- Database: qingsi
-- Date: 2026-06-20
--
-- 涉及表：
--   1. moments — 动态主表（重构，替换旧 moment 表）
--   2. moment_media — 动态文件关联表（新增）
--   3. moment_social_sync — 三方同步记录表（新增）
--
-- 注意事项：
--   - 旧 moment 表数据迁移到新的 moments 表（content + is_del 字段映射）
--   - 旧 wei_yan 表为废弃表，不在本脚本中处理
--   - file_info 表已存在，moment_media 通过 file_id 关联
-- ============================================================================

USE `qingsi`;

-- ============================================================================
-- 1. moments — 动态主表（重构）
-- ============================================================================

-- 如果旧 moment 表存在，先备份数据（可选）
-- CREATE TABLE IF NOT EXISTS `moment_backup` AS SELECT * FROM `moment`;

DROP TABLE IF EXISTS `moments`;

CREATE TABLE `moments` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) unsigned NOT NULL COMMENT '发布者ID',

  -- ===== 内容主体 =====
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '纯文本内容（支持emoji）',

  -- ===== 类型冗余（列表页快速筛选，避免连表查询） =====
  `has_text` tinyint(1) unsigned NOT NULL DEFAULT '0' COMMENT '是否有文本[0:无 1:有]',
  `has_images` tinyint(1) unsigned NOT NULL DEFAULT '0' COMMENT '是否有图片[0:无 1:有]',
  `has_videos` tinyint(1) unsigned NOT NULL DEFAULT '0' COMMENT '是否有视频[0:无 1:有]',
  `has_audios` tinyint(1) unsigned NOT NULL DEFAULT '0' COMMENT '是否有音频[0:无 1:有]',

  -- ===== 交互统计（冗余提升性能，由行为模块异步更新） =====
  `likes_count` int(11) unsigned NOT NULL DEFAULT '0' COMMENT '点赞数',
  `comments_count` int(11) unsigned NOT NULL DEFAULT '0' COMMENT '评论数',
  `views_count` int(11) unsigned NOT NULL DEFAULT '0' COMMENT '浏览数',

  -- ===== 生命周期状态（预留审核机制） =====
  `status` tinyint(4) unsigned NOT NULL DEFAULT '1' COMMENT '动态状态: 0-草稿 1-正常 2-审核中(预留) 3-违规下架(预留)',

  -- ===== 可见范围 =====
  `visibility` tinyint(4) unsigned NOT NULL DEFAULT '2' COMMENT '可见性: 0-私密 1-好友 2-公开',

  -- ===== 来源追溯（入向同步：从三方平台导入） =====
  `source` tinyint(4) unsigned NOT NULL DEFAULT '1' COMMENT '来源: 1-本站 2-微信 3-QQ 4-微博 5-抖音',
  `source_external_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '三方平台原始动态ID（配合source用于去重）',
  `source_user_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '三方平台用户ID（用于账号绑定关系）',

  -- ===== 软删除（独立于status，避免状态冲突） =====
  `is_del` tinyint(1) unsigned NOT NULL DEFAULT '0' COMMENT '软删除: 0-未删除 1-已删除',
  `del_time` datetime DEFAULT NULL COMMENT '删除时间（NULL表示未删除）',
  `del_by` bigint(20) unsigned DEFAULT NULL COMMENT '删除操作人ID（管理员删除/用户删除追溯）',

  -- ===== 审核预留 =====
  `audit_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核/驳回原因（status=2/3时填写）',
  `audited_at` datetime DEFAULT NULL COMMENT '审核时间',

  -- ===== 时间戳 =====
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (`id`),
  KEY `idx_user_del` (`user_id`, `is_del`),
  KEY `idx_del_status_time` (`is_del`, `status`, `create_time` DESC),
  KEY `idx_source_external` (`source`, `source_external_id`(128)),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态主表';


-- ============================================================================
-- 2. moment_media — 动态文件关联表
-- Purpose: 通过 file_id 关联 fileInfo 表，支撑混合内容（图文/视频/音频）
-- ============================================================================

DROP TABLE IF EXISTS `moment_media`;

CREATE TABLE `moment_media` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `moment_id` bigint(20) unsigned NOT NULL COMMENT '动态ID',
  `file_id` bigint(20) unsigned NOT NULL COMMENT '关联fileInfo表主键',

  -- 冗余类型（查询时无需JOIN fileInfo即可按类型筛选）
  `media_type` tinyint(4) unsigned NOT NULL COMMENT '媒体类型: 1-图片 2-视频 3-音频',

  -- 排序权重（前端展示顺序，数值越小越靠前）
  `sort_order` int(11) NOT NULL DEFAULT '0' COMMENT '排序权重',

  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_moment_file` (`moment_id`, `file_id`) COMMENT '防止重复关联',
  KEY `idx_file_id` (`file_id`),
  KEY `idx_moment_type` (`moment_id`, `media_type`),
  KEY `idx_moment_sort` (`moment_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态文件关联表';


-- ============================================================================
-- 3. moment_social_sync — 三方同步记录表
-- Purpose: 出向同步（本站→微信/微博/QQ）和入向同步（三方→本站）的异步解耦
-- ============================================================================

DROP TABLE IF EXISTS `moment_social_sync`;

CREATE TABLE `moment_social_sync` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `moment_id` bigint(20) unsigned NOT NULL COMMENT '本地动态ID',

  -- 同步方向
  `direction` tinyint(4) unsigned NOT NULL DEFAULT '1' COMMENT '同步方向: 1-出向(本站发往三方) 2-入向(三方拉取至本站)',

  -- 目标平台
  `platform` varchar(20) NOT NULL COMMENT '平台标识: wechat | qq | weibo | douyin',

  -- 三方返回信息
  `external_id` varchar(255) DEFAULT NULL COMMENT '三方平台返回的发布ID（用于删除/修改/幂等）',
  `external_url` varchar(500) DEFAULT NULL COMMENT '三方平台的内容链接',

  -- 同步状态机
  `sync_status` tinyint(4) unsigned NOT NULL DEFAULT '0' COMMENT '同步状态: 0-待同步 1-同步中 2-成功 3-失败',
  `retry_count` tinyint(4) unsigned NOT NULL DEFAULT '0' COMMENT '重试次数',
  `error_message` text COMMENT '失败时的报错信息',

  -- 调试/排查
  `sync_data` json DEFAULT NULL COMMENT '请求三方的原始入参（方便排查问题）',
  `synced_at` datetime DEFAULT NULL COMMENT '同步完成时间',

  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_moment_platform` (`moment_id`, `platform`) COMMENT '同一动态对同一平台只能同步一次',
  KEY `idx_sync_status` (`sync_status`),
  KEY `idx_direction_status` (`direction`, `sync_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='三方同步记录表';


-- ============================================================================
-- 4. 数据迁移（旧 moment → 新 moments）
-- ============================================================================

-- 如果旧 moment 表存在且有数据，执行以下迁移：
-- INSERT INTO `moments` (`user_id`, `content`, `has_text`, `likes_count`, `status`, `visibility`, `source`, `is_del`, `create_time`, `update_time`)
-- SELECT
--   `user_id`,
--   `content`,
--   CASE WHEN LENGTH(`content`) > 0 THEN 1 ELSE 0 END,
--   `likes`,
--   CASE WHEN `is_del` = 1 THEN 0 ELSE 1 END,
--   CASE WHEN `is_public` = 0 THEN 0 ELSE 2 END,
--   1,
--   `is_del`,
--   `create_time`,
--   `update_time`
-- FROM `moment`
-- WHERE `is_del` = 0;

-- ============================================================================
-- 迁移完成
-- ============================================================================
