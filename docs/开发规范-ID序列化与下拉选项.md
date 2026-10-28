# 开发规范：ID 序列化与下拉选项端点

> 定稿时间：2026-10-26　适用仓库：qs-blog（后端）/ qs-web（前端）
> 背景：后台文章编辑页分类/标签下拉显示 `NaN`，根因是 `dict` 端点用 `cateDesc` 充当选项 value，
> 而前端拿它 `Number()` 当主键。为避免每个新页面重复踩坑，此处一次性定死契约。

## 一、ID 全链路字符串

**决策：所有主键与外键在 JSON 出参中为字符串，前端按字符串消费。**

### 后端

机制是**全局**的：ID 的对外形态只由两处代码决定，字段上不再写任何序列化注解。

1. `common/config/JacksonConfig`：`SimpleModule.addSerializer(Long.class, ToStringSerializer.instance)`
   —— 包装类型 `Long` 出参一律下发字符串。**刻意不注册 `Long.TYPE`**，因此原生 `long`
   （`R.currentTimeMillis`、会话时间戳等）仍是数字。
2. `common/config/LongIdSchemaConverter`（`@Component ModelConverter`，经 `ModelConverterRegistrar` 注册）：
   rawClass 为 `Long` 时把 OpenAPI schema 改写为 `type=string`，并从 `getCtxAnnotations()`（会下钻一层元注解）
   回填 `description/example/format/nullable`；遇显式 `@Schema(type != string)` 或 `hidden()` 则放行给默认链。
   它同时作用于 DTO 字段与 `@RequestParam`/`@PathVariable` 形参（实测 10/10 下发 string）。
3. `common/annotation/@IdNum`：**唯一的豁免通道**，只用于「确属 bigint 但语义不是 ID」的数值（目前仅文件字节数
   `fileSize`）。它 = `@JacksonAnnotationsInside` + 属性级 `@JsonSerialize(using = IdNumLongSerializer.class)`
   + `@Schema(type = "integer", format = "int64")`。

**类型即契约**（不靠注解，因此结构上不存在“漏标”）：

| 语义 | Java 类型 | 出参形态 |
| --- | --- | --- |
| 主键 / 外键 ID | `Long` | string |
| 统计 / 分页 / 权重 / 码值 | `Integer` | number |
| bigint 级非 ID 数值 | `Long` + `@IdNum` | number |
| 时间戳 / 耗等天然大整数 | 原生 `long` | number |

> **实测红线（jackson-databind 2.13.5，不要再往代码里写）**
> `@JsonFormat(shape = NUMBER)` **无法**覆盖类型级注册的 `ToStringSerializer`：组合注解、裸字段、
> 直接注解、getter 四种写法实测全部仍下发字符串；`@JsonFormat(shape = STRING)` 同样**不会**改写 OpenAPI schema。
> 要保住数字只能走属性级 `@JsonSerialize(using = ...)`（属性级显式声明优先于类型级全局注册）。
> 这两条事实由 `IdNumFormatProbeTest` 锁死：若将来升级 Jackson 使 `@JsonFormat` 真能覆盖，该用例会失败并提醒重新评估豁免方案。
> 一切描述注解语义的文字均以实测生效者为准，不以文档/博客说法为准。

1. 非 ID 数值保持数字类型，禁止顺手转字符串：`sort`、`count`、`viewCount`、`total/pageNum/pageSize`、
   各类 `status/type/flag` 码值。MyBatis-Plus 返回的 `long` 分页计数必须 `toInt(...)` 后再放入 `PageResult`。
2. 入参方向不受影响：`Long` 形参 / DTO 字段可同时接收数字与字符串，前端直接传字符串即可。
3. HTTP 形参的 ID 一律 `Long`，不得用 `Integer`/`int` 接 bigint 主键（历史上 `DanmakuController.del` 等 12 处如此，
   已纠正）；否则 springdoc 下发 `int32`，生成层得到 `number`，与全局字符串契约直接冲突，且超 int 范围会溢出。
4. `IdSerializationGuardTest`（`mvn "-DskipTests=false" test`）扇描 198 个参与序列化的类，强制五条规则：
   Long 必须是 ID 语义或 `@IdNum`；`@IdNum` 不得用在 ID 上；ID 不得 `Integer/int`；ID 不得原生 `long`；
   HTTP 形参的 ID 不得是 `Integer`。违反即构建失败，因此不需要“CR 时记得标注解”。
   已知未覆盖：本仓编译未开 `-parameters`，形参名不可反射，只能按注解里的 HTTP 参数名判定；
   服务层形参（如 `subscribe(Long labelId)`）不进入 HTTP 契约，靠约定与评审保障。

### 前端

1. 数据层（`src/api/**`、`src/stores/**`）**禁止**对 ID 做 `Number()` / `parseInt()` 归一，只允许原样透传。
2. TS 类型中 ID 写 `string`；`route.query` 取出的值本就是 string，不再转数字。
3. `src/api/generated/` 与 `src/types/modules/` 为 orval 产物且不入库，`pnpm orval` 全量覆盖：类型不对要改后端
   Java 类型后重新生成，既不手改生成文件，也不在业务层打转换补丁。

### 迁移状态（2026-10-26 已全部完成，不再是渐进清单）

本次把全部 169 个 ID 字段、统计/分页字段、以及 12 处 HTTP 形参一次转到位，前端已重生成并通过
`vue-tsc --noEmit`（0 错误）与 73 个 vitest 用例。原《存量待迁移清单》里的每一项均已闭合：

