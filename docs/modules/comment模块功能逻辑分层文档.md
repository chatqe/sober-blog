# Comment 模块功能逻辑详细分层文档

> 生成日期: 2026-06-21
> 技术栈: Spring Boot 2.7 + MyBatis-Plus + Redis 限流 + @SaveCheck AOP + 邮件通知

---

## 模块概览

```
comment/
  ├── controller/
  │   ├── web/
  │   │   ├── CommentController.java          # 用户端：保存/删除/列表/评论数
  │   │   └── vo/
  │   │       └── CommentVO.java              # 评论视图对象（含 username/avatar/childComments）
  │   └── admin/
  │       └── AdminCommentController.java     # 管理端：作者删除/管理员删除/作者列表/管理员列表
  ├── service/
  │   ├── CommentService.java                 # 接口（extends CoreService<Comment>）
  │   └── CommentServiceImpl.java             # 实现
  ├── domain/
  │   ├── pojo/Comment.java                   # 评论实体
  │   ├── dao/CommentMapper.java              # extends CoreMapper<Comment>
  │   └── dto/
  └── enums/
      └── CommentTypeEnum.java                # 1=文章 2=树洞 3=表白墙
```

---

## 数据库实体：Comment

**表名**: `comment`
**继承**: `BaseDO`（提供 `id` 雪花ID、`isDel` 逻辑删除、`createTime`、`updateTime`）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 雪花ID（BaseDO） |
| parentId | Long | 父评论ID，0=根评论 |
| rootId | Long | 顶级祖先评论ID，0=自身即为根评论 |
| bizType | Integer | 业务类型：1=文章评论, 2=树洞留言, 3=表白墙留言 |
| bizId | Long | 业务主键ID（如文章ID） |
| authorId | Long | 评论人用户ID |
| authorName | String | 评论人名称（冗余字段） |
| authorAvatar | String | 评论人头像（冗余字段） |
| content | String | 评论内容 |
| replyToUserId | Long | 回复的目标用户ID，0=无特定回复对象 |
| status | Integer | 评论状态：1=正常, 2=禁言, 3=审核中 |
| ip | String | 提交IP地址 |
| ua | String | 提交时User-Agent |
| isOwner | Integer | 是否为内容作者：0=否, 1=是 |
| isTop | Integer | 是否置顶：0=否, 1=是 |
| likeCnt | Integer | 点赞数 |
| childCnt | Integer | 直接子评论数量 |
| contentHash | String | 内容SHA256哈希，用于防刷评 |
| version | Integer | 乐观锁版本号（@Version） |
| moderatorId | Long | 审核人ID |
| moderateTime | LocalDateTime | 审核时间 |

### 评论层级模型

采用 **双指针设计**：

- `parentId`：直接父节点ID（0表示根评论）。用于快速定位直接上级。
- `rootId`：顶级祖先ID（0表示自身即为根评论）。用于按话题线程分组加载。

示例层级：
```
根评论A (parentId=0, rootId=0)
  ├── 回复A1 (parentId=A.id, rootId=0)
  │     └── 回复A1-1 (parentId=A1.id, rootId=0)
  └── 回复A2 (parentId=A.id, rootId=0)

根评论B (parentId=0, rootId=0)
  └── 回复B1 (parentId=B.id, rootId=0)
```

---

## 场景一：保存评论

**Controller**: `POST /comment/save`
**注解**: `@LoginCheck` + `@SaveCheck`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| bizId | Long | 是 | 业务主键ID（如文章ID） |
| bizType | Integer | 是 | 业务类型（1/2/3） |
| content | String | 是 | 评论内容 |
| parentId | Long | 否 | 父评论ID，默认0（根评论） |
| rootId | Long | 否 | 顶级祖先ID，默认0 |
| replyToUserId | Long | 否 | 回复目标用户ID |

### 完整流程

