---
name: behavior-module-refactor
description: 行为模块（点赞、评论、收藏、浏览等）重构进展
metadata:
  type: project
---

# 行为模块重构

## 当前进展

### 架构设计（已完成）
- 行为策略工厂已完成（`modules/behavior/service/handler/` 下策略模式，`BehHandlerFactory` 自动发现各实体 Handler）
- 事件驱动异步日志（`BehActionEvent` + `@Async` 监听器写 `BehaviorLog` 表）
- 7/11 实体类型有处理器：ARTICLE、COMMENT、MOMENT、CATEGORY、TAG；VIDEO/MUSIC 走 NoOpHandler

### 重构 Step 1+2（2026-06-15 完成）
- **提取 `IActionService` 标记接口**：所有行为服务（Like/Dislike/Comment/Subscribe/Forward）均实现此接口，Controller 可统一注入
- **`AbstractBehService` 模板方法增强**：
  - `incrementCount`/`decrementCount`/`getPositiveAction` 改为 `protected`，供 `ViewServiceImpl` 复用
  - 新增 `getActiveState()` 查询任意活跃行为，支持互斥逻辑
  - 移除内部 `BehStateStatus` 类，常量提升到 `BehaviorState.BEH_ACTIVE`/`BEH_CANCELLED`
- **点赞/点踩互斥配置化**：新建 `MutexRegistry` 管理互斥关系，`LikeServiceImpl` 和 `DislikeServiceImpl` 共享同一套 `handleMutex` 逻辑，新增互斥只需注册一条规则
- **`ViewServiceImpl` 去重**：删除 17 行重复的 `incrementViewCount`，直接使用父类 `incrementCount`

### 文件清单
```
service/
  IActionService.java              # 新：标记接口
  MutexRegistry.java               # 新：互斥策略注册中心
  AbstractBehService.java          # 重构：protected 方法 + 常量升级
  BehaviorState.java               # 新增：BEH_ACTIVE/BEH_CANCELLED 常量
  like/LikeService.java            # 新增：extends IActionService
  like/LikeServiceImpl.java        # 重构：配置化互斥
  dislike/DislikeService.java      # 同上
  dislike/DislikeServiceImpl.java  # 同上
  comment/CommentActionService.java          # 同上
  comment/CommentActionServiceImpl.java      # 同上
  subscribe/SubscribeService.java            # 同上
  subscribe/SubscribeServiceImpl.java        # 同上
  forward/ForwardService.java                # 同上
  forward/ForwardServiceImpl.java            # 同上
  view/ViewServiceImpl.java                  # 重构：删除重复方法
```

## 待完成
- [ ] 行为统计聚合接口（查询某实体的点赞/收藏/浏览数）
- [ ] 计数操作加乐观锁（version 字段未使用，高并发丢计数风险）
- [ ] 行为模块单元测试
- [ ] 清理无效代码（Admin stub 文件、BehaviorLogServiceImpl dead code、BehConstant 未用常量）