| 原存量项 | 现状态 |
| --- | --- |
| `CategoryWebVO/TagWebVO.id`、`CategoryListVO/DetailVO`、`TagListVO/DetailVO` 为数字 | 均由全局机制下发 string，生成层同步为 `string` |
| 前端 `ArticleBase.id/userId` TS 写 `number` 但运行时是字符串 | 已改 `string`，评论/微言子系统 `source/userId` 整模块迁移完成 |
| `listAdminArticle` 的 `cateId/tagId` 生成层仍为 `number`（因此绕开 orval） | 生成层已是 `string`，已回到 orval 类型上，不再绕道 |
| `listPublicTag(categoryId?: number)` 与 `front/sort` 的 6 处 `parseInt` | 形参已改 `string`，`parseInt` 全部删除；对应端点实为 `GET /tag/list`（旧文档误写 `/tag/listPublic`） |

迁移原则不变：按模块一次改完并重新生成，改完必须回归该模块的下拉/选中态/路由参数比对；
**禁止单边迁移**——同一个比较表达式两边必须同时为字符串或同时为数字。

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

### 已知口径缺口（2026-10-26 运行态实测发现，尚未修）

1. `disabled` 分支在现有接口下**不可达**：它由 `status == 0` 派生，而 `CategorySaveReqDTO` 没有 `status` 字段
   （只有 `isShow`），新增/更新都改不动它，`pageList` 只能按 status 筛选。实测置灰效果只能直改库造数据。
   要么给 DTO 补 `status` 并开放后台启用/停用入口，要么去掉 `disabled` 语义——二选一，不要停在现在这种
   “字段存在但永远是 false”的状态。
2. `tag.sort` 库列为 `int NULL DEFAULT NULL`，新建标签不写 `sort`：`sort ASC` 下 NULL 排最前，
   且 `@JsonInclude(NON_NULL)` 使该选项不下发 `sort` 键（实测新标签出现在 options 首位）。
   应给列加 `NOT NULL DEFAULT 0` 或在保存时兜底填 0。

## 三、缓存规范

1. 缓存 key 必须带结构版本号（如 `:v2`）。VO 增删字段属于结构变更，必须升版本，否则旧 JSON 会被
   反序列化为字段全 `null` 的半空对象（实测 `/tag/dict` 的 `cateId` 全为 null 即此成因）。
2. `clearCache()` 必须覆盖该实体全部相关 key。历史缺陷：`TagServiceImpl.clearCache()` 只删
   `CATEGORY_INFO` + `TAG_DICT`，未删按分类分片的 `tag:public:cat_*`，导致后台改标签后前台分类页长期脏读。
   现统一用 `qingSiCache.keys(prefix + "*")` + `del(collection)` 模式清理。
3. options 缓存在旧 key 之上分片为 `public` / `all` 两份，两者口径不同，禁止互相复用。

## 附：非 ID `Long` 字段审计（2026-10-26，按用户要求逐字段判定能否换类型）

判定口径：先看值域上限是否可能超 `int`（21.4 亿），再看前端是否参与算术/比较。
迁移后全仓剩下的 `private Long/long` 已无一例是“身份不明的 Long”：

| 字段 | 处置 | 理由 |
| --- | --- | --- |
| `PageResult.total` | 已 `Long` → `Integer` | 分页总数受 `pageSize` 与表行数约束，且前端只用于渲染页数；MP 返回的 `long` 统一 `toInt(...)` 收口 |
| `PortalOverviewVO` 5 个统计字段、`ArticleStatsDTO` 3 个 | 已 `Long` → `Integer` | 文章/分类/标签数不可能过 21 亿，语义就是计数，换 `Integer` 后无需 `@IdNum` |
| `FileBaseVO.fileSize`、`FileInfo.fileSize` | **保留 `Long` + `@IdNum`** | 库列为 `bigint UNSIGNED`，上传大文件可超 int；前端要做字节除法格式化，必须为数字。换 `Integer` 会静默溢出，换 `String` 会打断格式化 |
| `R.currentTimeMillis`、`AuthSessionVO` / `SaTokenSessionAdapter` 的 `issuedAt`/`idleExpiresAt`/`absoluteExpiresAt`/`revision` | 保持原生 `long` | 毫秒时间戳必超 int，无法降级；`JacksonConfig` 刻意不注册 `Long.TYPE`，primitive long 天然下发数字，无需额外保护 |
| `FileInfo.tenantId` | 保持 `Integer` | 库列为 `int UNSIGNED`，多租户开关关闭且值域小；它名称形似 ID 但不是 bigint 主键，故列在守卫的 `INTEGER_ID_ALLOWED` 例外集里 |
| `InfraConst`/`CommonConst` 的 `static final long` | 不变 | 编译期常量，不参与序列化，不出现在 schema |
| `SaTokenSessionAdapter.userId` | 保持 `Long` | 属会话内部元数据，不是 HTTP 出参，不受字符串契约影响 |

另：12 处 `@RequestParam`/`@PathVariable` 的 ID 形参（`Integer`）已全部改 `Long`，
守卫新增规则五锁住；服务层形参（如 `subscribe(Long labelId)`）随之对齐，并顺带修掉
`ArticleServiceImpl` 里 `List<Integer>.contains(Long)` 恒为 false 导致订阅邮件静默不发的真实缺陷。
