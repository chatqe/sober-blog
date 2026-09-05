# Behavior 模块功能逻辑详细分层文档

> 生成日期: 2026-06-21
> 技术栈: Spring Boot 2.7 + MyBatis-Plus + Redis 缓存 + @Async 异步事件

---

## 模块概览

```
behavior/
  ├── controller/
  │   ├── web/
  │   │   ├── BehaviorController.java         # 统一行为控制器：6个行为类型，RESTful GET/DELETE
  │   │   └── dto/
  │   │       └── BehDTO.java                 # 统一请求DTO (bizId + bizType)
  │   └── admin/
  │       ├── AdminBehaviorController.java    # 后台行为管理（骨架，待扩展）
  │       └── dto/vo/                         # AdminBehStateReqDTO, AdminBehRespVO (占位)
  ├── service/
  │   ├── AbstractBehService.java             # 模板方法基类：doAction / cancelAction
  │   ├── IActionService.java                 # 标记接口（点赞/点踩/订阅/转发/评论均实现）
  │   ├── MutexRegistry.java                  # 互斥策略注册中心（LIKE <-> DISLIKE）
  │   ├── BehaviorStateService.java           # 行为状态 CRUD + checkHasBeh
  │   ├── BehaviorCntService.java             # 行为计数 CRUD
  │   ├── BehaviorLogService.java             # 行为日志 CRUD
  │   ├── like/
  │   │   ├── LikeService.java                # 点赞接口
  │   │   └── LikeServiceImpl.java            # 覆写 handleMutex（点赞时取消点踩）
  │   ├── dislike/
  │   │   ├── DislikeService.java             # 点踩接口
  │   │   └── DislikeServiceImpl.java         # 覆写 handleMutex（点踩时取消点赞）
  │   ├── subscribe/
  │   │   ├── SubscribeService.java           # 订阅接口
  │   │   └── SubscribeServiceImpl.java       # 纯委托，无互斥
  │   ├── forward/
  │   │   ├── ForwardService.java             # 转发接口
  │   │   └── ForwardServiceImpl.java         # 纯委托，无互斥
  │   ├── comment/
  │   │   ├── CommentActionService.java       # 评论行为接口（非评论内容CRUD）
  │   │   └── CommentActionServiceImpl.java   # 纯委托，无互斥
  │   ├── view/
  │   │   ├── ViewService.java                # 浏览接口（无取消操作）
  │   │   └── ViewServiceImpl.java            # 独立：@Async，无实体校验，游客可浏览
  │   ├── handler/
  │   │   ├── BehEntityHandler.java           # 实体处理器接口（supports/validate/afterAction）
  │   │   ├── BehHandlerFactory.java           # 策略工厂（注入所有Handler，ConcurrentHashMap缓存）
  │   │   ├── ArticleBehHandler.java          # 文章处理器
  │   │   ├── CategoryBehHandler.java         # 专栏/标签处理器
  │   │   ├── CommentBehHandler.java          # 评论处理器
  │   │   ├── MomentBehHandler.java           # 微言处理器
  │   │   └── NoOpHandler.java               # 空操作处理器（兜底）
  │   └── event/
  │       ├── BehActionEvent.java             # Spring 应用事件
  │       └── BehLogEventListener.java        # 异步监听器：@Async 写 BehaviorLog
  ├── domain/
  │   ├── pojo/
  │   │   ├── BehaviorState.java              # 用户行为状态表（幂等+状态翻转）
  │   │   ├── BehaviorCnt.java                # 行为统计表（计数+乐观锁）
  │   │   └── BehaviorLog.java                # 行为流水表（审计日志）
  │   └── dao/
  │       ├── BehaviorStateMapper.java
  │       ├── BehaviorCntMapper.java
  │       └── BehaviorLogMapper.java
  ├── enums/
  │   ├── BehActionType.java                  # 行为类型枚举（10种正向/取消 + 浏览）
  │   └── constant/
  │       └── BehConstant.java                # 常量定义
  └── convert/
      └── BehaviorConvert.java                # MapStruct 转换器
```

---

## 核心概念

### 三张表分工

| 表名 | 职责 | 关键字段 |
|------|------|---------|
| `behavior_state` | 用户行为状态（幂等控制） | userId + bizId + bizType + action + status(1=活跃/2=取消) |
| `behavior_cnt` | 行为计数统计 | bizId + bizType + action + count + version(乐观锁) |
| `behavior_log` | 行为流水审计 | userId + bizId + bizType + action + guestId |

### 行为类型枚举 (BehActionType)

