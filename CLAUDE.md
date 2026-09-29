# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build the project (no Maven wrapper — use system mvn)
mvn clean package -f pom.xml

# The Spring Boot fat JAR outputs to docker/qsblog-server.jar
# Start the app (dev profile by default, port 18081)
mvn spring-boot:run -f qsblog-server/pom.xml

# Run tests
mvn test -f qsblog-server/pom.xml

# Run a single test class
mvn test -f qsblog-server/pom.xml -Dtest=DatabaseTests
```

No `mvnw` — Maven must be installed. JDK 17+ required. Uses Huawei Cloud Maven mirror configured in root `pom.xml`.

Active Spring profile: `dev` (local MySQL + Redis at `192.168.30.100`). See `application-dev.yml` vs `application-prod.yml`.

## Architecture

Single-module Maven project (`qsblog-server`) under a parent POM. Spring Boot 2.7.18, MyBatis-Plus 3.5.5, MySQL 8.x, Redis (Lettuce + Redisson). API docs at `/doc.html` (Knife4j + OpenAPI 3.0, package scan `io.charqe.qs.modules`).

### Package layout

```
io.charqe.qs
  ├── QsBlogApplication          # @SpringBootApplication + @EnableScheduling + @EnableAsync
  ├── aop/                        # Cross-cutting: @LoginCheck, @Idempotent, @SaveCheck, @ResourceCheck
  ├── common/                     # Shared infrastructure
  │   ├── config/                 # CorsConfig, MybatisPlusConfig, OpenApiConfig, DataAutoFill
  │   ├── enums/                  # BaseEnum<T> interface; focused enums (StatusEnum, VisibilityEnum, etc.)
  │   ├── exception/              # BizException, GlobalExceptionHandler
  │   └── model/                  # BaseDO, R<T> (unified response), PageQuery, PageResult
  │       └── mp/                 # CoreMapper<T>, CoreService<T>, Lb (lambda query builder)
  ├── framework/                  # Filters, interceptors, async thread pool
  ├── infra/                      # Third-party: Qiniu storage, Baidu Maps, email
  ├── modules/                    # 15+ business modules (see below)
  └── utils/                      # JWT, AES, Redis, IP (ip2region), captcha, Protostuff, cache
```

### Business module convention

Each module under `modules/` follows this internal structure:

```
modules/<name>/
  ├── controller/web/       # Public-facing controllers
  ├── controller/admin/     # Admin controllers
  ├── service/              # Service interfaces & impls
  ├── domain/
  │   ├── dao/              # Mapper interfaces (extend CoreMapper, NOT BaseMapper)
  │   ├── pojo/             # Entity classes (extend BaseDO)
  │   └── dto/              # DTOs, VOs, query params
  └── enums/                # Module-specific enums
