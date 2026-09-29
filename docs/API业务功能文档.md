# qs-blog 项目业务功能 API 文档

> 生成时间：2026-06-28
> 总端点数：**156**（36 个 Controller 文件）
> 技术栈：Spring Boot 2.7.18 + MyBatis-Plus + MySQL + Redis

---

## 一、总体统计

### 1.1 按模块统计

| 模块 | 端点数 | GET | POST | PUT | DELETE | PATCH |
|------|--------|-----|------|-----|--------|-------|
| access（访问统计） | 1 | 1 | 0 | 0 | 0 | 0 |
| article（文章管理） | 12 | 6 | 6 | 0 | 0 | 0 |
| behavior（行为管理） | 11 | 0 | 6 | 0 | 5 | 0 |
| category（分类管理） | 9 | 5 | 1 | 2 | 1 | 0 |
| comment（评论管理） | 8 | 4 | 4 | 0 | 0 | 0 |
| danmaku（弹幕管理） | 6 | 3 | 3 | 0 | 0 | 0 |
| family（表白墙） | 8 | 4 | 2 | 0 | 1 | 1 |
| file（文件管理） | 13 | 4 | 9 | 0 | 0 | 0 |
| im（即时通讯） | 21 | 17 | 4 | 0 | 0 | 0 |
| moment（动态管理） | 12 | 3 | 5 | 1 | 2 | 1 |
| sysconfig（系统配置） | 4 | 3 | 1 | 0 | 0 | 0 |
| sysdict（字典管理） | 13 | 7 | 4 | 0 | 2 | 0 |
| tag（标签管理） | 6 | 3 | 1 | 1 | 1 | 0 |
| user（用户管理） | 20 | 10 | 10 | 0 | 0 | 0 |
| webInfo（网站信息） | 5 | 4 | 1 | 0 | 0 | 0 |
| weblinks（友链/资源） | 6 | 1 | 4 | 0 | 1 | 0 |
| **合计** | **156** | **76** | **61** | **5** | **13** | **2** |

### 1.2 按 HTTP 方法统计

| 方法 | 数量 | 占比 |
|------|------|------|
| GET | 76 | 48.7% |
| POST | 61 | 39.1% |
| DELETE | 13 | 8.3% |
| PUT | 5 | 3.2% |
| PATCH | 2 | 1.3% |

### 1.3 权限等级说明

| 注解 | 含义 |
|------|------|
| 无 | 公开接口，无需登录 |
| `@LoginCheck` | 需要登录 |
| `@LoginCheck(0)` | 仅管理员可访问 |
| `@LoginCheck(1)` | 仅普通用户可访问 |
| `@LoginCheck(2)` | 默认登录检查 |
| `@SaveCheck` | 防重复提交（限流） |
| `@Idempotent` | 幂等性保护 |

---

## 二、模块详细 API

---

### 2.1 access（访问统计）— 1 个端点

#### AdminAccessLogController — `admin/accessLog`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| GET | `/admin/accessLog/getData` | 获取网站统计信息（IP/用户/省份/小时分布） | 无 | `@LoginCheck(0)` |

---

### 2.2 article（文章管理）— 12 个端点

#### AdminArticleController — `/admin/article`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| POST | `/admin/article/user/list` | 用户查询文章列表（分页） | `@RequestBody BaseReqVO` | `@LoginCheck(1)` |
| POST | `/admin/article/admin/list` | 管理员查询所有文章列表（分页） | `@RequestBody BaseReqVO` | `@LoginCheck(0)` |
| GET | `/admin/article/changeArticleStatus` | 修改文章状态（可见/评论/推荐） | `articleId`, `viewStatus?`, `commentStatus?`, `recommendStatus?` | `@LoginCheck(1)` |
| GET | `/admin/article/getArticleWithPwd` | 查询文章详情（管理后台，支持密码） | `id`, `pwd?` | `@LoginCheck(1)` |

#### ArticleController — `/article`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| POST | `/article/save` | 保存文章 | `@RequestBody ArticleVO` | `@LoginCheck(1)` |
| POST | `/article/update` | 更新文章 | `@RequestBody ArticleVO` | `@LoginCheck(1)` |
| GET | `/article/del` | 删除文章 | `id` | `@LoginCheck(1)` |
| POST | `/article/listPage` | 查询文章分页列表 | `@RequestBody ArticlePageDTO` | 公开 |
| GET | `/article/listSortArticle` | 查询分类文章列表 | 无 | 公开 |
| GET | `/article/getArticleById` | 查询文章详情 | `id`, `password?` | 公开 |
| POST | `/article/addArticleLikeCount` | 添加文章点赞数 | `@RequestBody ArticleDTO` | 公开 |
| GET | `/article/checkHasLike` | 检查是否已点赞 | `articleId` | 公开 |

