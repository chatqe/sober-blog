# 前后端联合审计报告

更新日期：2026-10-24（第四轮：业务契约更正，动态写权限收敛为站长专属）。本文前半部分为当前结论；末尾折叠附录保留旧运行时审计记录，不代表当前版本的运行结果。

## Result

**PARTIAL：Sa-Token 双会话改造已完成源码复审、构建/单测与 fix 分支运行时回归。动态模块的鉴权链路（S1/S2）、回归中暴露的三处存量缺陷（S9–S11）以及第四轮按业务更正落地的站长专属写权限，均已在真实 MySQL/Redis + 浏览器环境中验证通过；A1/A2/B1/B2 等存量项仍未在运行时复核，项目整体不判定为通过。**

- 本轮最高优先级（S1）：动态模块 12 个写接口原为 `value=1` + `scope=ADMIN`，既拒绝普通用户又以 WEB 会话拦截所有人。第四轮按业务方更正后的口径重定契约：**新增/修改/删除仅站长（`userType=0`）**，其余人（含匿名）可读动态、点赞数、评论内容与作者信息，点赞/评论需登录；后台 `/admin/moment/*` 保留给站长(0)与管理员(1)。
- 双会话设计在源码层成立：WEB/ADMIN 各自独立的 `StpLogic`、revision 版本撤销、跨域调用显式拒绝、401/403 以真实 HTTP 状态返回、前端按域存储并按 generation 丢弃过期响应。
- 上轮 P0 的「普通用户可调管理接口」在源码层已收敛：`controller/admin` 下 69 个方法级操作中 61 个 ADMIN、5 个 EITHER、仅 3 个认证入口无注解；全仓 134 处 `@LoginCheck` 为 88 ADMIN / 11 EITHER / 35 默认 WEB。
- 存量未修：family 归属与审核（A1）、公开文章分页可见性（A2）、文章编辑双解包与旧字段（B1/B2）、标签缓存与字典（B4/B5）、友链（C1/D4）、活跃旧路径（D1）、关于页分页（D2）。
- 第二轮只更新本文件、不改业务代码；第三轮（fix 分支运行时回归）在本文件中回填运行时证据，并在 `fix/sa-token-scope-regression` 分支提交了 3 处后端缺陷修复（S9–S11），回归产生的临时账号/动态/行为/评论数据已全部清理（验证 SQL 计数归零）。

### 当前基线与证据口径

| 项目 | 本次实际基线 | 任务开始工作树 |
| --- | --- | --- |
| 后端 `E:\idea\qs-blog` | `main@3bd2670` | 仅本报告已修改，无业务改动 |
| 前端 `E:\web\qs-web` | `main@4f4fed9` | 干净（另有未跟踪 `scripts/auth-browser-harness.cjs`、`temp/`） |

本轮基线前进到两次鉴权专项提交：后端 `3bd2670 feat(auth): 前后台独立双会话改造（Sa-Token + Redis 双域）`（61 文件 +2559/−455）、前端 `4f4fed9 feat(auth): 前后台独立双会话与分域请求鉴权`（69 文件 +1968/−1345）。上一轮基线为后端 `d96e823`、前端 `05697b4`，本文中标注「上轮」的结论均指该基线。旧报告后端 `89e2a1c` 在当前仓库无法解析，且旧基线带有未提交改动，不能用提交差分完整重建当时工作树。

状态定义：
- `FIXED(STATIC)`：当前源码已体现指定修复，不代表本轮重新跑过该业务流程。
- `FIXED(VERIFIED)`：修复已在真实后端 + 数据库 + 浏览器链路上跑通并录得响应。
- `UNVERIFIED`：已列出源码事实和调用链，但缺少当前运行时/数据库证据；下文 P0/P1/P2 均为**建议处置优先级**，不是新增运行时确认数量。
- `FALSE POSITIVE`：原指控被当前证据推翻；`TECH DEBT`：维护风险，不直接等同于业务故障。
- `CONFIRMED(BUILD)`：本轮命令实际观察到的构建结果/警告，不代表相关 UI 故障已复现。
- `CONFIRMED(TEST)`：本轮单元测试覆盖到的行为，不等价于运行时的端到端验收。

范围：单次联合代码复审覆盖旧发现及相关活跃调用链，结合 Git 状态与修复历史；未进行云扫描、全量依赖安全扫描或当前 OpenAPI 双向全量统计。S2–S4 可归因到本轮两次鉴权改造提交，S1 的角色门槛与 S5–S8 为存量/设计风险，其余编号仍为存量；S1/S2/S9–S12 已做真实后端 + 数据库 + 浏览器验收，其余未跑。

## Findings

### Sa-Token 双会话改造：设计落地与技术事实

改造提交：后端 `3bd2670`、前端 `4f4fed9`。本小节先记录设计在源码中的落地点，再列本轮由此链路发现的问题（S1–S8）。