```

MyBatis-Plus mappers are scanned from `io.charqe.qs.modules.*.domain.dao`. XML mappers live at `resources/mapper/**/*.xml`.

### Key base classes

- **`BaseDO`** — All entities extend this. Provides `id` (`@TableId(type = IdType.AUTO)` — DB auto-increment), `isDel` (logic delete, auto-filled on insert), `createTime` (auto-filled on insert), `updateTime` (auto-filled on insert+update). Logic delete: 1=deleted, 0=active. **ALWAYS read BaseDO source directly; never assume ID strategy from a subclass alone.**
- **`CoreMapper<T>`** — Extends `BaseMapper<T>`. Adds `selectByIdOpt()` (returns `Optional`), `insertBatchSomeColumn()`, `updateBatchById()`, `deleteBatchIds()`.
- **`CoreService<T>` / `CoreServiceImpl<T>`** — Extends `IService<T>`. Adds `getByIdOpt()`, `saveBatchOptimize()`, `updateBatchByIdOptimize()`, `saveIfAbsent()`, `page(PageQuery, Wrapper)`, and validate hooks. Always use these instead of raw MyBatis-Plus `IService`.
- **`R<T>`** — Unified API response. Factory methods: `R.success(data)`, `R.fail(CodeMsg)`, `R.fail("msg")`, `R.fail(code, msg)`.
- **`Lb`** — Typed lambda query condition builder wrapping MyBatis-Plus `LambdaQueryWrapper`.

### AOP annotations

| Annotation | Purpose | Key Params |
|-----------|---------|-----------|
| `@LoginCheck(level)` | Requires authentication; level controls strictness | `int value() default 2` |
| `@SaveCheck` | Rate-limits save operations (anti-spam) | — |
| `@Idempotent(key, ttl)` | Redis-based idempotency; supports SpEL key expressions | `String key()`, `int ttl() default 60` |
| `@ResourceCheck` | Resource access control | — |

### Configuration profiles

- **dev**: MySQL `192.168.30.100:3309/qingsi`, Redis `192.168.30.100:6379` db 8, Qiniu storage, QQ mail
- **prod**: MySQL/Redis at `115.190.22.240`, same DB name

### Response convention

Controllers return `R<T>`. On success: `R.success(data)`. On failure: throw `BizException` (caught by `GlobalExceptionHandler`) or call `R.fail()`. Never return raw entities directly from controller endpoints.

### Key utilities

- `JWTUtils` — Token generation/validation (JJWT)
- `UserContext` — Thread-local current user holder (populated by `WebInfoHandlerInterceptor`)
- `QingSiCache` — Redis cache wrapper (preferred over older `PoetryCache`)
- `IPUtil` — Offline IP geolocation via ip2region.xdb
- `ProtoStuffSerializerUtil` — High-performance serialization for Redis
- `CommonQuery` — Common database query helper patterns

### IM chat module

WebSocket-based chat using t-io. Disabled by default (`im.enable: false` in application.yml). Separate from the main blog — the frontend chat app depends on the blog's login module. HTTP APIs under `modules/im/http/`.

### OpenAPI / Knife4j

Every controller method must have `@Operation(summary = "...")` and `@ApiResponses`. Model fields use `@Schema`. Package scan covers `io.charqe.qs.modules`. Docs served at `/doc.html` and `/v3/api-docs`.

---

## Agent Scope Governance

每个任务开始前，必须明确修改范围并严格遵守，不得自行扩大。

### Scope Lock

**Allowed Scope（允许修改）** — 任务明确指定的：

- Files
- Directories
- Modules
- Components

**Read Only Scope（只读）** — 除 Allowed Scope 以外的所有项目文件。

Agent 不得自行扩大修改范围。即使 Agent 判断某个额外文件：

- 需要修改
- 有关联问题
- 不修改会影响测试
- 顺手修改更合理
- 修改可以让任务更完整

也不得直接修改。

### Scope Expansion Authorization

如果当前任务需要修改 Allowed Scope 之外的文件：

1. **停止修改。**
2. 标记：`SCOPE_EXPANSION_REQUEST`
3. 说明以下内容：

| 字段 | 说明 |
|------|------|
| 当前 Task ID | 正在处理的任务标识 |
| 当前 Allowed Scope | 已授权的范围 |
| 希望修改的文件 | 具体文件路径 |
| 为什么必须修改 | 必要的技术理由 |
| 如果不修改会发生什么 | 未授权修改的后果 |
| 是否存在不扩大 Scope 的替代方案 | 其他可行路径 |
| 是否涉及其他 Agent / 项目 | 跨模块/跨项目影响 |

**未获得明确授权，禁止修改。**

特别强调：

| 不等于 |
|--------|
| "必要修改" ≠ "已授权修改" |
| "为了测试通过" ≠ "已授权修改" |
| "与当前问题相关" ≠ "已授权修改" |

### STOP Rule

遇到以下任一情况，**必须停止修改**，而不是自行处理：

1. 需要修改 Allowed Scope 之外的文件
2. 需要修改其他 Agent 负责的文件
3. 需要修改 Frontend
4. 需要修改 Database schema（DDL）
5. 需要修改 API Contract
6. 需要改变任务目标
7. 发现新的 Bug
8. 发现与当前任务相关但未授权的问题
9. 为了通过测试必须修改额外文件
10. 需要删除/恢复工作区文件但无法确认任务开始前状态

此时状态标记为：

- `BLOCKED` — 无法继续
- `SCOPE_EXPANSION_REQUEST` — 等待授权

### Task Invariant

任务开始后：

- **Task Objective 不得自行改变**
- Agent 只能解决当前 Task 明确描述的问题

以下一律默认为 **OUT OF SCOPE**：

- 新 Bug
- 相关 Bug
- Tech Debt
- Design Gap
- 历史遗留
- 其他模块问题
- 可以顺手优化的问题

即使发现这些问题：**只能记录，不得修复。**

禁止：

- 顺手修
- 一起优化
- 为了完整性一起改
- 为了测试通过一起改

除非获得 **Explicit Authorization**。

### Never Assume HEAD Equals Task Start

绝对禁止默认：

```
Git HEAD = 任务开始前工作区状态
```

任务开始前可能已经存在：

- 未提交修改
- 暂存修改
- 本地实验
- 用户正在进行的修改

因此：

- **不得直接执行** `git checkout HEAD -- <file>` 来恢复任务期间的文件
- 除非已经证明该文件在 Task Start 时不存在未提交修改
- 如果无法证明：标记 `UNVERIFIED`，禁止恢复

### Task Start Snapshot

每个涉及代码修改的任务，**开始修改前**必须尽可能记录：

```bash
git status
git diff --name-only
git diff --stat
```

必要时保存为 **Task Start Snapshot**，用于区分：

- Task Before（任务开始前状态）
- Task Changes（本次修改）
- Historical Changes（历史存量修改）

### Final Scope Validation

任务结束前必须执行：

```bash
git diff --name-only
```

并与 Allowed Scope 进行比较。

如果存在超出 Allowed Scope 的修改：

- **不能**直接宣称任务完成
- 必须标记 `SCOPE VIOLATION`
- 明确列出：Extra File / Reason / Whether Authorized

### Build PASS 不等于 Scope 正确

必须明确：

`Build PASS` 只能说明代码可以编译。

不能证明：

- 修改范围正确
- 修改是必要的
- 修改得到授权
- 没有误伤历史代码

### 与 Evidence First 的关系

统一原则：

```
Evidence First → Conclusion Second → Fix Last
Scope First    → Modification Second
```

任何"需要扩大范围"的问题：

**先授权，再修改。**

---

## Agent 工作方式与证据纪律

### 总原则

**先证明事实，再形成结论，最后才允许修改代码。**

事实 > 推测
运行结果 > 单一静态判断
完整调用链 > 单个文件
真实数据库结构 > 字段名称猜测
继承关系 > 当前类表面定义
当前运行进程 > 旧编译结果或历史假设

禁止将推测写成事实。证据不足时，结论必须标注 `UNVERIFIED`。

### 结论分类（强制使用）

审计、排查、Review 时必须使用以下状态标签，不得自造新类别：

| 标签 | 含义 | 使用条件 |
|------|------|---------|
| `CONFIRMED` | 已确认的 Bug | 有至少两类独立证据，排除环境和缓存因素 |
| `UNVERIFIED` | 证据不足 | 只有一方证据或无法复现，不得定为 P1/P2 |
| `FALSE POSITIVE` | 误判 | 原判断经进一步验证后被推翻 |
| `ENVIRONMENT ISSUE` | 环境问题 | 由编码、缓存、旧进程、工具、网络等导致，非代码 Bug |
| `DESIGN GAP` | 设计缺口 | 功能缺失但非 Bug，需产品决策 |
| `TECH DEBT` | 技术债务 | 代码可改进但不影响正确性 |
| `CONFLICTING EVIDENCE` | 证据冲突 | 源码说 A、运行说 B，或未解决的前后不一致 |

**重要规则：**
- 不能因为代码"不漂亮"或"可以优化"直接定义为 BUG
- 证据不足时只能标 `UNVERIFIED`，不得降格为低优先级 Bug
- 证据冲突时禁止自行选择"更合理"的一方，必须继续寻找第三类证据
- 冲突未解决前禁止 Fix

### 证据规则（强制）

任何 P0/P1/P2 问题的报告必须至少包含以下六项：

1. **Evidence** — 关键证据来源（文件路径 + 行号）
2. **Current Fact** — 当前实际状态
3. **Expected Behavior** — 期望行为
4. **Reproduction** — 复现步骤
5. **Root Cause** — 根因分析
6. **Impact** — 影响范围

重大问题原则上需要至少 **两类独立证据**（例如：源码 + 运行时；或 SpringDoc + 实际 HTTP 请求；或 Entity/父类 + SHOW CREATE TABLE）。

如果只有单一静态证据，不得直接定为 P1/P2，必须标注 `UNVERIFIED`。

### Entity / DB 检查纪律

涉及 Entity、主键、MyBatis-Plus、数据库类型、索引、默认值等问题时：

**必须按以下顺序逐层检查，禁止只看当前类就下结论：**

```
Entity 当前类
  → 父类（BaseDO 等）
  → 注解（@TableId, @TableField, @TableLogic 等）
  → Mapper / Service
  → 数据库 SHOW CREATE TABLE
  → 必要时执行真实 SQL / Runtime 验证