---

### 2.3 behavior（行为管理）— 11 个端点

#### BehaviorController — `/behavior`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| POST | `/behavior/like` | 点赞 | `@RequestBody BehDTO` | `@LoginCheck` |
| DELETE | `/behavior/like` | 取消点赞 | `@RequestBody BehDTO` | `@LoginCheck` |
| POST | `/behavior/dislike` | 点踩 | `@RequestBody BehDTO` | `@LoginCheck` |
| DELETE | `/behavior/dislike` | 取消点踩 | `@RequestBody BehDTO` | `@LoginCheck` |
| POST | `/behavior/subscribe` | 订阅 | `@RequestBody BehDTO` | `@LoginCheck` |
| DELETE | `/behavior/subscribe` | 取消订阅 | `@RequestBody BehDTO` | `@LoginCheck` |
| POST | `/behavior/forward` | 转发 | `@RequestBody BehDTO` | `@LoginCheck` |
| DELETE | `/behavior/forward` | 取消转发 | `@RequestBody BehDTO` | `@LoginCheck` |
| POST | `/behavior/comment` | 记录评论行为 | `@RequestBody BehDTO` | `@LoginCheck` |
| DELETE | `/behavior/comment` | 取消评论记录 | `@RequestBody BehDTO` | `@LoginCheck` |
| POST | `/behavior/view` | 记录浏览 | `@RequestBody BehDTO` | `@LoginCheck` |

---

### 2.4 category（分类管理）— 9 个端点

#### AdminCategoryController — `/admin/categories`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| GET | `/admin/categories/page` | 分页查询分类列表 | `keyword?`, `type?`, `status?`, `pageNum=1`, `pageSize=10` | `@LoginCheck` |
| GET | `/admin/categories/{id}` | 获取分类详情 | `@PathVariable Long id` | `@LoginCheck` |
| POST | `/admin/categories` | 新增分类 | `@RequestBody CategorySaveReqDTO` | `@LoginCheck` |
| PUT | `/admin/categories/{id}` | 更新分类 | `@PathVariable Long id`, `@RequestBody CategorySaveReqDTO` | `@LoginCheck` |
| DELETE | `/admin/categories/{id}` | 删除分类 | `@PathVariable Long id` | `@LoginCheck` |
| PUT | `/admin/categories/batchSort` | 批量排序 | `@RequestBody CategoryBatchSortDTO` | `@LoginCheck` |

#### CategoryWebController — `/api/categories`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| GET | `/api/categories/list` | 获取公开分类列表 | `type?` | 公开 |

#### CategorySubscribeController — `/category`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| GET | `/category/subscribe` | 订阅/取消订阅专栏 | `categoryId`, `flag` | `@LoginCheck` |

---

### 2.5 comment（评论管理）— 8 个端点

#### AdminCommentController — `/admin`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| GET | `/admin/comment/user/deleteComment` | 作者删除评论 | `id` | `@LoginCheck(1)` |
| GET | `/admin/comment/boss/deleteComment` | 管理员删除评论 | `id` | `@LoginCheck(0)` |
| POST | `/admin/comment/user/list` | 作者查询评论列表（分页） | `@RequestBody BaseReqVO` | `@LoginCheck(1)` |
| POST | `/admin/comment/boss/list` | 管理员查询评论列表（分页） | `@RequestBody BaseReqVO` | `@LoginCheck(0)` |

#### CommentController — `/comment`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| POST | `/comment/save` | 保存评论 | `@RequestBody CommentVO` | `@LoginCheck`, `@SaveCheck` |
| GET | `/comment/delete` | 删除评论 | `id` | `@LoginCheck` |
| GET | `/comment/getCount` | 查询评论数量 | `bizId`, `type` | 公开 |
| POST | `/comment/list` | 查询评论列表 | `@RequestBody BaseReqVO` | 公开 |

---

### 2.6 danmaku（弹幕管理）— 6 个端点

#### AdminDanmakuController — `/admin/danmaku`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| POST | `/admin/danmaku/boss/list` | 管理员查询弹幕列表 | `@RequestBody BaseReqVO` | `@LoginCheck(0)` |