| 设计目标 | 源码落地情况 |
| --- | --- |
| 前后台会话隔离 | [SaTokenSessionAdapter.java:38-48](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/user/auth/SaTokenSessionAdapter.java#L38-L48) 为 WEB/ADMIN 各建一个 `StpLogic`（loginType `web`/`admin`）；[SessionScope.java:7-9](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/user/auth/SessionScope.java#L7-L9) 的 `accepts()` 要求域完全相等，`EITHER` 仅用于明确共享的入口。 |
| 双超时（空闲/绝对） | `timeout=absolute`、`activeTimeout=idle`、`autoRenew=false`；活跃时间由 [RESOLVE 脚本:53-69](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/user/auth/SaTokenSessionAdapter.java#L53-L69) 原子更新，写入用 `SET ... XX KEEPTTL`，只推进 last-active、不延长绝对寿命。 |
| 撤销 | `auth:v2:revision:<userId>` 版本号参与 Lua 校验；改密/重置/冻结/改角色后 [revokeAll:130-131](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/user/auth/SaTokenSessionAdapter.java#L130-L131) 递增版本，旧会话在下一次访问时失效。 |
| 请求级去重 | [AuthSessionService.current:80-98](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/user/auth/AuthSessionService.java#L80-L98) 以 request attribute 缓存 `Resolved` 并按 token 比较，避免同一请求重复校验。 |
| 被动请求不续期 | `X-Session-Activity: passive` 可跳过 touch，前端会话初始化校验使用该头。 |
| 错误语义 | [GlobalExceptionHandler.java:29-32](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/common/exception/handle/GlobalExceptionHandler.java#L29-L32) 将 `AuthAccessException` 转为真实 HTTP 401/403 + `R.fail(status,msg)`。 |
| 前端凭据隔离 | localStorage 只存 64 位 token；[requestPolicy.ts:30-42](file:///E:/web/qs-web/src/auth/requestPolicy.ts#L30-L42) 默认 `anonymous`，强制删除外部 `Authorization` 后按域注入，并写入会话快照。 |
| 过期响应/跨标签页 | `generation` 快照 + `matches()` 丢弃旧会话响应；401/300 只清理发起端；storage 事件按域同步；`pageshow` 强制复核。 |

判定：改造方向正确，上述机制均有对应实现与单测（见 Verification）。但 scope 的取值没有与既有控制器的语义逐个对齐，由此引出 S1；S1 的期望值又在第四轮按业务更正重定。

### 旧报告逐项复核

| 旧编号 | 当前状态 | 当前结论/跟踪项 |
| --- | --- | --- |
| P0-1 管理纵向越权 | `FIXED(STATIC)`（改造后重新收敛） | `@LoginCheck` 改为 Sa-Token 双域后，管理端入口必须持 ADMIN 会话：`controller/admin` 下 69 个方法级操作中 61 个 `ADMIN`、5 个 `EITHER`、3 个认证入口（login/session/logout）无注解；全仓 134 处注解为 88 ADMIN / 11 EITHER / 35 默认 WEB。family 的 C 端复用设计见 A1，不能沿用“25 个端点都仍可越权”。 |
| P0-2 分类私密摘要泄露 | 指定路径 `FIXED(STATIC)` | `/article/articleCategoryList` 已加公开过滤、空正文保护、v2 缓存键；另一公开分页路径见 A2。 |
| P0-3 邮箱注册双阻断 | `FIXED(STATIC)` | 注册读/删 REGISTER=1 验证码；两个可见入口发送 `email+bizType`。未重新发码/注册。 |
| P1-1 CORS | 配置保留，利用链 `UNVERIFIED` | 通配源与允许凭据仍在；不能推导为第三方页面自动获得 localStorage token，见 D3。 |
| P1-2 编辑与上传 | 部分改善，`UNVERIFIED` | B3 的 `$common.saveResource` 缺失与上传路由已迁移（`UploadPicture.vue` 改调 `uploadApi.uploadMedia` 并按快照回填）；B1 双解包、B2 旧字段仍在。 |
| P1-3 友链 SQL | 映射未修，当前 SQL `UNVERIFIED` | `desc` 仍未显式转义；页面另有分组对象误判，见 C1、D4。 |
| P1-4 20 条契约错误 | 部分迁移，旧数量作废 | 必须区分活跃调用、生成残留和无引用封装，见 D1。 |
| P1-5 点赞 | 部分原解释错误，`UNVERIFIED` | 旧入口与新行为模型仍混用；`checkHasLike` 已查询行为状态，不再写“恒 false”，见 B6。 |
| P1-6 关于页分页 | 源码错配保留，`UNVERIFIED` | `pageNum/pageSize` 与 MP Page 的 `current/size` 不同，见 D2。 |
| P1-7 异常契约 | 部分改善 | `AuthAccessException` 现以真实 HTTP 401/403 + `R.fail` 返回，校验异常已 `setStatus(400)`；其余 `Exception` 兜底仍为 HTTP 200，404/405 未统一，见 D5。 |
| P2-1 旧找回密码 | 部分死路径 | `getCode1` 无模板引用；可见面板走新接口，成功反馈另有 C3。旧提交分支并未全部证明不可达。 |
| P2-2 非法 bizType | 原影响需纠正 | 未知值仍映射为 0，但新重置接口也使用客户端 bizType 计算键，不能称“没有任何读取方”。 |
| P2-3 密码提示拼接 | 不单列安全缺陷 | 已有空值回退；仅拼接提示文本不足以认定安全漏洞。 |
| P2-4 重复常量/AES | `TECH DEBT` | 枚举重复及前端静态密钥不能替代服务端安全；本报告不记录密钥值。 |
| P2-5 分类摘要 NPE/TTL | `FIXED(STATIC)` | 原分类聚合路径已保护空正文并设置 TTL，不能用另一函数推翻该修复。 |
| P2-6 上传声明签名 | 并入 B3 | 真正阻断是注入对象缺少实际方法，不重复计数。 |
| P2-7 delete/patch wrapper | 部分修复 | `delete` 已在 [request.ts:40-44](file:///E:/web/qs-web/src/utils/request.ts#L40-L44) 补齐，`patch` 仍缺；裸 Axios 的 C2 已随之关闭。 |
| 后续：标签云数据错位 | `FIXED(STATIC)` | `MyAside → listTagCloud → GET /tag/cloud`，返回未删除标签名称；不再调用分类字典构造标签云。 |
| 后续：标签缓存/字典 ID | 源码问题保留，`UNVERIFIED` | 分别见 B4、B5，与新标签云接口区分。 |

本轮状态变化汇总：C2（动态删除/取消点赞裸 Axios）与 D1 的「后台用户状态路径」已随前端改造关闭；P2-7 的 delete 已补齐；B3 部分关闭；其余编号结论不变。

### 管理端鉴权量化（Sa-Token 改造后）

口径：按 `controller/admin` 内已核验的方法级 HTTP 映射统计（含 `PatchMapping` 与无括号 Mapping，不含类级路径）；scope 取自 `@LoginCheck` 的 `scope` 属性，缺省为 `WEB`。

| 目录 | ADMIN | EITHER | 无注解 | 合计 |
| --- | ---: | ---: | ---: | ---: |
| `controller/admin` | 61 | 5 | 3 | 69 |

- 3 个无注解入口均为认证入口：`/admin/user/login`、`/admin/user/session`、`/admin/user/logout`，它们在方法体内部自行解析会话，不是漏鉴权。
- 5 个 `EITHER` 全部是 family 管理控制器：C 端用户也要能维护自己的卡片，因此不能计为弱鉴权；真正的缺口是对象授权，见 A1。
- 全仓 `@LoginCheck` 共 134 处：88 `ADMIN`、11 `EITHER`（family 5 + `UserController` 5 + Qiniu 1）、35 使用默认参数（`value=2, scope=WEB`）。
- 旧表 L0/L1/L2 口径建立在上代 token 与 `value` 语义上，Sa-Token 改造后已不适用，不能与新表混算；旧报告 62/26 的扫描遗漏 Patch 与无括号 Mapping 的问题同样作废。
- 本表不覆盖其他目录中带管理语义的接口；数值由本轮源码扫描得出，未经运行时 OpenAPI 复核。

### S1｜动态写接口的角色/域门槛与业务契约错配｜`FIXED(VERIFIED)`

- **Evidence**：[MomentController.java](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/moment/controller/web/MomentController.java) 的 publish/update/delete/sync 原为 `@LoginCheck(value = 1, scope = SessionScope.ADMIN)`；[LoginCheckAspect.java:23-31](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/aop/LoginCheckAspect.java#L23-L31) 以 `userType > value` 拒绝；`UserTypeEnum` 为 ADMIN(0)/DEV(1)/USER(2)；前端 [weiYan.ts](file:///E:/web/qs-web/src/api/modules/weiYan.ts) 以 `authScope: 'web'` 调用。
- **Current Fact（源码，改造前）**：存在**两道**拦截。① `value = 1` 拒绝 userType=2 的普通用户；② `scope = ADMIN` 拒绝 WEB 会话。`d96e823` 时这些方法已是 `@LoginCheck(1)`，①属存量契约错配，②为 `3bd2670` 新增限制。
- **Expected（2026-10-24 业务更正）**：动态的新增/修改/删除**仅站长**（`user.userType = 0`）；其余人不论登录与否均可查看动态、点赞数与评论内容，`/moment` 首行头像与作者名公开；只有点赞/评论需要登录，未登录直接弹登录/注册容器；后台动态管理 `/admin/moment/*` 允许站长(0)与管理员(1)。
- **Root Cause**：`@LoginCheck` 的 `value`（角色上限）被当成“仅需登录”误用为 1；第三轮曾按“C 端所有登录用户可写”放开为 `value` 默认 2，与业务口径不符。
- **Impact**：原状态下普通用户在动态页的写操作全部 403；误放开后普通用户可发布动态，超出站长专属写权限。
- **本轮处置（第四轮）**：publish/update/delete/sync 统一为 `@LoginCheck(value = 0, scope = SessionScope.EITHER)`；`/{id}/view` 去掉注解改为公开幂等；`/my/timeline`、like/unlike、comment 及其点赞仍为 `scope = WEB`；后台 `/admin/moment/*` 保持 `value = 1, scope = ADMIN`，并在 save 缺省作者为当前后台账号。
- **运行时验证（2026-10-24，42 项断言全过）**：匿名 publish/update/delete/like/comment 均 401，`/moment/{id}/view` 200；普通用户(userType=2) publish/update/delete/sync 均 403「权限不足」，like/unlike/comment/deleteComment 200；站长(userType=0) publish→update→匿名详情读到新内容→delete 全 200；管理员(userType=1) WEB 会话 publish 403、ADMIN 会话 `/admin/moment/page|save|update|delete` 全 200；WEB token 与 ADMIN token 交叉调用分别被 403 拒绝。浏览器侧：匿名与普通用户无发布 FAB/删除按钮/「我的动态」Tab，管理员前台同样无入口，站长三者齐全并可发布/删除。

### S2｜公开时间线被前端声明为匿名，登录用户丢失 `liked`｜`FIXED(VERIFIED)`

- **Evidence**：[MomentController.java:59-64](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/moment/controller/web/MomentController.java#L59-L64) 用 `BlogUtil.getUserId()` 计算用户视角；[weiYan.ts:92-95](file:///E:/web/qs-web/src/api/modules/weiYan.ts#L92-L95) 声明 `authScope: 'anonymous'`。
- **Current Fact（源码）**：`installSessionPolicy` 对匿名请求强制删除 `Authorization`，因此 `getUserId()` 恒为 null，`liked` 恒 false，可见性也按匿名视角裁剪。
- **Expected**：已登录用户看到自己的点赞态；未登录用户仍能匿名浏览。
- **Root Cause**：把「接口可匿名」误当成「请求必须匿名」。`authScope` 表达的是「该请求需要哪一域的凭据」，而不是「禁止携带凭据」。
- **Impact**：修复 S1 后紧接着出现的交互错误：赞过的动态仍显示未赞，重复点赞；`liked` 字段实际不可用。
- **最小修复**：改为 `authScope: 'web'`。未登录时快照无 token，拦截器不会注入 Authorization，实际仍是匿名请求；该接口本身不鉴权，也不会触发 401 清理分支。
- **本轮处置**：已在 `fix/sa-token-scope-regression` 分支修改 `getTimeline` 的 `authScope`，前端 `type-check` 与 43 个单测通过。
- **运行时验证（2026-10-24）**：浏览器中匿名加载 `/moment` 时 `POST /moment/timeline` 200 且不带 `Authorization`；登录后同一请求 200 且带裸 token（`Authorization: <token>`，无 Bearer，存储键 `localStorage["authorization:web:v2"]`，无 Cookie）。登录用户点赞后刷新页面，卡片仍为「已赞」且 `👍 1`；取消后刷新恢复为「赞」；匿名访问同一动态显示「赞」（不泄露 liked）。

### S3｜建议 P1：上传过滤器先解析会话再判空，匿名请求直接 401｜源码确认

- **Evidence**：[FileFilter.java:45-47](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/infra/storage/FileFilter.java#L45-L47)：`sessions.resolve(token, SessionScope.ADMIN, false)` 在第 46 行，`StringUtils.hasText(token)` 在第 47 行。
- **Current Fact（源码）**：`resolve(null, ...)` 必抛 `AuthAccessException(401)`，第 48 行起的「按用户/IP 计数限流」永远不会执行；`return true/false` 的「未登录交回注解链路」原设计也失效。
- **Expected**：先判断 token 是否存在；不存在时交回注解链路，存在时再解析并计入限流。
- **Root Cause**：改造时直接在原 `hasText` 分支前插入会话解析，没有调整语句顺序。
- **Impact**：`/file/admin/upload` 的匿名/过期访问从统一鉴权响应变成过滤器内抛异常；限流对所有请求都不再生效。另：该计数器仍沿用旧缺陷——`AtomicInteger` 反序列化后 `getAndIncrement` 不回写 Redis，实际永远达不到阈值（同 C4 类型）。
- **最小修复**：`hasText(token)` 前置，`identity` 及其后逻辑一起下移；计数改为 `Redis INCR + EXPIRE`。

### S4｜建议 P1：`POST /user/token` 语义被替换但契约未标注失效｜源码确认

- **Evidence**：[UserController.java:112-118](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/user/controller/web/UserController.java#L112-L118) 仅加 `@Deprecated`；[UserServiceImpl.java:515-519](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/user/service/UserServiceImpl.java#L515-L519) 返回 `AuthSessionVO.from(..., null)`。
- **Current Fact（源码）**：入参只接受 64 位新格式 token，出参 `accessToken` 恒为 null。
- **Expected**：要么按旧契约返回可用凭据，要么以明确的错误码宣告该入口下线。
- **Root Cause**：会话存储从「自定义 token→用户映射」迁到 Sa-Token 后，旧接口被就地改写而非删除。
- **Impact**：任何按旧契约调用的外部/App 客户端会得到 401；若前端把它接到 `authStore.accept()`，会因 `accessToken` 未通过 64 位正则而抛「登录响应无效」。当前前端已无活跃调用，风险限于外部调用方。
- **最小修复**：返回同一会话的新 token（续签语义），或返回 410 并在接口文档标注移除版本。

### S5｜建议 P2：两域共用 `tokenName`，Sa-Token 内部会话键跨域共享｜源码确认，运行时未验证

- **Evidence**：[SaTokenSessionAdapter.java:40-46](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/user/auth/SaTokenSessionAdapter.java#L40-L46)：两个 `StpLogic` 均 `setTokenName("auth:v2")`。
- **Current Fact（源码）**：同一账号同时持有 WEB 与 ADMIN 会话时，Sa-Token 的 token 值键、`login:session:<loginId>` 等簿记键在两域之间使用相同命名空间，`isConcurrent(true)` 下的多端登录列表也存在混装可能。
- **Expected**：两域在存储层完全隔离，登出/踢出/在线列表互不影响。
- **Root Cause**：只按 loginType 区分了 `StpLogic`，没有同时区分 `tokenName`。
- **Impact**：无法从源码断言会造成越权（业务校验仍走 revision 与 scope），但登出、踢出、在线设备列表等依赖框架内部键的能力可能互相干扰；需在隔离 Redis 中核对真实键名后再定性。
- **最小修复**：改为 `auth:web:v2` / `auth:admin:v2`；若确认共用是有意设计，则补一条契约测试并在注释中写明理由。

### S6｜建议 P2：撤销只递增版本，原生会话记录驻留到绝对过期｜源码确认

- **Evidence**：[SaTokenSessionAdapter.java:126-131](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/user/auth/SaTokenSessionAdapter.java#L126-L131)、[:72-75](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/user/auth/SaTokenSessionAdapter.java#L72-L75)。
- **Current Fact（源码）**：`revokeAll` 仅 `INCR auth:v2:revision:<userId>`，不枚举、不删除该用户的 token 映射与 Token-Session；被撤销的会话在下一次访问时因版本不匹配才失效。`revision` 键本身无 TTL。
- **Expected**：撤销后资源应尽快回收，且撤销状态本身不应永久增长。
- **Root Cause**：「版本失效」被当作完整的撤销机制，没有配套的存储清理。
- **Impact**：WEB 绝对 TTL 为 30 天时，被撤销 token 的记录最多再驻留 30 天；revision 键随改密/冻结次数单调增长（单键单值，量级有限但无清理策略）。安全上不构成越权，属存储与可观测性风险。
- **最小修复**：撤销时按 `loginId` 枚举并删除原生记录（注意时间窗口，避免误删撤销后新登录的会话），或给 revision 键设置与最长绝对 TTL 对齐的过期时间。

### S7｜建议 P2：`getAdminUser()` 启动缓存依赖仍在关键写链路｜源码确认

- **Evidence**：[UserServiceImpl.java:210](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/user/service/UserServiceImpl.java#L210) 与 [:216](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/user/service/UserServiceImpl.java#L216)（注册写 IM 双向好友）、[FileFilter.java:53](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/infra/storage/FileFilter.java#L53)、`SaveCheckAspect`、`AdminFamilyController` 仍调用 `BlogUtil.getAdminUser()`。
- **Current Fact（源码）**：该方法读取启动时写入的 `CommonConst.ADMIN` 缓存；未命中时 `JSON.parseObject(null, User.class)` 返回 null，随后 `.getId()` 触发 NPE。
- **Expected**：身份应从数据库或当前会话解析，而不是依赖进程启动期的缓存快照。
- **Root Cause**：改造把「当前登录用户」迁到了 `AuthSessionService`，但「站长」这一特殊身份没有一起迁移。
- **Impact**：缓存缺失/被清空时，注册会在写 IM 好友这一步整体失败（事务回滚），文件上传与 family 状态接口同样受影响。
- **最小修复**：改为按 `userType=0` 查库并做空值兜底，或把站长 ID 定义为配置项。

### S8｜建议 P2：`auth.session` 未外部化配置｜源码确认

- **Evidence**：`AuthSessionProperties` 默认 WEB idle 7d / absolute 30d、ADMIN idle 2h / absolute 12h；`application.yml`、`application-dev.yml`、`application-prod.yml` 中均检索不到 `auth.session`。
- **Current Fact**：四个超时值只能在代码里改；`@PostConstruct validate()` 只校验正整数与 `idle <= absolute`。
- **Impact**：不同环境无法通过配置调整后台空闲时长；压测与灰度需要改代码重新打包。
- **最小修复**：在 `application.yml` 显式写出默认值，便于运维覆盖与配置审计。

### S9｜建议 P1：动态点赞返回 500「评论不存在」｜`FIXED(VERIFIED)`（回归新发现）

- **Evidence**：[CommentBehHandler.java](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/behavior/handler/CommentBehHandler.java) 原将 `SET` 声明为 `CommentTypeEnum.ARTICLE(1)`、`MESSAGE(2)`、`LOVE(3)` 与 `BizTypeEnum.COMMENT(3)` 的并集；[BehHandlerFactory.java](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/behavior/handler/BehHandlerFactory.java) 的 `resolveHandler` 遍历 `handlerMap.values()` 取首个 `supports()` 命中者，`commentBehHandler` 的 bean 名先于 `momentBehHandler`。
- **Current Fact（运行时）**：普通用户给动态 `POST /moment/{id}/like` 返回 HTTP 200 但 body `code:500, msg:"评论不存在"`；`behavior_state` 不落库，点赞态不生效。
- **Root Cause**：`CommentTypeEnum` 与 `BizTypeEnum` 编码碰撞（1=文章、2=动态、3=评论），评论处理器抢占了文章/动态的 bizType，用 `comment` 表校验动态 id。
- **最小修复**：`SUPPORTED_TYPES` 收窄为 `Set.of(BizTypeEnum.COMMENT.getCode())`，文章/动态分别由 `ArticleBehHandler`/`MomentBehHandler` 负责；同时删除不再使用的 import。
- **运行时验证（2026-10-24）**：修复后点赞 200、`liked=true`、`likesCount=1`，匿名视角 `liked=false`；浏览器点赞按钮变「已赞」并显示 `👍 1`，刷新保持。

### S10｜建议 P1：动态评论恒 400，bizType/bizId 必填校验先行｜`FIXED(VERIFIED)`（回归新发现）

- **Evidence**：[MomentController.java](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/moment/controller/web/MomentController.java) 的 `/{id}/comment` 原为 `@Validated @RequestBody CommentVO`，`setBizId/setBizType` 在方法体内、校验之后。
- **Current Fact（运行时）**：`POST /moment/{id}/comment` 恒返回 400 `{"bizType":"业务类型不能为空","bizId":"业务主键ID不能为空"}`，前端提示评论失败。
- **Root Cause**：`@Validated` 在方法入口执行，早于服务端根据路径补全 bizId/bizType，导致 `CommentVO` 的必填注解永远不满足。
- **最小修复**：去掉 `@Validated`，改在方法体内用 `StringUtils.hasText` 校验内容非空，再补全 bizId/bizType；用 `BizException` 保持统一错误回包。
- **运行时验证（2026-10-24）**：评论 200、评论列表 1 条、卡片 `💬 1`；浏览器发表评论后列表立即出现，刷新后评论仍存在。

### S11｜建议 P1：取消点赞生成非法 SQL，返回 500｜`FIXED(VERIFIED)`（回归新发现）

- **Evidence**：[MomentBehHandler.java](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/behavior/handler/MomentBehHandler.java) 的 `decrementMomentCount` 原为 `.setSql("GREATEST(" + column + " - 1, 0)")`。
- **Current Fact（运行时）**：`DELETE /moment/{id}/like` 返回 500「服务异常」，后端栈为 `SQLSyntaxErrorException ... near '(likes_count - 1, 0) WHERE is_del=0 AND (id = ...)'`（SQL 无赋值目标）。
- **Root Cause**：MyBatis-Plus `setSql` 拼接的是完整 SET 子句片段，必须自带 `column =`，不能只写表达式。
- **最小修复**：`"GREATEST(...)"` → `column + " = GREATEST(...)"`。
- **运行时验证（2026-10-24）**：`unlike-probe` 六步（发布→点赞→取消→再点赞→再取消→删除）全 200；全量回归中取消点赞后 `liked=false`、`likesCount` 回到 0。

### S12｜建议 P2：后台动态列表的 `authorName/authorAvatar` 从未回填｜`FIXED(VERIFIED)`（第四轮新发现）

- **Evidence**：[MomentServiceImpl.java](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/moment/service/MomentServiceImpl.java) 的 `pageList` 直接用 `this::toListVO` 映射，而 `toListVO` 只拷实体字段，没有查 `user`；同一文件的 `toWebListVO` 却已通过 `buildUserMap` 回填作者。
- **Current Fact（运行时）**：`POST /admin/moment/page` 返回的 `authorName` 恒为 `null`，新建后台动态管理页的「作者」列为空。
- **Root Cause**：管理端列表 VO 与 C 端列表 VO 走了两套映射，作者信息只在 C 端那套补齐。
- **最小修复**：`pageList` 先按页内 `userId` 集合调 `buildUserMap`，再交给 `toListVO(entity, userMap)` 回填 `authorName/authorAvatar`。
- **运行时验证（2026-10-24）**：`POST /admin/moment/page` 返回 `author=qs_admin_probe` 等真实用户名；后台页面表格作者列正常显示 `Joy`/`regularuser`/`qs_admin_probe`。

### A1｜建议 P0：family 缺少对象归属与审核权限控制｜UNVERIFIED

- **Evidence**：[AdminFamilyController.java:66-99](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/family/controller/admin/AdminFamilyController.java#L66-L99)、[FamilyServiceImpl.java:40-93](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/family/service/FamilyServiceImpl.java#L40-L93)、[family.ts:91-151](file:///E:/web/qs-web/src/api/modules/family.ts#L91-L151)。
- **Current Fact（源码，2026-10-24 复核）**：5 个操作均为 `@LoginCheck(scope = SessionScope.EITHER)`，即 WEB 或 ADMIN 任一域登录即可调用；删除和切换状态按客户端 id 操作。保存覆盖 userId 为当前用户，但保留传入 id 并调用通用 `saveOrUpdate`。分页/管理详情未限制当前用户，前端才自行匹配 userId。Sa-Token 改造没有改变这一语义，只是把「仅要求登录」显式化为 EITHER。
- **Expected**：普通用户仅维护自己的卡片；审核状态仅由有审核权限的角色修改；新建不得利用客户端 id 更新他人记录。
- **Reproduction（待执行）**：隔离环境中由账号 A 建卡，账号 B 请求其详情、带该 id 保存/删除/切换状态；核对拒绝结果与记录未改变。
- **Root Cause**：登录校验没有补充对象授权，保存也绕过了专用服务中的每用户一条及缓存处理。
- **Impact**：他人卡片被修改/删除、归属被替换或审核状态被绕过的风险；未实际执行跨账号写入。
- **最小修复/验收**：服务层限定 `id + 当前用户ID`，明确管理例外；审核单独鉴权；保存统一走授权后的专用方法并失效缓存。不能把全部 family 操作直接收紧为 ADMIN 而破坏 C 端。

### A2｜建议 P0：公开文章分页仍缺可见性过滤｜UNVERIFIED

- **Evidence**：[ArticleController.java:139-143](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/article/controller/web/ArticleController.java#L139-L143)、[ArticleServiceImpl.java:595-618](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/article/service/ArticleServiceImpl.java#L595-L618)、[article.ts:42-49](file:///E:/web/qs-web/src/api/modules/article.ts#L42-L49)、[sort/index.vue:156-162](file:///E:/web/qs-web/src/views/front/sort/index.vue#L156-L162)、[MyAside.vue:281-285](file:///E:/web/qs-web/src/components/business/friend/MyAside.vue#L281-L285)。
- **Current Fact（源码）**：匿名 POST `/article/listPage` 的查询没有固定 `viewStatus=true`，随后构造正文摘要；分类页和侧栏推荐实际调用它。清掉 password 不等于清掉已返回的内容。
- **Expected**：公开分页和推荐列表仅包含可公开文章，管理查询另行授权。
- **Reproduction（待执行）**：隔离库准备符合分类/推荐筛选条件的私密文章，匿名分页，断言其 ID、标题、摘要不出现在响应中。
- **Root Cause**：原修复覆盖分类聚合，但未覆盖另一公开分页查询的服务端可见性谓词。
- **Impact**：符合筛选条件的私密文章摘要仍可能经公开页面返回；本轮未抽查真实数据量。
- **最小修复/验收**：公开查询固定添加 `.eq(Article::getViewStatus, true)`，不由客户端决定；同时回归分类聚合和详情密码检查。

### B1｜建议 P1：文章回显仍双重解包｜UNVERIFIED

- **Evidence**：[article.ts:31-34](file:///E:/web/qs-web/src/api/modules/article.ts#L31-L34)、[Edit.vue:218-224](file:///E:/web/qs-web/src/views/admin/article/Edit.vue#L218-L224)。
- **Current Fact（源码，2026-10-24 复核）**：`articleApi.getArticleById` 已返回业务对象，`Edit.vue` 仍写 `if (res.data)` 并 `Object.assign(article, res.data)`，即多解一层；上传侧的同类问题已在 B3 修复。
- **Expected**：组件消费模块声明的实际返回值，成功响应后填充表单或 URL。
- **Reproduction（待执行）**：返回合法文章对象，打开编辑页，检查表单是否回填。
- **Root Cause**：未统一“HTTP 响应、R 信封、业务数据”三层边界。
- **Impact**：成功响应不能正确回填；编译通过不能覆盖此数据形状问题。
- **最小修复**：直接使用模块返回值，并以真实响应契约补消费层测试；不要盲目给所有接口统一增删 `.data`。

### B2｜建议 P1：文章保存字段仍用旧分类/标签命名｜UNVERIFIED

- **Evidence**：[Edit.vue:123-134](file:///E:/web/qs-web/src/views/admin/article/Edit.vue#L123-L134)、[Edit.vue:319-350](file:///E:/web/qs-web/src/views/admin/article/Edit.vue#L319-L350)、[article.ts:92-104](file:///E:/web/qs-web/src/api/modules/article.ts#L92-L104)、[ArticleVO.java:109-121](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/article/controller/web/vo/ArticleVO.java#L109-L121)。
- **Current Fact（源码）**：表单使用 `sortId/labelId`，保存直接发送对象；后端校验必填的是 `categoryId/tagId`。选项名称也需与适配器输出的 `name` 对齐。
- **Expected**：回显、选择器、保存 DTO 的字段和类型一致。
- **Reproduction（待执行）**：新建文章选好分类/标签后提交，检查请求载荷及后端校验；编辑已存文章再次提交。
- **Root Cause**：查询适配已局部迁移，保存边界仍残留旧字段。
- **Impact**：修复 B1 后仍可能无法完成创建/编辑，不应把回显修复视为整个编辑链路闭环。
- **最小修复**：统一表单为 `categoryId/tagId`，或显式双向转换；与 B5 的字典 ID 一并回归。

### B3｜上传回调和资源 API 迁移未完成｜部分 `FIXED(STATIC)`

- **Evidence**：[UploadPicture.vue:116-124](file:///E:/web/qs-web/src/components/media/UploadPicture.vue#L116-L124)、[upload.ts](file:///E:/web/qs-web/src/api/modules/upload.ts)、[resource.ts:18-64](file:///E:/web/qs-web/src/api/modules/resource.ts#L18-L64)、[FileAdminController.java:43-68](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/file/controller/admin/FileAdminController.java#L43-L68)。
- **Current Fact（源码，2026-10-24 复核）**：`UploadPicture.vue` 已改调 `uploadApi.uploadMedia(...)`，`handleSuccess` 校验会话快照后 `emit('addPicture', response.url)`；原先不存在的 `$common.saveResource` 调用已移除。剩余问题转移到 D1：`resource.ts` 仍保留 `/file/list`、`/file/save`、`/file/update` 与 `changeResourceStatus` 空实现。
- **Expected**：上传、资源登记、URL 回填依次成功，调用角色满足权限要求。
- **Reproduction（待执行）**：分别走本地/七牛上传，捕获实际路由、资源登记请求、异常及回填事件；使用隔离存储并回收测试文件。
- **Root Cause**：工具导出、文件模型和路由演进后消费者未整体迁移。
- **Impact**：上传回填链路已恢复；后台资源管理页的列表/保存/状态仍可能失败或假成功，见 D1。
- **最小修复**：按 D1 逐条迁移资源管理入口；C 端权限需单独设计，不能把普通用户上传一律迁往 ADMIN 接口。

### B4｜建议 P1：分类标签列表永久缓存未失效｜UNVERIFIED

- **Evidence**：[TagServiceImpl.java:116-153](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/tag/service/TagServiceImpl.java#L116-L153)、[QingSiCache.java:111-117](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/utils/cache/QingSiCache.java#L111-L117)、[sort/index.vue:125-138](file:///E:/web/qs-web/src/views/front/sort/index.vue#L125-L138)。
- **Current Fact（源码）**：`tag:public:cat_*` 无 TTL 写入，增删改仅删除其他缓存键。
- **Expected**：标签改名、移分类、软删除后列表及时反映变化。
- **Reproduction（待执行）**：先填充标签列表缓存，再改名/移分类/删除，复查新旧分类及不带分类条件的响应。
- **Root Cause**：缓存读写与失效键集合不一致。
- **Impact**：分类页长期展示旧标签甚至已删除标签；重启应用本身不能保证清掉 Redis 旧键。新 `/tag/cloud` 不使用该缓存，不受此问题影响。
- **最小修复**：提交成功后失效相关列表键，设置 TTL 兜底，并制定已有永久键的迁移/清理方案。

### B5｜建议 P1：标签字典将描述当作 ID｜UNVERIFIED

- **Evidence**：[TagServiceImpl.java:162-168](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/tag/service/TagServiceImpl.java#L162-L168)、[category.ts:28-42](file:///E:/web/qs-web/src/api/modules/category.ts#L28-L42)、[Edit.vue:240-246](file:///E:/web/qs-web/src/views/admin/article/Edit.vue#L240-L246)。
- **Current Fact（源码）**：后端 `value=tagDesc`，前端 `id=Number(value)`；描述不等于标签主键。
- **Expected**：字典 value 是真实标签 ID，label 是名称。
- **Reproduction（待执行）**：使用非数字描述的标签，查看字典、选择器模型和提交载荷，确认 ID 为真实主键。
- **Root Cause**：字典 value 的前后端语义不同。
- **Impact**：数字描述会被误当主键，非数字描述会转为 `NaN`；未查询当前库中实际描述值。
- **最小修复**：返回 `String.valueOf(t.getId())` 并失效旧字典缓存；不改变标签云纯名称接口。

### B6｜建议 P1：旧文章点赞入口与行为模型混用｜UNVERIFIED

- **Evidence**：[ArticleController.java:204-210](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/article/controller/web/ArticleController.java#L204-L210)、[ArticleServiceImpl.java:415-479](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/article/service/ArticleServiceImpl.java#L415-L479)、[article/index.vue:753-765](file:///E:/web/qs-web/src/views/front/article/index.vue#L753-L765)、[AbstractBehService.java:54-89](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/behavior/service/AbstractBehService.java#L54-L89)。
- **Current Fact（源码）**：旧入口无登录检查，接受客户端 userId；前端固定 `operation:0` 却提示点赞成功。旧服务另行改文章计数，状态查询已使用行为表。
- **Expected**：身份由服务端取得，点赞/取消的状态和计数使用一致模型，并保持幂等。
- **Reproduction（待执行）**：隔离文章分别测试匿名、伪造 userId、重复点赞、取消及并发，核对行为状态与计数一致。
- **Root Cause**：旧计数入口没有完整迁移到统一行为服务。
- **Impact**：身份、反馈与计数不一致的风险；本轮未刷赞或验证并发计数。
- **最小修复**：按钮接统一行为 API（文章类型 `ARTICLE=1`），旧入口删除或受控兼容。统一行为层已有幂等检查，不能继续写“所有点赞都无幂等/hasLike 恒 false”。

### C1｜建议 P1：友链分组对象被当成数组判空｜UNVERIFIED

- **Evidence**：[friend/index.vue:201-206](file:///E:/web/qs-web/src/views/front/friend/index.vue#L201-L206)、[WeblinksController.java:58-60](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/weblinks/controller/web/WeblinksController.java#L58-L60)；实际调用链是 `web.ts → generated/weblinks → mutator`。
- **Current Fact（源码）**：返回值是 `Map<string, WeblinksVO[]>`，页面却 `if (!res.length) return`。
- **Expected**：按分组对象读取和渲染友链。
- **Reproduction（待执行）**：用非空分组响应加载友链页，确认各组进入页面状态。
- **Root Cause**：对象响应仍沿用数组判空逻辑。
- **Impact**：即使修复 SQL 查询，非空正常响应仍可能不回填；这是独立于 D4 的第二个阻断。
- **最小修复**：按对象消费（例如 `friendList.value = res ?? {}`），同时覆盖空分组响应。

### C2｜动态删除/取消点赞绕过统一请求客户端｜`FIXED(STATIC)`（本轮关闭）

- **Evidence**：[weiYan.ts:109-122](file:///E:/web/qs-web/src/api/modules/weiYan.ts#L109-L122)、[requestPolicy.ts:29-53](file:///E:/web/qs-web/src/auth/requestPolicy.ts#L29-L53)。
- **Current Fact（源码，2026-10-24 复核）**：`deleteMoment` 与 `unlikeMoment` 已改为 `request.delete(..., { authScope: 'web' })`，裸 `axios.delete` 已不存在；业务码校验与会话快照由统一拦截器完成。本节保留为历史记录。
- **Expected**：带有效身份请求，仅在业务成功后移除卡片/更新点赞状态。
- **Reproduction（待执行）**：分别返回有效成功及 HTTP 200/业务失败信封，验证请求头和页面行为。
- **Root Cause（历史）**：裸 Axios 绕过认证及业务错误拒绝逻辑。
- **Impact**：原假成功风险已消除；这两个调用目前仍会返回 403，原因见 S1，与本条无关。
- **最小修复**：无需再改客户端；随 S1 修复后一并回归。

### C3｜建议 P2：找回密码成功后缺少成功反馈｜UNVERIFIED

- **Evidence**：[user.ts:40-43](file:///E:/web/qs-web/src/api/modules/user.ts#L40-L43)、[user/index.vue:951-960](file:///E:/web/qs-web/src/views/front/user/index.vue#L951-L960)、[AuthModal.vue:348-357](file:///E:/web/qs-web/src/components/business/auth/AuthModal.vue#L348-L357)。
- **Current Fact（源码）**：模块返回 `Promise<void>`，两个消费者却检查返回值非空才提示成功并关闭面板。
- **Expected**：请求正常完成即可提示成功，业务失败由统一错误链处理。
- **Reproduction（待执行）**：隔离账号成功重置密码，检查提示与面板关闭；失败时不得关闭或提示成功。
- **Root Cause**：无返回值方法被按有业务对象的方法消费。
- **Impact**：密码可能已修改，但 UI 无成功反馈并诱发重复提交；不能写成“后端密码未修改”。
- **最小修复**：await 成功后直接反馈，不用返回对象判空；不实际修改任何用户密码来补本轮证据。

### C4｜建议 P1：邮件限流计数只在本地对象自增｜UNVERIFIED

- **Evidence**：[UserServiceImpl.java:726-739](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/user/service/UserServiceImpl.java#L726-L739)、[CommonConst.java:119-121](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/common/constants/CommonConst.java#L119-L121)。
- **Current Fact（源码）**：首次将计数 1 写 Redis，后续反序列化本地 `AtomicInteger` 自增，却未写回；限制值为 3。
- **Expected**：多次请求共享并原子累积配额，到达阈值后拒绝发送。
- **Reproduction（待执行）**：在邮件发送替身/隔离 Redis 下连续请求超过阈值，验证计数、TTL 和发送调用次数，不向真实邮箱压测。
- **Root Cause**：误把反序列化对象的自增当作 Redis 原子计数。
- **Impact**：本方法的次数限制不能按预期累计；外围网关/供应商是否另有限流未验证，不能断言系统完全无限流。
- **最小修复**：使用原子计数和到期策略，在发送前检查配额，并明确发送失败的计数处理。

### D1｜建议 P1：活跃旧调用和假成功操作清单｜UNVERIFIED

以下是当前实际调用链，不是把搜索命中的旧 URL 一律算作线上故障。表中无当前 HTTP 状态断言。

| 页面/操作 | 源码证据 | 当前事实与预期差异 |
| --- | --- | --- |
| 后台用户状态 | [user.ts:74-79](file:///E:/web/qs-web/src/api/modules/user.ts#L74-L79)、[AdminUserController.java](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/user/controller/admin/AdminUserController.java) | **本轮关闭**：实际请求 `/admin/user/changeUserStatus`（不再有 `Status1` 后缀），并携带 `authScope: 'admin'`。 |
| 旅行资源 | [travel/index.vue:144](file:///E:/web/qs-web/src/views/front/travel/index.vue#L144)、[webInfo.ts:56](file:///E:/web/qs-web/src/api/modules/webInfo.ts#L56) | 仍请求旧 `/webInfo/listResourcePath`；需对齐当前资源聚合契约。 |
| 后台资源路径 | [PathList.vue:245](file:///E:/web/qs-web/src/views/admin/resource/PathList.vue#L245)、[resource.ts:18-64](file:///E:/web/qs-web/src/api/modules/resource.ts#L18-L64) | 仍请求旧 `/file/list`，不能用日志里的旧请求证明当前有映射。 |
| 文章附属动态 | [article/index.vue:533-540](file:///E:/web/qs-web/src/views/front/article/index.vue#L533-L540)、[article/index.vue:614-617](file:///E:/web/qs-web/src/views/front/article/index.vue#L614-L617) | `getNews → listWeiYan` 实际请求旧 POST `/moment/list`。 |
| 后台文件删除 | [resource/List.vue:199](file:///E:/web/qs-web/src/views/admin/resource/List.vue#L199)、[resource.ts:46](file:///E:/web/qs-web/src/api/modules/resource.ts#L46)、[FileAdminController.java:74-88](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/file/controller/admin/FileAdminController.java#L74-L88) | JSON 请求体与 `@RequestParam id` 不匹配。 |
| 后台资源状态 | [resource/List.vue:225](file:///E:/web/qs-web/src/views/admin/resource/List.vue#L225)、[resource.ts:63](file:///E:/web/qs-web/src/api/modules/resource.ts#L63) | 方法直接 `Promise.resolve(null)`，不发请求，页面仍可提示成功。 |

- **Expected**：路径、HTTP 方法、参数位置和 DTO 与当前后端一致；无服务端变更时不得提示保存成功。
- **Reproduction（待执行）**：逐个从页面触发并记录 Network、业务码、页面状态和持久化结果；禁止仅用直接请求旧 URL 代替页面可达性证明。
- **Root Cause**：重构后的手写模块迁移不完整，部分保留空实现。
- **Impact**：上表对应功能失败或假成功，不推导为整个用户/动态/资源模块全部不可用。
- **最小修复**：按活跃入口逐条迁移；不全局字符串替换路径，不把 Orval 方法数字后缀当成后端 URL。
- **排除重复/误报**：后台用户列表已走 `/admin/user/page`；新动态页使用 `getTimeline/publishMoment`；旧 `getUserList`、友链 GET 删除封装及 `getCode1` 未建立当前活跃页面引用，不计为已确认页面故障。

### D2｜建议 P1：关于页分页模型错配｜UNVERIFIED

- **Evidence**：[about.ts:54-60](file:///E:/web/qs-web/src/api/modules/about.ts#L54-L60)、[ResourceAggregationController.java](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/file/controller/web/ResourceAggregationController.java)。
- **Current Fact（源码）**：前端使用 `pageNum/pageSize`，后端接收 MP Page 派生请求，绑定字段是 `current/size`。
- **Expected**：请求页码/页大小决定响应分页。
- **Reproduction（待执行）**：隔离数据超过一页，对比两组参数及第 1/2 页内容；旧响应样例仅见历史附录。
- **Root Cause**：多套分页 DTO 混用，边界没有转换。
- **Impact**：分页意图被忽略，可能持续停在默认页而漏展示后续项目。
- **最小修复**：调用边界转换为 `current/size`，同时核验响应的 `records/total` 消费层。

### D3–D5｜保留的安全配置和异常契约事项｜UNVERIFIED

| 编号/建议级别 | Evidence / Current Fact（源码） | Expected / Reproduction（待执行） | Root Cause / Impact / 建议 |
| --- | --- | --- | --- |
| D3 / P1 | [CorsConfig.java:21-24](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/common/config/CorsConfig.java#L21-L24)：通配 Origin + 允许凭据。 | 仅放行受信域；在匹配生产配置的隔离环境检查跨域预检和实际凭据类型。 | 开发便利配置未收敛；是否能跨域利用认证取决于凭据/网关，不能据此推导可读取 localStorage token。建议环境化白名单。 |
| D4 / P1 | [Weblinks.java:39-42](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/weblinks/domain/pojo/Weblinks.java#L39-L42)：`desc` 未显式转义，仍走通用 Mapper 查询。 | 生成合法 SQL；在隔离 MySQL 下执行该查询并核对生成 SQL。 | SQL 保留字映射未修，历史已出现语法异常，本轮未执行 SQL。建议显式转义映射后复测，再处理 C1；不写“本次 100% 报错”。 |
| D5 / P1 | [GlobalExceptionHandler.java](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/common/exception/handle/GlobalExceptionHandler.java)、[request.ts](file:///E:/web/qs-web/src/utils/request.ts)、[responseHandler.ts](file:///E:/web/qs-web/src/api/client/responseHandler.ts)：HTTP 状态与 R 业务码分离，统一封装会拒绝业务失败。 | 统一 404/405/校验异常表达；对照两套客户端的处理。 | 鉴权错误已返回真实 401/403，但兜底 `Exception` 仍为 HTTP 200；空实现导致的假成功见 D1，不是“HTTP 200 自动成功”问题。 |

### 本次排除/收窄的指控

- `FALSE POSITIVE`：由列表摘要问题推导“私密详情绕过密码”。详情仍走 `getArticleWithPwd`，与 A2 分开评估。
- `FALSE POSITIVE`：PUBLIC sysConfig 必然泄露私密配置；当前有 PUBLIC 类型过滤。未验证部署环境实际配置分类是否正确。
- `FALSE POSITIVE`：访问日志管理无鉴权；当前有 L0。后台默认 userToken 也不必然失败，后台登录会设置两种 token。
- `FALSE POSITIVE`：返回 User 类型必然泄露敏感字段；[CommonQuery.java:220-237](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/utils/CommonQuery.java#L220-L237) 使用有限字段投影，需看实际查询而非类名。
- 标签云已改为未删除名称列表并销毁实例，门户已接 `/portal/overview`；门户包含私密文章的统计总数不等于正文泄露。
- 当前本地 Orval 生成文件仍存在；停止 Git 跟踪不等于本机缺文件。干净克隆如何生成、是否可重复构建另需验收。
- 旧 `/resource/upload`“存在”的 HTTP 证据属于旧运行基线，不能证明当前源码有该路由；相反，仅看当前源码也不能声称已实测部署实例 404。

Sa-Token 改造相关的收窄（避免把设计写成缺陷）：

- `FALSE POSITIVE`：“`SET ... XX KEEPTTL` 使闲置期无法续期”。原生键 TTL 绑定的是绝对超时，空闲超时由 Lua 内的 `last + idle <= now` 判定；KEEPTTL 正是“刷新活跃时间但不延长绝对寿命”的正确写法，单测也断言了这一点。
- `FALSE POSITIVE`：“两域共用 `tokenName` 必然导致越权”。业务校验仍由 loginType + revision + scope 三重把关；共用键名影响的是会话簿记与运营能力，已降为 S5（P2，需运行时验证）。
- 不把“注册接口返回 `AuthSessionVO`”当作契约破坏：注册即登录是本次改造的显式设计，前端 `accept()` 也能按 64 位 token + `sessionType='WEB'` 正常消费；真正异常的是旧 `/user/token`（S4）。

## Evidence

本轮证据来自：Sa-Token 双会话改造的逐文件核对（`modules/user/auth` 下 `AuthSessionService`/`SaTokenSessionAdapter`/`AuthSessionProperties`/`UserCredentialService`/`AuthSessionVO`/`SessionScope`，`aop/LoginCheck` + `LoginCheckAspect`，`GlobalExceptionHandler`，前端 `src/auth/*`、`stores/modules/auth.ts`、`router/index.ts`、`utils/request.ts`、`api/client/responseHandler.ts`），以及下列本地命令。当前条目链接对应本轮工作树；历史附录链接仍是旧报告记录的行号，不保证与当前文件对齐。

量化口径：

- `@LoginCheck` 134 处：88 `scope=ADMIN`、11 `scope=EITHER`、35 默认（`value=2, scope=WEB`）。
- `controller/admin` 方法级 HTTP 映射 69 个：61 ADMIN、5 EITHER、3 无注解（均为认证入口）。
- 扫描方式：逐文件匹配注解行与相邻 Mapping，排除类级 `@RequestMapping`；注解与 Mapping 的书写顺序不一致，窗口过窄会误报。

不重复沿用旧 `187` 个操作、`65` 个手写调用、`20` 条错配、Orval `187/187` 数字。本轮未重新生成 OpenAPI 或扫描出一份当前全量双向契约表，不能称“全接口已无遗漏”。

## Changes

- 仅更新 `docs/project-Frontend&Backend-audit.md`（第二轮）：新增 Sa-Token 改造的设计落地表与 S1–S8，重写鉴权量化口径，刷新旧发现状态、构建与测试结果、回归计划及证据限制。
- 第三轮在本分支提交 3 处后端修复：`CommentBehHandler`（S9）、`MomentController#comment`（S10）、`MomentBehHandler#decrementMomentCount`（S11）；前端本轮无业务代码改动。回归脚本与临时工具均在未跟踪的 `temp/regress/`，不进入提交。
- 第四轮按业务更正落地站长专属写权限：后端 `MomentController` 写接口改 `value=0, scope=EITHER`、`/view` 公开；新增 `GET /portal/owner`（`UserService.getOwner` + `PortalOwnerVO` + `portal:owner` 300s 缓存）；`AdminMomentController#save` 补缺省作者；`MomentServiceImpl#pageList` 回填作者（S12）。前端新增 `request.put`、后台动态管理页 `/admin/moment/list`（路由、`ADMIN_ROLES`、侧边栏菜单），`/moment` 头部改读站长公开信息、发布 FAB/删除按钮/「我的动态」Tab 按 `userType=0` 显隐，未登录点赞/评论弹 `uiStore.openAuthModal()`，评论列表对匿名开放。前端本轮新增 1 个页面与 4 个 API 封装。
- 保留旧原文为折叠历史附录；其测试数据操作、运行状态及清理披露均属于过去，不作为本轮行为。
- 第二轮业务源代码、配置、数据库和缓存未修改，也未提交/合并/推送 Git；第三轮只改上述 3 个后端文件并提交到 fix 分支，回归数据已清理。编译/构建/测试只更新本地生成产物（`target/`、`dist/`、`logs/audit-build-1024.log`）。

## Verification

| 本轮实际执行 | 结果 | 证明范围 |
| --- | --- | --- |
| 两仓库 `git log -3`、`git status --short` | 后端 `main@3bd2670`（仅本报告 M）、前端 `main@4f4fed9`（干净，另有未跟踪 `scripts/auth-browser-harness.cjs`） | 固定本次源码快照 |
| `mvn -B -f E:\idea\qs-blog\qsblog-server\pom.xml -DskipTests compile` | `BUILD SUCCESS` | 改造后的主源码可编译 |
| `mvn -B -f ... test "-DskipTests=false"` | 两批共 **67 个用例全部通过**（13 AuthSessionService + 10 SaTokenSessionAdapter〔1 skipped〕+ 5 UserCredentialService；39 含 IM 会话契约等） | 双会话签发/解析/撤销、凭据升级、IM 断连的代码级行为；**仍未覆盖真实 Redis 与 HTTP 链路** |
| 同上，`SaTokenRedisIntegrationTest` | **未执行**，被 `@EnabledIfSystemProperty(named="auth.redis.integration")` 门控 | Lua 脚本与真实 Redis 键格式的兼容性仍待验证 |
| `pnpm -C E:\web\qs-web type-check` | 通过（`vue-tsc --noEmit`） | 当前依赖与生成客户端下的类型检查 |
| `pnpm -C E:\web\qs-web exec vitest run` | **43/43 通过** | 会话策略、快照丢弃、storage 同步等前端单元行为 |
| `pnpm -C E:\web\qs-web build` | 通过；Vite 7.3.0，20.52s | 可生成生产产物，不是生产页面运行验收 |
| 构建/测试后 `git status --short`、`git diff --check` | 无业务文件改动、无空白错误 | 编译与测试未改动受版本控制的业务文件 |
| 第三轮：10 个写接口 + 12 个读接口的 API 回归脚本（`temp/regress/moment-regress.cjs`，23 条断言） | **23/23 通过**（首轮 14/23 → 修复后 22/23 → 修 S11 后 23/23） | 真实后端 18081 + dev MySQL/Redis 下的动态写路径、跨域拒绝与身份隔离 |
| 第三轮：Playwright MCP 浏览器验收（`http://localhost:5173/moment`） | 登录→点赞→刷新保持→取消→发评论→刷新落库→退出，全链路 200 且无控制台报错 | S1/S2/S9/S10/S11 的 UI 侧表现 |
| 第三轮：`mvn -o -B test`（修复后全量单测） | **80 用例：0 failure / 3 error**，错误均在 `DatabaseTests`（`saveDanmakuList` DuplicateKey、`updateArticleCoverUrl` NPE、`updateDanmakuUrl` IllegalArgument），均为依赖真实库既有数据的存量集成用例 | auth/behavior 相关单测全绿；`DatabaseTests` 的 3 个错误与本轮修复无关 |
| 第四轮：`mvn -o -B -pl qsblog-server -DskipTests compile` | `BUILD SUCCESS` | 站长专属写权限、`/portal/owner`、后台作者回填均编译通过 |
| 第四轮：`npx vue-tsc --noEmit` | 通过（修掉 `portalApi.getOwner` 与 `isMine` 两处类型错误后清零） | 新增后台页与 `/moment` 改动的类型安全 |
| 第四轮：`npx vitest run --config vitest.config.ts` | **43/43 通过** | 会话快照/存储同步等前端单元行为未被破坏 |
| 第四轮：动态契约 API 回归（`temp/regress/contract-regress.cjs`，4 身份 42 条断言） | **42/42 通过**（首轮 40/42：评论作者字段名误判、后台作者未回填；修正后全绿） | 匿名/普通用户/站长/管理员的读、写、点赞、评论、后台 CRUD 与跨域拒绝 |
| 第四轮：Playwright MCP 四身份 UI 验收 | 匿名（头部 Joy、无发布/删除、点赞弹登录容器、评论公开可读）、普通用户（同上）、站长（FAB+删除+发布/删除回环）、管理员（前台无入口 + 后台 `/admin/moment/list` 增删改） | S1/S12 与 `/portal/owner`、后台页菜单/白名单的实际界面表现 |
| 第五轮：`mvn -o -B -pl qsblog-server -DskipTests compile` | `BUILD SUCCESS` | 14 个控制器注解调整后编译通过 |
| 第五轮：`npx vue-tsc --noEmit` + `npx vitest run` | 类型检查通过；**43/43 通过** | 角色首页/白名单/安全回跳断言已同步新规则 |
| 第五轮：ADMIN 域登录角色门槛 API 探针（`temp/regress/role-login-check.cjs`） | `userType=0/1` 登录 200 且会话 `userType` 正确；`userType=2` **403「该账号没有后台访问权限」且不落 token** | 登录门槛符合“仅 0/1 可进后台” |
| 第五轮：Playwright MCP 逐页回归（role=0 与 role=1） | 两者登录后均落 `/admin/main`，侧边栏 **15 项全展示**；role=1 逐页访问 12 个页面接口全 200；role=1 用户类型标签 `cursor=not-allowed` 且不弹窗 | 菜单全量开放与提权入口收口 |

### 第三轮运行时回归（2026-10-24，fix/sa-token-scope-regression）

环境：后端 `spring-boot:run -Pdev` 端口 18081，MySQL `192.168.30.100:3309/qingsi`，Redis `192.168.30.100:6379` db8，前端 `pnpm dev` 5173（`VITE_BASE_URL=http://localhost:18081`）。测试账号 `qs_regress_probe`（`userType=2`）通过 JDBC 直接写入，密码按前端 CryptoJS AES-ECB 契约加密后交后端解密/BCrypt 校验。

- 断言覆盖：R0 登录、R1 匿名发布 401、R2 发布、R3 timeline 可见、R4 liked 初始 false、R5 点赞、R6 liked=true、R7 匿名不泄露 liked、R8 评论、R9 评论列表、R10 点赞评论、R11 取消点赞评论、R12 删除评论、R13 更新、R14 详情回显、R15 浏览计数、R16 my/timeline、R17 WEB token 调后台 403、R18 越权删除被拒、R19 取消点赞、R20 删除动态、R21 删除后消失、R22 退出登录。
- 浏览器额外核实：token 存 `localStorage["authorization:web:v2"]`、无 Cookie；退出走 `POST /user/logout` 200 且存储清理；登出后回到匿名视角（探测动态已随清理消失）。
- 清理：回归账号、探测动态、`behavior_state/behavior_log/behavior_cnt/comment` 对应行按 id 删除，验证 SQL 六项计数均为 0。
- 未覆盖：管理员/DEV 角色在动态页的行为、S3/S4、双域并行（同账号前后台同时在线）、A1/A2/B1/B2 等存量项。

### 第四轮运行时回归（2026-10-24，业务契约更正）

环境与上一轮一致（后端 18081、MySQL/Redis dev、前端 5173）。临时账号 `qs_user_probe`(userType=2)、`qs_owner_probe`(userType=0)、`qs_admin_probe`(userType=1) 由 JDBC 写入，密码用前端同款 AES-ECB 加密后交后端校验。

- 匿名：`/moment/timeline`、`/moment/{id}`、`/moment/{id}/comments`、`/portal/owner` 均 200 且包含作者名/点赞数/评论内容；写接口一律 401。
- 普通用户：写接口 403「权限不足」；点赞/取消点赞/评论/删评论 200；评论列表公开可见且带 `username`；本人 `liked=true`、匿名视角不泄露。
- 站长：发布→修改→匿名详情读到新内容→删除全 200；`/portal/owner` 仍返回最小 ID 站长（id=1 Joy），不受探测账号影响。
- 管理员：WEB 会话发布 403；ADMIN 会话 `/admin/moment/page|save|update|delete` 全 200，新建动态作者缺省为当前后台账号；ADMIN token 调前台写接口同样 403。
- 浏览器：匿名点赞触发 `.backdrop-overlay > .auth-container` 登录/注册容器且不发网络请求；匿名可展开评论并读到他人评论；普通用户/管理员前台无发布 FAB、无删除按钮、无「我的动态」；站长发布→首条卡片带删除→确认删除回环成功；管理员侧边栏只显示有权限的 3 个菜单，「动态管理」可增删改。
- 清理：探针账号、动态、评论及 `behavior_state/behavior_log/behavior_cnt` 对应行已删除，验证 SQL：探针用户 0、孤儿评论/行为 0、动态存活 9 条（公开时间线 4 条）。
- 未覆盖：站长前台“仅好友/私密”动态的可见性回归、DEV 域外的第三方登录、双域并行登出互不影响。

### 第五轮运行时回归（2026-10-24，后台登录门槛与菜单全量开放）

环境与上一轮一致。临时账号 `qs_role0_probe`(0)、`qs_role1_probe`(1)、`qs_role2_probe`(2) 由 JDBC 写入，密码沿用同一 AES-ECB+BCrypt 契约；验证后已全部删除（探针用户 0、总用户 3、动态存活 9）。

- 登录门槛：ADMIN 域 `qs_role0/1` 200，`qs_role2` 403 且前端不写 `authorization:admin:v2`；根因是 `AuthSessionService#issue` 的 `isManager`（0/1）校验，本轮未改动该逻辑，仅补运行时证据。
- 默认首页：`adminHome` 统一为 `/admin/main`（原 role=1 落 `/admin/article/list`），守卫与 `safeAdminRedirect` 同步；role=0/1 登录后均直达报表页。
- 菜单：`ADMIN_ROLES` 全部改为 `[0, 1]`；侧边栏 13 个一级项 + 2 个子项对两种角色完全一致，普通用户仍在路由守卫处被重定向 `/verify`。
- 接口：管理端 `@LoginCheck(value = 0)` 批量放开为 `value = 1`（0/1 均可），覆盖 accessLog、article、category、comment、danmaku（含 `/danmaku/del`）、file、message、sysconfig、sysdict、tag、user、webInfo 与 `/webInfo/update`；**例外** `AdminUserController#changeUserType` 保持 `value = 0`，前端用户类型标签对 role=1 禁用，避免管理员自我提权为站长。
- 兼容修正：`comment/List.vue` 的全量评论分支改判 0/1（原仅站长），否则管理员会落到“只看自己的评论”接口。
- 逐页网络核查（role=1）：report `/admin/accessLog/getData`、article `/admin/article/pageList`、comment `/admin/comment/list`、category `/admin/category/page`、family `/admin/families/*`、danmaku `/admin/danmaku/list`、file `/file/admin/list`、tag `/admin/tag/page`、webInfo `/admin/webInfo/get`、user `/admin/user/page`、message `/admin/comment/list`、moment `/admin/moment/page` 均 200。
- 遗留（既有缺陷，非本轮引入）：`/admin/resourcePathList` 仍 404 — 前端 `resource.ts` 调 `/file/list|save|delete|update`，而后端只有 `/file/admin/*`，且该页业务实体实为 `weblinks`（`/weblinks/listWeblinks` 用 query 参数、`/resAgg/*` 用 body），需单独做一次端点与字段适配后才能使用。
- 未覆盖：role=1 在各页的写操作（新增/编辑/删除）仅按注解静态判定，未逐页执行；`changeUserType` 的 403 未做浏览器验收。

### CONFIRMED(BUILD) / CONFIRMED(TEST)：本轮实际观察

1. **Rollup 分包循环依赖警告已消失**：上轮 `categoryInfo.ts → stores/index.ts` 的跨 chunk 循环本轮未再出现。属构建结果变化，未在浏览器验证是否影响执行顺序。
2. **CSS 语法/弃用警告仍在**：生成 CSS 中 `&--register[data-v-*]` 不符合原生 CSS nesting 规则；多处 `>>>`、`/deep/` 已弃用。未推断为注册功能失效。
3. **大包警告仍在**：最大 JS chunk 约 866.52 kB（gzip 约 299.5 kB），超过 500 kB 阈值。属加载性能治理项。
4. **Java 编译警告仍在**：Lombok 父类 equals/hashCode、MapStruct 未映射字段、隐式注解处理、弃用及未检查操作。未因警告修改映射或认定业务丢字段。

### 推荐回归顺序

1. ~~**S1 优先**：匿名/普通用户/管理员三身份回归动态页写路径~~ → 已在第三、四轮完成：匿名/普通用户/管理员/站长四身份全部回归。
2. ~~**S2**：登录后加载公开时间线确认 `liked`，登出后仍可匿名浏览~~ → 已由 API 断言与浏览器刷新双重验证。
3. **S3/S4**：匿名与过期 token 访问 `/file/admin/upload`，确认响应统一、限流重新生效；用旧契约调用 `/user/token`，确认错误码明确。
4. **双域并行**：同一账号同时登录前后台，验证登出前台不影响后台、改密后两域旧会话均失效、跨标签页（storage 事件）与 `pageshow` 复核正常；同步核对 Redis 中两域的键名与 TTL（S5/S6）。
5. 旧发现：A1 跨账号读写与审核、A2 私密文章分页/推荐过滤、B1/B2 文章编辑与保存、B4/B5 标签缓存与字典、C1/D4 友链、D1 各活跃旧入口、D2 关于页分页。
6. 用邮件替身验证限流与找回成功反馈；最后对生产构建做路由切换/刷新与注册样式验收。

## Remaining Uncertainty

- 第二轮没有 HTTP/浏览器/DB/Redis 复测，S1–S8 当时均为源码结论；第三、四轮已补齐 S1/S2、S9–S12 与站长专属写权限的运行时证据，但 S3–S8 仍为源码结论。
- 第四轮只验证了「站长可写 + 其他角色不可写」的主链路；「站长只能管理自己发布的动态」这类对象授权（若后续要求）尚未定义，当前删除按钮对 `userType=0` 全量可见。
- 第五轮把后台管理端大量 `value = 0` 放宽为 `value = 1`，属权限模型收紧口径的放宽：`changeUserType` 是唯一保留站长专属的写接口，其余管理操作现由 0/1 共同掌握，是否符合最终产品授权需要产品方确认。
- 第五轮只逐页验证了读接口；role=1 的写接口（保存/删除/审核）未逐项执行，仅依据注解与前端调用链判定。
- `/admin/resourcePathList`（资源聚合）仍是既有 404 页面，前端端点与后端模块不匹配，未在本轮修复。
- 未在真实 Redis 上执行 Lua 脚本与 Sa-Token 1.46.0 的键格式契约测试（集成测试被 system property 门控）；S5 的`login:session` 键共享与 S6 的日志/存储增长因此只能定性不能定量。
- 不执行有状态探针：即使 GET 也可能回填缓存/记录访问/增加计数，认证切面会续期；不能将“GET”自动当作零副作用。
- 未确认运行 JVM 是否等于当前 HEAD、网关兼容路由、生产 CORS/限流及真实数据分布；不沿用上次 PID、端口或“实例必然旧”的说法。
- 未完成当前 OpenAPI 与全部生成/手写调用的双向全量核对；本地忽略的生成物可用不等于干净克隆的构建流程已验证。
- 旧 token 到 Sa-Token 的切换对已登录用户是硬切换：本轮未验证上线时的强制重新登录规模、前端旧 localStorage 键清理是否完全。
- 评论初始列表分支、MdEditor 上传回调、Graffiti/免密登录组件的完整可达性未全部闭环，不加入已确认故障数量。
- 构建与单测通过不证明权限、响应契约、缓存一致性或页面交互正确；建议下一轮在明确授权的隔离环境复现，再决定修复范围。

## 历史附录：首次运行时审计原文（已被上文复审状态覆盖）

> 下列内容保留用于追溯。其 62/26 鉴权统计、20 条接口数量、已修复项以及部分原因判断均有过时/错误之处，以上文逐项对照为准。原文中的“当前”“已实测”“已清理”“全部 P0/P1 已复现”仅是旧报告陈述，不是本轮确认；日志和辅助脚本是否仍存在也未重新核验。

<details>
<summary>展开旧版运行时报告与当时的副作用披露</summary>

已在**真实运行的后端实例**上完成契约复现与清理，审计结论从"静态推断"升级为"运行时取证"。基线：后端 `prod@89e2a1c`（51 个未提交改动，重新 `mvn clean package` 后以 `dev` profile 启动于 `18081`，依赖 MySQL `192.168.30.100:3309` / Redis `db=8`）；前端 `refactor/navbar-capsule-transition@1a8eebe`（59 个未提交改动）。

核心量化结果：

| 指标                               | 结果                                                       |
| :--------------------------------- | :--------------------------------------------------------- |
| 后端运行时操作数（`/v3/api-docs`） | **187**                                                    |
| 前端手写调用点                     | **65**                                                     |
| 前端调用后端不存在/方法不符        | **20**（13×404，6×405 被吞成 200/code500，1 条误报已纠正） |
| Orval 生成层 vs 后端               | **187/187 完全同步，双向 0 差异**                          |
| 管理端端点鉴权分级                 | 62 个：L0=21、L1=15、**L2=23**、**无注解=3**               |
| 弱鉴权端点（L2 + 无注解）          | **26**（其中 `/sysConfig/listSysConfig` 为按设计公开）     |

分级结论：**P0 3 项 / P1 7 项 / P2 7 项**，另修正 4 条历史误判。

------

## Findings

### P0-1 纵向越权：普通用户 token 可读写 25 个管理端点 `CONFIRMED`（运行时铁证）

- Evidence
  - [LoginCheck.java:11](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/aop/LoginCheck.java#L11)：`int value() default 2;`
  - [LoginCheckAspect.java:45-60](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/aop/LoginCheckAspect.java#L45-L60)：`USER_ACCESS_TOKEN` 仅在 `value` 为 0/1 时拒绝；随后 `if (loginCheck.value() < user.getUserType())` 才做等级比较 → **默认 2 对普通用户（userType=2）完全放行**
  - 弱鉴权清单（脚本统计）：`AdminCategoryController` 5 个、`AdminTagController` 4 个、`FileAdminController` 4 个、`AdminFamilyController` 3 个、`AdminSysDictController` 7 个（含 [AdminSysDictController.java:100](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/sysdict/controller/admin/AdminSysDictController.java#L100) / [:112](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/sysdict/controller/admin/AdminSysDictController.java#L112) 完全无注解）
- Current Fact（运行结果）
  - 无 token → `GET /admin/category/page` = `{"code":300,"msg":"未登录..."}`
  - 注入 `userType=2` 的 `user_access_token_auditprobe0001` → 同端点 `{"code":200}`，返回 5 条真实分类
  - 同 token `POST /admin/category` → `code:200`，落库 `id=8`；`DELETE /admin/category/8` → `code:200`
  - 匿名 `GET /admin/sysDict/data/listByTypeCode?typeCode=article` → `{"code":200,"data":[]}`（完全无鉴权）
  - 对照组：`POST /admin/article/pageList`（`@LoginCheck(0)`）→ `{"code":300,"msg":"请输入管理员账号！"}`，证明等级机制本身有效
- **Expected Behavior**：管理端写操作必须管理员（userType=0/1）方可执行
- **Reproduction**：Redis 写入 `user_access_token_*` 为 `{"id":"999999","userType":2,...}` → 带 `Authorization` 头访问 `/admin/category/page`
- **Root Cause**：`@LoginCheck` 默认值被定义为"登录即可"的 2，而非"管理员"的 0/1；管理端控制器大量方法使用裸 `@LoginCheck` 或漏注解
- **Impact**：任意注册用户可增删改分类/标签/文件/家庭/字典；因分类删除会影响文章归属，属数据破坏级风险

### P0-2 私密文章正文摘要公开泄露 `CONFIRMED`（运行时铁证）

- Evidence
  - [ArticleServiceImpl.java:373-394](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/article/service/ArticleServiceImpl.java#L373-L394)：仅 `.eq(Article::getIsDel, 0)`，**无 viewStatus 过滤**，并对正文截取摘要返回
  - [ArticleController.java:167-170](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/article/controller/web/ArticleController.java#L167-L170)：公开端点，无鉴权
- **Current Fact**：创建 `viewStatus=false / password=p@ss1234` 文章（`id=3`）后，**匿名** `GET /article/articleCategoryList` 返回
  `title=__AUDIT_PROBE_PRIVATE__, viewStatus=False, tips='runtime audit probe', contentLen=80, contentHead=PRIVATE_MARKER_9F3A ...`
  仅 `password` 被置空
- **Expected Behavior**：私密文章不得出现在任何公开列表；或至少不返回正文片段/tips
- **Reproduction**：管理员创建私密文章 → 匿名调用分类列表 → 在响应中找到该文章
- **Root Cause**：列表查询缺少可见性谓词，且"截断摘要"逻辑默认所有文章都可公开
- **Impact**：私密文章标题、提示语、前 80 字正文（含密钥/隐私片段）可被任意匿名访客批量抓取
- **对照**：`GET /article/getArticleById?id=3` 无密码/错密码均返回 `密码错误runtime audit probe`，正确密码才返回全文 → 详情端点本身是安全的（历史误判已纠正）

### P0-3 邮箱注册链路 100% 失败 `CONFIRMED`（双阻断）

- Evidence
  - 发码写入：[UserServiceImpl.java:492](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/user/service/UserServiceImpl.java#L492) → `code_mail_<email>_2`
  - 注册读取：[UserServiceImpl.java:215](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/user/service/UserServiceImpl.java#L215) → `forget_password_<email>_2`
  - 全库检索确认 `forget_password_` **只有读/删，没有任何写入**
  - 前端发码传空参：[user/index.vue:475-478](file:///E:/web/qs-web/src/views/front/user/index.vue#L475-L478) `let params = {}` → `authApi.emailCode(params)`；[auth.ts:60-62](file:///E:/web/qs-web/src/api/modules/auth.ts#L60-L62) → `GET /user/mailCode`
- Current Fact
  - 预置 `code_mail_audit_probe_2026@example.com_2 = 123456` 后 `POST /user/register`（code 同为 `123456`）→ `{"code":500,"msg":"验证码错误！"}`
  - `GET /user/mailCode`（前端空参形态）→ `{"code":400,"msg":"参数异常"}`
- **Expected Behavior**：发码与校验使用同一 key；发码必须携带 `email`+`bizType`
- **Reproduction**：见上；两项可独立复现
- **Root Cause**：常量前缀误用（`FORGET_PASSWORD` ↔ `CODE_MAIL`）；前端发码处参数对象未赋值
- **Impact**：邮箱注册完全不可用；即使修复 key，前端按钮仍发不出验证码

### P1-1 CORS 任意源 + 允许凭据 `CONFIRMED`

- **Evidence**：[CorsConfig.java:21-24](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/common/config/CorsConfig.java#L21-L24) `setAllowCredentials(true)` + `allowedOriginPatterns=["*"]`
- **Current Fact**：`OPTIONS /article/save` 带 `Origin: http://evil.example.com` → 回显 `Access-Control-Allow-Origin: http://evil.example.com`、`Access-Control-Allow-Credentials: true`
- **Expected**：白名单固定站点，或 `allowCredentials=false`
- **Reproduction**：任意 `Origin` 请求头预检
- **Root Cause**：为开发便利使用通配源，且未与凭据策略对齐
- **Impact**：任意站点可跨域读取公开接口、可携带浏览器凭据发起请求；虽当前 token 存于 header/localStorage（利用链需先窃取 token），该配置仍属明确的纵深防御缺口

### P1-2 管理端"发布文章"链路三处断裂 `CONFIRMED`

- Evidence
  - [UploadPicture.vue:169](file:///E:/web/qs-web/src/components/media/UploadPicture.vue#L169)：`$common.saveResource(...)`，但 [common.ts:221-232](file:///E:/web/qs-web/src/utils/common.ts#L221-L232) 只导出 10 个函数，**无 saveResource** → 运行时 `TypeError`，后续 `emit("addPicture", url)` 不执行
  - [upload.ts:68-69](file:///E:/web/qs-web/src/api/modules/upload.ts#L68-L69) 调用 `/resource/saveResource` → 运行时 **404**
  - [Edit.vue:262-265](file:///E:/web/qs-web/src/views/admin/article/Edit.vue#L262-L265) `const res = await articleApi.getArticleById(...); if (res.data)`，而 [article.ts:31-34](file:///E:/web/qs-web/src/api/modules/article.ts#L31-L34) 已 `return res.data` → **双解包，编辑页永不回显**
- **Current Fact**：`POST /resource/saveResource` → `HTTP 404`；`POST /resource/upload`（真实上传端点）→ `{"code":400,"msg":"参数异常"}`（缺 `file/type/relativePath`）
- **Expected**：上传成功后登记资源并回填 URL；编辑页加载时填充表单
- **Reproduction**：管理端上传图片 / 打开文章编辑页
- **Root Cause**：工具库重构时移除 `saveResource` 但调用点未迁移；API 模块层已解包却被调用方二次解包
- **Impact**：管理员无法插图、编辑页空白，内容运营主流程不可用（且会产生已上传但未登记的孤儿文件）

### P1-3 友链页面数据接口 100% SQL 报错 `CONFIRMED`

- **Evidence**：[Weblinks.java:42](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/weblinks/domain/pojo/Weblinks.java#L42) 字段 `desc` 未加反引号，MySQL 保留字；[friend/index.vue:203](file:///E:/web/qs-web/src/views/front/friend/index.vue#L203) → `webApi.listFriend()` → [weblinks.ts:34](file:///E:/web/qs-web/src/api/modules/weblinks.ts#L34) `/weblinks/list`
- **Current Fact**：`GET /weblinks/list` → `{"code":500,"msg":"服务异常"}`；日志：`SQLSyntaxErrorException ... near 'desc,remark,type,sort,... FROM weblinks'`
- **Expected**：返回友链列表
- **Reproduction**：匿名访问前端友链页
- **Root Cause**：实体字段名与 SQL 关键字冲突，未使用 `@TableField("`desc`")`
- **Impact**：友链页面永久空白（且该错误在 `select *` 路径上，任何使用该实体的全字段查询都会失败）

### P1-4 20 条前端调用与后端契约不符 `CONFIRMED`

- Evidence / Current Fact
  - 14 条 **HTTP 404**：`/user/me`、`/user/login1`、`/article/listSortArticle`、`/webInfo/getWebInfo`、`/webInfo/getSortInfo`、`/webInfo/listResourcePath`、`/admin/user/list`、`/admin/comment/boss/deleteComment`、`/admin/user/changeUserStatus1`、`/file/list`、`/file/save`、`/file/delete`、`/file/update`、`/resource/saveResource`
  - 6 条 **HTTP 200 + code 500**（405 被异常处理器吞掉）：`POST /moment/list`、`/moment/listNews`、`/moment/save`、`/moment/saveNews`、`GET /moment/del`、`GET /weblinks/deleteWeblinks`
- **Expected**：调用点与 OpenAPI 187 操作一致
- **Reproduction**：逐一请求，响应体见上
- **Root Cause**：前端 `src/api/modules/*` 为旧版 SDK 残留，重构后未随后端路由演进；`request.ts` 无 `delete/patch` 封装，导致删除操作被迫用 GET
- **Impact**：旧版"说说/文件/友链"入口全部不可用；且失败形态不一致（404 走 axios 错误分支，405 走业务成功分支 code=500）

### P1-5 点赞接口无鉴权 + 语义反向 + 计数非原子 `CONFIRMED`（鉴权部分）

- **Evidence**：[ArticleController.java:219-226](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/article/controller/web/ArticleController.java#L219-L226) 无 `@LoginCheck`；[ArticleServiceImpl.java:411-450](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/article/service/ArticleServiceImpl.java#L411-L450)：只有 `operation==1` 才判重，`userId` 为空时直接落到"点赞数 +1"；[article/index.vue:757-766](file:///E:/web/qs-web/src/views/front/article/index.vue#L757-L766) 传 `operation: 0` 却提示"点赞成功"
- **Current Fact**：匿名 `POST /article/addArticleLikeCount {"articleId":"99999999"}` → `{"code":500,"msg":"文章不存在"}`（说明请求已进入业务逻辑，无任何鉴权拦截）；匿名 `GET /article/checkHasLike` → `200/false`
- **Expected**：点赞需登录、同一用户幂等、取消点赞需真实已赞
- **Reproduction**：不带 `Authorization` 调用上述端点
- **Root Cause**：控制器漏注解；`USER_LIKES` 只 `del` 不 `set`，`hasLike` 恒 false；计数为"读-改-写"
- **Impact**：可无限刷赞；`operation=0` 也会 +1；并发下计数丢失

### P1-6 关于页分页参数被完全忽略 `CONFIRMED`

- **Evidence**：[about.ts:54-58](file:///E:/web/qs-web/src/api/modules/about.ts#L54-L58) 传 `{pageNum, pageSize}`；后端 `BaseReqVO extends Page`（只认 `current/size`），[ResourceAggregationController.java:94-105](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/file/controller/web/ResourceAggregationController.java#L94-L105)
- **Current Fact**：`{"pageNum":2,"pageSize":2}` → 响应 `size:10, current:1`（两者均被忽略）；`{"current":3,"size":1}` → `size:1, current:3`（生效）
- **Expected**：分页参数应决定页大小与页码
- **Reproduction**：见上
- **Root Cause**：三套分页模型混用（MP `Page` / `BaseReqVO` / `PageQuery`），前端按第三套命名传参
- **Impact**：关于页项目卡片固定取第 1 页 10 条，超过 10 条静默丢失

### P1-7 全局异常契约不一致 `CONFIRMED`

- **Evidence**：[GlobalExceptionHandler.java:26-58](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/common/exception/handle/GlobalExceptionHandler.java#L26-L58) 捕获 `Exception` 并统一返回 HTTP 200
- Current Fact
  - 不存在的路由 → **404 + Spring 默认结构** `{"timestamp","status","error","path"}`（未被统一封装）
  - 方法与后端不符（405）→ **200 + {"code":500,"msg":"服务异常"}**
  - 缺参 → `200 + code 400 参数异常`
- **Expected**：404/405/业务错误统一信封（`code/msg/data/currentTimeMillis`）
- **Reproduction**：`GET /user/me`（404）vs `GET /weblinks/deleteWeblinks`（405→200/500）
- **Root Cause**：`@ControllerAdvice` 不处理 `NoHandlerFoundException`/`HttpRequestMethodNotSupportedException`，但 `Exception` 兜底又把 405 吞掉
- **Impact**：前端拦截器对 404 与 code≠200 走两套分支；监控无法按 HTTP 状态区分真实故障

### P2-1 找回密码旧链路为死代码 `CONFIRMED`

- **Evidence**：[user/index.vue:851-869](file:///E:/web/qs-web/src/views/front/user/index.vue#L851-L869) `getCode1` 指向 `/user/getCodeForForgetPassword`（模板中无任何按钮绑定）；[:804-805](file:///E:/web/qs-web/src/views/front/user/index.vue#L804-L805) `updateForForgetPassword(params)` 只传 `code/password`
- **Current Fact**：`GET /user/getCodeForForgetPassword` → **404**；`POST /user/updateForForgetPassword`（前端载荷）→ `{"code":400,"msg":"参数异常"}`
- **Expected**：删除死代码或补齐 `place/flag`
- **Impact**：当前可见"忘记密码"面板走新链路（`mailCode` + `resetPwdForFgtPwd`），已实测可用；旧链路仅增加维护噪音 — **历史"找回密码断链"评级由 P1 下调为 P2**

### P2-2 `mailCode` 非法 `bizType` 静默写 `_0` `CONFIRMED`

- **Evidence**：[EmailBizType.java:65-68](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/common/enums/EmailBizType.java#L65-L68) 未知 code 返回 `0`，[UserServiceImpl.java:737](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/user/service/UserServiceImpl.java#L737) 据此拼 key
- **Current Fact**：小写 `register` 会写入 `code_mail_<email>_0`，任何校验方都不会读取（且邮件已真实发出）
- **Impact**：大小写错误时"发码成功但永远校验失败"，排障成本高

### P2-3 密码错误提示拼接 `tips` `CONFIRMED`

- **Evidence**：[ArticleServiceImpl.java:292-301](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/article/service/ArticleServiceImpl.java#L292-L301) `R.fail("密码错误" + article.getTips())`
- **Current Fact**：实测 `{"code":500,"msg":"密码错误runtime audit probe"}`（未做空格分隔）
- **Impact**：提示语不可控、可被写入任意文本回显给匿名用户

### P2-4 常量双份定义 `TECH DEBT`

- **Evidence**：[constant/index.ts:22-26](file:///E:/web/qs-web/src/constant/index.ts#L22-L26) 与 [utils/constant.ts:28-32](file:///E:/web/qs-web/src/utils/constant.ts#L28-L32) 各维护一份 `EmailBizType`；`APP_CONSTANTS.cryptojs_key` 硬编码于前端
- **Impact**：枚举漂移风险；AES 密钥随前端产物下发（本次实测前后端 key 一致，`resetPwdForFgtPwd` 解密成功）

### P2-5 摘要构建存在 NPE 与缓存无 TTL `UNVERIFIED`（代码级）

- **Evidence**：[ArticleServiceImpl.java:384](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/article/service/ArticleServiceImpl.java#L384) `article.getArticleContent().length()` 未判空；`CATEGORY_ARTICLE_LIST` 写入无过期时间
- **Impact**：历史脏数据（正文为 null）会导致整张列表 500；缓存只能靠写操作失效

### P2-6 组件声明签名与真实 API 不一致 `TECH DEBT`

- **Evidence**：[UploadPicture.vue:61](file:///E:/web/qs-web/src/components/media/UploadPicture.vue#L61) 声明 `(prefix,url,size,type,name,storeType,isAdmin)` 7 参，而 [upload.ts:68](file:///E:/web/qs-web/src/api/modules/upload.ts#L68) 实际是 `(resourceObject, isAdmin)`；[Graffiti.vue:393](file:///E:/web/qs-web/src/components/business/comment/Graffiti.vue#L393) 同样调用

### P2-7 `request.ts` 缺 `delete/patch` 封装 `TECH DEBT`

- **Evidence**：[request.ts:149-231](file:///E:/web/qs-web/src/utils/request.ts#L149-L231) 仅 `post/get/upload`
- **Impact**：直接导致 `deleteWeblinks` 用 GET 调 DELETE 端点（P1-4 的 6 条 405 中之一）

------

### 历史结论纠正（FALSE POSITIVE）

| 原结论                                       | 运行时/代码复核                                              | 结论               |
| :------------------------------------------- | :----------------------------------------------------------- | :----------------- |
| `getArticleById` 可匿名绕过密码读私密文章    | Controller 走 `getArticleWithPwd`；实测无密码/错密码均拒绝   | **FALSE POSITIVE** |
| `/sysConfig/listSysConfig` 泄露七牛/邮箱密钥 | 匿名返回 8 个 PUBLIC 配置，无 accessKey/secret               | **FALSE POSITIVE** |
| `/resource/upload` 为死端点                  | 实际存在，缺参返回 400                                       | **FALSE POSITIVE** |
| `AdminAccessLogController` 无鉴权            | [第 53 行](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/access/controller/admin/AdminAccessLogController.java#L53) 有 `@LoginCheck(0)` | **FALSE POSITIVE** |
| "找回密码断链" P1                            | 可见 UI 走新链路且运行可用，旧链路为不可达死代码             | **降级 P2**        |

------

## Evidence

**运行时基线**

- `mvn -B -DskipTests clean package` → BUILD SUCCESS（46.5s）→ `docker/qsblog-server.jar`（102,266,013 B）
- 启动日志：`Tomcat started on port(s): 18081` / `Started QsBlogApplication in 27.439 seconds`
- `GET /v3/api-docs` → 200，198,242 B → 187 操作（`logs/be-ops-runtime.txt`）；前端 65 调用点（`logs/fe-calls.txt`）

**关键运行时回放**

| 用例     | 请求                                                        | 响应                                           |
| :------- | :---------------------------------------------------------- | :--------------------------------------------- |
| 越权读   | 无 token → `/admin/category/page`                           | `code 300 未登录`                              |
| 越权读   | USER token → `/admin/category/page`                         | `code 200`，返回 5 条分类                      |
| 越权写   | USER token → `POST /admin/category`                         | `code 200`，落库 `id=8`；`DELETE` 后清理       |
| 无鉴权   | 匿名 → `/admin/sysDict/data/listByTypeCode`                 | `code 200, data []`                            |
| 注册     | Redis 预置 `code_mail_..._2=123456` → `POST /user/register` | `code 500 验证码错误！`                        |
| 发码     | `GET /user/mailCode`（空参）                                | `code 400 参数异常`                            |
| 私密泄露 | 匿名 → `/article/articleCategoryList`                       | 返回 `viewStatus=False` 文章 + 80 字正文       |
| 详情保护 | 匿名 → `/article/getArticleById?id=3`                       | `密码错误runtime audit probe`                  |
| 点赞鉴权 | 匿名 → `/article/addArticleLikeCount`                       | `code 500 文章不存在`（已进业务层）            |
| 分页     | `{"pageNum":2,"pageSize":2}` → `/resAgg/listWeblinks`       | 回显 `size:10,current:1`                       |
| 分页     | `{"current":3,"size":1}`                                    | 回显 `size:1,current:3`                        |
| CORS     | `Origin: http://evil.example.com`                           | `ACAO: http://evil.example.com` + `ACAC: true` |
| 友链     | `GET /weblinks/list`                                        | `code 500 服务异常`（`desc` 语法错误）         |

**代码证据**：见各 Finding 的文件链接（均为当前分支工作区实际行号）。

------

## Changes

**业务代码：0 行改动**（保持 READ ONLY）。

新增审计辅助文件：

- [scripts/run-be.ps1](file:///E:/web/qs-web/scripts/run-be.ps1)：从 `.env` 注入环境变量启动后端（密钥不落盘）
- [scripts/redis_probe.py](file:///E:/web/qs-web/scripts/redis_probe.py)：RESP 客户端（GET/SET/DEL/TTL/COPYKEY）
- [scripts/audit_admin_auth.cjs](file:///E:/web/qs-web/scripts/audit_admin_auth.cjs)：管理端 `@LoginCheck` 分级统计

产物：`logs/openapi.json`、`logs/be-ops-runtime.txt`、`logs/fe-calls.txt`、`logs/be-build.log`、`logs/be-out.log`

**测试数据清理（已二次核验）**

- 分类探针 `id=8` → `DELETE` → `REMAIN=0`
- 私密文章 `id=3` → 物理删除 → 列表 `REMAIN=0`，`getArticleById` 返回 `文章不存在`
- Redis 9 个键全部 `DEL=1`，复查 `GET=None`：`user_access_token_auditprobe0001`、`user_access_token_auditprobedev01`、`admin_access_token_auditprobe0002`、`user_token_999999`、`user_token_interval_999999`、`admin_token_1`、`admin_token_interval_1`、`code_mail_..._2`（`_3` 已被业务自动删除）
- 后端进程已停止，`18081` 端口已关闭

**副作用披露**：探针使用管理员 token 时，`LoginCheckAspect` 的续期逻辑覆写了 `admin_token_1` / `admin_token_interval_1`；清理时已删除。若清理前存在真实管理员会话映射，该会话已失效，需重新登录。

------

## Verification

- 后端成功构建并启动，OpenAPI 可访问，187 操作解析成功
- 26 个弱鉴权端点由脚本统计（向上/向下双向识别注解，已修正 `@LoginCheck` 写在 Mapping 之前导致的漏判）
- 全部 P0/P1 结论均有"请求→响应"原文，而非静态推断
- 清理动作逐项复核：数据删除后列表复查为 0、Redis 键 `GET` 为 `None`、端口不可达
- 生成层与后端 187/187 双向零差异，说明"前后端契约问题"集中在手写 `modules` 层而非 Orval 层

------

## Remaining Uncertainty

1. **私密文章泄露影响面**：dev 库仅 2 篇公开文章，泄露通过临时创建的私密文章证明；生产数据量下的实际暴露范围未评估。
2. **刷赞实际增涨未做**：为避免永久污染 `likeCount`（无匿名减一接口），仅证明"无鉴权进入业务层"，未验证真实 +1/并发丢计数。
3. **数据库直连不可用**：MySQL 8 `caching_sha2_password` + 本地客户端版本不兼容，`password IS NULL` 账号数量、软删除残留等无法直接核对。
4. **弱鉴权数量口径**：脚本统计 26（23×L2 + 3×无注解），其中 `/sysConfig/listSysConfig` 为按设计公开，实际风险面为 25；其他目录（非 `controller/admin` 路径）的管理端点未纳入该脚本。
5. **前端未提交改动**：59 个未提交文件可能包含未审计的临时代码，本报告仅覆盖当前工作区快照。
6. **运行环境差异**：本次为 `dev` profile + 本机 `18081`，生产 CORS 白名单、前端域名与网关策略可能不同（`application-prod.yml` 未逐一比对）。
7. **管理员会话副作用**：`admin_token_1` 被覆写后删除，若之前存在有效管理员登录态，需要重新登录恢复。

</details>