```
POST /comment/save
  |
  +--> @LoginCheck (拦截未登录请求)
  |
  +--> @SaveCheck (AOP 限流拦截)
  |       |
  |       +-- 步骤1: 管理员豁免
  |       |       解析 token 获取当前用户，与 admin 缓存用户比对
  |       |       若是管理员(Boss)，直接放行，不限流
  |       |
  |       +-- 步骤2: 用户维度限流
  |       |       Key: "save_count_user_id_{userId}"
  |       |       INCR 自增，首次设置 TTL = 86400秒(24小时)
  |       |       上限: SAVE_MAX_COUNT = 15次/天
  |       |
  |       +-- 步骤3: IP维度限流
  |               Key: "save_count_ip_{realIp}"
  |               INCR 自增，首次设置 TTL = 86400秒
  |               上限: 15次/天
  |               任一维度超限 -> BizException("今日提交次数已用尽")
  |
  +--> Controller层预处理
  |       |
  |       +-- 步骤4: HTML标签过滤
  |       |       StrUtil.removeHtml(): < -> "《", > -> "》"
  |       |       防止XSS攻击
  |       |
  |       +-- 步骤5: 内容非空校验
  |       |       过滤后仍无文本 -> R.fail("评论内容不合法")
  |       |
  |       +-- 步骤6: 清除评论数缓存
  |               del "comment_count_{bizId}_{bizType}"
  |               (下次查询评论数时重新计算)
  |
  +--> Service: saveComment(CommentVO)
          |
          +-- 步骤7: bizType 合法性校验
          |       CommentTypeEnum.getByCode(bizType) == null -> R.fail("评论来源类型不存在")
          |
          +-- 步骤8: 文章存在性与评论开关校验 (仅 bizType=1 文章评论)
          |       查询 Article 的 userId, articleTitle, commentStatus
          |       不存在 -> R.fail("文章不存在")
          |       commentStatus=false -> R.fail("评论功能已关闭")
          |
          +-- 步骤9: 构建 Comment 实体
          |       bizId, bizType, content, parentId, rootId, replyToUserId
          |       authorId = BlogUtil.getUserId() (从 token 解析)
          |
          +-- 步骤10: save(comment) 持久化
          |         (MyBatis-Plus 自动填充 createTime/updateTime)
          |
          +-- 步骤11: 发送邮件通知 (try-catch 非阻塞)
          |         mailSendUtil.sendCommentMail(commentVO, article, commentService)
          |         |
          |         +-- 若 replyToUserId 不为空:
          |         |       查询被回复用户，若非本人且有邮箱 -> 发送通知邮件
          |         |       邮件频率限制: COMMENT_IM_MAIL_COUNT=1次/天
          |         |
          |         +-- 若为根评论且 article 存在:
          |         |       查询文章作者 -> 发送通知邮件
          |         |
          |         +-- 异常捕获: 邮件发送失败不阻断评论保存
          |
          +--> R.success()
```

### @SaveCheck 限流机制详解

```java
// Redis 限流核心逻辑 (SaveCheckAspect)
String userKey = "save_count_user_id_" + userId;
Long userIdCount = stringRedisTemplate.opsForValue().increment(userKey, 1);
if (userIdCount == 1) {
    stringRedisTemplate.expire(userKey, 86400, TimeUnit.SECONDS); // 24小时过期
}
if (userIdCount > 15) {
    // 超限，标记 flag = true
}

// IP 维度同样限流
String ipKey = "save_count_ip_" + realIp;
Long ipCount = stringRedisTemplate.opsForValue().increment(ipKey, 1);
if (ipCount > 15) {
    flag = true;
}

if (flag) {
    throw new BizException("今日提交次数已用尽，请一天后再来！");
}
```

---

## 场景二：删除评论（用户端）

**Controller**: `GET /comment/delete?id={id}`
**注解**: `@LoginCheck`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 评论ID |

### 流程

```
GET /comment/delete?id={id}
  |
  +--> @LoginCheck (需登录)
  |
  +--> Service: deleteComment(id)
          |
          +-- 获取当前用户ID: BlogUtil.getUserId()
          |
          +-- lambdaUpdate()
          |       .eq(Comment::getId, id)
          |       .eq(Comment::getAuthorId, userId)  // 所有权校验
          |       .remove()                          // 逻辑删除 (isDel=1)
          |
          +--> R.success()
```

