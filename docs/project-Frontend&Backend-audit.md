# 前后端联合审计报告

更新日期：2026-10-26（第九轮：文件模块专项审计 F1–F17，只读源码 + 数据库事实核对，未改动业务代码）。本文前半部分为当前结论；末尾折叠附录保留旧运行时审计记录，不代表当前版本的运行结果。

## Result

**PARTIAL：Sa-Token 双会话改造已完成源码复审、构建/单测与运行时回归。动态模块的鉴权链路（S1/S2）、回归中暴露的三处存量缺陷（S9–S11）、第四轮按业务更正落地的站长专属写权限、第五轮的后台登录门槛与菜单全量开放，第七轮按业务口径修复的 A2（私密文章列表正文暴露），以及第八轮的首页分类放开私密文章 + 分类区块解包修复，均已在真实 MySQL/Redis + 浏览器环境中验证通过；第五轮改动已提交并快进合并至两仓库 `main`（第七、八轮已提交：后端 `361cd07`、前端 `bcd56f2`，两仓库均未推送远程）。第九轮新增文件模块专项审计（F1–F17）：dev 库 `file_info`/`file_biz_ref` 均为 0 行，登记从未成功落库；前端压缩、后端重绘、派生变体三项内部能力全部缺失，详见「文件模块专项审计（第九轮）」。A1/B1/B2 等存量项仍未在运行时复核，项目整体不判定为通过。**

- 本轮最高优先级（S1）：动态模块 12 个写接口原为 `value=1` + `scope=ADMIN`，既拒绝普通用户又以 WEB 会话拦截所有人。第四轮按业务方更正后的口径重定契约：**新增/修改/删除仅站长（`userType=0`）**，其余人（含匿名）可读动态、点赞数、评论内容与作者信息，点赞/评论需登录；后台 `/admin/moment/*` 保留给站长(0)与管理员(1)。
- 双会话设计在源码层成立：WEB/ADMIN 各自独立的 `StpLogic`、revision 版本撤销、跨域调用显式拒绝、401/403 以真实 HTTP 状态返回、前端按域存储并按 generation 丢弃过期响应。
- 上轮 P0 的「普通用户可调管理接口」在源码层已收敛：`controller/admin` 下 69 个方法级操作中 61 个 ADMIN、5 个 EITHER、仅 3 个认证入口无注解；全仓 134 处 `@LoginCheck` 为 88 ADMIN / 11 EITHER / 35 默认 WEB。
- 存量未修：family 归属与审核（A1）、文章编辑双解包与旧字段（B1/B2）、标签缓存与字典（B4/B5）、友链（C1/D4）、活跃旧路径（D1）、关于页分页（D2）。
- 第二轮只更新本文件、不改业务代码；第三轮（fix 分支运行时回归）在本文件中回填运行时证据，并在 `fix/sa-token-scope-regression` 分支提交了 3 处后端缺陷修复（S9–S11），回归产生的临时账号/动态/行为/评论数据已全部清理（验证 SQL 计数归零）。

### 当前基线与证据口径

| 项目 | 本次实际基线 | 任务开始工作树 |
| --- | --- | --- |
| 后端 `E:\idea\qs-blog` | `main@a59b9e9` | 干净 |
| 前端 `E:\web\qs-web` | `main@244ebe3` | 干净（另有未跟踪 `scripts/auth-browser-harness.cjs`、`temp/`） |

本轮基线前进到两次鉴权专项提交：后端 `3bd2670 feat(auth): 前后台独立双会话改造（Sa-Token + Redis 双域）`（61 文件 +2559/−455）、前端 `4f4fed9 feat(auth): 前后台独立双会话与分域请求鉴权`（69 文件 +1968/−1345）。上一轮基线为后端 `d96e823`、前端 `05697b4`，本文中标注「上轮」的结论均指该基线。旧报告后端 `89e2a1c` 在当前仓库无法解析，且旧基线带有未提交改动，不能用提交差分完整重建当时工作树。

第六轮把基线推进到合并后的 `main`：后端 `a59b9e9`、前端 `244ebe3`（均由 `git merge --ff-only` 快进，内容与第五轮验证快照一致，见「第六轮：提交与合并」）。两仓库 `main` 尚未推送远程。

状态定义：
- `FIXED(STATIC)`：当前源码已体现指定修复，不代表本轮重新跑过该业务流程。
- `FIXED(VERIFIED)`：修复已在真实后端 + 数据库 + 浏览器链路上跑通并录得响应。
- `UNVERIFIED`：已列出源码事实和调用链，但缺少当前运行时/数据库证据；下文 P0/P1/P2 均为**建议处置优先级**，不是新增运行时确认数量。
- `FALSE POSITIVE`：原指控被当前证据推翻；`TECH DEBT`：维护风险，不直接等同于业务故障。
- `CONFIRMED(BUILD)`：本轮命令实际观察到的构建结果/警告，不代表相关 UI 故障已复现。
- `CONFIRMED(TEST)`：本轮单元测试覆盖到的行为，不等价于运行时的端到端验收。

范围：单次联合代码复审覆盖旧发现及相关活跃调用链，结合 Git 状态与修复历史；未进行云扫描、全量依赖安全扫描或当前 OpenAPI 双向全量统计。S2–S4 可归因到本轮两次鉴权改造提交，S1 的角色门槛与 S5–S8 为存量/设计风险，其余编号仍为存量；S1/S2/S9–S12 与 A2 已做真实后端 + 数据库 + 浏览器验收，其余未跑。

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

### A2｜私密文章在公开列表返回整篇正文｜`FIXED(VERIFIED)`