| 编码 | 含义 | 编码 | 含义 |
|------|------|------|------|
| 0 | VIEW(浏览) | 1 | LIKE(点赞) |
| 2 | DISLIKE(点踩) | 3 | SUBSCRIBE(订阅) |
| 4 | FORWARD(转发) | 5 | COMMENT(评论) |
| 6 | UN_LIKE(取消点赞) | 7 | UN_DISLIKE(取消点踩) |
| 8 | UN_SUBSCRIBE(取消订阅) | 9 | UN_FORWARD(取消转发) |
| 10 | UN_COMMENT(取消评论) | - | - |

### 业务类型枚举 (BizTypeEnum)

| 编码 | 含义 | 处理器 |
|------|------|--------|
| 1 | ARTICLE(文章) | ArticleBehHandler |
| 2 | COMMENT(评论) | CommentBehHandler |
| 3 | VIDEO(视频) | NoOpHandler(兜底) |
| 4 | MUSIC(音乐) | NoOpHandler(兜底) |
| 5 | MOMENT(微言) | MomentBehHandler |
| 6 | CATEGORY(专栏) | CategoryBehHandler |
| 7 | TAG(标签) | CategoryBehHandler |

---

## 场景一：点赞行为

**Controller** `POST /behavior/like`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| bizId | Long | 是 | 业务ID（文章ID/评论ID等） |
| bizType | Integer | 是 | 业务类型（1=文章, 2=评论, 5=微言等） |

**前置条件：** `@LoginCheck` 必须登录

**Service 流程（模板方法 doAction）：**

```
@Transactional(rollbackFor = Exception.class)
public void like(BehDTO dto, Long userId) {
    doAction(dto, userId, BehActionType.LIKE);
}

// AbstractBehService.doAction 核心步骤：

// Step 1: 实体校验
BehEntityHandler handler = handlerFactory.getHandler(dto.getBizType());
handler.validate(dto.getBizId());  // ArticleBehHandler -> 查 ArticleMapper

// Step 2: 查询用户对该实体的已有行为状态
BehaviorState existingState = getExistingState(userId, bizId, bizType, LIKE);
// 查询条件: userId + bizId + bizType + action=1

if (existingState != null) {
    if (existingState.status == BEH_ACTIVE) {
        // 已点赞，幂等返回（重复点赞不重复计数）
        return;
    }
    // 之前取消过，重新激活（status=1）
    existingState.setStatus(BEH_ACTIVE);
    behaviorStateService.updateById(existingState);
} else {
    // Step 3: 互斥处理（LikeServiceImpl 覆写）
    handleMutex(dto, userId, BehActionType.LIKE);
    // MutexRegistry 配置: LIKE -> DISLIKE
    // 查询该实体上用户的活跃行为，如果是点踩则取消

    // Step 4: 保存新行为状态
    BehaviorState state = new BehaviorState();
    state.setUserId(userId);
    state.setBizId(dto.getBizId());
    state.setBizType(dto.getBizType());
    state.setAction(BehActionType.LIKE.code());  // 1
    state.setStatus(BEH_ACTIVE);
    behaviorStateService.save(state);
}

// Step 5: 递增计数
incrementCount(dto, BehActionType.LIKE);
// 查 behavior_cnt WHERE bizId=bizId AND bizType=bizType AND action=1
// 不存在则插入 count=1，存在则 count++

// Step 6: 实体后置处理
handler.afterAction(dto.getBizId(), userId);
// ArticleBehHandler: 日志记录（预留清除缓存扩展点）

// Step 7: 发布事件（异步写日志）
eventPublisher.publishEvent(new BehActionEvent(...));
// BehLogEventListener.handleBehAction(@Async) -> 写入 behavior_log 表
```

**互斥处理逻辑（LikeServiceImpl.handleMutex）：**

```
protected void handleMutex(BehDTO dto, Long userId, BehActionType actionType) {
    // MutexRegistry 返回: LIKE -> DISLIKE
    BehActionType mutexType = mutexRegistry.getMutexType(actionType);

    // 查询该用户在该实体上的活跃行为（不区分 actionType）
    BehaviorState mutexState = getActiveState(userId, bizId, bizType);

    if (mutexState != null && mutexState.action == DISLIKE.code()) {
        // 将点踩状态设为已取消
        mutexState.setStatus(BEH_CANCELLED);
        behaviorStateService.updateById(mutexState);
        // 同时递减点踩计数
        decrementCount(dto, BehActionType.DISLIKE);
    }
}
```

---

## 场景二：取消点赞

**Controller** `DELETE /behavior/like`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| bizId | Long | 是 | 业务ID |
| bizType | Integer | 是 | 业务类型 |