**关键**: 使用 `lambdaUpdate().eq(id).eq(authorId).remove()` 确保 **只有评论作者本人** 能删除自己的评论。SQL 等价于 `DELETE FROM comment WHERE id=? AND author_id=? AND is_del=0`。

---

## 场景三：删除评论（管理端 - 文章作者）

**Controller**: `GET /admin/comment/user/deleteComment?id={id}`
**注解**: `@LoginCheck(1)`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Integer | 是 | 评论ID |

### 流程

```
GET /admin/comment/user/deleteComment?id={id}
  |
  +--> @LoginCheck(1) (需登录，非严格模式)
  |
  +-- 步骤1: 查询评论 bizId + bizType
  |       仅允许删除文章类评论 (bizType=1)
  |       非文章类 -> R.fail("权限不足")
  |
  +-- 步骤2: 查询文章作者ID
  |       Article.userId 与当前登录用户比对
  |       不匹配 -> R.fail("权限不足")
  |
  +-- 步骤3: 执行删除
  |       commentService.removeById(id)
  |
  +--> R.success()
```

---

## 场景四：删除评论（管理端 - 超级管理员）

**Controller**: `GET /admin/comment/boss/deleteComment?id={id}`
**注解**: `@LoginCheck(0)`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Integer | 是 | 评论ID |

### 流程

```
GET /admin/comment/boss/deleteComment?id={id}
  |
  +--> @LoginCheck(0) (需登录，严格模式=管理员)
  |
  +--> commentService.removeById(id)  // 无权限校验，直接删除
  |
  +--> R.success()
```

---

## 场景五：查询评论列表（用户端）

**Controller**: `POST /comment/list`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 是 | 页码 |
| pageSize | Integer | 是 | 每页条数 |
| bizId | Long | 是 | 业务ID |
| commentType | String | 是 | 评论类型（数字如"1"或描述如"文章评论"） |
| rootId | Long | 否 | 若传值，则查询该根评论下的直接回复 |

### 流程

```
POST /comment/list
  |
  +-- 步骤1: 参数校验
  |       bizId==null 或 commentType==空 -> R.fail(PARAMETER_ERROR)
  |
  +-- 步骤2: 解析评论类型
  |       parseCommentType(commentType):
  |         - 先尝试 Integer.parseInt(type) 解析数字
  |         - 失败则 CommentTypeEnum.getByDesc(type) 按描述匹配
  |         - 仍失败 -> R.fail("评论来源类型不存在")
  |
  +-- 步骤3: 文章评论时检查评论开关
  |       若 bizType=1，查询 Article.commentStatus
  |       false -> R.fail("评论功能已关闭")
  |
  +-- 步骤4: 分支判断 (rootId 是否为空)
  |       |
  |       +-- 分支A: rootId==null (根评论列表)
  |       |       |
  |       |       +-- 查询根评论:
  |       |       |       WHERE bizId=? AND bizType=? AND parentId=0
  |       |       |       ORDER BY createTime DESC
  |       |       |       分页 (BaseReqVO.page)
  |       |       |
  |       |       +-- 对每条根评论:
  |       |       |       1. buildCommentVO(c) -> 填充 username/avatar/parentUsername
  |       |       |       2. 查询直接子回复 (rootId=c.id, 最多5条):
  |       |       |              WHERE bizId=? AND bizType=? AND rootId=c.id
  |       |       |              ORDER BY createTime ASC
  |       |       |       3. 子回复也 buildCommentVO
  |       |       |       4. 设置 commentVO.childComments = 子回复分页
  |       |       |
  |       |       +-- 组装结果: baseReqVO.records = CommentVO列表
  |       |
  |       +-- 分支B: rootId!=null (单条评论的直接回复列表)
  |               |
  |               +-- 查询:
  |               |       WHERE bizId=? AND bizType=? AND rootId=?
  |               |       ORDER BY createTime ASC
  |               |       分页
  |               |
  |               +-- 每条 buildCommentVO -> 填充用户信息
  |               |
  |               +-- baseReqVO.records = CommentVO列表
  |
  +--> R.success(baseReqVO)
```

### buildCommentVO 用户信息填充

