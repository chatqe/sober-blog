# SoberUP-Blog · 青肆博客

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java 17](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)
[![Spring Boot 2.7](https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Build-Maven-brightgreen.svg)](https://maven.apache.org/)
[![MySQL 8](https://img.shields.io/badge/MySQL-8.x-blue.svg)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7.x-red.svg)](https://redis.io/)
[![Sa-Token](https://img.shields.io/badge/Sa--Token-1.46.0-green.svg)](https://sa-token.cc/)

> 青肆，青春肆意。一个基于 Spring Boot + Vue3 的前后端分离个人博客系统，支持文章管理、评论互动、弹幕、IM 聊天室、资源收藏、访问统计等完整功能。

---

## 目录

- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
  - [环境要求](#环境要求)
  - [数据库初始化](#数据库初始化)
  - [后端启动](#后端启动)
  - [前端启动](#前端启动)
- [核心功能](#核心功能)
- [模块说明](#模块说明)
- [API 文档](#api-文档)
- [部署说明](#部署说明)
- [环境变量配置](#环境变量配置)
- [架构设计](#架构设计)
- [变更记录](#变更记录)

---

## 技术栈

### 后端

| 组件 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 运行时 |
| Spring Boot | 2.7.18 | 应用框架 |
| MyBatis-Plus | 3.5.5 | ORM / 数据访问层 |
| MySQL | 8.x | 关系型数据库 |
| Redis | 7.x | 缓存 / 会话存储（Sa-Token 双域会话，需支持 `SET KEEPTTL`） |
| Druid | 1.2.18 | 数据库连接池 |
| Lettuce + commons-pool2 | — | Redis 客户端与连接池（Redisson 已移除） |
| Sa-Token | 1.46.0 | 前后台独立双会话鉴权（`sa-token-spring-boot-starter` + `sa-token-redis-template`） |
| Knife4j | 4.5.0 | API 文档（OpenAPI 3.0） |
| Spring Security Crypto | — | BCrypt 密码加密 |
| JJWT | 0.11.5 | JWT 工具（登录凭据已迁至 Sa-Token opaque token） |
| t-io | 3.7.5.v20211028-RELEASE | WebSocket 即时通信 |
| Hutool | 5.8.32 | 通用工具库 |
| FastJSON2 | 2.0.43 | JSON 序列化 |
| Protostuff | 1.1.5 | Redis 高性能序列化 |
| MapStruct | 1.5.5.Final | DTO/VO 编译期映射 |
| Lombok | 1.18.30 | 代码简化 |
| Hibernate Validator | 6.1.7.Final | 参数校验 |
| Freemarker | 2.3.31 | MyBatis-Plus 代码生成模板 |
| Qiniu Java SDK | 7.13.0 | 七牛云对象存储 |
| ip2region | 2.7.0 | 离线 IP 地理定位 |
| yauaa | 7.31.0 | User-Agent 解析 |

### 前端（开源版）

| 项目 | 技术 | 仓库 |
|------|------|------|
| 博客前端 | Vue3 + Element Plus | [gitee.com/littledokey/poetize-vue2](https://gitee.com/littledokey/poetize-vue2) |
| IM 聊天室 | Vue3 + Naive UI | [gitee.com/littledokey/poetize-im-vue3](https://gitee.com/littledokey/poetize-im-vue3) |

---

## 项目结构

```
qs-blog/
├── pom.xml                          # 父 POM（依赖版本管理）
├── qsblog-server/                   # 后端主模块
│   ├── pom.xml                      # 服务端依赖配置
│   └── src/main/
│       ├── java/io/charqe/qs/
│       │   ├── QsBlogApplication.java        # 启动类 (@EnableScheduling @EnableAsync)
│       │   ├── aop/                          # AOP 切面
│       │   │   ├── LoginCheckAspect.java     # 登录鉴权拦截
│       │   │   ├── IdempotentAspect.java     # 幂等性保护
│       │   │   ├── SaveCheckAspect.java      # 防刷保存限流
│       │   │   └── ResourceCheckAspect.java  # 资源权限校验
│       │   ├── common/                       # 公共基础设施
│       │   │   ├── config/                   # 配置类（CORS、MyBatis、OpenAPI 等）
│       │   │   ├── enums/                    # 枚举定义（StatusEnum、VisibilityEnum 等）
│       │   │   ├── exception/                # 全局异常处理（BizException、GlobalExceptionHandler）
│       │   │   └── model/                    # 基础模型（BaseDO、R<T>、PageQuery、PageResult）
│       │   ├── framework/                    # 框架层（拦截器、过滤器、异步线程池）
│       │   ├── infra/                        # 第三方服务集成（七牛、百度地图、邮件）
│       │   ├── modules/                      # 业务模块（19 个子模块）
│       │   └── utils/                        # 工具类（JWT、AES、Redis、IP、验证码等）
│       └── resources/
│           ├── mapper/                       # MyBatis XML Mapper 文件
│           └── application*.yml              # 多环境配置（dev / prod）
├── docker/                                   # 构建产物输出目录
├── docs/                                     # 项目文档
├── CLAUDE.md                                 # Claude Agent 工作规范
└── README.md
```

### 业务模块目录规范

每个业务模块遵循统一结构：

```
modules/<name>/
├── controller/web/    # 前台公开接口
├── controller/admin/  # 后台管理接口
├── service/           # 服务接口与实现
├── domain/
│   ├── dao/           # Mapper 接口（继承 CoreMapper<T>）
│   ├── pojo/          # 实体类（继承 BaseDO）
│   └── dto/           # 数据传输对象 / VO / 查询参数
└── enums/             # 模块专属枚举
```

---

## 快速开始

### 环境要求

| 环境 | 最低版本 | 推荐版本 |
|------|----------|----------|
| JDK | 17 | 17 LTS |
| Maven | 3.6+ | 3.8+ |
| MySQL | 8.0 | 8.0+ |
| Redis | 6.2（需支持 `SET KEEPTTL`） | 7.2 |

### 数据库初始化

1. 创建数据库（默认 `qingsi`，端口 `3309`）：

```sql
CREATE DATABASE IF NOT EXISTS qingsi DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行 SQL 初始化脚本（导入 `qsblog.sql`）。

3. 数据库连接地址（dev profile）：

| 配置项 | 值 |
|--------|----|
| Host | `192.168.30.100:3309` |
| Database | `qingsi` |
| Redis Host | `192.168.30.100:6379` |
| Redis DB | `8` |

### 后端启动

```bash
# 编译打包
mvn clean package -f pom.xml

# 运行测试
mvn test -f qsblog-server/pom.xml

# 启动服务（默认 dev profile，端口 18081）
mvn spring-boot:run -f qsblog-server/pom.xml
```

打包后的 Fat JAR 输出至 `docker/qsblog-server.jar`：

```bash
java -jar docker/qsblog-server.jar --spring.profiles.active=dev
```

### 前端启动

```bash
# 安装依赖
pnpm install

# 开发模式启动
pnpm dev
```

---

## 核心功能

### 前台功能

- **文章系统**：文章发布/编辑/分类/标签/加密/订阅，支持视频嵌入、文章目录
- **评论互动**：多级评论、点赞/点踩（防刷限流）、评论审核
- **弹幕系统**：文章弹幕发送与展示
- **资源收藏**：收藏夹管理、音乐盒、图片墙
- **访问统计**：浏览量计数、访客 IP 地域分布（ip2region 离线库）
- **友链管理**：友情链接列表
- **表白墙**：匿名告白墙功能
- **时间线**：朋友圈动态展示

### 后台管理

- 文章 CRUD、批量操作、草稿箱（`publish_status`）、分类/标签管理（启用停用、排序、`home_show` 首页展示开关）
- 评论审核、用户管理（新增/编辑/删除，Boss/Admin 提权隔离）、访问日志分析
- 系统配置（通过 MySQL 动态配置，无需重新打包）
- 文件资源管理（本地 / 七牛云切换、服务端下发上传 Key、引用绑定、回收站、孤儿治理、派生变体、随机封面场景）
- 字典管理、权限控制

### 即时通信（可选）

基于 t-io 的 WebSocket 聊天室，支持好友、群组。默认关闭（`im.enable: false`），前端需从博客登录态接入。

---

## 模块说明

| 模块 | 说明 |
|------|------|
| `article` | 文章管理（公开 + 后台 CRUD、分类/标签关联、订阅） |
| `behavior` | 用户行为（点赞/点踩/关注，含幂等保护） |
| `comment` | 评论管理（多级评论、审核、后台配置） |
| `danmaku` | 弹幕管理（发送、过滤、展示） |
| `category` | 文章分类（父子目录、排序、缓存管理） |
| `tag` | 文章标签管理 |
| `file` | 文件资源（上传、存储、七牛云集成、收藏夹） |
| `moment` | 朋友圈动态（时间线） |
| `family` | 表白墙 |
| `access` | 访问日志（IP 地域统计、UA 解析） |
| `sysconfig` | 系统配置（动态化，无需重启） |
| `sysdict` | 数据字典管理（Redis 缓存） |
| `user` | 用户管理（注册、登录、JWT、密码加密） |
| `webInfo` | 网站信息（元数据、SEO 配置） |
| `weblinks` | 友链管理 |
| `im` | IM 聊天室（WebSocket，默认关闭） |
| `system` | 系统基础（权限、角色） |

---

## API 文档

服务启动后，API 文档自动可用：

| 地址 | 说明 |
|------|------|
| http://localhost:18081/doc.html | Knife4j 增强 UI |
| http://localhost:18081/v3/api-docs | OpenAPI 3.0 JSON |
| http://localhost:18081/swagger-ui.html | Swagger UI |

所有 Controller 方法均标注 `@Operation(summary = "...")` 和 `@Schema`，包扫描路径为 `io.charqe.qs.modules`。

---

### 测试账号
zhangsan09
123456

---

## 部署说明

### 环境变量（必需）

| 变量 | 说明 |
|------|------|
| `QS_JWT_SECRET` | JWT 签名密钥（Base64 编码的 256 位密钥） |
| `QS_AES_KEY` | AES 加密密钥 |
| `QS_BAIDU_AK` | 百度地图 API Key（IP 地理定位使用） |

### 生产环境配置

```bash
# 切换至 prod profile
java -jar qsblog-server.jar --spring.profiles.active=prod

# 或通过 JVM 参数传入密钥
java -jar qsblog-server.jar \
  --spring.profiles.active=prod \
  -DQS_JWT_SECRET=<your-secret> \
  -DQS_AES_KEY=<your-key> \
  -DQS_BAIDU_AK=<your-baidu-ak>
```

生产配置（`application-prod.yml`）指向远程 MySQL / Redis 服务（`115.190.22.240`）。

### Nginx 反向代理（推荐）

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态资源
    location / {
        root /path/to/frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API
    location /api/ {
        proxy_pass http://127.0.0.1:18081/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # WebSocket（IM 聊天室）
    location /ws/ {
        proxy_pass http://127.0.0.1:18081/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

---

## 架构设计

### 认证与授权

前后台独立双会话（Sa-Token 改造后，2026-10）：

```
请求 → 提取 Authorization（opaque token，非 JWT）
     → AuthSessionService / SaTokenSessionAdapter（WEB / ADMIN 双 StpLogic，Redis 会话 + revision 版本撤销）
     → @LoginCheck(scope = WEB | ADMIN | EITHER) 切面（域匹配 + userType 角色门槛）
     → @ResourceCheck 切面（资源权限校验）
     → Controller → Service → Mapper → DB
```

- 登录凭据为 64 位 opaque token，存 Redis 会话；改密/冻结/改角色通过递增 `auth:v2:revision:<userId>` 撤销旧会话。
- 后台入口 `/admin/user/login` 仅放行站长(0)与管理员(1)；动态写接口为站长专属（`value=0, scope=EITHER`）。
- `401/403` 以真实 HTTP 状态码返回（`GlobalExceptionHandler`），不再统一 200 信封。

### 防刷与幂等

| 注解 | 作用 | 实现 |
|------|------|------|
| `@LoginCheck` | 强制登录校验（含域与角色门槛） | Sa-Token 双域会话解析 + AOP 切面 |
| `@Idempotent` | 接口幂等性 | Redis SET NX + TTL |
| `@SaveCheck` | 保存操作限流 | IP + 用户维度计数 |
| `@ResourceCheck` | 资源访问控制 | 归属校验 |

### 数据访问层

```
BaseDO（id / isDel / createTime / updateTime 逻辑删除自动填充）
  → CoreMapper<T>（扩展批量操作、Optional 查询）
    → CoreService<T>（分页、批量优化、存在即跳过）
      → 业务 ServiceImpl
```

### 缓存策略

- **Redis（Lettuce + commons-pool2）**：热点数据、Sa-Token 双域会话、限流计数（Redisson 已移除，Sa-Token 会话要求 Redis 支持 `SET KEEPTTL`）
- **QingSiCache**：统一缓存封装，支持 TTL 和缓存失效

---

## 变更记录

### 2026-10 鉴权与能力改造

- **Sa-Token 前后台独立双会话**：WEB/ADMIN 双域 `StpLogic`、opaque token、revision 版本撤销、401/403 真实 HTTP 状态；移除 Redisson，改用 Lettuce + commons-pool2（要求 Redis 支持 `SET KEEPTTL`，dev 已升级 7.2）
- **文件模块迭代 0–8**：登记链路打通、上传 Key 改服务端下发（ULID）+ 大小/类型/场景约束、引用绑定与回收站（`force` 永久删除）、孤儿扫描/补登记/批量治理、媒体详情 + ETag 304 + 派生变体（七牛 `imageView2`）、上传前压缩（前端 OffscreenCanvas）+ 服务端重绘净化（JDK ImageIO 去 EXIF）、`/file/admin/update` 元信息编辑、`/file/my/list` C 端选图
- **随机封面场景**：`scene=random_cover`，历史 `backgroundImg` 资源登记为随机封面
- **全站分页响应统一 `PageResult`**（`list/total/pages/pageNum/pageSize`），前端统一读 `list`
- **ID 全链路字符串序列化**：全局 Jackson `@JsonFormat(shape=STRING)` + 守卫单测锁死契约（见 `docs/开发规范-ID序列化与下拉选项.md`）
- **分类/标签下拉选项端点** `/category/options`、`/tag/options`（含 admin 侧，OptionVO + Scope 隔离）；补 `status`/`sort` 字段打通停用与排序
- **文章草稿箱**：独立 `publish_status` 字段，后台列表/编辑/发布链路支持草稿
- **分类 `home_show`**：支持分类从首页区块排除
- **后台用户管理**：新增 `POST /admin/user/add`、编辑/删除（Boss/Admin 提权隔离：Admin 不能创建/编辑/删除 Boss）；BOSS/Admin 可修改任意文章；标题超长返回可读校验提示
- **动态（moment）**：写权限收敛为站长专属，作者信息回填，`pageList` 空结果防 `IN()` 语法错误

### 更早重构（2026）

- 主键策略统一为数据库自增（`IdType.AUTO`）
- 行为模块重构：点赞/点踩互斥逻辑修复，Like/Unlike 拆分
- 访问日志：接入 UA 解析（yauaa），完善异步落库链路
- 分类模块：缓存失效优化（批量排序后主动清除）
- 字典模块：完善 CRUD + Redis 缓存
- Moment 模块：三表架构重构，补齐 Admin Controller

### 历史版本

详见 Git 提交历史。主要里程碑：

- **2022-08**：首个版本发布
- **2023-01**：新增音乐盒、表白墙
- **2023-07**：新增旅拍、看板娘
- **2023-08**：新增访客统计、搜索功能
- **2023-10**：文章加密、订阅、文件上传多平台支持
- **2024-01**：首页分类预览、文章视频、目录生成
- **2024-04**：配置动态化（MySQL 存储），前端编译优化

---

## License

MIT License
