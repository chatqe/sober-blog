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

- **`BaseDO`** — All entities extend this. Provides `id` (ASSIGN_ID /雪花), `isDel` (logic delete), `createTime`, `updateTime` (auto-filled by `DataAutoFill`). Logic delete: 1=deleted, 0=active.
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