#### DanmakuController — `/danmaku`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| POST | `/danmaku/save` | 保存弹幕留言 | `@RequestBody Danmaku` | `@SaveCheck` |
| POST | `/danmaku/saveList` | 批量保存弹幕留言 | `@RequestBody List<Danmaku>` | 公开 |
| GET | `/danmaku/del` | 删除弹幕留言 | `id` | `@LoginCheck(0)` |
| GET | `/danmaku/list` | 查询弹幕留言列表 | 无 | 公开 |
| GET | `/danmaku/latestDanmaku` | 获取最新弹幕留言 | 无 | 公开 |

---

### 2.7 family（表白墙）— 8 个端点

#### AdminFamilyController — `/admin/families`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| GET | `/admin/families/page` | 分页查询家庭列表 | `FamilyPageQueryDTO` | `@LoginCheck` |
| GET | `/admin/families/{id}` | 获取家庭详情 | `@PathVariable Long id` | `@LoginCheck` |
| POST | `/admin/families` | 新增/更新家庭卡片 | `@RequestBody Family` | `@LoginCheck` |
| DELETE | `/admin/families/{id}` | 软删除家庭卡片 | `@PathVariable Long id` | `@LoginCheck` |
| PATCH | `/admin/families/{id}/status` | 切换启用状态 | `@PathVariable Long id` | `@LoginCheck` |

#### FamilyWebController — `/api/families`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| GET | `/api/families/list` | 获取公开家庭列表 | `size=10` | 公开 |
| GET | `/api/families/{id}` | 获取单个家庭详情 | `@PathVariable Long id` | 公开 |
| POST | `/api/families/{id}/like` | 点赞 | `@PathVariable Long id` | `@LoginCheck` |

---

### 2.8 file（文件管理）— 13 个端点

#### FileAdminController — `/file/admin`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| POST | `/file/admin/save` | 保存文件记录 | `@RequestBody FileCreateReqVO` | `@LoginCheck` |
| POST | `/file/admin/upload` | 上传文件 | `MultipartFile file`, `FileVO` | `@LoginCheck` |
| POST | `/file/admin/delete` | 删除文件 | `id` | `@LoginCheck` |
| POST | `/file/admin/list` | 查询文件列表 | `@RequestBody BaseReqVO` | `@LoginCheck` |

#### QiniuController — `/qiniu`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| GET | `/qiniu/getUpToken` | 获取七牛云上传凭证 | `key` | `@LoginCheck`, `@SaveCheck` |

#### MusicController — `/music`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| GET | `/music/listFunny` | 查询音乐列表 | 无 | 公开 |
| POST | `/music/saveFunny` | 保存音乐 | `@RequestBody WeblinksVO` | `@LoginCheck`, `@SaveCheck` |

#### PictureController — `/webInfo`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| GET | `/webInfo/listAdminLovePhoto` | 查询爱情照片 | 无 | 公开 |
| POST | `/webInfo/saveLovePhoto` | 保存爱情照片 | `@RequestBody WeblinksVO` | `@LoginCheck`, `@SaveCheck` |

#### CollectController — `/collect`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| GET | `/collect/list` | 查询收藏 | 无 | 公开 |

#### ResourceAggregationController — `/resAgg`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| POST | `/resAgg/saveWeblinks` | 保存通用资源 | `@RequestBody WeblinksVO` | `@LoginCheck(0)` |
| GET | `/resAgg/deleteWeblinks` | 删除资源 | `id` | `@LoginCheck(0)` |
| POST | `/resAgg/updateWeblinks` | 更新资源 | `@RequestBody WeblinksVO` | `@LoginCheck(0)` |
| POST | `/resAgg/listWeblinks` | 分页查询资源 | `@RequestBody BaseReqVO` | 公开 |

---

### 2.9 im（即时通讯）— 21 个端点

> 注：IM 模块基于 WebSocket (t-io)，以下为 HTTP API 辅助接口

#### ImChatGroupController — `/imChatGroup`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| POST | `/imChatGroup/creatGroupCommon` | 创建普通群组 | `@RequestBody ImChatGroup` | `@LoginCheck`, `@SaveCheck` |
| POST | `/imChatGroup/creatGroupTopic` | 创建话题群组 | `@RequestBody ImChatGroup` | `@LoginCheck(0)` |
| POST | `/imChatGroup/updateGroup` | 更新群组 | `@RequestBody ImChatGroup` | `@LoginCheck` |
| GET | `/imChatGroup/deleteGroup` | 解散群组 | `id` | `@LoginCheck` |
| POST | `/imChatGroup/listGroupForAdmin` | 管理员查询所有群 | `@RequestBody BaseReqVO` | `@LoginCheck(0)` |
| GET | `/imChatGroup/addGroupTopic` | 加入话题 | `id` | `@LoginCheck` |
| GET | `/imChatGroup/listGroup` | 用户查询所有群 | 无 | `@LoginCheck` |