**Service 流程（模板方法 cancelAction）：**

```
@Transactional(rollbackFor = Exception.class)
public void unlike(BehDTO dto, Long userId) {
    cancelAction(dto, userId, BehActionType.UN_LIKE);
}

// AbstractBehService.cancelAction 核心步骤：

// Step 1: 实体校验
BehEntityHandler handler = handlerFactory.getHandler(dto.getBizType());
handler.validate(dto.getBizId());

// Step 2: 查询活跃行为状态
BehaviorState existingState = getExistingState(userId, bizId, bizType, LIKE);
// 查找 action=1 且 status=BEH_ACTIVE 的记录

if (existingState == null || existingState.status != BEH_ACTIVE) {
    // 没有活跃行为，幂等返回（重复取消不报错）
    return;
}

// Step 3: 状态设为已取消
existingState.setStatus(BEH_CANCELLED);
behaviorStateService.updateById(existingState);

// Step 4: 递减计数
decrementCount(dto, BehActionType.LIKE);
// behavior_cnt WHERE action=1, count-- （不低于0）

// Step 5: 发布取消事件（异步写日志）
eventPublisher.publishEvent(new BehActionEvent(..., UN_LIKE));
```

**关键设计：** 不是删除 `behavior_state` 记录，而是将 status 改为 2（已取消）。保留完整审计轨迹。

---

## 场景三：点踩行为（含互斥）

**Controller** `POST /behavior/dislike`

**Service 流程：** 与点赞完全对称，互斥方向相反。

```
// DislikeServiceImpl.handleMutex 覆写：
protected void handleMutex(BehDTO dto, Long userId, BehActionType actionType) {
    // MutexRegistry 返回: DISLIKE -> LIKE
    BehActionType mutexType = mutexRegistry.getMutexType(actionType);  // LIKE

    BehaviorState mutexState = getActiveState(userId, bizId, bizType);
    if (mutexState != null && mutexState.action == LIKE.code()) {
        mutexState.setStatus(BEH_CANCELLED);
        behaviorStateService.updateById(mutexState);
        decrementCount(dto, BehActionType.LIKE);  // 同时递减点赞计数
    }
}
```

**互斥关系：** `LIKE <-> DISLIKE`（双向互斥，通过 `MutexRegistry` 配置驱动，非硬编码）

---

## 场景四：订阅行为

**Controller** `POST /behavior/subscribe`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| bizId | Long | 是 | 专栏ID 或 标签ID |
| bizType | Integer | 是 | 6=专栏, 7=标签 |

**Service 流程：** 纯委托，无互斥逻辑。

```
// SubscribeServiceImpl.subscribe() -> doAction(dto, userId, SUBSCRIBE)
// SubscribeServiceImpl.unsubscribe() -> cancelAction(dto, userId, UN_SUBSCRIBE)
```

**实体校验：** `CategoryBehHandler` 同时校验 `CategoryMapper` 和 `TagMapper`（因为 bizType=6 是专栏，bizType=7 是标签）。

---

## 场景五：转发行为

**Controller** `POST /behavior/forward`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| bizId | Long | 是 | 业务ID |
| bizType | Integer | 是 | 业务类型 |

**Service 流程：** 纯委托，无互斥逻辑。

```
// ForwardServiceImpl.forward() -> doAction(dto, userId, FORWARD)
// ForwardServiceImpl.unforward() -> cancelAction(dto, userId, UN_FORWARD)
```

---

## 场景六：评论行为记录

**Controller** `POST /behavior/comment`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| bizId | Long | 是 | 评论ID |
| bizType | Integer | 是 | 2=评论 |

**注意：** 此接口仅记录"谁对什么发表了评论"的行为统计，评论内容本身的 CRUD 由 `CommentController` 负责。

**Service 流程：** 纯委托，无互斥逻辑。

```
// CommentActionServiceImpl.comment() -> doAction(dto, userId, COMMENT)
// CommentActionServiceImpl.uncomment() -> cancelAction(dto, userId, UN_COMMENT)
```

**实体校验：** `CommentBehHandler` 支持 `BizTypeEnum.COMMENT(2)` 和 `CommentTypeEnum.ARTICLE(1)/MESSAGE(2)/LOVE(3)`。

---

## 场景七：浏览行为

**Controller** `POST /behavior/view`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| bizId | Long | 是 | 业务ID |
| bizType | Integer | 是 | 业务类型 |

**前置条件：** 无 `@LoginCheck`，游客可调用

**Service 流程（完全独立，不走模板方法）：**