- **Evidence**：[ArticleServiceImpl.java](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/article/service/ArticleServiceImpl.java)、[ArticleList.vue](file:///E:/web/qs-web/src/components/business/article/ArticleList.vue)。
- **Current Fact（业务口径更正，2026-10-24）**：产品口径为「私密文章可以出现在列表，展示封面、标题与开头摘要」，不要求在公开分页过滤私密文章。原实现在 `listArticle` 中仅当正文超过 `SUMMARY=80` 才截断，长度不超过 80 的私密文章会把**整篇正文**返回给匿名列表；`listCategoryArticle` 原只查 `viewStatus=true`，第八轮已按业务放开。
- **Root Cause**：截断条件只覆盖超长正文，且列表直接复用实体字段，缺少按可见性分层的投影；“给摘要”与“给全文”在短文本上没有分界。
- **修复**：新增 `projectForPublicList` / `toListSummary` 统一列表投影——`password`、`videoUrl` 置空（`hasVideo` 先计算再清空）；正文 >80 字截断为 80 字 + `…`；**私密文章正文 ≤80 字直接返回 `null`**（给摘要等于给全文，故不返回）；公开短文保持原样。`listCategoryArticle` 复用同一投影。前端 `ArticleList` 对空摘要兜底显示「该文章已加密，输入密码后查看」。
- **验证（2026-10-24，真实 MySQL + 后端 18081 + 浏览器）**：插入 4 条探针后，匿名 `POST /article/listPage` 返回——私密长文 81 字（80+`…`）、私密短文 `articleContent=null`、公开长文 81 字、公开短文原样；`password`/`videoUrl` 均为空。`/sort?sortId=1` 页面 4 张卡片渲染与接口一致（私密短文显示加密提示）。详情 `GET /article/getArticleById` 回归：私密无密码返回「密码错误+提示」、带密码返回全文 120 字、公开正常。探针已删除（探针 0 条、存活 2 条），并清理 Redis `category_article_list_v2` 缓存。
- **未覆盖/后续**：`articleCategoryList` 已于第八轮放开私密文章并修复首页解包；遗留差异是首页卡片组件 `SortArticle.vue` 不渲染摘要区，私密文章在首页只出现标题/封面/分类/时间，摘要仅在 `/sort` 展示。

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

## 文件模块专项审计（第九轮）

范围：`modules/file` 的 Controller/Service、`infra/storage`（FileFilter/QiniuUtil/LocalUtil）、前端上传封装与组件、`file_info`/`file_biz_ref` 建表语句、dev 库实际数据。**只读审计**：未改业务代码、未建表、未启动服务、未产生测试数据。

### 事实基线

| 项 | 事实 | 证据 |
| --- | --- | --- |
| 表数据 | `file_info` 0 行、`file_biz_ref` 0 行 | dev 库 `SELECT COUNT(*)`（第九轮） |
| SQL 模式 | `ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,...` | `SELECT @@sql_mode`（第九轮） |
| 必填列 | `file_hash`、`bucket`、`owner_id` 均 NOT NULL 且无默认值 | [qingsi-model.sql:270-304](file:///E:/idea/qs-blog/docs/sql/qingsi-model.sql#L270-L304) |
| 写入实现 | 仅填 `storedName`/`fileExt`/`fileCategory`，上述三列未赋值 | [FileInfoServiceImpl.java:24-35](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/file/service/FileInfoServiceImpl.java#L24-L35) |
| 结论 | 严格模式下 INSERT 必然失败；前端只有 `ElMessage.warning('文件已上传，但资源登记失败')`，故历史验证未发现 | [upload.ts:70-118](file:///E:/web/qs-web/src/api/modules/upload.ts#L70-L118) |

### F1–F14｜接口与链路缺陷

| 编号 | 级别 | Current Fact（源码） | Impact | 最小修复 |
| --- | --- | --- | --- | --- |
| F1 | P0 | 三列 NOT NULL 无默认却未赋值，表 0 行 | 登记 100% 失败，媒体库永远为空 | 三列放宽（hash 可空、bucket 默认 `''`、owner 可空）+ 服务端补全 |
| F2 | P0 | `upload` 只返回 `visitPath` 不落库；登记靠前端二次 `saveResource`，且仅 `admin && userType===0` | C 端上传无任何记录，全部成为孤儿 | 上传即登记，或由服务端统一调用 `register` |
| F3 | P1 | `delete` 用 `@RequestParam id`，前端发 JSON body | 400，后台删除不可用 | 改 `@RequestBody`，或前端改 query |
| F4 | P1 | `list` 返回 `BaseReqVO`（`records`），前端读 `res.list` | 列表恒为空 | 统一 `{list,total,page,pageSize}` |
| F5 | P1 | 前端 `resource.ts` 调 `/file/list`、`/file/save`、`/file/delete`、`/file/update`，后端无这些路由 | 全部 404 | 对齐 `/file/admin/*` |
| F6 | P1 | `resource/List.vue` 先判 `res.data` 再读 `res.list` | 双解包误判为空 | 去掉 `.data` |
| F7 | P1 | `getToken` 无 `insertOnly`/`mimeLimit`/`returnBody`，key 由前端拼 | 可覆盖同名对象、任意 MIME 直传 | 服务端生成 key + PutPolicy |
| F8 | P1 | `FileFilter` 先 `resolve` 再判空 token；`BlogUtil.getAdminUser()` 可能 NPE；计数不回写 Redis | 匿名 401、NPE、限流失效 | 顺序修正 + 原子计数 |
| F9 | P1 | `LocalUtil` 用 `uploadUrl + path` 字符串拼接 | 子路径/反代下 URL 错误 | URI 规范化 |
| F10 | P1 | `Edit.vue#imgAdd` 用 `VITE_QINIU_URL`/`VITE_QINIU_DOWNLOAD`，`.env*` 未定义 | 回填域名 `undefined` | 改用后端 `visitPath` 或补 env |
| F11 | P2 | `UploadPicture` 的 `storeType` 取自 localStorage | 与后端 `store.type` 漂移 | 改读 `/file/config` |
| F12 | P2 | `beforeUpload` 空实现，仅校验大小 | 无类型/尺寸/压缩校验 | 见 F15 |
| F13 | P0（设计） | `file_biz_ref` 无 `biz_field` 列，全仓无写入方 | 无法表达封面/正文/头像，引用不可查 | 加 `biz_field` + 重建唯一键 |
| F14 | P2 | `QiniuUtil#submitPfop` 无调用方 | 派生转码为死代码 | 迭代 4 接回或删除 |

### F15–F17｜三项内部能力（均不新增 HTTP 接口）

#### F15｜前端压缩缺失（OffscreenCanvas）｜已落地（迭代 5，上传前压缩）

- **Evidence**：[UploadPicture.vue:136-160](file:///E:/web/qs-web/src/components/media/UploadPicture.vue#L136-L160) `beforeUpload` 为空实现，只校验 `file.size`；[upload.ts:70-118](file:///E:/web/qs-web/src/api/modules/upload.ts#L70-L118) 直接 `FormData.append` 原文件。
- **Expected**：上传前用 `OffscreenCanvas` 等比缩到长边 ≤2560、`quality=0.85` 转 WebP（不支持回退 JPEG），按 EXIF `Orientation` 归一；压缩失败降级原图并提示。
- **Impact**：手机原图 5–15MB 直传，白耗七牛流量与存储；列表仍加载原图，首屏变慢。
- **最小修复**：在 `beforeUpload` 返回压缩后的 `File`；提示压缩前后体积；同步收紧 `accept` 与 MIME 校验。
- **落地**：迭代 5 新增 `src/utils/image/compressImage.ts`，在 `uploadMedia` 内先压缩再申请凭证；`UploadPicture.vue` 经 `onCompressed` 提示体积并前置拦截非图片。详见「迭代 5 落地与运行时验收」。

#### F16｜后端重绘缺失（Thumbnailator / ImageIO）｜已落地（迭代 5，JDK ImageIO 重绘）

- **Evidence**：[QiniuUtil.java:147-175](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/infra/storage/QiniuUtil.java#L147-L175) `saveFile` 只做流写入；`LocalUtil` 直接落盘，无二次编码。
- **Expected**：服务端解码→重绘→再编码（`ImageIO` 或 Thumbnailator），剥离 EXIF/GPS、纠正方向、限制像素总数（防解压炸弹）、按魔数嗅探真实类型。
- **Impact**：前端校验可被绕过；带 GPS 原图直接公开；超大像素图可打满堆内存。
- **最小修复**：`saveFile` 后增加仅对图片生效的 `ImageSanitizer`，失败拒绝入库；非图片只做魔数校验。
- **落地**：迭代 5 用纯 JDK ImageIO 实现 `ImageSanitizer`（无新依赖），接入 `QiniuUtil#saveFile` 与 `LocalUtil#saveFile`：魔数嗅探 → 像素上限 → EXIF 方向纠正 → 重绘去元数据 → 再编码，失败拒绝入库。详见「迭代 5 落地与运行时验收」。

#### F17｜派生变体缺失（thumb/card/hero）｜已落地（迭代 4，七牛 URL 变体）

- **Evidence**：`file_info` 无变体表/字段；[QiniuUtil.java:559-584](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/infra/storage/QiniuUtil.java#L559-L584) `submitPfop` 无调用方；列表与详情只返回单一 `visitPath`。
- **Expected**：上传后异步生成 3 档（thumb 320 / card 800 / hero 1600），详情返回 `variants{thumb,card,hero}`；库内只留原图与 `file_hash`，变体走 CDN 变换或对象前缀，不重复登记。
- **Impact**：列表页被迫加载原图；后续换 CDN 需回刷全量 URL。
- **最小修复**：七牛用 `imageView2` URL 变体（零额外存储）；本地用 Thumbnailator 生成 `_thumb/_card/_hero`；`detail` 统一返回 `variants`，前端不再拼域名。
- **口径**：属内部能力，不占接口数；审计后原方案的 `POST /file/derive` 已删除。
- **落地**：迭代 4 在 `detail` 返回 `variants{thumb,card,hero}`（七牛 `imageView2/2/w/320|800|1600`），本地存储与 gif 降级为原图；库内仍只存原图与 `file_hash`。

### 与参考文章的接口口径对照

| 文章能力 | 本项目落点 | 状态 |
| --- | --- | --- |
| 图片列表 | `POST /file/admin/list` | 有端点，契约已修（迭代 0） |
| 图片详情 | `GET /file/detail/{id}`（支持 HEAD） | 已建（迭代 4：HEAD + ETag + 派生变体） |
| 上传 | `getUpToken` / `uploadToken` / `admin/upload` + `save` | 已建（迭代 1 服务端 Key） |
| 引用查询 | `POST /file/ref/list`（按 fileId 或 bizType+bizId） | 已建（迭代 2） |
| 删除（force） | `POST /file/admin/delete?force=` | 已建（迭代 2） |
| 回收站列表 | `POST /file/recycle/list` | 已建（迭代 2） |
| 恢复 | `POST /file/recycle/restore` | 已建（迭代 2） |
| 未引用清单 | `POST /file/unreferenced/list` | 已建（迭代 3） |
| 批量清理 | `POST /file/batch`（≤100，`dryRun`） | 已建（迭代 3） |
| 孤儿扫描 | `POST /file/orphan/scan`（`dryRun`） | 已建（迭代 3） |
| 补登记 | `POST /file/orphan/backfill` | 已建（迭代 3） |

口径汇总：文章 **11**（原 15：删除与永久删除合并为 `force`、批量删除与孤儿清理合并、重复检测并入登记、派生改 URL 变体）→ 本项目 **17**（5 现有 + 12 新增），迭代 7 因卡片需要真实编辑入口再加 1 个 `update`，迭代 8 因 C 端选图需要「我的图片」列表再加 1 个 `my/list`，合计 **19**。已落地 19 个（5 现有 + 迭代 1 的 2 + 迭代 2 的 5 + 迭代 3 的 4 + 迭代 4 的 1 + 迭代 7 的 1 + 迭代 8 的 1），无待建项；迭代 8 起前端已接入其中 15 个端点。

### 分迭代路线

| 迭代 | 内容 | 接口增量 |
| --- | --- | --- |
| 0 打通登记 | `file_hash` 可空、`bucket` 默认 `''`、`owner_id` 可空；`save` 补 `ownerId/ownerType/bucket/thirdPartyName`；`list` 改 `{list,total}`；`delete` 改 `@RequestBody` | 0（现有 5 个） |
| 1 安全 | 服务端 key + `insertOnly`/`mimeLimit`/`returnBody`；`GET /file/config` | +2 |
| 2 引用与回收站 | ref 绑定/解绑/查询；`recycle/list`、`recycle/restore`；`delete` 加 `force` | +5 |
| 3 治理 | `orphan/scan`、`orphan/backfill`、`unreferenced/list`、`batch` | +4 |
| 4 媒体库 | `detail`（+HEAD）、异步变体（F17） | +1 |
| 内部能力 | F15 压缩、F16 重绘、F17 变体 | 0 |
| 7 页面拆分与编辑 | 三页拆分、卡片视图；`POST /file/admin/update`（仅文件名/场景） | +1 |
| 8 选图容器 | 文章封面/logo/头像可选图；`POST /file/my/list`（C 端可用） | +1 |

落地进度：迭代 0–8 已完成（`/file` 端点 19 个，迭代 5、6 不新增接口，迭代 7 新增 `update`，迭代 8 新增 `my/list`）；F17 以七牛 URL 变体落地，F15（上传前压缩）与 F16（服务端重绘）已在迭代 5 落地，迭代 6 把详情/引用/回收站/治理/批量接到前端页面并完成 `resource`→`file` 命名归一，迭代 7 把文件库/回收站/治理拆成三个二级菜单页面、改用卡片网格并为卡片补上可落库的编辑入口，迭代 8 把“选图”从“只能手填 URL”升级为通用容器并接入三处业务表单。

### 契约修订（不改接口数，实现时必须带上）

| # | 修订 | 依据 |
| --- | --- | --- |
| 1 | `delete` 带 `force`；默认只写 `deleted_at` | WP `DELETE /wp/v2/media/<id>` |
| 2 | `save`/`uploadToken`/`batch` 支持 `Idempotency-Key`（Redis 存 24h 响应） | MDN Idempotency-Key |
| 3 | `uploadToken` 返回服务端 `key` + `fsizeLimit` + `mimeLimit` + `insertOnly` + 期望 `sha256` | S3 presigned + ChecksumSHA256 |
| 4 | `batch` 单次 ≤100、支持 `dryRun`、返回逐条 `{ok, skipped, reason}` | Cloudinary Delete resources / Sonatype |
| 5 | `list` 返回 `{list,total,page,pageSize}`，删掉 `records` 错位 | Strapi `/upload/files/page` |
| 6 | `detail` 支持 `HEAD`，返回 `ETag=fileHash` 与 `Cache-Control` | Strapi `/upload/files/:id` / S3 HeadObject |
| 7 | 派生 URL 由 CDN 变换拼接，库内只存原图 + 一份 `file_hash`；变体由 F17 提供 | Cloudinary 变换 URL |
| 8 | `bind` 必填 `bizType`/`bizId`/`bizField`，唯一键含 `biz_field` | Strapi `ref`/`refId`/`field` |
| 9 | 错误语义：404 未登记、409 引用中禁删、412 校验和不符、429 限流 | Strapi / S3 |
| 10 | 未登记的暂存对象 24h 定时清理（任务，非接口） | S3 multipart abort / lifecycle |
| 11 | F15 压缩（上传前）、F16 重绘（入库前）作为流水线阶段实现，**不新增端点** | 内部能力口径 |

落地状态：修订 4 已在迭代 3 落实（`batch` ≤100 + `dryRun` + 逐条 `{ok,skipped,reason}`）；修订 6 与 7 已在迭代 4 落实（`detail` 支持 HEAD、返回 `ETag=fileHash` 与 `Cache-Control`，三档变体由七牛 `imageView2` URL 拼接，库内不存派生文件）；修订 9 只覆盖 404/409（412 校验和、429 限流未做）；修订 10 的「未登记暂存对象 24h 定时清理」仍未实现，迭代 3 只提供手动 `orphan/scan` + `orphan/backfill`。

### 结论与限制

- 当前不是「功能不全」，而是「登记链路从未成功」：`file_info` 0 行是硬证据，F1/F2 未修之前，引用、回收站、孤儿治理都没有可作用的对象。
- F15–F17 不占接口数，但决定存储成本、首屏速度与上传安全；F17 已在迭代 4 以七牛 URL 变体落地，F15/F16 已在迭代 5 以「前端压缩 + 服务端重绘」落地。
- 本小节 F1–F17 结论来自源码与数据库元数据；迭代 0–5 的登记/列表/删除/引用/回收站/治理/详情与上传压缩/重绘已在 2026-10-26 补跑运行时验收（见下节），F3 的 `force` 已在迭代 2 验证，F15/F16 在迭代 5 以本地存储中转链验证；迭代 6 补跑前端页面链路（详情抽屉、回收站恢复、未引用清单、批量预演）。

### 迭代 0 落地与运行时验收（2026-10-26）

分支 `fix/file-registration-bootstrap`（后端自 `main@67f7d68`，前端自 `main@bcd56f2`）。

数据库（`192.168.30.100:3309/qingsi`，执行前已建 `file_info_bak_1026`/`file_biz_ref_bak_1026`）8 条 ALTER：`file_hash` 可空、`bucket` `varchar(63) DEFAULT ''`、`owner_id` 可空、`original_name` 255、`stored_name` 128、`storage_path` 255、新增 `scene varchar(32) DEFAULT 'unknown'`、新增 `idx_scene_owner(scene,owner_id,status)`。表仍为 0 行。

代码改动：后端新增 `FileSceneEnum`（avatar/moment/comment 可 C 端上传），`FileInfoService#register(vo, ownerId, ownerType)` 统一派生 `storedName/fileExt/fileCategory/bucket/scene/status/refCount/tenantId`；`/file/register` 落 `FileController`（`scope=EITHER`，拥有者强制取自会话），`/file/admin/save` 保留 ADMIN；`list` 返回 `{list,total,page,pageSize}` 并支持 `scene/keyword` 过滤；`delete` 同时接受 JSON 体 `{id}` 与 query。前端 `upload.ts` 按会话域分流（后台 → `/file/admin/save`，C 端 → `/file/register`）并按前缀映射 scene，管理端资源列表字段改按 `file_info` 口径（ownerId/scene/storagePath/fileSize/thirdPartyName）。

运行时验收（后端 `spring-boot:run` 18081，前端 `vite --port 5173`，`regularuser` userType=2、`Joy` userType=0）：

| # | 操作 | 结果 |
| --- | --- | --- |
| 1 | C 端 `POST /file/register` scene=avatar | `code 200`；库内 `owner_id=3 owner_type=user scene=avatar status=1 ref_count=0 bucket=out-pb-bkt file_hash=null` |
| 2 | C 端同端点 scene=article-cover | `code 500 该场景不允许当前账号上传`，未落库 |
| 3 | C 端同端点缺 scene | 同上被拒（unknown 不在白名单） |
| 4 | WEB token 调 `/file/admin/list` | HTTP 403 `当前会话不能用于此入口` |
| 5 | 后台 `POST /file/admin/save` scene=article-cover | `code 200`；库内 `owner_id=1 owner_type=admin` |
| 6 | 后台 `/file/admin/list` | `{list,total:1,page:1,pageSize:10}`，`records` 已消失 |
| 7 | `pageNum/pageSize`+`scene` 过滤 | `total=1 page=1 pageSize=1` |
| 8 | 后台 `/file/admin/delete` JSON 体 | `code 200`，`is_del=1`，存活行 0 |
| 9 | 浏览器 `/admin/resourceList`（注入 admin 会话） | 表头=ID/名称/拥有者ID/场景/状态/路径/大小/MIME/存储平台/创建时间；行渲染 `2 accept-admin.png 1 article-cover 启用 ... 七牛云`；切场景=用户头像后 Total 0；点删除确认后 Total 0 |

验收后已物理删除两条探针（`file_info`、`file_biz_ref` 均 0 行）。

未覆盖：七牛直传（`insertOnly`/`mimeLimit`/`fsizeLimit`）与真实 `file_hash` 计算仍空；F15–F17 未实现；前端 `PathList.vue` 仍调已不存在的 `/file/list`（控制台 404，存量问题）；C 端未知前缀会因 `scene=unknown` 被拒（与改造前“不登记”一致）。

### 迭代 1 落地与运行时验收（2026-10-26，安全：服务端 Key + 上传约束 + SHA256）

接口数 5 → 7：新增 `POST /file/uploadToken`、`GET /file/config`。

Key 规则：`{scene}/{ownerId}/{yyyyMM}/{ulid}.{ext}`。ULID 为 26 位 Crockford Base32（前 10 位毫秒时间戳，同前缀内按时间有序）；扩展名取原始文件名，缺省按 MIME 映射并清洗为 `[a-z0-9]{1,8}`，避免中文/特殊后缀与路径穿越。

凭证策略：`fsizeLimit`（`upload.fsize-limit`，默认 20MB）、`mimeLimit`（MIME 白名单：png/jpeg/gif/webp/avif/bmp、mp4/webm/quicktime、mpeg/wav/ogg/mp4、pdf、text/plain，拒绝 html/svg）、`insertOnly=1`、`returnBody`（key/etag/fsize/mimeType）、有效期 `upload.token-ttl`（默认 3600s）。旧 `GET /qiniu/getUpToken` 保留（60s、无策略），前端上传链不再使用。

场景与归属：`FileSceneEnum.of()` 精确匹配，未知场景在申请凭证阶段即拒绝（不再落 `unknown`）；C 端仅 avatar/moment/comment，站长/管理员可申请任意已登记场景。

`file_hash`：凭证返回 `hashAlgo=sha256`，前端上传前用 Web Crypto 计算（无 `crypto.subtle` 时省略，服务端容忍），登记时写入 `fileHash`（`@Pattern` 64 位十六进制，服务层统一小写）。

配置下发：`GET /file/config` 匿名可读，返回 `storeType/qiniuEnabled/uploadUrl/downloadUrl/fsizeLimit/clientScenes/hashAlgo/tokenTtl`。

运行时验收（后端 18081 dev，`regularuser` userType=2；DB `192.168.30.100:3309/qingsi`）：

| # | 操作 | 结果 |
| --- | --- | --- |
| 1 | `GET /file/config` 匿名 | `200`；`storeType=qiniu qiniuEnabled=true fsizeLimit=20971520 hashAlgo=sha256 ttl=3600 scenes=avatar,moment,comment downloadUrl=https://img.soberup.top/` |
| 2 | `POST /file/uploadToken` 无 token | `401 登录已失效，请重新登录` |
| 3 | C 端申请 avatar 凭证 | `200`；`key=avatar/3/202610/01M5T14BH8RCPAQHEVWGBN771V.png`，token 414B，`insertOnly=1 mimeLimit=image/png` |
| 4 | 解码凭证 putPolicy | `{"insertOnly":1,"scope":"out-pb-bkt:avatar/3/202610/…png","mimeLimit":"image/png","returnBody":"{…$(etag)…}","fsizeLimit":20971520,"deadline":1792952310}` |
| 5 | C 端申请 article-cover 凭证 | `code 500 该场景不允许当前账号上传` |
| 6 | 申请 `text/html` 凭证 | `code 500 不支持的文件类型：text/html` |
| 7 | 申请 30MB 凭证 | `code 500 文件大小超过 20MB 限制` |
| 8 | `POST /file/register` 携带 sha256("abc") | `200`；响应 `fileHash=ba7816…15ad`，库内 `owner_id=3 owner_type=user scene=avatar file_hash=ba7816…15ad status=1` |
| 9 | `POST /file/register` 携带 `fileHash="forged"` | HTTP `400`（`@Valid` 拒绝） |
| 10 | `/v3/api-docs` | `/file/*` 共 7 条；`FileCreateReqVO` 含 `fileHash`，`FileUploadTokenRespVO`/`FileConfigVO` 字段齐全 |
| 11 | 清理 | 探针物理删除，`file_info` 0 行；18081 已停止 |

代码级验证：后端 `mvn ... "-Dtest=FileAdminHttpContractTest,FileRegisterHttpContractTest,FileUploadHttpContractTest,FileKeyGeneratorTest" -DskipTests=false test` → **49 条全绿**（28+5+9+7）；前端 `npx vitest run` → **45 条通过**，`npx vue-tsc --noEmit` → EXIT=0。

未覆盖：真实七牛直传未执行，策略字段来自凭证解码，不代表 bucket 侧一定接受；`qiniu.uploadUrl` 未配置，`/file/config.uploadUrl` 为空，前端仍以 webInfo 的上传域名调用；`file_hash` 未与对象 ETag 比对（412 校验和语义未实现，七牛 etag 与 sha256 算法不同）；本地存储分支仍只上传不登记 `file_info`；F15–F17 未实现。

### 迭代 2 落地与运行时验收（2026-10-26，引用与回收站）

接口数 7 → 12：新增 `POST /file/ref/bind`、`POST /file/ref/unbind`、`POST /file/ref/list`、`POST /file/recycle/list`、`POST /file/recycle/restore`；`POST /file/admin/delete` 增加 `force` 语义（不新增路径）。列表接口均为 `POST` + 请求体，与模块内 `admin/list` 口径一致；列表响应统一 `{list,total,page,pageSize}`。

数据库：`file_biz_ref` 增加 `biz_field`（默认 `''`），唯一键由 `(file_id,biz_id,biz_type)` 改为 `(file_id,biz_type,biz_id,biz_field)`，使同一业务对象的多个挂载点（`cover`/`content` 等）可分别登记；`file_info` 增加 `deleted_at` 与 `idx_recycle(is_del,deleted_at)`，回收站按进入时间倒序。

引用语义：绑定幂等（同键重复请求返回既有记录、不重复计数），首次绑定 `ref_count+1`；解绑物理删除引用行（唯一键不含 `is_del`，逻辑删除会让同一引用无法再绑定），`ref_count-1` 下限 0，重复解绑返回 `data=false`；引用只能挂到未删除文件，非管理员只能操作自己拥有的文件，按 `bizType+bizId` 查询仅管理员可用。

删除语义：默认软删除（`is_del=1` + `deleted_at`），引用中的文件允许进回收站；`force=true` 先查 `ref_count`，`>0` 返回 `409` 并给出引用数，`=0` 时先删存储对象（七牛 `deleteFile`，失败只告警不阻断元数据删除）再物理删除行；回收站文件不可再绑定引用。

运行时验收（后端 18081 dev，`regularuser` userType=2、admin `Joy`；DB `192.168.30.100:3309/qingsi`）：

| # | 操作 | 结果 |
| --- | --- | --- |
| 1 | 双会话登录 | `200`；admin `Joy` 与 web `regularuser` 均取得 token |
| 2 | 申请 avatar 凭证并登记探针 | `200`；`key=avatar/3/202610/01M5T1VCQVNT4HPMRWHNVTC0E0.png`，`id=5` |
| 3 | `POST /file/ref/bind`（bizType=2,bizId=77001,bizField=cover）首次 + 重复 | 均 `200`；`refId=2` 两次一致，未重复建行 |
| 4 | 绑定后查库 | `ref_count=1 is_del=0 deleted_at=null`；`file_biz_ref` 行 `biz_type=2 biz_id=77001 biz_field=cover` |
| 5 | `ref/list` byFileId / C 端按业务维度 / 管理员按业务维度 | `200 total=1 page=1 pageSize=10`；C 端 `500 仅管理员可按业务维度查询引用`；管理员 `200 total=1` |
| 6 | `delete?force=true` 引用中 | `code 409 文件仍被 1 处引用，禁止永久删除`，行未被删 |
| 7 | `delete`（默认）+ `recycle/list` | `200`；库内 `is_del=1 deleted_at=2026-10-26 01:31:15.978`；回收站 `total=1` 且含探针 |
| 8 | 对回收站文件 `ref/bind` | `code 500 文件不存在或已删除` |
| 9 | `recycle/restore` | `200`；库内 `ref_count=1 is_del=0 deleted_at=null` |
| 10 | `ref/unbind` 首次 + 重复 | `data=true` / `data=false`；库内 `ref_count=0 refs=0` |
| 11 | `delete?force=true` 无引用 | `200`；回收站不再含探针；库内 `file_rows=0 ref_rows=0` |
| 12 | `/v3/api-docs` | `/file/*` 共 12 条，含 `ref/bind|unbind|list` 与 `recycle/list|restore` |
| 13 | 清理 | 探针 0 行，18081 已停止 |

代码级验证：后端 `mvn ... "-DskipTests=false" -Dtest="File*"` → **80 条全绿**（31+6+13+5+9+5+4+7）。新增 `FileRefHttpContractTest` 13 条（幂等绑定、跨用户拦截、未知 bizType 先拒绝、回收站文件拒绝、标准列表信封、作用域）、`FileRecycleHttpContractTest` 6 条（列表口径、管理员作用域、恢复与 404）、`FileInfoStoragePurgeTest` 4 条（先清存储后删元数据、存储失败不阻断、未知存储类型/空路径跳过）；`FileAdminHttpContractTest` 补 3 条 force 语义；`FileConvertTest` 随 `FileRespVO` 新增 `fileHash`/`scene` 同步期望。

未覆盖：前端媒体库/引用面板尚未接这 5 个接口，本轮只交付服务端契约；引用计数由服务层增减，若业务侧直接删文章而不调 `unbind`，会残留引用与虚高 `ref_count`，需迭代 3 的孤儿扫描兜底；未引入 `Idempotency-Key`（契约修订 2），幂等靠唯一键与状态判断；回收站无自动过期清理，`deleted_at` 只作排序与后续清理依据；真实七牛对象删除只验证了“不存在的 key 不阻断”（日志无“清理存储对象失败”），未验证已存在对象的删除。

### 迭代 3 落地与运行时验收（2026-10-26，治理：孤儿、未引用、批量）

接口数 12 → 16：新增 `POST /file/orphan/scan`、`POST /file/orphan/backfill`、`POST /file/unreferenced/list`、`POST /file/batch`，全部 `@LoginCheck(value=1, scope=ADMIN)`。本轮无 DDL，只新增 `QiniuUtil#listObjects`（翻页列举裸 key）与 `file_info.storage_path` 的批量存在性查询。

孤儿扫描：按裸 key 前缀列举七牛对象（单次上限 5000，多取 1 条判断 `truncated`），用 `storage_path IN (...)`（500 一批，含回收站行）比对登记表，返回 `{dryRun,storeType,prefix,scanned,registered,orphanCount,truncated,list[{key,size,mimeType,putTime}]}`；只读，不写库。以 `/` 结尾的目录占位对象（七牛控制台生成的 0 字节 `application/qiniu-object-manager`）不计入 scanned/orphan，也不可补登记。

补登记：key 可为裸 key 或 CDN 完整地址，服务端去域名、去重、单次 ≤100；先按 `storage_path` 判重（含回收站），再批量 `stat` 取 size/mimeType；目录占位、存储中不存在、已登记均逐条 `{ok:false,skipped:true,reason}`；默认 `dryRun=true`，关闭后才以 `ownerId=null / ownerType=system / thirdPartyName=3` 落库，场景取 key 首段（未登记场景落 `unknown`），单条失败不阻断其余。

未引用清单：`ref_count=0`（含 NULL）、`status=1`、`create_time <= now - days`，按 `create_time` 升序；`days` 默认 7、`0` 表示不设时间下限；返回 `{list,total,page,pageSize,days}`。

批量清理：`ids` ≤100、去重保序；逐条返回 `{id,ok,skipped,reason,action}`；引用中一律跳过（`force` 也不放行），`force=false` 软删（已在回收站跳过），`force=true` 物理删（可清理回收站行）；`dryRun` 默认 true。

运行时验收（后端 18081 dev，admin `Joy`、web `regularuser`；DB `192.168.30.100:3309/qingsi`，起始两表 0 行）：

| # | 操作 | 结果 |
| --- | --- | --- |
| 1 | 双会话登录 + 起始计数 | `file_rows=0 ref_rows=0` |
| 2 | `orphan/scan`（prefix=`qingsi/emoji/`, limit=20） | `200 scanned=20 orphan=20 truncated=true`；目标 `qingsi/emoji/q1.gif`（目录占位已过滤） |
| 3 | 同 key `orphan/backfill` `dryRun=true` | `registered=0`，逐条 `reason=dryRun scene=unknown`，库内未新增 |
| 4 | 同 key `dryRun=false` | `registered=1 fileId=9`；库内 `owner_id=null owner_type=system file_size=115994 mime_type=image/gif ref_count=0 is_del=0` |
| 5 | 重复补登记 | `registered=0 reason=已登记`，未重复建行 |
| 6 | 登记探针 B/C（avatar） | `id=10`、`id=11`，Key 均为 `avatar/3/202610/*.png` |
| 7 | C 的 `create_time` 回拨 30 天后 `unreferenced/list` | `days=7 total=1` 含 C、不含 B；`days=0 total=3` 含 B/C/补登记行 |
| 8 | `ref/bind` B（bizType=2,bizId=88001,bizField=cover） | `200 refId=4`；库内 `ref_count=1` |
| 9 | `batch` `dryRun=true` ids=[B,C] | `deleted=1 skipped=1`；B `文件仍被 1 处引用`、C `soft-delete`；库内两者 `is_del=0` |
| 10 | `batch` `dryRun=false` ids=[B,C] | `deleted=1 skipped=1`；C `is_del=1 deleted_at=2026-10-26 01:54:30.226`，B 未动 |
| 11 | `batch` `force=true` 分别 C/B | C `deleted=1`；B `skipped=1 文件仍被 1 处引用`；库内 `c_rows=0 b_rows=1` |
| 12 | `ref/unbind` B 后 `batch force` B | `data=true`；`deleted=1`；`b_rows=0 b_refs=0` |
| 13 | 清理补登记行（SQL 只删元数据） | `file_rows=0 ref_rows=0`，七牛对象未删 |
| 14 | `/v3/api-docs` | `/file/*` 共 16 条，含 `orphan/scan|backfill`、`unreferenced/list`、`batch` |

代码级验证：后端 `mvn -o -B -pl qsblog-server test "-DskipTests=false" -Dtest="File*"` → **107 条全绿**（31+12+6+13+5+9+5+15+4+7）。新增 `FileGovernanceHttpContractTest` 12 条（扫描信封、补登记非空/≤100、未引用 `{list,total,page,pageSize,days}`、批量逐条结果与默认 `dryRun`、ADMIN 作用域）、`FileGovernanceServiceTest` 15 条（截断与目录占位过滤、非七牛拒绝、补登记幂等/缺对象/CDN 归一化、未引用过滤、`dryRun` 不写、引用保护与回收站跳过、ID 去重）。

未覆盖：前端媒体库尚未接这 4 个接口；扫描依赖 bucket 列举，5000 条以上需按前缀分批，未做游标续扫；只比对 `storage_path`，无法发现“已登记但对象被删”的反向孤儿；补登记以 `system` 归属且不计算 `file_hash`（七牛 etag 非 SHA256）；契约修订 10 的“未登记暂存对象 24h 定时清理”仍是手动治理，未加定时任务；修订 9 的 412/429 仍未实现。

### 迭代 4 落地与运行时验收（2026-10-26，媒体库：详情 + ETag + 派生变体）

接口数 16 → 17：新增 `GET /file/detail/{id}`（`@LoginCheck(value=1, scope=ADMIN)`），同一映射由 Spring 隐式响应 HEAD 并丢弃响应体。本轮无 DDL，无新增表字段。

详情字段：在 `FileRespVO` 之上补 `ownerId/ownerType/bucket/refCount/status/createTime/deletedAt/visitPath/etag/variantMode/variants`。`visitPath` 按「`cdnDomain` 优先，其次按 `thirdPartyName` 选七牛/本地下载域名」+ 裸 key 拼接，前缀统一补末尾 `/`；`etag` 取小写 `file_hash`，为空时不下发 ETag 头；未登记或已进回收站返回 `code 404`（HTTP 200，与模块内其他端点信封一致）。

条件请求与缓存：成功响应带 `ETag="<sha256>"` 与 `Cache-Control: public, max-age=300`（`file.detail.cache-control` 可配）；`If-None-Match` 支持强/弱校验与 `*` 通配，命中返回 `304`（仍带 ETag 与缓存头）。

派生变体（F17）：仅七牛 + `image/*` + 非 gif 时 `variantMode=transform`，`variants{thumb,card,hero}` 为 `visitPath + ?imageView2/2/w/{320/800/1600}/q/85`（key 已带 `?` 时改用 `&`）；本地存储、非图片与 gif 返回 `variantMode=none`、`variants=null`，不落派生文件。

运行时验收（后端 18081 dev，admin `Joy`；DB `192.168.30.100:3309/qingsi`，起始两表 0 行）：

| # | 操作 | 结果 |
| --- | --- | --- |
| 1 | 起始计数 | `file_rows=0 ref_rows=0` |
| 2 | `admin/save` 图片探针（`fileHash=sha256("abc")`） | `code 200 id=12`；库内 `owner_id=1 owner_type=admin file_hash=ba7816…15ad third_party_name=3 scene=avatar` |
| 3 | `GET /file/detail/12` | `200`；`visitPath=https://img.soberup.top/avatar/1/202610/m5-probe-a.png`，`variantMode=transform`，thumb/card/hero 分别为 `imageView2` 的 `w/320`、`w/800`、`w/1600`（`q/85`），`etag=ba7816…15ad` |
| 4 | 响应头 | `ETag="ba7816…15ad"`、`Cache-Control=public, max-age=300` |
| 5 | `HEAD /file/detail/12` | `200`，ETag 与缓存头同上，响应体长度 `0` |
| 6 | `If-None-Match` 命中 / `*` / `"stale"` | `304` / `304` / `200` |
| 7 | 登记 pdf 探针后查详情 | `code 200 variantMode=none variants=null` |
| 8 | `GET /file/detail/999999` | `code 404 文件不存在`，无 ETag 头 |
| 9 | 软删探针后查详情 → 恢复 | `code 404`；`restore` 后 `code 200` |
| 10 | `force` 清理两探针 | `200/200`；`file_rows=0 ref_rows=0` |
| 11 | `/v3/api-docs` | `/file/*` 共 17 条，含 `detail/{id}` |

代码级验证：`mvn -o -B -pl qsblog-server test "-DskipTests=false" -Dtest="File*"` → **126 条全绿**（107 + 19）。新增 `FileMediaServiceTest` 10 条（三档宽度、gif/非图片/七牛关闭降级、本地域名、`cdnDomain` 优先、query 拼接、空 hash、回收站与不存在）、`FileDetailHttpContractTest` 9 条（详情信封、ETag 与缓存头、强/弱/通配 304、过期 ETag 返回全量、HEAD 无体、404 信封、ADMIN 作用域）。

未覆盖：`detail` 仅对后台会话开放，C 端读详情尚未定义；变体只拼 URL，未在浏览器验证七牛 `imageView2` 的真实返回（探针 key 本身无对象，公开读策略也未校验）；本地存储的物理派生（Thumbnailator `_thumb/_card/_hero`）未实现，`variantMode=none` 是显式降级；ETag 由 `file_hash` 提供，未与七牛 ETag 比对，412 语义仍未实现。

### 迭代 5 落地与运行时验收（2026-10-26，F15 上传前压缩 + F16 服务端重绘）

接口数不变（仍 17 个），本轮只改上传流水线，无 DDL、无新增表字段。

F15 前端压缩：新增 [compressImage.ts](file:///E:/web/qs-web/src/utils/image/compressImage.ts)，`uploadMedia` 在申请上传凭证之前调用。`createImageBitmap(file, {imageOrientation:'from-image'})` 按 EXIF 方向解码 → 长边等比缩到 ≤2560 → `OffscreenCanvas.convertToBlob` 优先 WebP（`q=0.85`），内核不支持时 PNG 源保 PNG、其余回退 JPEG → 压缩后 ≥95% 原体积则判 `no-gain` 保留原文件。gif/svg、非图片、小于 200KB、无 `createImageBitmap`/canvas 时直接跳过，任何异常都降级原图。`mimeType`/`fileSize`/`originalName`/SHA-256/`FormData`/`file_info` 登记全部改用压缩后的文件，避免哈希与对象不一致。`UploadPicture.vue` 新增 `onCompressed` 体积提示与 `beforeUpload` 非图片拦截。

F16 服务端重绘：新增 [ImageSanitizer.java](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/infra/storage/ImageSanitizer.java)、`SanitizedImage` 与 `ExifOrientation`（纯 JDK ImageIO，未引入 Thumbnailator 依赖，离线可构建）。流程：魔数嗅探（JPEG/PNG/GIF/WEBP/BMP/AVIF/PDF/MP4/WEBM/OGG/WAV/MP3/ZIP）→ 声明 `image/*` 或图片扩展名但内容不是图片则拒绝 → 读尺寸校验像素上限（`upload.image.max-pixels`，默认 4000 万，不解码全图）→ 解码 → 按 EXIF Orientation 1–8 像素级纠正 → 重绘到全新 `BufferedImage`（丢弃 EXIF/GPS/ICC）→ 按原格式再编码（JPEG 质量 `upload.image.quality`，默认 0.85）。gif 保留原字节以维持动图；WEBP/AVIF 在 JDK 无解码器时保留原字节；BMP 归一为 PNG。`QiniuUtil#saveFile` 与 `LocalUtil#saveFile` 均先净化再写存储，扩展名随真实类型重建 Key，失败直接拒绝入库；`LocalUtil` 同时修掉目录尾斜杠被误判（前端按服务端 Key 拆出的 `cover/22/202610/` 之前必抛“路径不合法”），改为目录归一后逐段校验并 `writeBytes` 落盘。

运行时验收（后端 18081 dev + `SPRING_APPLICATION_JSON` 覆盖为本地存储 `temp/upload`，admin `Joy`）：

| # | 操作 | 结果 |
| --- | --- | --- |
| 1 | 管理员登录 | OK |
| 2 | 生成 4000×3000 JPEG 并注入 EXIF `Orientation=6` | `bytes=235998`，含 `Exif` 段 |
| 3 | `POST /file/admin/upload`（`storeType=local`，`relativePath=m6/1/202610/`） | `code 200`，`visitPath=http://localhost:18081/files/m6/1/202610/m6-rotated.jpg` |
| 4 | 落盘文件 | `bytes=226538`，尺寸 `3000x4000`（方向已纠正），字节中无 `Exif` |
| 5 | 伪造 PNG（内容 `%PDF-1.4`） | `code 500 文件内容与图片类型不符，已拒绝上传`，未落盘 |
| 6 | `relativePath=../evil/` | `code 500 文件路径不合法！` |
| 7 | PDF 上传 | `code 200`，落盘字节与原文件一致（非图片只做魔数校验） |
| 8 | 同 Key 重复上传 | `code 500 文件已存在！` |

代码级验证：后端 `mvn -o -B -pl qsblog-server test "-DskipTests=false" -Dtest="File*,ImageSanitizerTest,LocalUtilTest"` → **144 条全绿**（文件模块 126 + `ImageSanitizerTest` 12 + `LocalUtilTest` 6）。`ImageSanitizerTest` 覆盖 JPEG 重绘去 EXIF、`Orientation=6` 宽高交换、Orientation=1 不变、伪造图片拒绝（按 MIME 与按扩展名）、非图片原样、gif 原样、像素上限、BMP→PNG、路径剥离、空内容、未知类型放行；`LocalUtilTest` 覆盖尾斜杠目录、重绘落盘、目录穿越、空目录、禁止覆盖、伪造图片。前端 `pnpm test:auth` → **54 条全绿**（`session.test.ts` 45 + `compressImage.test.ts` 9），`pnpm type-check` 通过；`vitest.config.ts` 的 `include` 由 `src/auth/**` 扩为 `src/**` 以收录新测试。

未覆盖：七牛直传不经过后端，F16 只覆盖 `/file/admin/upload` 中转路径，直传链仍依赖 F15 压缩与七牛侧处理；WEBP/AVIF 无 JDK 解码器，未重绘也未剥离元数据；gif 为避免丢帧保留原字节；像素级方向纠正与重绘是同步 CPU 操作，未做异步队列或超时；前端压缩只在 jsdom 中用替身验证，未在真实移动端浏览器确认 WebP 支持与方向；拒绝以既有 `code 500 + 原因` 信封返回（HTTP 200），未改为 400。

### 迭代 6 落地与运行时验收（2026-10-26，前端接入 + 命名归一）

接口数不变（仍 17 个），本轮只改前端：后端 0 行代码、无 DDL，故后端仓库仅本报告变更。分支 `fix/file-registration-bootstrap`（前端自 `main@bcd56f2`）。

命名迁移（`resource` → `file`）。只迁「文件媒体库」链路；「资源聚合」链路对应后端 `resAgg`/`weblinks`，与 `file_info` 无关，保留原名：

| 项 | 迁移前 | 迁移后 |
| --- | --- | --- |
| 页面 | `views/admin/resource/List.vue` | [views/admin/file/List.vue](file:///E:/web/qs-web/src/views/admin/file/List.vue)（旧文件已删除） |
| 路由 | `resourceList`（`/admin/resourceList`） | `fileList`（`/admin/fileList`） |
| 菜单 | 「资源管理」 | 「文件管理」 |
| 页签 | `adminTabs['/admin/resourceList']` | `adminTabs['/admin/fileList']` |
| 权限白名单 | [navigation.ts](file:///E:/web/qs-web/src/auth/navigation.ts) 中 `/admin/resourceList` | `/admin/fileList` |
| API 模块 | `resource.ts`（`/file/list\|save\|delete\|update`） | [file.ts](file:///E:/web/qs-web/src/api/modules/file.ts) 12 个封装；[resource.ts](file:///E:/web/qs-web/src/api/modules/resource.ts) 只留 `ResourcePath*` 四函数 |
| 函数 | `listResource`/`deleteResource` | `listFiles`/`deleteFile` |
| 上传登记 | `uploadApi.saveResource` | `uploadApi.saveFile` |
| 类型 | `ResourceInfo`（死类型） | 删除；新增 `FileItem`/`FileDetail`/`FileVariants`/`FilePage`/引用与治理类型 |
| 保留 | `ResourcePath*`、`/admin/resourcePathList`、`views/admin/resource/PathList.vue`、`components/business/resource/Card.vue`、前台 friend/love/travel | 同左（资源聚合语义，不随本轮改名） |

前端结构：`List.vue` 为三 Tab 容器（文件库 / 回收站 / 治理），文件库侧复用 `UploadPicture`（`storeType` 取 `defaultStoreType`，默认 `qiniu`）并支持多选批量（移入回收站 / 永久删除）；新增 [FileDetailDrawer.vue](file:///E:/web/qs-web/src/views/admin/file/components/FileDetailDrawer.vue)（描述项 + 访问地址复制 + thumb/card/hero 三档变体 + 引用表）、[RecyclePanel.vue](file:///E:/web/qs-web/src/views/admin/file/components/RecyclePanel.vue)（列表 / 恢复 / 永久删除 / 分页）、[GovernancePanel.vue](file:///E:/web/qs-web/src/views/admin/file/components/GovernancePanel.vue)（孤儿扫描 / 补登记 / 未引用清单 / 批量清理）；格式化集中在 [fileFormat.ts](file:///E:/web/qs-web/src/utils/file/fileFormat.ts)。`file.ts` 的 12 个函数：`listFiles`、`getFileDetail`、`deleteFile`、`listRecycleFiles`、`restoreFile`、`scanFileOrphans`、`backfillFileOrphans`、`listUnreferencedFiles`、`batchFiles`、`listFileRefs`、`bindFileRef`、`unbindFileRef`（`uploadToken`/`config` 仍由 `upload.ts` 承担）。补登记与批量清理默认 `dryRun=true`，UI 以开关 + 动态提示区分「仅预演 / 真实执行」「移入回收站 / 永久删除」。

运行时验收（后端 18081 dev + 前端 5173，`VITE_BASE_URL=http://localhost:18081`，admin `Joy`）：

| # | 操作 | 结果 |
| --- | --- | --- |
| 1 | 登录后进入 `/admin/fileList` | 菜单「文件管理」、页签标题、三 Tab、表头（预览/名称/场景/大小/MIME/存储/引用/状态/时间/操作）正常 |
| 2 | 登记探针 A(id=14)、B(id=15)，仅写 `file_info` 不写对象；`ref/bind` 绑定 A（`bizType=2`/`bizId=1`/`bizField=cover`） | 列表 Total 1，引用列显示 1 |
| 3 | 打开 A 的详情抽屉 | `ETag`、`visitPath`、`variantMode=transform`、三档变体、引用表 1 行（`bizType=2`/`bizId=1`/`cover`）均渲染 |
| 4 | B 软删（`force=false`）→ 回收站恢复 | 回收站 Total 1 → Total 0；文件库 Total 2 |
| 5 | 治理「未引用」`days=0` | Total 1（B，`refCount=0`） |
| 6 | 治理批量清理 `dryRun` | 逐条返回 `action=soft-delete`，数据未变 |
| 7 | 解绑 A + 两条 `force` 永久删除 | `keyword=ui-verify` 残留 0，两表回到 0 行 |

代码级验证：前端 `pnpm test:auth` → **66 条全绿**（`session.test.ts` 45 + `compressImage.test.ts` 9 + 新增 `file.test.ts` 12），`pnpm type-check` 通过。`file.test.ts` 覆盖 URL/参数透传、软删与硬删、回收站列表与恢复、扫描默认值、补登记 `dryRun` 切换、批量默认与永久、ref 查询/绑定/解绑、失败透传。

未覆盖：孤儿扫描未在页面点击（仅 API 单测与后端契约测试）；补登记与「永久删除」按钮未在 UI 端到端执行（永久删除由脚本直调接口验证）；上传对话框未实跑（探针走 `/file/admin/save`，无对象存储）；`resourcePath` 仍指向不存在的后端端点，本轮明确保留；真实七牛直传链路仍未执行。验收截图仅在本地 `logs/playwright-mcp/file-list-verified.png`、`file-governance-verified.png`，不入库。

### 迭代 7 落地与运行时验收（2026-10-26，三页拆分 + 卡片视图 + 卡片编辑 + 站点素材落库）

接口 +1（17 → 18）：新增 `POST /file/admin/update`，其余仅改前端 + 数据落库，无 DDL、无新增表字段。

**信息架构**：文件库 / 回收站 / 治理从同一页三个 Tab 拆为「文件管理」下三个二级菜单页面，各自独立路由、独立页签与面包屑：

| 菜单 | 路由 | 页面 |
| --- | --- | --- |
| 文件库（默认） | `/admin/file/list`（`name=fileList`） | [views/admin/file/List.vue](file:///E:/web/qs-web/src/views/admin/file/List.vue) |
| 回收站 | `/admin/file/recycle`（`fileRecycle`） | [views/admin/file/Recycle.vue](file:///E:/web/qs-web/src/views/admin/file/Recycle.vue) |
| 治理工具 | `/admin/file/governance`（`fileGovernance`） | [views/admin/file/Governance.vue](file:///E:/web/qs-web/src/views/admin/file/Governance.vue) |

- 旧地址 `/admin/fileList` 保留 `redirect → /admin/file/list`，`ADMIN_ROLES` 同时保留旧键，登录回跳与书签不 404。
- 新增 [FilePageHeader.vue](file:///E:/web/qs-web/src/views/admin/file/components/FilePageHeader.vue)（图标 + 标题 + 描述 + actions 插槽）统一三页页头；`RecyclePanel.vue`/`GovernancePanel.vue` 删除，逻辑内联进各自页面。
- [Sidebar](file:///E:/web/qs-web/src/layouts/AdminLayout/components/Sidebar/index.vue) 文件管理改 `el-sub-menu`，`default-openeds` 按当前路径自动展开；父级 `/admin/file` 不在白名单，新增 `menuVisible` 按子项判定，避免整组被隐藏。
- `adminTabs` 的 `ROUTE_TITLE_MAP` 增补三条标题（文件库/回收站/治理工具），页签与 Header 面包屑自动跟随。

**卡片视图**：文件库默认卡片网格（每行 6 列，≤1400/1200/960/680 逐级降为 5/4/3/2 列），工具栏 `el-segmented` 可切列表并记忆到 `localStorage.fileViewMode`；卡片含缩略图（4:3）、文件名、场景、大小、引用数徽标，以及**悬停才出现的左上角操作组**（详情 / 复制链接 / 编辑 / 移入回收站，26px 方块 + 半透深底白图标，`@media (hover: none)` 下常驻），选择框相应移到右上角避免重叠；选中加靛色描边并复用批量条。缩略图由新增的 `thumbUrl()`（[fileFormat.ts](file:///E:/web/qs-web/src/utils/file/fileFormat.ts)）拼七牛 `imageView2/2/w/480/q/75`，加载失败先回退原图再降级为「类型图标 + 扩展名」，表格预览列同步补 error 插槽，不再露出 `FAILED` 文本；默认分页改 20（20/40/60）。

**卡片编辑**：编辑图标必须有真实能力承载，因此后端补一个最小元信息更新入接口：[FileUpdateReqVO](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/file/controller/admin/vo/FileUpdateReqVO.java)（`id` 必填、`originalName` ≤255、`scene`）+ `FileInfoService#updateMeta` + [FileAdminController#update](file:///E:/idea/qs-blog/qsblog-server/src/main/java/io/charqe/qs/modules/file/controller/admin/FileAdminController.java)（`@LoginCheck(value = 1, scope = ADMIN)`）。只开放文件名与业务场景，存储路径/MIME/归属仍由上传链路决定；字段留空保持原值，回收站中的记录不允许编辑（先查 `is_del` 再 `updateById`，逻辑删除条件由 MyBatis-Plus 自动拼接，未命中则返回 404）。前端 `fileApi.updateFile` + 编辑对话框（文件名输入框 + 场景下拉），卡片与列表两个入口共用。

**站点素材落库**：`src/assets/file` 下 18 张位图（排除 `like.svg`、`fontawesome-webfont.svg` 与脚手架图标；后端 MIME 白名单不收 SVG）按官方链路登记——`POST /file/uploadToken` 取服务端 Key 与七牛凭证 → 直传 `up-as0.qiniup.com` → `POST /file/admin/save`，全部 `scene=system`、`thirdPartyName=3`，`file_info` id 16–33。其中 `comment.jpg`、`letter-2/3/4.jpg`、`top.jpg` 实际内容为 PNG，按文件头纠正 MIME 与扩展名后登记为 `comment.png`、`letter-2/3/4.png`、`top.png`，避免存储 Key 与内容不一致。批处理脚本在未跟踪的 `temp/register-site-assets.mjs`，幂等跳过已登记项。

运行时验收（后端 18081 dev + 前端 5173，admin `Joy`）：

| # | 操作 | 结果 |
| --- | --- | --- |
| 1 | 登录后落到 `/admin/file/list` | 侧边栏「文件管理」自动展开、`文件库` 高亮，页签与面包屑「仪表盘/文件库」正确 |
| 2 | 深链 `/admin/file/recycle` | 标题「回收站」、面包屑正确、空态「回收站为空」，0 console error |
| 3 | 侧边栏点「治理工具」 | 跳到 `/admin/file/governance`，两张卡片（孤儿扫描/未引用清单）渲染，tagbar 为 [回收站, 治理工具] |
| 4 | 访问旧地址 `/admin/fileList` | 重定向到 `/admin/file/list`，标题与高亮回落到文件库 |
| 5 | 文件库卡片视图（1920 宽） | 6 列；1366 宽降为 5 列；文件名/场景/大小/引用徽标/选中描边/批量条均正常 |
| 6 | 切「列表」视图 | 表格 ID/预览/名称/场景/大小/MIME/存储/引用/状态/时间/操作齐全，`comment.png`/`top.png` 的 MIME 为 `image/png` |
| 7 | 站点素材落库 18 张 | 全部返回 id，列表 Total 18；再次执行脚本全部 `SKIP`（幂等） |
| 8 | 缩略图加载 | 本机 `img.soberup.top` 证书链不受信任（首页既有头像/封面同样 `ERR_CERT_AUTHORITY_INVALID`），卡片与表格均降级为类型图标，无 `FAILED` 文本 |
| 9 | 卡片默认态 / 悬停态 | 默认无操作图标（元素截图确认 `opacity: 0`）；悬停后左上角出现详情/复制链接/编辑/移入回收站 4 个 26px 按钮，computed 为 `rgba(51,65,85,.72)` 底 + 白图标，选择框在右上角 |
| 10 | 卡片编辑改名并保存 | `POST /file/admin/update` 成功，对话框关闭、列表即时刷新为 `letter-2-edit.png`，重新导航后仍为新名（已落库），随后改回 `letter-2.png` |
| 11 | 列表视图编辑入口 | 操作列为详情/编辑/删除三按钮，点编辑打开同一对话框并回填当前文件名与场景 |

代码级验证：前端 `pnpm test:auth` → **72 条全绿**（`session.test.ts` 46 + `compressImage.test.ts` 9 + `file.test.ts` 13 + 新增 `navigation.test.ts` 4），`pnpm type-check` 通过；后端 `mvn -pl qsblog-server spring-boot:run` 编译并启动成功。新增用例覆盖三子路由白名单、旧地址 redirect、`safeAdminRedirect` 保留 query 与非法路径回落、`updateFile` 的路径与参数透传。

未覆盖：上传对话框未实跑（素材走「凭证 + 直传 + save」脚本）；孤儿扫描、补登记、永久删除按钮未在 UI 端到端点击；站点素材只登记未替换页面引用（页面仍用打包资源）；卡片缩略图的真实视觉未在可用 CDN 环境确认，本机仅验证降级分支；编辑未跑负面用例（超 255 字节的 `@Size` 校验、回收站记录 404 分支、非法场景值），且未提供批量改场景入口；列表视图在 1366 以下未逐档截图。

### 迭代 8 落地与运行时验收（2026-10-26，选图容器 + C 端「我的图片」+ 直传域名回归）

接口 +1（18 → 19）：新增 `POST /file/my/list`（`SessionScope.EITHER`，拥有者强制取自会话），其余为前端组件与配置修正，无 DDL。

**为什么要新接口**：选图容器要覆盖 C 端「修改头像」，而 `/file/admin/list` 是站长专属（`value=1, scope=ADMIN`），C 端会话拿不到任何列表。新增的 `my/list` 与后台列表同构（`{list,total,page,pageSize}` + `scene`/`keyword`），但 `owner_id` 由服务端按会话写入、不接受入参指定，因此普通用户只能看到自己上传过的图，不会互相翻图。

**容器**：`src/components/media/ImagePickerDialog.vue`，按参考图布局——标题栏、顶部工具栏（类型下拉 + 关键词搜索 + 搜索按钮 + **上传按钮** + 右侧「共 N 张」）、5 列卡片网格（缩略图 + 底部名称条，单选描边与勾选徽标）、居中分页（12/24/48）、底部「未选择图片 / 已选：xxx」+「取消」/「使用该图片」（未选时禁用）。`append-to-body` 使其可嵌在既有对话框内（C 端「修改头像」本身就是弹窗）。

**上传按钮的语义**：容器里没有想要的图时当场上传，走的是同一条 `uploadMedia` 链（压缩 → 凭证 → 直传 → 登记），成功后 `search()` 回到第一页，新图立即可选——不给「只能选已有的」留死角。上传前按 `maxSize`/`image/*` 拦截；本地存储只对已验证站长开放，C 端固定七牛。

**接入点**（都是在原 URL 输入框旁加一个选图入口，不删原有上传与手填 URL 能力）：

| 场景 | 页面 | 列表来源 | 上传前缀 → scene |
| --- | --- | --- | --- |
| 文章封面 | `views/admin/article/Edit.vue` | `library`（后台媒体库） | `articleCover` → `article-cover` |
| 站点头像（logo）/ 背景图片 | `views/admin/settings/WebEdit.vue` | `library`，预置筛「站点素材」 | `webAvatar`/`webBackgroundImage` → `system` |
| 用户头像 | `views/front/user/index.vue` | `mine`（我的图片） | `userAvatar` → `avatar` |

**顺带修掉的两处既有缺陷**：

1. `SCENE_BY_PREFIX` 缺 `articleCover`/`userAvatar`/`webAvatar`/`webBackgroundImage`/`randomAvatar`/`randomCover` 六项，这些页面上传的图全部落 `scene=unknown`，选图容器按场景筛选时永远看不到它们；补齐后新上传才归到 `article-cover`/`avatar`/`system`（历史数据不回改）。
2. 七牛直传域名写死 `https://upload.qiniup.com`，而 dev bucket `out-pb-bkt` 在亚洲区，浏览器上传一律 `400 incorrect region, please use up-as0.qiniup.com`（即上传按钮在本地根本不可能成功）。改为**以服务端凭证下发的 `uploadUrl` 为准**、前端常量只兜底，并在 `application-dev.yml` 补 `qiniu.uploadUrl: "https://up-as0.qiniup.com"`（prod 未配 `uploadUrl`，行为保持原样）。

验收（本地 18081 + 5173 + 真实七牛）：

| # | 步骤 | 结果 |
| --- | --- | --- |
| 1 | 文章编辑页点「从图片库选择」 | 标题「选择文章封面」，工具栏齐全（含上传按钮），共 18 张、卡片网格、分页 24/page、底部「未选择图片」且「使用该图片」禁用 |
| 2 | 选中 `letter-2.png` → 使用该图片 | 描边 + 勾选 + 「已选」文案生效，弹窗关闭，封面输入框回填 `https://img.soberup.top/system/1/...png` |
| 3 | 网站设置「站点头像」→ 选择图片 | 标题「选择站点头像」，类型下拉预置「站点素材」，18 张 |
| 4 | C 端「修改头像」→ 从我上传的图片选择 | `/file/my/list` 200，场景预置「用户头像」→ 共 0 张 + 空态提示「没有匹配的图片，可点击右上角上传」 |
| 5 | 容器内点上传并选本地图片 | `up-as0.qiniup.com` 200 → `/file/register` 200 → 列表刷新为「共 1 张」，卡片名即上传文件名 |
| 6 | 选中该图 → 使用该图片 | `/user/updateUserInfo` 200，头像换成新图；`file_info` 落库 `scene=avatar`、`storagePath=avatar/1/...` |
| 7 | 收尾 | 头像改回原值，测试文件 `force=true` 永久删除（列表检索 0 残留） |

代码级验证：`pnpm test:auth` **73 条全绿**（`file.test.ts` 14，新增 `listMyFiles` 路径与 `authScope: 'web'` 断言），`pnpm type-check` 通过；后端 `mvn -o -B -pl qsblog-server compile` 成功并启动，`GET /file/config` 返回 `uploadUrl: "https://up-as0.qiniup.com"`。

未覆盖：容器只支持单选，多选/裁剪/替换已选图未做；正文配图、评论涂鸦、弹幕、表白墙等仍在用 `UploadPicture` 直传，未接入容器；`my/list` 的可见口径（只看自己上传）未经产品确认；容器内上传的失败分支（登记失败仅告警、超限拦截）未逐条 UI 端到端跑；prod 的 `qiniu.uploadUrl` 仍缺省，若生产 bucket 也在非默认区域需另行配置。

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
- 第六轮只做 Git 收口：后端 `a59b9e9 feat(admin): 后台仅放行站长与管理员并开放全量菜单`（15 文件，14 个控制器 + 本报告）、前端 `244ebe3 feat(admin): 后台菜单对站长与管理员全量开放`（4 文件）。两仓库均以 `git merge --ff-only` 合并到 `main`，无冲突、无合并提交；`fix/sa-token-scope-regression` 与 `fix/admin-login-role-menu` 分支保留在本地。未推送：后端 `main` 相对 `github/main` ahead 6，前端 `main` 相对 `origin/main` ahead 4。未跟踪的 `temp/` 与 `scripts/auth-browser-harness.cjs` 仍未入库。
- 第七轮修复 A2：`ArticleServiceImpl` 新增 `projectForPublicList`/`toListSummary`，`listArticle` 与 `listCategoryArticle` 统一列表投影——`password`/`videoUrl` 置空，私密文章正文 >80 字截断为 80+`…`、≤80 字返回 `null`；前端 `ArticleList.vue` 对空摘要显示加密提示。探针文章与 Redis `category_article_list_v2` 缓存已清理。
- 第八轮按产品决定放开首页分类：`listCategoryArticle` 去掉 `viewStatus=true` 过滤，`CommonConst.CATEGORY_ARTICLE_LIST` 由 `v2` 升为 `v3`（语义已变，避免旧快照命中）；前端 `index.vue#getSortArticles` 去掉多余的 `.data` 解包。验证时清理了无 TTL 的陈旧 `category:public`/`category:navbar_`（缺 `TestCategory01`，导致首页分类区块无法渲染）。
- 第九轮只读审计文件模块：新增「文件模块专项审计（第九轮）」F1–F17（14 条链路缺陷 + 3 项内部能力 F15 压缩 / F16 重绘 / F17 变体），并补入文章接口口径对照（11 对 17）与五迭代路线；业务代码 0 行改动，未建表、未启动服务、未产生测试数据。
- 迭代 0（2026-10-26，`fix/file-registration-bootstrap`）：后端 8 条 ALTER + 新增 `FileSceneEnum`/`FileListReqVO`/`FileDeleteReqVO`，`FileInfoService#register` 补齐 owner/bucket/scene/status/refCount，`/file/register` 支持 C 端（EITHER），`list` 改 `{list,total,page,pageSize}`，`delete` 兼容 JSON 体；前端 `upload.ts` 按域分流登记并按前缀映射 scene，`resource/List.vue` 字段对齐 `file_info`；契约测试 31 条全绿，HTTP + DB + 浏览器验收 9 项通过，探针数据已清理。
- 迭代 1（2026-10-26，同分支）：后端新增 `FileKeyGenerator`（ULID Key）、`FileSceneEnum#of/clientCodes`、`FileUploadService`/`FileUploadServiceImpl`、`POST /file/uploadToken` 与 `GET /file/config`；`QiniuUtil#getToken` 支持 `fsizeLimit`/`mimeLimit`/`insertOnly`/`returnBody`；`FileBaseVO` 增加 `fileHash`（64 位十六进制校验）并在服务层小写化；前端 `upload.ts` 改用服务端 Key、登记前计算 SHA-256，`session.test.ts` 同步打桩。契约测试 49 条全绿，前端 45 条 vitest 与 `vue-tsc` 通过，HTTP + DB + OpenAPI 验收 11 项通过，探针已物理删除。
- 迭代 2（2026-10-26，同分支）：`file_biz_ref` 增加 `biz_field` 并调整唯一键、`file_info` 增加 `deleted_at` 与回收站索引；后端新增 `FileBizRefService#bind/unbind/pageRefs`、`FileInfoService#findAny/softDelete/restore/purge/pageDeleted`、`FileRecycleController`、`/file/ref/*`；`FileAdminController#delete` 支持 `force`（引用中 409，永久删先清存储对象）；`FileInfoStoragePurgeTest`/`FileRefHttpContractTest`/`FileRecycleHttpContractTest` 新增 23 条，文件模块合计 80 条全绿；HTTP + DB + OpenAPI 验收 13 项通过，探针永久删除后 0 残留。
- 迭代 3（2026-10-26，同分支）：新增 `QiniuUtil#listObjects`（翻页列举裸 key）与 `StorageObject`；`FileStorageMapper` 增加 `selectAnyByStoragePath`/`selectExistingPaths`，`FileInfoService` 增加 `findAnyByStoragePath`/`existingStoragePaths`；新增 `FileGovernanceService`/`Impl`、`FileGovernanceController`（`/file/orphan/scan`、`/file/orphan/backfill`、`/file/unreferenced/list`、`/file/batch`）与 7 个 VO；扫描过滤目录占位对象，补登记/批量均默认 `dryRun`，批量对引用中文件一律跳过；无 DDL；新增 `FileGovernanceHttpContractTest` 12 条与 `FileGovernanceServiceTest` 15 条，文件模块合计 107 条全绿；HTTP + DB + OpenAPI 验收 14 项通过，两表回到 0 行。
- 迭代 4（2026-10-26，同分支）：新增 `GET /file/detail/{id}`（ADMIN，Spring 隐式响应 HEAD）与 `FileMediaService`/`Impl`、`FileDetailRespVO`/`FileVariantsVO`、`FileVariantEnum`；`visitPath` 按 `cdnDomain`→存储域顺序推导，`etag` 取小写 `file_hash` 并据此支持 `If-None-Match` 304，缓存 `public, max-age=300` 可配；F17 以七牛 `imageView2` URL 变体落地（320/800/1600），本地/gif/非图片降级 `variantMode=none`；无 DDL；新增 `FileMediaServiceTest` 10 条与 `FileDetailHttpContractTest` 9 条，文件模块合计 126 条全绿；HTTP + DB + OpenAPI 验收 11 项通过，两表回到 0 行。
- 迭代 5（2026-10-26，双仓库）：F15 前端新增 `src/utils/image/compressImage.ts` 并接入 `uploadMedia`（凭证前压缩、哈希/登记改用压缩后文件），`UploadPicture.vue` 加体积提示与非图片拦截，`vitest.config.ts` 扩到 `src/**`，新增 `compressImage.test.ts` 9 条；F16 后端新增 `ImageSanitizer`/`SanitizedImage`/`ExifOrientation`（纯 JDK ImageIO，无新依赖）并接入 `QiniuUtil#saveFile`、`LocalUtil#saveFile`，`LocalUtil` 同时修掉尾斜杠目录不可用的问题；新增 `ImageSanitizerTest` 12 条与 `LocalUtilTest` 6 条，后端 144 条、前端 54 条全绿，`vue-tsc` 通过；无 DDL、无新增接口；运行时验收 8 项通过（方向纠正、EXIF 剥离、伪造图片/穿越/覆盖拒绝、非图片原样），本地存储探针文件仅写入未跟踪的 `temp/`。
- 迭代 6（2026-10-26，前端仓库）：把前端「文件媒体库」链路从 `resource` 改为 `file` —— 新增 `src/api/types/file.ts`、`src/api/modules/file.ts`（12 个封装）与 `file.test.ts`（12 条）、`src/utils/file/fileFormat.ts`、`views/admin/file/List.vue` 及 `FileDetailDrawer`/`RecyclePanel`/`GovernancePanel` 三组件；`resource.ts` 只留 `ResourcePath*`，`upload.ts`/`article.ts` 的 `saveResource` 改 `saveFile`，删除死类型 `ResourceInfo` 与 `Graffiti.vue` 死声明；路由 `resourceList`→`fileList`，菜单「资源管理」→「文件管理」，`adminTabs`/`navigation.ts` 同步；删除 `views/admin/resource/List.vue`。`pnpm test:auth` 66 条全绿、`pnpm type-check` 通过；前后端联调 7 项通过，探针解绑并永久删除后 0 残留。未跟踪的 `temp/`、`scripts/auth-browser-harness.cjs` 与截图不入库。
- 迭代 7（2026-10-26，双仓库 + 数据）：文件管理拆为 `file/list|recycle|governance` 三个二级菜单页并兼容 `/admin/fileList`，新增 `FilePageHeader`，删除 `RecyclePanel`/`GovernancePanel`；文件库默认卡片网格（固定 6 列 + 窄屏降级）并保留列表切换，新增 `thumbUrl()` 与两级图片降级，默认分页改 20；卡片操作图标改为悬停才出现的左上角按钮组（详情/复制链接/编辑/移入回收站），选择框移到右上角，触屏常驻；为支撑编辑图标新增 `POST /file/admin/update`（`FileUpdateReqVO` + `FileInfoService#updateMeta` + `FileAdminController#update`，仅文件名/场景，回收站记录 404）与前端 `updateFile` + 编辑对话框；`ADMIN_ROLES` 增 3 条、`adminTabs` 增 3 条标题，新增 `navigation.test.ts` 4 条与 `updateFile` 1 条用例，前端 72 条全绿；用官方「凭证 → 七牛直传 → 登记」链路把 `src/assets/file` 的 18 张位图登记进 `file_info`（id 16–33，`scene=system`），5 个伪 `.jpg` 按文件头纠正为 `.png`。
- 迭代 8（2026-10-26，双仓库）：新增通用选图容器 `src/components/media/ImagePickerDialog.vue`（类型筛选 + 关键词搜索 + 容器内上传 + 卡片单选 + 分页 + 「使用该图片」），接入文章封面、站点头像/背景图片、C 端用户头像三处；后端新增 `POST /file/my/list`（`FileController#myList`，EITHER，拥有者取自会话）与前端 `listMyFiles`；修两处既有缺陷——`SCENE_BY_PREFIX` 补齐六个前缀映射（此前这些页面上传全落 `scene=unknown`）、七牛直传改以凭证 `uploadUrl` 为准并在 `application-dev.yml` 配 `up-as0.qiniup.com`（此前写死域名导致浏览器上传必 400 incorrect region）；新增 `listMyFiles` 1 条用例，前端 73 条全绿，运行时 7 项验收通过，测试图与头像改动均已回滚。
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
| 第六轮：两仓库 `git add` + `git commit -F` | 后端 `a59b9e9`（15 文件）、前端 `244ebe3`（4 文件）；提交后工作区干净（前端仅剩未跟踪 `temp/`、`scripts/`） | 第五轮改动已固化到版本库 |
| 第六轮：`git merge-base --is-ancestor main fix/admin-login-role-menu` + `git merge --ff-only` | 两仓库均可快进；后端 `main` `59ffed0`→`a59b9e9`（27 文件 +307/−99），前端 `4f4fed9`→`244ebe3`（12 文件 +440/−47），无冲突 | 合并未引入内容变化，第五轮运行时结论对 `main` 同样成立 |
| 第六轮：合并后 `git status -sb` | 后端 `main...github/main [ahead 6]`、前端 `main...origin/main [ahead 4]`，均未 push | 远程与本地 `main` 的差异范围 |
| 第六轮：合并后进程与端口核查 | 18081（`spring-boot:run`）与 5173（`vite --strictPort`）已退出；18082（旧 jar，2026-10-23）与 5175（旧 vite，2026-10-16）仍在监听 | 当前无最新代码实例在跑；残留实例不可作为本轮证据 |
| 第七轮：`mvn -o -B -pl qsblog-server -DskipTests compile` | `BUILD SUCCESS` | 列表投影改动编译通过 |
| 第七轮：匿名 `POST /article/listPage` 四类探针（`temp/regress/a2-seed.sql`） | 私密长文 81 字（80+`…`）、私密短文 `articleContent=null`、公开长文 81 字、公开短文原样；`password`/`videoUrl` 恒空 | 私密文章不再返回整篇正文 |
| 第七轮：详情 `GET /article/getArticleById` 回归 | 私密无密码返回「密码错误+A2 提示」；带密码返回全文 120 字；公开文章全文 120 字 | 详情密码链路未被列表改动影响 |
| 第七轮：Playwright `/sort?sortId=1` | 4 张探针卡片摘要与接口一致，私密短文显示「该文章已加密，输入密码后查看」；控制台无 JS 运行时错误（仅外部封面图 404） | 列表页实际渲染 |
| 第七轮：`npx vue-tsc --noEmit` | 通过 | 前端兜底文案类型安全 |
| 第七轮：数据清理 | 探针 0 条、存活文章 2 条；Redis `category_article_list_v2` 已删除 | 未污染 dev 数据与缓存 |
| 第八轮：`mvn -o -B -pl qsblog-server -DskipTests compile` + `npx vue-tsc --noEmit` | `BUILD SUCCESS`、exit 0 | 后端放开过滤与前端解包修复可编译、类型安全 |
| 第八轮：重启后端 18081、前端 5173 | 后端 `Started QsBlogApplication in 9.299 seconds`（PID 27348）、前端 `VITE v7.3.0 ready` | 新代码在运行实例中 |
| 第八轮：匿名 `GET /article/articleCategoryList`（4 条探针） | 6 条，含 2 条私密；私密长 81 字（80+`…`）、私密短 `null`、公开长 81、公开短原样；`password`/`videoUrl` 恒空 | 首页分类已放开私密且沿用列表投影 |
| 第八轮：匿名 `POST /article/listPage` | 6 条，探针摘要与分类接口一致 | 两个列表入口规则一致 |
| 第八轮：Playwright 首页 `/` | `.sort-article-first=1`，4 个 A2PROBE 标题全部渲染；区块内 `.recent-post-desc=0`（`SortArticle.vue` 不渲染摘要）；控制台无 JS 错误 | 解包修复生效、私密文进入首页分类区块 |
| 第八轮：Playwright `/sort?sortId=1` | 6 张卡片；私密短显示加密提示（14 字）、私密长 81 字、公开长 81、公开短 4 | 分类列表口径未回退 |
| 第八轮：清理 | 探针 0 条、存活 2 条；`category_article_list_v3` 已删并重建；`category:public` 已重建为 5 个分类 | 数据与缓存未污染 |

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

### 第六轮：提交与合并（2026-10-24）

- 提交：后端 `a59b9e9`（14 个控制器 + 本报告，15 文件 +67/−46）、前端 `244ebe3`（4 文件 +35/−26）。两个 fix 分支此前分别停在 `7b75dc0` / `93606a5`，本轮补齐第五轮改动。
- 合并：两仓库 `main` 均满足快进条件，执行 `git merge --ff-only fix/admin-login-role-menu`；后端 `59ffed0→a59b9e9`、前端 `4f4fed9→244ebe3`。无冲突、无合并提交，代码内容与第五轮验证快照一致。
- 推送：未执行。后端 `main` 相对 `github/main` ahead 6，前端 `main` 相对 `origin/main` ahead 4。
- 服务状态（合并后核查）：第五轮验收用的后端 18081（`spring-boot:run`，运行 `target/classes`）与前端 5173（`vite --port 5173 --strictPort`）进程已退出；仍有两条**更早的残留实例**在监听：18082（`java -jar docker/qsblog-server.jar --spring.profiles.active=dev`，2026-10-23 18:19 启动，`/portal/owner` 返回 404）与 5175（`vite`，2026-10-16 22:02 启动，返回 200）。两者均不是当前 HEAD 构建，排查时不得当作最新实例。

### 第七轮：A2 修复（2026-10-24，私密文章列表正文暴露）

- 业务口径更正：私密文章允许出现在列表（封面、标题、开头摘要），不再按“公开分页必须过滤私密文章”处置；真正的缺陷是正文不足 80 字的私密文章会整篇返回。
- 后端：`ArticleServiceImpl` 新增 `projectForPublicList` / `toListSummary`；`listArticle` 与 `listCategoryArticle` 统一投影，`password`/`videoUrl` 恒空，私密文 ≤80 字返回 `null`。
- 前端：`ArticleList.vue` 对空摘要兜底「该文章已加密，输入密码后查看」。
- 验证：见 Verification 表第七轮 6 行；`/sort?sortId=1` 截图 `.playwright-mcp/a2probe-sort-desktop.png`。
- 发现但未修（既有）：首页 `index.vue#getSortArticles` 读 `response.data`，而 `listCategoryArticle` 返回值在 `handleResponse` 已剥掉 `R` 信封，导致 `sortArticles` 恒为空、分类区块不渲染；`articleCategoryList` 本身只返回公开文章，修好后也不会出现私密文。

### 第八轮：首页分类放开私密文章 + 解包修复（2026-10-25）

- 产品决定：首页 `articleCategoryList` 与 `/sort` 口径统一，私密文章也进入分类区块（展示封面、标题；正文沿用第七轮列表投影）。
- 后端：`ArticleServiceImpl#listCategoryArticle` 去掉 `viewStatus=true` 过滤；`CommonConst.CATEGORY_ARTICLE_LIST` 由 `v2` 升为 `v3`，使旧快照不再命中。
- 前端：`index.vue#getSortArticles` 改为直接使用 `response`（`handleResponse` 已剥 `R` 信封），分类区块恢复渲染。
- 验证链路：重启后接口返回 6 条（含 2 条私密，摘要分级正确）；首页分类区块渲染 1 个区块、4 张探针卡片；`/sort?sortId=1` 摘要与第七轮一致。
- 验证中发现的环境问题：`category:public` / `category:navbar_` 是无 TTL 旧快照，缺 `TestCategory01`（id=1），导致首页即使数据正确也只有 4 个分类、无对应区块；清除后由 DB 重建为 5 个分类。该缓存策略属既有 B4 类风险，本轮只做一次性清理，未改策略。
- 已知差异：首页卡片组件 `SortArticle.vue` 不渲染摘要区，私密文章在首页仅显示标题/封面/分类/时间；如需首页也展示摘要，需改该组件。
- 未提交：后端 3 文件（`ArticleServiceImpl`、`CommonConst`、本报告）、前端 2 文件（`ArticleList.vue`、`index.vue`）仍在 `main` 工作区。

### CONFIRMED(BUILD) / CONFIRMED(TEST)：本轮实际观察

1. **Rollup 分包循环依赖警告已消失**：上轮 `categoryInfo.ts → stores/index.ts` 的跨 chunk 循环本轮未再出现。属构建结果变化，未在浏览器验证是否影响执行顺序。
2. **CSS 语法/弃用警告仍在**：生成 CSS 中 `&--register[data-v-*]` 不符合原生 CSS nesting 规则；多处 `>>>`、`/deep/` 已弃用。未推断为注册功能失效。
3. **大包警告仍在**：最大 JS chunk 约 866.52 kB（gzip 约 299.5 kB），超过 500 kB 阈值。属加载性能治理项。
4. **Java 编译警告仍在**：Lombok 父类 equals/hashCode、MapStruct 未映射字段、隐式注解处理、弃用及未检查操作。未因警告修改映射或认定业务丢字段。

### 推荐回归顺序

1. ~~**S1 优先**：匿名/普通用户/管理员三身份回归动态页写路径~~ → 已在第三、四轮完成：匿名/普通用户/管理员/站长四身份全部回归。
2. ~~**S2**：登录后加载公开时间线确认 `liked`，登出后仍可匿名浏览~~ → 已由 API 断言与浏览器刷新双重验证。
3. **后台 role=1 写操作验收（第五轮遗留）**：以管理员身份逐页执行新增/编辑/删除/上传，重点 `article` 保存与删除、`file/admin/upload`、`webInfo/update`、`sysconfig`、`danmaku/del`；确认 `changeUserType` 对 role=1 仍返回 403。
4. **推送前复核**：两仓库 `git fetch` 后确认 `main` 仍可快进再推送；不要先往 fix 分支追加提交，否则本地 `main` 与远程的关系需要重新判断。
5. **S3/S4**：匿名与过期 token 访问 `/file/admin/upload`，确认响应统一、限流重新生效；用旧契约调用 `/user/token`，确认错误码明确。
6. **双域并行**：同一账号同时登录前后台，验证登出前台不影响后台、改密后两域旧会话均失效、跨标签页（storage 事件）与 `pageshow` 复核正常；同步核对 Redis 中两域的键名与 TTL（S5/S6）。
7. 旧发现：A1 跨账号读写与审核、B1/B2 文章编辑与保存、B4/B5 标签缓存与字典（含 `category:public` 无 TTL 旧快照）、C1/D4 友链、D1 各活跃旧入口、D2 关于页分页。
8. 用邮件替身验证限流与找回成功反馈；最后对生产构建做路由切换/刷新与注册样式验收。

## Remaining Uncertainty

- 第二轮没有 HTTP/浏览器/DB/Redis 复测，S1–S8 当时均为源码结论；第三、四轮已补齐 S1/S2、S9–S12 与站长专属写权限的运行时证据，第七轮补齐 A2；S3–S8 仍为源码结论。
- A2 的修复把「私密短文不返回正文」定为规则：列表对 ≤80 字的私密文章返回 `articleContent=null`，前端显示加密提示；若产品希望短私密文也展示开头，需要改为固定长度截断并接受“摘要即全文”的残留风险。
- 首页分类区块解包缺陷已于第八轮修复并经浏览器验证；遗留差异是 `SortArticle.vue` 不渲染摘要，首页只有标题/封面。
- `category:public` / `category:navbar_` 无 TTL，仅分类写操作才失效；第八轮遇到一次陈旧快照导致首页分类区块缺项，已手工清除，但缓存策略本身未改，换环境或长期运行后仍可能复现。
- 第四轮只验证了「站长可写 + 其他角色不可写」的主链路；「站长只能管理自己发布的动态」这类对象授权（若后续要求）尚未定义，当前删除按钮对 `userType=0` 全量可见。
- 第五轮把后台管理端大量 `value = 0` 放宽为 `value = 1`，属权限模型收紧口径的放宽：`changeUserType` 是唯一保留站长专属的写接口，其余管理操作现由 0/1 共同掌握，是否符合最终产品授权需要产品方确认。
- 第五轮只逐页验证了读接口；role=1 的写接口（保存/删除/审核）未逐项执行，仅依据注解与前端调用链判定。
- `/admin/resourcePathList`（资源聚合）仍是既有 404 页面，前端端点与后端模块不匹配，未在本轮修复。
- 文件模块已覆盖迭代 0–8（登记链路、服务端 Key、大小/类型/场景约束、SHA256 落库、引用绑定/解绑/查询、回收站软删与恢复、`force` 永久删除、孤儿扫描/补登记、未引用清单、批量清理、详情/ETag/派生变体、上传前压缩、服务端重绘、三页拆分、卡片视图与卡片编辑、选图容器与 C 端「我的图片」；迭代 7 已用真实七牛直传登记 18 张站点素材，迭代 8 补上浏览器直传域名修正并真实跑通），但仍未覆盖七牛直传的失败重试/大文件/私有空间签名；`file_hash` 未与对象 ETag 比对（412 未实现）；前端孤儿扫描、补登记未在 UI 端到端验收；站点素材仅登记，未替换页面引用。
- 迭代 7 新增的 `update` 是「只改元数据」的最小接口：不级联更新引用表的展示名、不校验新场景与文件实际类型是否兼容（如把 `image/*` 改成 `avatar` 以外的场景）、无乐观锁（`BaseDO#version` 仍被注释），并发改名以最后写入为准；`originalName` 也不参与存储 Key，改名不影响访问地址。
- 迭代 8 的选图容器是「单选 + 直接回填 URL」：选完不会自动 `ref/bind`，因此仍可在文件库把一张正被封面使用的图直接删除（引用计数不增长）；也不校验尺寸/宽高比是否适配目标位（头像与封面均无裁切）；`my/list` 与 `admin/list` 共用分页契约但可见范围不同，若后续要“全站素材池开放给所有登录用户挑”需先定权限与隐私口径。
- 迭代 5 的 F16 只作用于经过服务端的 `/file/admin/upload`：七牛直传链后端拿不到字节，仍依赖 F15 前端压缩与七牛侧处理；WEBP/AVIF 无 JDK 解码器时保留原字节（元数据未剥离），gif 为保动图同样不重绘；重绘为同步 CPU 操作，4000 万像素上限下未验证大图耗时与堆压力；前端压缩的 WebP 回退与 EXIF 方向仅在 jsdom 替身中验证，未经真实移动端浏览器。
- 迭代 3 的治理是手动触发：扫描上限 5000、无游标续扫，只检测“对象在库不在”而发现不了“库在对象不在”的反向孤儿；补登记统一归 `system` 且不计算 `file_hash`；契约修订 10 的 24h 定时清理与修订 9 的 412/429 均未实现。
- 未在真实 Redis 上执行 Lua 脚本与 Sa-Token 1.46.0 的键格式契约测试（集成测试被 system property 门控）；S5 的`login:session` 键共享与 S6 的日志/存储增长因此只能定性不能定量。
- 不执行有状态探针：即使 GET 也可能回填缓存/记录访问/增加计数，认证切面会续期；不能将“GET”自动当作零副作用。
- 未确认运行 JVM 是否等于当前 HEAD、网关兼容路由、生产 CORS/限流及真实数据分布；不沿用上次 PID、端口或“实例必然旧”的说法。第六轮核查到两个更早的残留实例（18082 旧 jar，2026-10-23 启动；5175 旧 vite，2026-10-16 启动），均非当前 HEAD 构建。
- 两仓库 `main` 已完成本地快进合并但未推送（后端 ahead `github/main` 6、前端 ahead `origin/main` 4）；推送前需 `git fetch` 复核远程无新提交，否则本地领先关系可能变为分叉。
- 第五轮放宽的权限面尚未经产品方逐模块确认，role=1 的写操作未执行；`/admin/resourcePathList` 仍 404，三者共同决定后台是否已“可用”。
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
8. **文件模块仅覆盖迭代 0**：`/file/register`、`/file/admin/save|list|delete` 已做 HTTP + DB + 浏览器验收，但七牛直传（`getUpToken` + bucket 策略）未实测，`file_hash`、引用绑定、回收站、孤儿治理与 F15–F17 仍为源码结论。
9. **七牛直传策略未实测**：`insertOnly`/`mimeLimit`/`fsizeLimit` 的组合行为依赖 bucket 侧配置，本轮无上传凭证实测。

</details>