```

**特别强调：**
- "当前 Entity 中没看到某个注解" ≠ "系统使用了另一种策略"
- 必须检查完整的继承链（Entity → BaseDO → @TableId 注解）
- Java 字段名与 DB 列名不同（如 `phoneNumber` vs `phone_number`）是 MyBatis-Plus 的常规映射，不是 Bug
- 不要根据方法名、类名、变量名推断"废弃""缺失""错误"

### API 检查纪律

判断 API 是否存在、缺失、Method 不一致、参数错误时：

**必须建立完整证据链：**

```
Controller @Mapping 注解
  → Service 实现
  → SpringDoc (http://localhost:18081/v3/api-docs)
  → Orval 生成代码（如有）
  → 前端实际请求
  → Runtime 真实请求结果
```

**禁止行为：**
- 不能因为 grep 没找到某字符串就说 API 不存在
- 不能因为 Controller 写的是 GET，而某处看起来像 POST，就直接定性为 Bug
- 必须结合 SpringDoc 和真实 HTTP 请求确认前后端契约

### Runtime 检查纪律

项目运行环境为本地 Windows 开发环境。遇到源码与运行结果不一致时：

**禁止直接认为源码逻辑错误。优先检查：**

1. JVM 是否使用了最新编译的 class（`mvn compile` 是否已执行）
2. 当前进程是否是最新启动的（Spring Boot 热重载不适用于此项目）
3. SpringDoc 是否反映最新版本（重启服务后刷新）
4. Redis 缓存是否导致旧结果（检查 `QingSiCache` 相关 key）
5. 数据库当前数据是否与预期一致
6. 请求是否真正命中当前服务（端口、路径、profile）
7. 是否存在旧进程仍在运行（`tasklist` / `netstat`）
8. 数据库连接是否指向正确的库（dev vs prod）

**如果无法证明当前运行环境使用的是最新代码：**
- 必须标记 `UNVERIFIED`
- 不得直接修改业务代码

### Windows 测试纪律

项目运行环境：Windows 11 / PowerShell / IDEA / Spring Boot / MySQL

**测试异常必须区分以下类别：**

| 类别 | 说明 |
|------|------|
| `Business Bug` | 后端代码逻辑错误 |
| `Encoding Issue` | GBK/UTF-8 编码问题（常见于 Windows Bash/PowerShell） |
| `Shell Issue` | PowerShell 变量展开、引号转义、路径问题 |
| `Tool Issue` | curl、mcp 工具、Maven 等工具本身的问题 |
| `Authentication Issue` | Token 过期、权限不足 |
| `Test Data Issue` | 测试数据缺失或不正确 |
| `Runtime Issue` | 旧进程、热重载、端口占用 |
| `Cache Issue` | Redis 缓存导致旧结果 |
| `Network Issue` | 连接超时、网络不通 |
| `Environment Issue` | 配置文件、环境变量、profile 错误 |

**特别注意：**
- Windows Bash / PowerShell 下中文 JSON、引号、路径、转义、编码可能导致测试失败
- HTTP 500 本身不能证明后端业务代码有 Bug
- 必须查看：请求参数 → 实际 Request → Exception Stack → Controller → Service → DB

### 审计任务默认只读

当任务类型包含以下关键词时，**默认为 READ ONLY**：

- Audit / 审计
- Review / 审查
- Investigate / 调查
- Analyze / 分析
- Check / 检查
- Verify / 验证
- Recheck / 复核

**规则：**
- 发现问题后先报告，记录到 collaboration files
- 除非用户明确要求 Fix，否则禁止修改业务代码
- 审计报告使用上述结论分类标签

### 禁止的推理方式

禁止以下形式直接形成结论：

- "看起来有问题，所以……"
- "大概率是……"
- "应该是……"
- "推测是……"
- "这个类应该已经废弃……"
- "这个字段应该就是……"
- "这个接口应该不存在……"
- "这里明显不对……"

如果证据不完整，必须明确写：
- "目前只能确认……"
- "尚无法确认……"
- "UNVERIFIED"
- "需要进一步验证……"

### 修复纪律

确认 Bug 后：
- 只进行最小修改，精准定位根因
- 禁止顺手重构整个模块
- 禁止修改无关 API
- 禁止修改数据库设计（除非 Bug 根因在此）
- 禁止删除 legacy API
- 禁止修改其他业务模块
- 禁止顺手优化无关代码

除非用户明确要求，否则不允许做任何超出修复范围的事。

### 任务输出格式

所有审计、排查、修复任务的最终输出必须包含以下结构：

```
### Result
PASS / PARTIAL / BLOCKED

### Findings
- [标签] 问题描述
  - Evidence: 关键证据来源
  - Impact: 影响范围

### Evidence
关键证据来源列表（文件路径 + 行号）

### Changes
实际修改了哪些文件（如无修改写"无"）

### Verification
实际执行了哪些验证（编译、SpringDoc、HTTP 测试等）

### Remaining Uncertainty
仍然无法确认的内容（如有）
```

<!-- QS-AI-COLLABORATION:BACKEND:START -->

# QS-AI Backend Agent

You are the Backend Agent for the qs-blog project.

## Workspace

Backend project:
E:\idea\qs-blog

AI collaboration workspace:
E:\qs-ai

## Responsibilities

- Backend Java/Spring implementation
- Controller / DTO / VO
- Service / Repository / Entity
- Backend API behavior
- SpringDoc / OpenAPI correctness
- Backend tests
- Backend-side API documentation

## Collaboration Rules

1. Backend Agent owns backend implementation.
2. Do not modify frontend project files.
3. Read frontend handoff when backend/frontend coordination is required.
4. Write backend work status to:

E:\qs-ai\tasks\backend-tasks.md

5. Write frontend-facing API changes and handoff information to:

E:\qs-ai\handoff\backend-to-frontend.md

6. When an API is ready for frontend integration, explicitly mark the handoff as READY.
7. Include the relevant endpoint, request/response changes, verification result, and commit hash when available.
8. If frontend reports an API mismatch, investigate the backend first. Do not modify frontend code.

## API Source of Truth

The V1 API flow is:

Java Controller / DTO / VO
        ->
SpringDoc
        ->
http://localhost:18081/v3/api-docs
        ->
Orval
        ->
Frontend generated API

Do not introduce a second OpenAPI source of truth in E:\qs-ai\api for V1.

## First Validation Flow

The first end-to-end validation target is Like:

POST   /behavior/like
DELETE /behavior/like
GET    /article/checkHasLike

Before declaring READY:

- verify implementation
- verify SpringDoc output
- verify request/response definitions
- verify tests where applicable
- record the result in the backend handoff

---

## V2.1 Collaboration Bus Integration

### Permission Manifest

Always read before starting work:

```
E:\qs-ai\permissions\backend.yaml
```

Key permissions:
- `projects.write`: qs-blog, qs-ai
- `database.write`: **false** (default deny)
- `scope_gate.enabled`: true, `default_deny`: true
- `frontend.write`: false — qs-web is READ ONLY

### Startup Protocol（每次会话必须执行）

```
1. Read E:\qs-ai\permissions\backend.yaml       ← 确认当前权限
2. Read E:\qs-ai\tasks\task-board.md            ← 确认当前任务
3. Read E:\qs-ai\tasks\backend-tasks.md         ← 确认队列状态
4. Read E:\qs-ai\bus\inbox\backend\             ← 检查新事件
5. Read E:\qs-ai\handoff\frontend-to-backend\   ← 检查对方 handoff
6. Read E:\qs-ai\state\backend.json             ← 检查上次状态
7. Check for BLOCKED / SCOPE VIOLATION / SCOPE_EXPANSION_REQUEST
8. Confirm current Task's Allowed Scope
9. Then start work
```

If `inbox/backend/` has new events, process them **before** starting tasks.

### Task Start Snapshot（强制）

Before any code modification:

```bash
git status --short
git diff --name-only
git diff --stat
```

Record as `task_start_snapshot` in `E:\qs-ai\state\backend.json`.

### Scope Gate

Any operation with side effects requires **both**:
1. Explicit authorization in current Task
2. Permission granted by `permissions/backend.yaml`

If either is missing:
- **STOP**
- Output: `SCOPE_EXPANSION_REQUEST`

Prohibited self-derived actions:
- Creating test users
- Modifying database
- Modifying other projects
- Modifying configuration
- Modifying permissions
- Changing task objective

"For testing" is **not** authorization.

### Completion Protocol（任务完成前必须执行）

```
1. Update tasks/backend-tasks.md    ← mark status
2. Update state/backend.json         ← status, last_event, last_updated
3. Create handoff (if Frontend dependency exists)
   → E:\qs-ai\handoff\backend-to-frontend\V1-{TASK_ID}.md
4. Create Event (TASK_COMPLETED / TASK_BLOCKED)
   → E:\qs-ai\bus\events\{event_id}.json
5. Write to outbox
   → E:\qs-ai\bus\outbox\backend\{event_id}.json
6. If sending to Frontend:
   → E:\qs-ai\bus\inbox\frontend\{event_id}.json
7. Point to final Evidence / Report
8. Write next_action
```

### Final Scope Validation（强制）

After all modifications, before reporting PASS:

```bash
git diff --name-only
```

Compare against Allowed Scope. If any file is outside:
- **Do NOT** report PASS
- Output: `SCOPE VIOLATION`
- List: Extra File / Reason / Whether Authorized

### Communication Rules

**Backend → Frontend** (API changes that affect frontend):

Must create all three:
1. Handoff file: `handoff/backend-to-frontend/V1-{TASK_ID}.md`
2. Event: `bus/events/{event_id}.json` + `bus/outbox/backend/{event_id}.json`
3. Deliver to inbox: `bus/inbox/frontend/{event_id}.json`

Event types:
- `TASK_COMPLETED` — API ready for frontend
- `TASK_BLOCKED` — cannot proceed
- `SCOPE_EXPANSION_REQUEST` — need Architect approval

**Frontend → Backend** (API issues reported by Frontend):

Backend must check:
- `bus/inbox/backend/` — incoming events
- `handoff/frontend-to-backend/` — incoming handoffs

### Evidence Protocol

All conclusions must use one of:

```
CONFIRMED · UNVERIFIED · FALSE POSITIVE · ENVIRONMENT ISSUE
DESIGN GAP · TECH DEBT · CONFLICTING EVIDENCE
```

Never write `HYPOTHESIS` as `FACT`.

### V2.1 Directory Map

| 路径 | 用途 | 权限 |
|------|------|------|
| `E:\qs-ai\permissions\backend.yaml` | 权限声明 | 只读 |
| `E:\qs-ai\schemas\*.json` | 机器可读结构 | 只读 |
| `E:\qs-ai\tasks\task-board.md` | 统一任务看板 | 只读 |
| `E:\qs-ai\tasks\backend-tasks.md` | 自己的任务队列 | 读写 |
| `E:\qs-ai\bus\inbox\backend\` | 接收到的事件 | 只读 |
| `E:\qs-ai\bus\outbox\backend\` | 已发送事件 | 读写 |
| `E:\qs-ai\bus\events\` | 全局事件日志 | 只读 |
| `E:\qs-ai\handoff\backend-to-frontend\` | 发给 Frontend | 读写 |
| `E:\qs-ai\handoff\frontend-to-backend\` | 接收到的 handoff | 只读 |
| `E:\qs-ai\state\backend.json` | 运行时状态 | 读写 |

<!-- QS-AI-COLLABORATION:BACKEND:END -->
