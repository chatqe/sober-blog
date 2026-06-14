---
name: qs-blog-project-overview
description: 项目概述、技术栈、核心模块和开发规范
metadata:
  type: reference
---

# qs-blog 项目概述

## 技术栈
- Spring Boot 2.7.18 + MyBatis-Plus 3.5.5
- JDK 17+，Maven 单模块项目
- MySQL 8.x（本地 dev 环境 `192.168.30.100:3309`，生产 `115.190.22.240`）
- Redis（Lettuce + Redisson）
- 七牛云存储、QQ 邮箱、百度地图 API、t-io（WebSocket IM，默认关闭）
- Knife4j + OpenAPI 3.0 接口文档

## 包结构
```
io.charqe.qs
  ├── aop/            # 切面：@LoginCheck, @Idempotent, @SaveCheck, @ResourceCheck
  ├── common/         # 共享基础设施
  │   ├── config/     # 配置类
  │   ├── enums/      # 通用枚举
  │   ├── exception/  # 业务异常 + 全局异常处理器
  │   └── model/      # BaseDO, R<T>, PageQuery, PageResult, CoreMapper/CoreService
  ├── framework/      # 过滤器、拦截器、异步线程池
  ├── infra/          # 第三方集成：七牛、百度地图、邮件
  ├── modules/        # 业务模块（15+）
  └── utils/          # JWT, AES, IP, 验证码等工具类
```

## 业务模块约定
每个模块遵循：`controller/web` + `controller/admin` + `service` + `domain/dao+pojo+dto` + `enums`

## 开发规范
- 所有实体继承 `BaseDO`（雪花 ID、逻辑删除、自动填充时间）
- Mapper 继承 `CoreMapper`，Service 继承 `CoreService`
- Controller 统一返回 `R<T>`，失败抛 `BizException`
- 每个 Controller 方法必须有 `@Operation(summary="...")` 和 `@ApiResponses`
- 使用 `Lb` lambda 查询构造器，不用原始 Wrapper
- UserContext 通过 WebInfoHandlerInterceptor 填充当前用户
- 优先用 `QingSiCache`（Redis 缓存封装）