```
@Async
public void view(BehDTO dto, Long userId) {
    // 浏览不做实体校验（游客也可浏览）
    Long effectiveUserId = userId != null ? userId : 0L;  // 匿名用户 userId=0

    // 直接递增计数（复用父类 protected incrementCount）
    incrementCount(dto, BehActionType.VIEW);

    // 发布事件（异步写日志）
    eventPublisher.publishEvent(new BehActionEvent(this, effectiveUserId, bizId, bizType, VIEW));
}
```

**特殊之处：**
1. 无 `@Transactional`（不需要事务）
2. 无实体存在性校验（允许游客浏览不存在的资源而不报错）
3. 无 `behavior_state` 记录（不追踪谁看过什么，只记计数）
4. 无取消操作（不能"取消浏览"）
5. `@Async` 异步处理（不阻塞HTTP响应）
6. 匿名用户 userId=0L

---

## 架构模式详解

### 1. 模板方法模式 (AbstractBehService)

`AbstractBehService` 定义了 doAction/cancelAction 两个模板方法，封装了所有行为共有的流程骨架：

```
正向操作 doAction():
  实体校验 -> 查询状态 -> 幂等返回/重新激活 -> 互斥处理 -> 保存状态 -> 递增计数 -> 后置处理 -> 发布事件

反向操作 cancelAction():
  实体校验 -> 查询活跃状态 -> 幂等返回 -> 状态设为取消 -> 递减计数 -> 发布事件
```

5个具体服务（Like/Dislike/Subscribe/Forward/Comment）各自只实现 2 个薄方法（正向/反向），复用模板。

### 2. 策略模式 (BehHandlerFactory + BehEntityHandler)

```
BehHandlerFactory (工厂)
  |
  +-> 注入所有 BehEntityHandler 实现 (Spring Map<String, BehEntityHandler>)
  |
  +-> cache: ConcurrentHashMap<Integer, BehEntityHandler>
  |
  +-> getHandler(bizType)
        |
        +-> 遍历所有 Handler.supports(bizType)
        |
        +-> 命中: ArticleBehHandler / CategoryBehHandler / CommentBehHandler / MomentBehHandler
        +-> 未命中: NoOpHandler.INSTANCE (兜底)
```

各 Handler 职责：
- **ArticleBehHandler**: 校验 ArticleMapper.selectById，afterAction 预留缓存清除
- **CategoryBehHandler**: 同时校验 CategoryMapper 和 TagMapper（订阅专栏/标签）
- **CommentBehHandler**: 校验 CommentMapper，支持多个 CommentTypeEnum
- **MomentBehHandler**: 校验 MomentMapper
- **NoOpHandler**: validate 和 afterAction 均为空操作

### 3. 配置驱动的互斥注册 (MutexRegistry)

```
@PostConstruct 初始化:
  mutexMap = {
    LIKE -> DISLIKE,
    DISLIKE -> LIKE
  }

扩展方式: 在 init() 中添加新规则即可，无需修改业务代码
```

LikeServiceImpl / DislikeServiceImpl 在 `handleMutex()` 中覆写基类空实现，通过 `MutexRegistry.getMutexType()` 获取互斥目标，查询并取消对方的活跃状态。

### 4. 事件驱动的异步日志 (BehActionEvent + BehLogEventListener)

```
Service.doAction() 同步流程:
  1. 保存 BehaviorState
  2. 更新 BehaviorCnt
  3. eventPublisher.publishEvent(new BehActionEvent(...))

BehLogEventListener.handleBehAction(@Async):
  4. 异步线程池执行
  5. 新建 BehaviorLog 记录
  6. behaviorLogService.save(logEntry)
```

事件携带数据：userId, bizId, bizType, actionCode。异步写入失败不影响主业务流程（catch 异常仅日志记录）。

---

## 接口契约

### 统一请求 DTO (BehDTO)

```java
public class BehDTO {
    private Long userId;     // 用户ID（通常从 token 获取，前端可不传）
    private Long bizId;      // 业务ID（文章/评论/专栏/标签ID）
    private Integer bizType; // 业务类型（BizTypeEnum / CommentTypeEnum 编码）
}
```

### 行为服务接口 (IActionService)

所有支持正向/反向操作的服务均实现此标记接口：

```java
public interface IActionService { }  // 标记接口，用于 Controller 类型判断

// 各服务接口约定：
LikeService extends IActionService {
    void like(BehDTO dto, Long userId);
    void unlike(BehDTO dto, Long userId);
}
// DislikeService / SubscribeService / ForwardService / CommentActionService 同理
```

ViewService 不继承 IActionService（无取消操作，独立流程）。

---

## 幂等性与状态翻转

### 幂等保证

