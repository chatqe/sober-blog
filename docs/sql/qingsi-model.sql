/*
 Navicat Premium Data Transfer

 Source Server         : just_play8.0.23
 Source Server Type    : MySQL
 Source Server Version : 80023
 Source Host           : 192.168.30.100:3309
 Source Schema         : qingsi

 Target Server Type    : MySQL
 Target Server Version : 80023
 File Encoding         : 65001

 Date: 10/10/2026 09:55:26
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for access_log
-- ----------------------------
DROP TABLE IF EXISTS `access_log`;
CREATE TABLE `access_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `trace_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '链路追踪ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `access_time` datetime NOT NULL COMMENT '访问时间',
  `ip` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '访问IP地址（IPv4最长15 / IPv6最长45）',
  `ip_location` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'IP完整地理位置（省/市/区）',
  `country` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '国家',
  `province` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '省份',
  `city` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '市',
  `district` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '区县',
  `isp` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '运营商（如：中国移动、中国电信）',
  `referer` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '来源页URL',
  `ua` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'User-Agent 原文',
  `browser` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '浏览器类型（Chrome/Firefox/微信内置等）',
  `os` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作系统（Windows/macOS/Android/iOS等）',
  `device_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备类型：PC/Mobile/Pad等',
  `req_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '请求URL',
  `req_method` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '请求方式',
  `resp_status` smallint NOT NULL COMMENT '响应HTTP状态码（100~599）',
  `resp_time` int NOT NULL COMMENT '响应耗时（毫秒）',
  `ext_info` json NULL COMMENT '扩展字段',
  `is_del` tinyint(1) NOT NULL DEFAULT 0 COMMENT '软删除标识（0:未删除 1:已删除）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_access_time`(`access_time` ASC) USING BTREE,
  INDEX `idx_ip`(`ip` ASC) USING BTREE,
  INDEX `idx_city`(`city` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13893 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '网站访问记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for article
-- ----------------------------
DROP TABLE IF EXISTS `article`;
CREATE TABLE `article`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  `article_cover` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '封面',
  `article_title` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '博文标题',
  `article_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '博文内容',
  `video_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '视频链接',
  `view_count` int NOT NULL DEFAULT 0 COMMENT '浏览量',
  `like_count` int NOT NULL DEFAULT 0 COMMENT '点赞数',
  `view_status` tinyint NOT NULL DEFAULT 1 COMMENT '是否可见[0:否，1:是]',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '密码',
  `tips` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '提示',
  `recommend_status` tinyint NOT NULL DEFAULT 0 COMMENT '是否推荐[0:否，1:是]',
  `comment_status` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用评论[0:否，1:是]',
  `publish_status` tinyint NOT NULL DEFAULT 1 COMMENT '发布状态[0:草稿，1:已发布]',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最终修改时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最终修改人',
  `is_del` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标识(0-未删,1-已删)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_category_id`(`category_id` ASC) USING BTREE,
  INDEX `idx_tag_id`(`tag_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '文章表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for behavior_cnt
-- ----------------------------
DROP TABLE IF EXISTS `behavior_cnt`;
CREATE TABLE `behavior_cnt`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_id` bigint UNSIGNED NOT NULL COMMENT '业务实体ID',
  `biz_type` tinyint NOT NULL COMMENT '业务实体类型 1-文章 2-微言/动态 3-评论 4-相册 5-视频 6-音乐 7-分类/专栏 8-标签',
  `action` tinyint NOT NULL DEFAULT 0 COMMENT '行为类型',
  `count` int NOT NULL DEFAULT 0 COMMENT '行为数量',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号，解决并发更新问题',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_del` tinyint(1) NOT NULL DEFAULT 0 COMMENT '软删除 0否，1真',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_biz_action_del`(`biz_id` ASC, `biz_type` ASC, `action` ASC, `is_del` ASC) USING BTREE,
  INDEX `idx_biztype_action`(`biz_type` ASC, `action` ASC) USING BTREE,
  INDEX `idx_bizid`(`biz_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 56 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户行为计数表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for behavior_log
-- ----------------------------
DROP TABLE IF EXISTS `behavior_log`;
CREATE TABLE `behavior_log`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '用户ID（NULL表示匿名用户）',
  `guest_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '匿名标识（未登录用户：设备ID/Token）',
  `biz_id` bigint UNSIGNED NOT NULL COMMENT '业务实体ID',
  `biz_type` tinyint NOT NULL COMMENT '业务实体类型 1-文章 2-微言/动态 3-评论 4-相册 5-视频 6-音乐 7-分类/专栏 8-标签',
  `action` tinyint NOT NULL DEFAULT 0 COMMENT '行为类型',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_del` tinyint NOT NULL DEFAULT 0 COMMENT '软删除 0否，1真',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_login_user_behavior`(`user_id` ASC, `biz_id` ASC, `biz_type` ASC, `action` ASC, `is_del` ASC) USING BTREE COMMENT '登录用户操作唯一性',
  UNIQUE INDEX `uk_anonymous_user_behavior`(`guest_id` ASC, `biz_id` ASC, `biz_type` ASC, `action` ASC, `is_del` ASC) USING BTREE COMMENT '匿名用户操作唯一性',
  INDEX `idx_biz`(`biz_id` ASC, `biz_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 377 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户行为流水表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for behavior_state
-- ----------------------------
DROP TABLE IF EXISTS `behavior_state`;
CREATE TABLE `behavior_state`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键（自增ID）',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '用户ID',
  `biz_id` bigint UNSIGNED NOT NULL COMMENT '业务实体ID',
  `biz_type` tinyint NOT NULL COMMENT '业务实体类型 1-文章 2-微言/动态 3-评论 4-相册 5-视频 6-音乐 7-分类/专栏 8-标签',
  `action` tinyint NOT NULL DEFAULT 0 COMMENT '行为类型',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '行为状态 1-正常 2-取消',
  `mutex_key` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '互斥标识（如赞/踩互斥）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_behavior`(`user_id` ASC, `biz_id` ASC, `biz_type` ASC, `action` ASC) USING BTREE,
  INDEX `idx_biz`(`biz_id` ASC, `biz_type` ASC) USING BTREE,
  INDEX `idx_user_mutex`(`user_id` ASC, `biz_id` ASC, `mutex_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 68 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户行为状态表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for category
-- ----------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否可用（0:否 1:是）',
  `is_show` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否展示（0:否 1:是）',
  `home_show` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否首页展示（0:否 1:是）：仅控制首页分类文章区块',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `cate_desc` varchar(75) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '分类描述',
  `sort` int NULL DEFAULT NULL COMMENT '分类优先级：数字小的在前面',
  `type` tinyint NOT NULL DEFAULT 1 COMMENT '分类类型 1:普通分类 2:导航栏分类',
  `icon` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分类图标',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最终修改人',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_del` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name` ASC) USING BTREE COMMENT '分类名称唯一',
  INDEX `idx_status_del`(`status` ASC, `is_del` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for comment
-- ----------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '父评论ID，0=根评论',
  `root_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '顶级祖先ID，0=自身为根',
  `biz_type` tinyint NOT NULL COMMENT '评论所属业务类型',
  `biz_id` bigint UNSIGNED NOT NULL COMMENT '业务主键ID',
  `author_id` bigint UNSIGNED NOT NULL COMMENT '评论人ID',
  `author_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评论人昵称（冗余）',
  `author_avatar` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像Key（冗余）',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '内容',
  `reply_to_user_id` bigint NULL DEFAULT 0 COMMENT '回复目标用户ID',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=正常 2=禁言 3=审核中',
  `ip` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'IP地址',
  `ua` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'User‑Agent',
  `is_owner` tinyint NOT NULL DEFAULT 0 COMMENT '0=否 1=是内容作者',
  `is_top` tinyint NOT NULL DEFAULT 0 COMMENT '0=否 1=是置顶',
  `is_del` tinyint NOT NULL DEFAULT 0 COMMENT '0=否 1=是（软删除）',
  `like_cnt` int NULL DEFAULT 0 COMMENT '点赞数',
  `child_cnt` int NULL DEFAULT 0 COMMENT '直接子评论数',
  `content_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '内容SHA256，防刷评',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `moderator_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '审核人ID',
  `moderate_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '评论更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_biz`(`biz_type` ASC, `biz_id` ASC, `is_del` ASC, `is_top` ASC, `create_time` ASC, `id` ASC) USING BTREE COMMENT '业务评论列表查询，支持游标分页',
  INDEX `idx_root`(`root_id` ASC, `parent_id` ASC, `create_time` ASC) USING BTREE COMMENT '评论树查询：拉取某根评论下的全部子评论',
  INDEX `idx_creator`(`author_id` ASC) USING BTREE COMMENT '用户维度：查询某用户发表的所有评论',
  INDEX `idx_reply_to`(`reply_to_user_id` ASC, `create_time` ASC) USING BTREE COMMENT '回复通知：查询某用户收到的回复，按时间排序',
  INDEX `idx_status`(`status` ASC, `biz_type` ASC, `create_time` ASC) USING BTREE COMMENT '管理后台：按审核状态 + 业务类型筛选评论'
) ENGINE = InnoDB AUTO_INCREMENT = 91004 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '评论表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for danmaku
-- ----------------------------
DROP TABLE IF EXISTS `danmaku`;
CREATE TABLE `danmaku`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `avatar` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户头像',
  `message` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '弹幕留言内容',
  `is_del` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0=否 1=是',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人ID/名称',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 88 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '树洞(留言弹幕)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for family
-- ----------------------------
DROP TABLE IF EXISTS `family`;
CREATE TABLE `family`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `bg_cover` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '背景封面',
  `man_cover` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '男生头像',
  `woman_cover` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '女生头像',
  `man_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '男生昵称',
  `woman_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '女生昵称',
  `timing` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '计时',
  `countdown_title` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '倒计时标题',
  `countdown_time` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '倒计时时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用[0:否，1:是]',
  `family_info` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '额外信息',
  `like_count` int NOT NULL DEFAULT 0 COMMENT '点赞数',
  `is_del` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标识(0-未删,1-已删)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最终修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '家庭信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for file_biz_ref
-- ----------------------------
DROP TABLE IF EXISTS `file_biz_ref`;
CREATE TABLE `file_biz_ref`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `file_id` bigint UNSIGNED NOT NULL COMMENT '文件ID（file_storage.id）',
  `biz_id` bigint UNSIGNED NOT NULL COMMENT '业务ID',
  `biz_type` tinyint NOT NULL COMMENT '业务类型',
  `sort` smallint UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序序号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1-正常 0-隐藏',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '创建人',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_del` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_file_biz`(`file_id` ASC, `biz_id` ASC, `biz_type` ASC) USING BTREE,
  INDEX `idx_biz`(`biz_id` ASC, `biz_type` ASC, `is_del` ASC, `sort` ASC) USING BTREE,
  INDEX `idx_file`(`file_id` ASC, `is_del` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件业务关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for file_info
-- ----------------------------
DROP TABLE IF EXISTS `file_info`;
CREATE TABLE `file_info`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID（SaaS多租户扩展）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '创建人',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `create_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `is_del` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
  `original_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原始文件名（用户上传的文件名）',
  `stored_name` varchar(75) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '存储文件名（服务端生成 UUID.ext）',
  `file_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件SHA256哈希（去重+秒传）',
  `storage_path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '存储路径（如 2024/06/18/abc123.jpg）',
  `cdn_domain` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'CDN域名（为空时使用默认域名）',
  `file_size` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
  `file_category` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件分类: image/video/document/audio/other',
  `mime_type` varchar(127) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'MIME类型（如 image/jpeg）',
  `file_ext` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件扩展名(小写, 如 jpg)',
  `width` int UNSIGNED NULL DEFAULT NULL COMMENT '图片/视频宽度(px)',
  `height` int UNSIGNED NULL DEFAULT NULL COMMENT '图片/视频高度(px)',
  `duration` int UNSIGNED NULL DEFAULT NULL COMMENT '音视频时长(秒)',
  `third_party_name` tinyint NOT NULL DEFAULT 1 COMMENT '存储类型 1-本地 2-腾讯COS 3-七牛云 4-S3 5-阿里OSS 6-Minio',
  `bucket` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'Bucket名称',
  `region` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '存储区域',
  `third_party_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '第三方平台文件ID',
  `owner_id` bigint UNSIGNED NOT NULL COMMENT '文件拥有者ID',
  `owner_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '拥有者类型 user/admin/system',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1-正常 0-禁用 2-审核中',
  `ref_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '被业务引用次数',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_hash_size_storage`(`file_hash` ASC, `file_size` ASC, `third_party_name` ASC) USING BTREE,
  UNIQUE INDEX `uk_storage_path`(`storage_path` ASC, `third_party_name` ASC) USING BTREE,
  INDEX `idx_owner`(`owner_id` ASC, `owner_type` ASC, `file_category` ASC, `is_del` ASC) USING BTREE,
  INDEX `idx_category`(`file_category` ASC, `status` ASC, `is_del` ASC) USING BTREE,
  INDEX `idx_created_by`(`create_by` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件元数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for history_info
-- ----------------------------
DROP TABLE IF EXISTS `history_info`;
CREATE TABLE `history_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID',
  `ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'ip',
  `nation` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '国家',
  `province` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '省份',
  `city` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '城市',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 44 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '历史信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for im_chat_group
-- ----------------------------
DROP TABLE IF EXISTS `im_chat_group`;
CREATE TABLE `im_chat_group`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `group_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '群名称',
  `master_user_id` bigint NOT NULL COMMENT '群主用户ID',
  `avatar` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '群头像',
  `introduction` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '简介',
  `notice` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '公告',
  `in_type` tinyint(1) NOT NULL DEFAULT 1 COMMENT '进入方式[0:无需验证，1:需要群主或管理员同意]',
  `group_type` tinyint NOT NULL DEFAULT 1 COMMENT '类型[1:聊天群，2:话题]',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '聊天群' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for im_chat_group_user
-- ----------------------------
DROP TABLE IF EXISTS `im_chat_group_user`;
CREATE TABLE `im_chat_group_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `group_id` bigint NOT NULL COMMENT '群ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `verify_user_id` bigint NULL DEFAULT NULL COMMENT '审核用户ID',
  `remark` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `admin_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否管理员[0:否，1:是]',
  `user_status` tinyint NOT NULL COMMENT '用户状态[0:未审核，1:审核通过，2:禁言]',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_group_id`(`group_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '聊天群成员' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for im_chat_user_friend
-- ----------------------------
DROP TABLE IF EXISTS `im_chat_user_friend`;
CREATE TABLE `im_chat_user_friend`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `friend_id` bigint NOT NULL COMMENT '好友ID',
  `friend_status` tinyint NOT NULL COMMENT '朋友状态[0:未审核，1:审核通过]',
  `remark` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_friend_id`(`friend_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '好友' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for im_chat_user_group_message
-- ----------------------------
DROP TABLE IF EXISTS `im_chat_user_group_message`;
CREATE TABLE `im_chat_user_group_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `group_id` bigint NOT NULL COMMENT '群ID',
  `from_id` bigint NOT NULL COMMENT '发送ID',
  `to_id` bigint NULL DEFAULT NULL COMMENT '接收ID',
  `content` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '内容',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '群聊记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for im_chat_user_message
-- ----------------------------
DROP TABLE IF EXISTS `im_chat_user_message`;
CREATE TABLE `im_chat_user_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `from_id` bigint NOT NULL COMMENT '发送ID',
  `to_id` bigint NOT NULL COMMENT '接收ID',
  `content` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '内容',
  `message_status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已读[0:未读，1:已读]',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `union_index`(`to_id` ASC, `message_status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '单聊记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for message
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '留言用户ID，0=游客',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '昵称',
  `avatar` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '邮箱，必填，不对外展示',
  `email_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '邮箱SHA256，防刷/去重',
  `website` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '个人网址，选填',
  `content` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '留言内容',
  `content_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '内容SHA256，防刷',
  `ip` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'IP地址',
  `ua` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'User-Agent原文',
  `ip_location` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'IP完整地理位置，页面展示如：香港',
  `country` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '国家',
  `province` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '省份',
  `city` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '城市',
  `district` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '区县',
  `isp` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '运营商',
  `os` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作系统（含版本），如 Windows 10、Windows 11',
  `browser` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '浏览器（含版本），如 Chrome 154.0.0.0',
  `device_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备类型：PC/Mobile/Pad',
  `like_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数',
  `dislike_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '踩数',
  `reply_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '回复总数',
  `last_reply_time` datetime NULL DEFAULT NULL COMMENT '最后回复时间',
  `is_top` tinyint NOT NULL DEFAULT 0 COMMENT '0=否 1=置顶',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=正常 2=审核中 3=拒绝 4=隐藏',
  `is_del` tinyint NOT NULL DEFAULT 0 COMMENT '0=否 1=软删除',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `moderator_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '审核人ID',
  `moderate_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_latest`(`status` ASC, `is_del` ASC, `is_top` ASC, `create_time` ASC, `id` ASC) USING BTREE COMMENT '留言列表：最新/最早',
  INDEX `idx_hot`(`status` ASC, `is_del` ASC, `is_top` ASC, `like_count` ASC, `reply_count` ASC, `create_time` ASC) USING BTREE COMMENT '留言列表：热门',
  INDEX `idx_user`(`user_id` ASC, `create_time` ASC) USING BTREE COMMENT '用户维度查询',
  INDEX `idx_email_hash`(`email_hash` ASC, `create_time` ASC) USING BTREE COMMENT '邮箱防刷',
  INDEX `idx_content_hash`(`content_hash` ASC, `create_time` ASC) USING BTREE COMMENT '内容防刷',
  INDEX `idx_ip`(`ip` ASC, `create_time` ASC) USING BTREE COMMENT 'IP限流'
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '留言墙主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for message_reply
-- ----------------------------
DROP TABLE IF EXISTS `message_reply`;
CREATE TABLE `message_reply`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `message_id` bigint UNSIGNED NOT NULL COMMENT '主留言ID',
  `parent_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '直接父回复ID，0=回复主留言',
  `root_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '顶级回复ID，0=自身为根',
  `reply_to_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '被回复的回复ID，0=回复主留言',
  `reply_to_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '被回复人昵称，冗余展示',
  `user_id` bigint UNSIGNED NOT NULL DEFAULT 0 COMMENT '回复用户ID，0=游客',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '昵称',
  `avatar` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '回复者邮箱，必填，不对外展示',
  `email_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '邮箱SHA256，防刷/去重',
  `website` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '个人网址，选填',
  `content` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '回复内容',
  `content_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '内容SHA256，防刷',
  `ip` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'IP地址',
  `ua` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'User-Agent原文',
  `ip_location` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'IP完整地理位置，页面展示如：香港',
  `country` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '国家',
  `province` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '省份',
  `city` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '城市',
  `district` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '区县',
  `isp` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '运营商',
  `os` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作系统（含版本），如 Windows 10、Windows 11',
  `browser` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '浏览器（含版本），如 Chrome 154.0.0.0',
  `device_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备类型：PC/Mobile/Pad',
  `like_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数',
  `dislike_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '踩数',
  `child_cnt` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '直接子回复数',
  `is_admin` tinyint NOT NULL DEFAULT 0 COMMENT '0=否 1=管理员/站长回复',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=正常 2=审核中 3=拒绝 4=隐藏',
  `is_del` tinyint NOT NULL DEFAULT 0 COMMENT '0=否 1=软删除',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `moderator_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '审核人ID',
  `moderate_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_message_latest`(`message_id` ASC, `status` ASC, `is_del` ASC, `create_time` ASC, `id` ASC) USING BTREE COMMENT '某留言下回复列表：最新/最早',
  INDEX `idx_message_hot`(`message_id` ASC, `status` ASC, `is_del` ASC, `like_count` ASC, `create_time` ASC) USING BTREE COMMENT '某留言下回复列表：热门',
  INDEX `idx_root_tree`(`root_id` ASC, `parent_id` ASC, `create_time` ASC) USING BTREE COMMENT '回复树查询',
  INDEX `idx_reply_to`(`reply_to_id` ASC, `create_time` ASC) USING BTREE COMMENT '回复通知',
  INDEX `idx_user`(`user_id` ASC, `create_time` ASC) USING BTREE COMMENT '用户维度查询',
  INDEX `idx_email_hash`(`email_hash` ASC, `create_time` ASC) USING BTREE COMMENT '邮箱防刷',
  INDEX `idx_ip`(`ip` ASC, `create_time` ASC) USING BTREE COMMENT 'IP限流'
) ENGINE = InnoDB AUTO_INCREMENT = 109 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '留言墙回复表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for moment_media
-- ----------------------------
DROP TABLE IF EXISTS `moment_media`;
CREATE TABLE `moment_media`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `moment_id` bigint UNSIGNED NOT NULL COMMENT '动态ID',
  `file_id` bigint UNSIGNED NOT NULL COMMENT '关联fileInfo表主键',
  `media_type` tinyint UNSIGNED NOT NULL COMMENT '媒体类型: 1-图片 2-视频 3-音频',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序权重',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_moment_file`(`moment_id` ASC, `file_id` ASC) USING BTREE COMMENT '防止重复关联',
  INDEX `idx_file_id`(`file_id` ASC) USING BTREE,
  INDEX `idx_moment_type`(`moment_id` ASC, `media_type` ASC) USING BTREE,
  INDEX `idx_moment_sort`(`moment_id` ASC, `sort_order` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '动态文件关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for moment_social_sync
-- ----------------------------
DROP TABLE IF EXISTS `moment_social_sync`;
CREATE TABLE `moment_social_sync`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `moment_id` bigint UNSIGNED NOT NULL COMMENT '本地动态ID',
  `direction` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '同步方向: 1-出向(本站发往三方) 2-入向(三方拉取至本站)',
  `platform` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台标识: wechat | qq | weibo | douyin',
  `external_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '三方平台返回的发布ID（用于删除/修改/幂等）',
  `external_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '三方平台的内容链接',
  `sync_status` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '同步状态: 0-待同步 1-同步中 2-成功 3-失败',
  `retry_count` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '重试次数',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '失败时的报错信息',
  `sync_data` json NULL COMMENT '请求三方的原始入参（方便排查问题）',
  `synced_at` datetime NULL DEFAULT NULL COMMENT '同步完成时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_moment_platform`(`moment_id` ASC, `platform` ASC) USING BTREE COMMENT '同一动态对同一平台只能同步一次',
  INDEX `idx_sync_status`(`sync_status` ASC) USING BTREE,
  INDEX `idx_direction_status`(`direction` ASC, `sync_status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '三方同步记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for moments
-- ----------------------------
DROP TABLE IF EXISTS `moments`;
CREATE TABLE `moments`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '发布者ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '纯文本内容（支持emoji）',
  `has_text` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否有文本[0:无 1:有]',
  `has_images` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否有图片[0:无 1:有]',
  `has_videos` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否有视频[0:无 1:有]',
  `has_audios` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否有音频[0:无 1:有]',
  `likes_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数',
  `comments_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '评论数',
  `views_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '浏览数',
  `status` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '动态状态: 0-草稿 1-正常 2-审核中(预留) 3-违规下架(预留)',
  `visibility` tinyint UNSIGNED NOT NULL DEFAULT 2 COMMENT '可见性: 0-私密 1-好友 2-公开',
  `source` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '来源: 1-本站 2-微信 3-QQ 4-微博 5-抖音',
  `source_external_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '三方平台原始动态ID（配合source用于去重）',
  `source_user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '三方平台用户ID（用于账号绑定关系）',
  `is_del` tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除 1-已删除',
  `del_time` datetime NULL DEFAULT NULL COMMENT '删除时间（NULL表示未删除）',
  `del_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '删除操作人ID（管理员删除/用户删除追溯）',
  `audit_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审核/驳回原因（status=2/3时填写）',
  `audited_at` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_del`(`user_id` ASC, `is_del` ASC) USING BTREE,
  INDEX `idx_del_status_time`(`is_del` ASC, `status` ASC, `create_time` DESC) USING BTREE,
  INDEX `idx_source_external`(`source` ASC, `source_external_id`(128) ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 90011 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '动态主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `config_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '名称',
  `config_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '键名',
  `config_value` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '键值',
  `config_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '1 私用 2 公开',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '参数配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `type_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典类型编码，关联 sys_dict_type.type_code',
  `item_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典项编码，同一 type_code 下唯一，如 \"M\"',
  `item_value` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典项实际存储值，如 0/1 或具体代码值',
  `item_label` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典项显示标签，如 \"男\"',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序值，值越小越靠前',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `status` tinyint UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `is_default` tinyint NOT NULL DEFAULT 0 COMMENT '是否默认值：0-否, 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人标识（可选）',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人标识（可选）',
  `is_del` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_type_item`(`type_code` ASC, `item_code` ASC) USING BTREE,
  INDEX `idx_type_code`(`type_code` ASC) USING BTREE,
  INDEX `idx_query_effective`(`type_code` ASC, `status` ASC, `sort` ASC) USING BTREE COMMENT '高效查询：按类型查有效数据',
  CONSTRAINT `fk_dict_item_type` FOREIGN KEY (`type_code`) REFERENCES `sys_dict_type` (`type_code`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 195 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '字典数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `type_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典类型编码，全局唯一, 如 \"gender\"',
  `type_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '字典类型名称, 用于展示，如 \"性别\"',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0-禁用, 1-启用',
  `remark` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注说明',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序权重，值越小越靠前',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `is_del` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否1真',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_type_code`(`type_code` ASC) USING BTREE COMMENT '唯一索引，保证编码不重复',
  INDEX `idx_status_del_sort`(`status` ASC, `is_del` ASC, `sort` ASC) USING BTREE COMMENT '查询有效数据时加速'
) ENGINE = InnoDB AUTO_INCREMENT = 40 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '字典类型表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tag
-- ----------------------------
DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标签名称',
  `tag_desc` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '标签描述',
  `sort` int NULL DEFAULT NULL COMMENT '分类优先级：数字小的在前面',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '创建人（账号/姓名）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '修改人（账号/姓名）',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_del` tinyint NOT NULL DEFAULT 0 COMMENT '软删除标识（0:未删除 1:已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_category_name`(`category_id` ASC, `name` ASC, `is_del` ASC) USING BTREE COMMENT '同一分类标签名称唯一',
  INDEX `idx_category_del`(`category_id` ASC, `is_del` ASC) USING BTREE COMMENT '分类+软删除联合索引'
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '标签表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `username` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '密码',
  `phone_number` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户邮箱',
  `user_status` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用[0:否，1:是]',
  `gender` tinyint NULL DEFAULT NULL COMMENT '性别[1:男，2:女，0:保密]',
  `open_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'openId',
  `avatar` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像',
  `admire` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '赞赏',
  `subscribe` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '订阅',
  `introduction` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '简介',
  `user_type` tinyint NOT NULL DEFAULT 2 COMMENT '用户类型[0:超级管理员，1:管理员，2:普通用户]',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最终修改时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最终修改人',
  `is_del` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标识(0-未删,1-已删)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for web_info
-- ----------------------------
DROP TABLE IF EXISTS `web_info`;
CREATE TABLE `web_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `web_name` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '网站名称',
  `web_title` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '网站信息',
  `notices` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '公告',
  `footer` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '页脚',
  `background_image` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '背景',
  `avatar` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '头像',
  `random_avatar` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '随机头像',
  `random_name` varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '随机名称',
  `random_cover` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '随机封面',
  `waifu_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '看板娘消息',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用[0:否，1:是]',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '网站信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for weblinks
-- ----------------------------
DROP TABLE IF EXISTS `weblinks`;
CREATE TABLE `weblinks`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键（自增ID）',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `classify` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分类',
  `url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '友链地址',
  `cover` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '友链头像',
  `desc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '友链描述',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '资源类型',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态[0:未审核，1:已审核]',
  `update_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最终修改人',
  `is_del` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标识（0-未删，1-已删）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_classify`(`classify` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_sort`(`sort` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '友链表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