```java
private CommentVO buildCommentVO(Comment c) {
    CommentVO vo = new CommentVO();
    BeanUtils.copyProperties(c, vo);

    // 1. 查询作者用户信息
    User user = commonQuery.getUser(vo.getAuthorId());
    if (user != null) {
        vo.setUsername(user.getUsername());
        vo.setAvatar(user.getAvatar());
    }

    // 2. 用户不存在时的降级: 使用随机昵称
    if (!StringUtils.hasText(vo.getUsername())) {
        vo.setUsername(BlogUtil.getRandomName(vo.getAuthorId().toString()));
    }

    // 3. 若有回复目标用户，填充被回复者用户名
    if (vo.getReplyToUserId() != null) {
        User replyUser = commonQuery.getUser(vo.getReplyToUserId());
        if (replyUser != null) {
            vo.setParentUsername(replyUser.getUsername());
        }
        if (!StringUtils.hasText(vo.getParentUsername())) {
            vo.setParentUsername(BlogUtil.getRandomName(vo.getReplyToUserId().toString()));
        }
    }

    return vo;
}
```

---

## 场景六：管理端评论列表查询

### 6.1 管理员全量列表 (Boss)

**Controller**: `POST /admin/comment/boss/list`
**注解**: `@LoginCheck(0)`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum/pageSize | Integer | 是 | 分页参数 |
| bizId | Long | 否 | 按业务ID筛选 |
| commentType | String | 否 | 按评论类型筛选 |

### 流程

```
POST /admin/comment/boss/list
  |
  +--> commentService.listAdminComment(baseReqVO, isBoss=true)
          |
          +-- 构建 wrapper = lambdaQuery()
          |
          +-- 条件筛选:
          |       bizId!=null -> eq(bizId, bizId)
          |       commentType!=空 -> 解析类型 -> eq(bizType, type)
          |
          +-- orderByDesc(createTime).page(baseReqVO)
          |
          +--> R.success(baseReqVO)
```

### 6.2 作者查看自己文章的评论 (User)

**Controller**: `POST /admin/comment/user/list`
**注解**: `@LoginCheck(1)`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum/pageSize | Integer | 是 | 分页参数 |
| bizId | Long | 否 | 若传值，限定某篇文章的评论 |

### 流程

```
POST /admin/comment/user/list
  |
  +--> commentService.listAdminComment(baseReqVO, isBoss=false)
          |
          +-- 步骤1: 查询当前用户所有文章ID
          |       commonQuery.getUserArticleIds(currentUserId)
          |       优先从 Redis 缓存 "user_article_list_{userId}" 读取
          |       缓存未命中 -> 查 ArticleMapper -> 写缓存(TTL=1800s)
          |
          +-- 步骤2: 若无文章 -> 返回空分页
          |
          +-- 步骤3: 条件筛选
          |       bizId!=null -> eq(bizId, bizId).eq(bizType=文章)
          |       bizId==null -> in(bizId, 用户文章ID列表).eq(bizType=文章)
          |
          +-- 步骤4: orderByDesc(createTime).page(baseReqVO)
          |
          +--> R.success(baseReqVO)
```

---

## 场景七：查询评论数量

**Controller**: `GET /comment/getCount`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| bizId | Long | 是 | 业务ID |
| type | String | 是 | 评论类型（数字或描述） |

### 流程

```
GET /comment/getCount?bizId={bizId}&type={type}
  |
  +--> commonQuery.getCommentCount(bizId, type)
          |
          +-- 步骤1: 尝试从缓存读取
          |       Key: "comment_count_{bizId}_{type}"
          |       命中 -> 返回缓存值
          |
          +-- 步骤2: 缓存未命中 -> 查询数据库
          |       SELECT COUNT(*) FROM comment
          |       WHERE bizId=? AND bizType=? AND isDel=0
          |
          +-- 步骤3: 写入缓存 (TTL=1800s)
          |       qingSiCache.put(key, count, 1800)
          |
          +--> count
```

---

## 评论类型枚举：CommentTypeEnum

| 枚举值 | code | desc | 说明 |
|--------|------|------|------|
| ARTICLE | 1 | 文章评论 | 博客文章下的评论 |
| MESSAGE | 2 | 树洞留言 | 树洞功能的留言 |
| LOVE | 3 | 表白墙留言 | 表白墙功能的留言 |