#### ImChatGroupUserController — `/imChatGroupUser`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| GET | `/imChatGroupUser/enterGroup` | 申请加群 | `id`, `remark?` | `@LoginCheck` |
| GET | `/imChatGroupUser/quitGroup` | 退群 | `id` | `@LoginCheck` |
| GET | `/imChatGroupUser/changeUserStatus` | 改变群组用户状态 | `groupId`, `userId`, `userStatus`, `oldUserStatus` | `@LoginCheck` |
| GET | `/imChatGroupUser/changeAdmin` | 设置群组管理员 | `groupId`, `userId`, `adminFlag` | `@LoginCheck` |
| GET | `/imChatGroupUser/getGroupUser` | 群用户查询群成员 | `groupId`, `current=1`, `size=20` | `@LoginCheck` |
| GET | `/imChatGroupUser/getGroupUserByStatus` | 群管理员查询群用户 | `groupId?`, `userStatus?`, `current=1`, `size=20` | `@LoginCheck` |

#### ImChatUserFriendController — `/imChatUserFriend`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| GET | `/imChatUserFriend/addFriend` | 添加好友申请 | `friendId`, `remark?` | `@LoginCheck` |
| GET | `/imChatUserFriend/getFriend` | 查询好友列表 | `friendStatus?` | `@LoginCheck` |
| GET | `/imChatUserFriend/changeFriend` | 修改好友信息 | `friendId`, `friendStatus?`, `remark?` | `@LoginCheck` |

#### ImChatUserMessageController — `/imChatUserMessage`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| GET | `/imChatUserMessage/listSystemMessage` | 获取系统消息 | `current=1`, `size=10` | `@LoginCheck` |
| GET | `/imChatUserMessage/saveSystemMessage` | 管理员添加系统消息 | `content` | `@LoginCheck(0)` |
| GET | `/imChatUserMessage/deleteSystemMessage` | 删除系统消息 | `id` | `@LoginCheck(0)` |
| GET | `/imChatUserMessage/listFriendMessage` | 获取朋友消息 | `friendId`, `current=1`, `size=40` | `@LoginCheck` |

#### ImChatUserGroupMessageController — `/imChatUserGroupMessage`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| GET | `/imChatUserGroupMessage/listGroupMessage` | 获取群消息 | `groupId`, `current=1`, `size=40` | `@LoginCheck` |

---

### 2.10 moment（动态管理）— 12 个端点

#### AdminMomentController — `/admin/moment`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| GET | `/admin/moment/page` | 分页查询动态列表 | `MomentPageQueryDTO` | `@LoginCheck` |
| GET | `/admin/moment/{id}` | 获取动态详情 | `@PathVariable Long id` | `@LoginCheck` |
| POST | `/admin/moment` | 新增动态 | `@RequestBody MomentSaveReqDTO` | `@LoginCheck` |
| PUT | `/admin/moment/{id}` | 更新动态 | `@PathVariable Long id`, `@RequestBody MomentSaveReqDTO` | `@LoginCheck` |
| DELETE | `/admin/moment/{id}` | 删除动态 | `@PathVariable Long id` | `@LoginCheck` |
| DELETE | `/admin/moment/batch` | 批量删除动态 | `@RequestBody List<Long> ids` | `@LoginCheck` |
| PATCH | `/admin/moment/{id}/audit` | 审核动态 | `@PathVariable Long id`, `status`, `auditReason?` | `@LoginCheck` |

#### MomentController — `/moment`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| POST | `/moment/save` | 保存朋友圈动态 | `@RequestBody Moment` | `@LoginCheck`, `@SaveCheck` |
| POST | `/moment/saveNews` | 保存新闻来源动态 | `@RequestBody Moment` | `@LoginCheck` |
| POST | `/moment/list` | 查询动态列表 | `@RequestBody BaseReqVO` | 公开 |
| POST | `/moment/listNews` | 查询新闻列表 | `@RequestBody BaseReqVO` | 公开 |
| GET | `/moment/del` | 删除动态 | `id` | `@LoginCheck` |

