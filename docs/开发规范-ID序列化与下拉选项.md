# 开发规范：ID 序列化与下拉选项端点

> 定稿时间：2026-10-26　适用仓库：qs-blog（后端）/ qs-web（前端）
> 背景：后台文章编辑页分类/标签下拉显示 `NaN`，根因是 `dict` 端点用 `cateDesc` 充当选项 value，
> 而前端拿它 `Number()` 当主键。为避免每个新页面重复踩坑，此处一次性定死契约。

## 一、ID 全链路字符串

**决策：所有主键与外键在 JSON 出参中为字符串，前端按字符串消费。**

### 后端

1. ID 字段（`id`、`categoryId`、`tagId`、`userId`、`bizId`、`resourceId` 等）Java 类型仍为 `Long`，
   但必须标注 `@JsonFormat(shape = JsonFormat.Shape.STRING)`，使 SpringDoc 与前端生成类型同为 `string`。
2. 非 ID 数值保持数字类型，禁止顺手转字符串：`sort`、`count`、`viewCount`、`total/pageNum/pageSize`、
   各类 `status/type/flag` 码值。
3. 入参方向不受影响：`Long` 形参 / DTO 字段可同时接收数字与字符串，前端直接传字符串即可。
4. 新增 ID 字段漏标 `@JsonFormat` 按 CR blocker 处理——前端 `el-select` 与 `===` 是严格相等，会静默失配。

### 前端

1. 数据层（`src/api/**`、`src/stores/**`）**禁止**对 ID 做 `Number()` / `parseInt()` 归一，只允许原样透传。
2. TS 类型中 ID 写 `string`；`route.query` 取出的值本就是 string，不再转数字。
3. `src/api/generated/` 为 orval 产物，`pnpm orval` 全量覆盖：类型不对要改后端注解后重新生成，
   既不手改生成文件，也不在业务层打转换补丁。

### 存量待迁移清单（渐进，不做大爆炸改动）

| VO | 现状 | 影响 |
| --- | --- | --- |
| `ArticleVO.id/userId/categoryId/tagId` | 已标 STRING | 符合规范 |
| `CategoryWebVO.id` | 数字 | 前台 NavBar / 分类页内部自洽，暂无跨源比较；迁移需同步 `pnpm orval` |
| `TagWebVO.id` / `TagWebVO.categoryId` | 数字 | 同上 |
| `CategoryListVO/DetailVO`、`TagListVO/DetailVO`（admin） | 数字 | 后台列表页仅展示，未做严格相等匹配；迁移时一并处理 |
| 前端 `ArticleBase.id/userId` | TS 类型写 `number`，但运行时实为字符串 | 评论/微言子系统全链路按 `source: number` 建模，需整模块迁移；已标存量注释 |
| 前端 `listAdminArticle` 的 `cateId/tagId` | 生成的 `AdminArticlePageReqDTO` 仍为 `number` | 模块层改走手写请求层以字符串透传，避开生成类型（后端 Long 形参可反序列化字符串） |
| 前端 `listPublicTag(categoryId?: number)` 与 `views/front/sort/index.vue` 的 `parseInt` | 分类页把路由参数 `parseInt` 后传给 `/tag/listPublic`，并拿返回的 `TagWebVO.id`（数字）做严格相等比较 | 当前两侧都是数字，**自洽且不产生 Bug**，故本次不动；待 `TagWebVO/CategoryWebVO` 转 STRING 时同批迁移，否则单边改字符串会静默失配 |

迁移原则：按模块一次改完并重新生成前端类型，改完必须回归该模块的下拉/选中态/路由参数比对。
**禁止单边迁移**：同一个比较表达式两边必须同时为字符串或同时为数字（例如把 `TagWebVO.id` 转 STRING 时，
必须同批改掉 `front/sort` 里的 `parseInt(tagId) === tag.id`）。

## 二、下拉选项不走通用字典

`sysdict` 模块（`/sysDict/listByTypeCode`、`/batchList`、`/itemLabel`）服务于**值域封闭的枚举**
（性别、审核状态等），其 `value` 是代码值。分类与标签是有主键、可增删、存在级联关系（`tag.category_id`）
且被业务表引用（`article.category_id`）的**主数据**，塞进字典表会导致数据双写、外键漂移、缓存与权限边界错位。

**结论：主数据的下拉数据源使用各自的 options 端点，不使用 `/sysDict/*`，也不再使用 `dict` 端点。**

### Options 端点

| 端点 | 鉴权 | 口径 |
| --- | --- | --- |
| `GET /category/options` | 匿名 | 仅公开：`status=1` 且 `is_show=1` |
| `GET /tag/options` | 匿名 | 全部未删除标签（`tag` 表无状态列） |
| `GET /admin/category/options?scope=all\|public` | `@LoginCheck(scope=ADMIN)` | 默认 `all`：含停用/隐藏并带状态标记 |
| `GET /admin/tag/options?scope=all\|public` | `@LoginCheck(scope=ADMIN)` | 默认 `all` |

1. 公开端点**不接受**客户端用参数放宽可见性：`/category/options` 无 `scope` 入参，服务调用固定 `PUBLIC`。
2. 后台表单与筛选必须使用 admin options（前端 `authScope: 'admin'`），禁止依赖前台匿名端点，
   避免权限倒挂与 `is_show=0` 数据口径混淆。
3. `/category/dict`、`/tag/dict` 已 `@Deprecated`，仅为外部兼容保留，前端不再调用。

### OptionVO 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | Long → STRING | 真实主键，严禁用 `desc` 文本代替 |
| `label` | String | 显示文案（= `name`） |
| `value` | String | 恒等于 `String(id)`，供只认 value 的选择器组件直接使用 |
| `categoryId` | Long → STRING | 级联关系，标签必填、分类为空 |
| `sort` | Integer | 排序权重，接口已按 `sort, id` 升序返回 |
| `status` | Integer | 仅后台口径返回：0-停用 1-启用 |
| `isShow` | Integer | 仅后台口径返回：0-隐藏 1-展示 |
| `disabled` | Boolean | 仅后台口径返回，由 `status==0` 派生，前端可直接绑 `el-option :disabled` |

禁止出现在选项 VO 中：`desc`、`icon`、`content`、`createTime`、`updateTime` 等展示/管理字段。
`null` 字段不出现在响应里（`@JsonInclude(NON_NULL)`），标签选项天然不带状态噪声。

排序统一：分类与标签的 options 均按 `sort ASC, id ASC`，修复此前分类按 `sort`、标签按 `id` 造成的顺序不一致。

## 三、缓存规范

1. 缓存 key 必须带结构版本号（如 `:v2`）。VO 增删字段属于结构变更，必须升版本，否则旧 JSON 会被
   反序列化为字段全 `null` 的半空对象（实测 `/tag/dict` 的 `cateId` 全为 null 即此成因）。
2. `clearCache()` 必须覆盖该实体全部相关 key。历史缺陷：`TagServiceImpl.clearCache()` 只删
   `CATEGORY_INFO` + `TAG_DICT`，未删按分类分片的 `tag:public:cat_*`，导致后台改标签后前台分类页长期脏读。
   现统一用 `qingSiCache.keys(prefix + "*")` + `del(collection)` 模式清理。
3. options 缓存在旧 key 之上分片为 `public` / `all` 两份，两者口径不同，禁止互相复用。