| 场景 | 保证机制 |
|------|---------|
| 重复点赞 | doAction 查询已有 ACTIVE 状态，直接 return |
| 重复取消 | cancelAction 查询不到 ACTIVE 状态，直接 return |
| 重复订阅/转发/评论 | 同点赞逻辑 |

### 状态翻转（非删除）

```
点赞:   INSERT behavior_state (status=1, 活跃)
取消:   UPDATE behavior_state SET status=2 (已取消)
重赞:   UPDATE behavior_state SET status=1 (重新激活)
```

优势：保留完整操作历史，支持审计追溯和数据分析。

### 计数安全

```
递增:   INSERT behavior_cnt (count=1) 或 UPDATE count++
递减:   UPDATE count-- (count > 0 才减，防止负数)
```

`BehaviorCnt` 包含 `version` 乐观锁字段，但当前 `incrementCount` / `decrementCount` 实现中未使用 CAS 更新（存在并发竞态条件，高并发场景建议引入 Redis 或乐观锁 UPDATE ... SET version=version+1 WHERE version=?）。

---

## 附录：Controller 端点一览

| 端点 | 方法 | 行为 | 登录要求 |
|------|------|------|---------|
| `/behavior/like` | POST | 点赞 | `@LoginCheck` |
| `/behavior/like` | DELETE | 取消点赞 | `@LoginCheck` |
| `/behavior/dislike` | POST | 点踩 | `@LoginCheck` |
| `/behavior/dislike` | DELETE | 取消点踩 | `@LoginCheck` |
| `/behavior/subscribe` | POST | 订阅 | `@LoginCheck` |
| `/behavior/subscribe` | DELETE | 取消订阅 | `@LoginCheck` |
| `/behavior/forward` | POST | 转发 | `@LoginCheck` |
| `/behavior/forward` | DELETE | 取消转发 | `@LoginCheck` |
| `/behavior/comment` | POST | 记录评论 | `@LoginCheck` |
| `/behavior/comment` | DELETE | 取消评论记录 | `@LoginCheck` |
| `/behavior/view` | POST | 浏览 | 无需登录 |

---

## 附录：状态与权限矩阵

| 操作 | 权限要求 | 实体校验 | 互斥处理 | 状态存储 | 计数更新 | 异步日志 |
|------|---------|---------|---------|---------|---------|---------|
| 点赞 | `@LoginCheck` | Article/Comment/Moment/Category | 取消点踩 | behavior_state | behavior_cnt | behavior_log |
| 取消点赞 | `@LoginCheck` | Article/Comment/Moment/Category | 无 | status=2 | count-- | behavior_log |
| 点踩 | `@LoginCheck` | Article/Comment/Moment/Category | 取消点赞 | behavior_state | behavior_cnt | behavior_log |
| 取消点踩 | `@LoginCheck` | Article/Comment/Moment/Category | 无 | status=2 | count-- | behavior_log |
| 订阅 | `@LoginCheck` | Category/Tag | 无 | behavior_state | behavior_cnt | behavior_log |
| 取消订阅 | `@LoginCheck` | Category/Tag | 无 | status=2 | count-- | behavior_log |
| 转发 | `@LoginCheck` | 任意 | 无 | behavior_state | behavior_cnt | behavior_log |
| 取消转发 | `@LoginCheck` | 任意 | 无 | status=2 | count-- | behavior_log |
| 评论记录 | `@LoginCheck` | Comment | 无 | behavior_state | behavior_cnt | behavior_log |
| 取消评论 | `@LoginCheck` | Comment | 无 | status=2 | count-- | behavior_log |
| 浏览 | 无需登录 | 无 | 无 | 无 | behavior_cnt | behavior_log |

---

## 待实现清单

| 功能 | 优先级 | 说明 |
|------|--------|------|
| Admin 行为管理 | P1 | 后台查看/管理用户行为状态（AdminBehaviorController 目前为空骨架） |
| 乐观锁升级 | P2 | BehaviorCnt 的 increment/decrement 引入 CAS 版本控制 |
| 浏览去重 | P2 | 当前浏览仅计数不记录 state，如需去重需补充 behavior_state 记录 |
| 互斥策略扩展 | P2 | 如需新增互斥对（如收藏 vs 举报），在 MutexRegistry.init() 添加即可 |
| 订阅通知 | P3 | SubscribeServiceImpl.afterAction 扩展：触发邮件/站内信通知 |
| 行为统计聚合 | P3 | 基于 behavior_cnt 提供热门内容排行等统计接口 |
| OpenAPI 注解补充 | P3 | 已有部分 @Operation/@ApiResponses，需完善参数描述 |