---

### 2.11 sysconfig（系统配置）— 4 个端点

#### SysConfigController — `/sysConfig`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| GET | `/sysConfig/listSysConfig` | 查询系统参数 | 无 | 公开 |
| GET | `/sysConfig/listConfig` | 查询所有系统配置 | 无 | `@LoginCheck(0)` |
| POST | `/sysConfig/saveOrUpdateConfig` | 保存或更新系统配置 | `@RequestBody SysConfig` | `@LoginCheck(0)` |
| GET | `/sysConfig/deleteConfig` | 删除系统配置 | `id` | `@LoginCheck(0)` |

---

### 2.12 sysdict（字典管理）— 13 个端点

#### AdminSysDictController — `/admin/sysDict`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| GET | `/admin/sysDict/type/list` | 分页查询字典类型 | `typeName?`, `status?`, `pageNum=1`, `pageSize=10` | `@LoginCheck` |
| POST | `/admin/sysDict/type/save` | 新增字典类型 | `@RequestBody SysDictTypeReq` | `@LoginCheck` |
| POST | `/admin/sysDict/type/update` | 更新字典类型 | `@RequestBody SysDictTypeReq` | `@LoginCheck` |
| DELETE | `/admin/sysDict/type/batchDelete` | 删除字典类型 | `@RequestBody Long[] ids` | `@LoginCheck` |
| GET | `/admin/sysDict/type/listEnabled` | 获取所有启用的字典类型 | 无 | 公开 |
| GET | `/admin/sysDict/data/listByTypeCode` | 根据类型编码查询字典数据 | `typeCode` | 公开 |
| POST | `/admin/sysDict/data/save` | 新增字典数据 | `@RequestBody SysDictDataReq` | `@LoginCheck` |
| POST | `/admin/sysDict/data/update` | 更新字典数据 | `@RequestBody SysDictDataReq` | `@LoginCheck` |
| DELETE | `/admin/sysDict/data/batchDelete` | 批量删除字典数据 | `@RequestBody Long[] ids` | `@LoginCheck` |

#### SysDictController — `/sysDict`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| GET | `/sysDict/listByTypeCode` | 根据类型编码查询字典数据 | `typeCode` | 公开 |
| GET | `/sysDict/batchList` | 批量查询多个字典类型数据 | `typeCodes` | 公开 |
| GET | `/sysDict/type/list` | 获取所有启用的字典类型 | 无 | 公开 |
| GET | `/sysDict/itemLabel` | 代码值转显示标签 | `typeCode`, `itemValue` | 公开 |

---

### 2.13 tag（标签管理）— 6 个端点

#### AdminTagController — `/admin/tags`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| GET | `/admin/tags/page` | 分页查询标签列表 | `categoryId?`, `keyword?`, `pageNum=1`, `pageSize=10` | `@LoginCheck` |
| GET | `/admin/tags/{id}` | 获取标签详情 | `@PathVariable Long id` | `@LoginCheck` |
| POST | `/admin/tags` | 新增标签 | `@RequestBody TagSaveReqDTO` | `@LoginCheck` |
| PUT | `/admin/tags/{id}` | 更新标签 | `@PathVariable Long id`, `@RequestBody TagSaveReqDTO` | `@LoginCheck` |
| DELETE | `/admin/tags/{id}` | 删除标签 | `@PathVariable Long id` | `@LoginCheck` |

#### TagWebController — `/api/tags`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| GET | `/api/tags/list` | 获取标签列表 | `categoryId?` | 公开 |

---

### 2.14 user（用户管理）— 20 个端点

#### AdminUserController — `/admin/user`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| POST | `/admin/user/list` | 查询用户列表（分页） | `@RequestBody BaseReqVO` | `@LoginCheck(0)` |
| GET | `/admin/user/changeUserStatus` | 修改用户状态（封禁/解封） | `userId`, `flag` | `@LoginCheck(0)` |
| GET | `/admin/user/changeUserAdmire` | 修改用户赞赏信息 | `userId`, `admire` | `@LoginCheck(0)` |
| GET | `/admin/user/changeUserType` | 修改用户类型 | `userId`, `userType` | `@LoginCheck(0)` |