**辅助方法**:
- `getByCode(Integer code)` - 根据编码获取枚举
- `getByDesc(String desc)` - 根据描述获取枚举（忽略大小写）
- `isValid(Integer code)` - 验证编码有效性
- `getEnumMap()` - 获取编码到描述的 Map

---

## 附录一：权限矩阵

| 接口 | 路径 | 认证级别 | 权限说明 |
|------|------|---------|---------|
| 保存评论 | POST /comment/save | `@LoginCheck` (level=2) | 需登录用户 |
| 删除评论(自己) | GET /comment/delete | `@LoginCheck` (level=2) | 仅评论作者本人 |
| 删除评论(作者) | GET /admin/comment/user/deleteComment | `@LoginCheck(1)` | 仅文章作者 |
| 删除评论(管理员) | GET /admin/comment/boss/deleteComment | `@LoginCheck(0)` | 仅超级管理员 |
| 评论列表 | POST /comment/list | 公开 | 无需登录 |
| 评论数 | GET /comment/getCount | 公开 | 无需登录 |
| 作者评论列表 | POST /admin/comment/user/list | `@LoginCheck(1)` | 仅文章作者 |
| 管理员评论列表 | POST /admin/comment/boss/list | `@LoginCheck(0)` | 仅超级管理员 |

`@LoginCheck` 等级说明:
- **0 (Boss)**: 严格模式，必须是超级管理员
- **1 (User)**: 文章作者可操作
- **2 (Default)**: 普通登录用户

---

## 附录二：@SaveCheck 限流配置

| 维度 | Redis Key 前缀 | 上限 | 过期时间 |
|------|---------------|------|---------|
| 用户ID | `save_count_user_id_` | 15次/天 | 86400秒 |
| IP地址 | `save_count_ip_` | 15次/天 | 86400秒 |
| 管理员 | 豁免 | 不限 | - |

---

## 附录三：缓存策略

| 缓存Key | 用途 | TTL | 失效时机 |
|---------|------|-----|---------|
| `comment_count_{bizId}_{type}` | 评论数量 | 1800s | 保存评论时主动删除 |
| `user_article_list_{userId}` | 用户文章ID列表 | 1800s | 发布/删除文章时失效 |
| `comment_im_mail_{userId}` | 邮件发送频率限制 | 86400s | 每天重置 |

---

## 附录四：特殊特性

### 1. HTML标签转义防XSS
`StrUtil.removeHtml()`: 将 `<` 替换为 `《`，`>` 替换为 `》`。简单粗暴但有效防止脚本注入。

### 2. 嵌套评论双指针设计
- `parentId=0` 表示根评论
- `rootId=0` 表示该评论是某个话题线程的根
- 加载根评论列表时按 `parentId=0` 查询
- 加载子回复时按 `rootId=根评论ID` 查询（一次性拿到整个线程的回复）

### 3. 乐观锁
`@Version` 注解在 `version` 字段上，MyBatis-Plus 自动处理并发更新时的版本号校验。

### 4. 邮件通知非阻塞
评论保存后通过 `try-catch` 包裹邮件发送，确保邮件发送失败不会回滚评论保存。

### 5. 用户信息降级
当用户被删除或不存在时，通过 `BlogUtil.getRandomName(authorId)` 从预设的随机昵称池中选取一个固定昵称（基于 authorId 的 hash 定位），保证评论展示始终有用户名。

---

## 待实现清单

| 功能 | 优先级 | 说明 |
|------|--------|------|
| 评论内容 SHA256 哈希计算 | P2 | 实体有 `contentHash` 字段但未在保存逻辑中填充 |
| 评论审核流程 | P2 | 实体有 `status`(审核中)、`moderatorId`、`moderateTime` 字段但无对应服务方法 |
| 评论点赞 | P2 | 实体有 `likeCnt` 字段但无点赞/取消点赞接口 |
| 评论置顶/取消置顶 | P3 | 实体有 `isTop` 字段但无操作接口 |
| 评论编辑 | P3 | 目前仅有新增和删除，缺少修改接口 |
| 评论举报 | P3 | 用户端举报不良评论 |
