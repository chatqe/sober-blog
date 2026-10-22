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