#### UserController — `/user`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| POST | `/user/register` | 用户注册 | `@RequestBody UserVO` | 公开 |
| POST | `/user/login` | 用户登录 | `account`, `password`, `isAdmin?` | 公开 |
| POST | `/user/token` | Token登录 | `userToken` | 公开 |
| GET | `/user/logout` | 退出登录 | 无 | `@LoginCheck` |
| POST | `/user/updateUserInfo` | 更新用户信息 | `@RequestBody UserVO` | `@LoginCheck` |
| POST | `/user/updateSecretInfo` | 更新安全信息（手机/邮箱） | `place`, `flag`, `code?`, `password` | `@LoginCheck` |
| POST | `/user/updateForForgetPassword` | 忘记密码更新密码 | `place`, `flag`, `code`, `password` | 公开 |
| POST | `/user/resetPwdForFgtPwd` | 忘记密码重置密码（邮箱） | `email`, `bizType`, `code`, `password` | 公开 |
| GET | `/user/getCode` | 获取验证码 | `flag` | `@LoginCheck`, `@SaveCheck` |
| GET | `/user/getCodeByRegister` | 注册获取验证码 | `place`, `flag` | `@SaveCheck` |
| GET | `/user/getCodeForBind` | 绑定手机/邮箱获取验证码 | `place`, `flag` | `@LoginCheck`, `@SaveCheck` |
| GET | `/user/getUserByUsername` | 根据用户名查找用户 | `username` | `@LoginCheck` |
| GET | `/user/captcha` | 获取图形验证码 | `flag`, `email` | 公开 |
| POST | `/user/captchaCheck` | 校验图形验证码 | `uuid`, `code` | 公开 |
| GET | `/user/mailCode` | 发送邮箱验证码 | `bizType`, `email` | 公开 |

---

### 2.15 webInfo（网站信息）— 5 个端点

#### AdminWebInfoController — `/admin/webInfo`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| GET | `/admin/webInfo/get` | 获取网站信息 | 无 | `@LoginCheck(0)` |

#### WebInfoController — `/webInfo`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| GET | `/webInfo/get` | 获取网站信息 | 无 | 公开 |
| POST | `/webInfo/update` | 更新网站信息 | `@RequestBody WebInfo` | `@LoginCheck(0)` |
| GET | `/webInfo/getAdmire` | 获取赞赏用户信息 | 无 | 公开 |
| GET | `/webInfo/getWaifuJson` | 获取看板娘消息 | 无 | 公开 |

---

### 2.16 weblinks（友链/资源）— 6 个端点

#### WeblinksController — `/weblinks`

| 方法 | 路径 | 描述 | 参数 | 权限 |
|------|------|------|------|------|
| POST | `/weblinks/save` | 保存友链 | `@RequestBody WeblinksVO` | `@LoginCheck`, `@SaveCheck` |
| GET | `/weblinks/list` | 查询友链列表 | 无 | 公开 |
| POST | `/weblinks/saveWeblinks` | 保存通用资源 | `@RequestBody WeblinksVO` | `@LoginCheck(0)` |
| POST | `/weblinks/update` | 更新资源 | `@RequestBody WeblinksVO` | `@LoginCheck(0)` |
| DELETE | `/weblinks/deleteWeblinks` | 删除资源 | `id` | `@LoginCheck(0)` |
| POST | `/weblinks/listWeblinks` | 分页查询资源 | `resourceType?`, `classify?`, `order`, `desc`, `pageNum`, `pageSize` | 公开 |

---

## 三、路径前缀约定

| 前缀 | 用途 | 示例 |
|------|------|------|
| `/admin/...` | 后台管理接口 | `/admin/article/admin/list` |
| `/api/...` | 公开 Web 接口 | `/api/categories/list` |
| `/...` (无前缀) | 混合接口 | `/article/save` |

---

## 四、统一响应格式

所有接口均返回 `R<T>` 统一响应体：

```json
{
  "code": 200,
  "msg": "success",
  "data": { ... }
}
```

- `code`: 状态码，200 表示成功
- `msg`: 提示信息
- `data`: 响应数据，泛型类型因接口而异

---

## 五、分页查询规范

分页查询统一使用 `BaseReqVO` 或 `PageQuery` 作为请求参数：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| pageNum | Integer | 1 | 页码 |
| pageSize | Integer | 10 | 每页条数 |
| keyword | String | - | 搜索关键词 |
| orderBy | String | createTime | 排序字段 |
| desc | Boolean | true | 是否降序 |

返回类型为 `PageResult<T>`，包含：
- `records`: 数据列表
- `total`: 总记录数
- `pageNum`: 当前页码
- `pageSize`: 每页条数
