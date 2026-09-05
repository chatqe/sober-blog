# Moment 模块功能逻辑详细分层文档

> 生成日期: 2026-06-21
> 技术栈: Spring Boot 2.7 + MyBatis-Plus + Redis 异步队列

---

## 模块概览

```
moment/
  ├── controller/web/
  │   └── MomentController.java          # 用户端接口
  ├── controller/admin/                  # 待实现：管理端接口
  │   └── AdminMomentController.java
  ├── service/
  │   ├── MomentService.java             # 动态主表 Service
  │   ├── MomentMediaService.java        # 文件关联 Service
  │   ├── MomentSocialSyncService.java   # 三方同步 Service
  │   ├── impl/
  │   │   ├── MomentServiceImpl.java
  │   │   ├── MomentMediaServiceImpl.java
  │   │   └── MomentSocialSyncServiceImpl.java
  │   ├── helper/
  │   │   ├── MomentStateMachine.java    # 状态机守卫
  │   │   ├── FileTypeResolver.java      # 文件类型解析器
  │   │   └── RedundantUpdater.java      # 冗余标识更新器
  │   └── job/
  │       ├── SyncOutboundJob.java       # 出向同步定时任务
  │       └── SyncInboundJob.java        # 入向导入定时任务
  ├── domain/
  │   ├── pojo/
  │   │   ├── Moment.java                # moments 主表
  │   │   ├── MomentMedia.java           # moment_media 关联表
  │   │   └── MomentSocialSync.java      # moment_social_sync 同步表
  │   ├── dao/
  │   │   ├── MomentMapper.java
  │   │   ├── MomentMediaMapper.java
  │   │   └── MomentSocialSyncMapper.java
  │   └── dto/
  │       ├── MomentCreateDTO.java       # 发布/编辑请求体
  │       ├── MomentListQuery.java       # 列表查询参数
  │       ├── MomentAuditDTO.java        # 审核请求体
  │       └── MomentSyncCallbackDTO.java # 三方回调请求体
  └── enums/
      └── MomentStatusEnum.java          # 状态枚举
```

---

## 场景一：发布/创建动态

### 1.1 草稿发布

**Controller** `POST /moment/save`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| content | String | 是 | 动态文本内容（HTML 会被过滤） |
| fileIds | Long[] | 否 | 已上传至 fileInfo 的文件 ID 数组 |
| status | Integer | 否 | 状态：0-草稿（默认） |

**Service 核心事务流程：**

```
@Transactional(rollbackFor = Exception.class)
public void createDraft(MomentCreateDTO dto) {

    // Step 1: 参数校验
    if (!StringUtils.hasText(dto.getContent()) &&
        (dto.getFileIds() == null || dto.getFileIds().length == 0)) {
        throw new BizException("动态内容不能为空");
    }

    // Step 2: 插入 moments 主表
    Moment moment = new Moment();
    moment.setUserId(currentUserId());
    moment.setContent(dto.getContent());
    moment.setHasText(StringUtils.hasText(dto.getContent()));
    moment.setStatus(dto.getStatus() != null ? dto.getStatus() : 0); // 默认草稿
    moment.setVisibility(VisibilityEnum.PUBLIC.code());
    moment.setSource(1); // 本站原创
    momentService.save(moment);
    Long momentId = moment.getId();

    // Step 3: 关联文件
    if (ArrayUtil.isNotEmpty(dto.getFileIds())) {
        List<MomentMedia> mediaList = new ArrayList<>();
        for (int i = 0; i < dto.getFileIds().length; i++) {
            MomentMedia media = new MomentMedia();
            media.setMomentId(momentId);
            media.setFileId(dto.getFileIds()[i]);
            media.setSortOrder(i);
            media.setMediaType(fileTypeResolver.resolve(dto.getFileIds()[i])); // 1/2/3
            mediaList.add(media);
        }
        momentMediaService.saveBatch(mediaList);

        // Step 4: 更新冗余标识
        redundantUpdater.updateHasFlags(momentId);
    }

    // Step 5: 草稿不触发审核和同步
    // （status=0，直接返回）
}
```

### 1.2 直接发布（跳过审核）

**Controller** `POST /moment/publish`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| content | String | 是 | 动态文本内容 |
| fileIds | Long[] | 否 | 已上传文件 ID 数组 |
| visibility | Integer | 否 | 可见性：0-私密 1-好友 2-公开（默认2） |

**Service 核心事务流程：**

```
@Transactional(rollbackFor = Exception.class)
public void publish(MomentCreateDTO dto) {

    // Step 1: 参数校验（同草稿）
    validateContent(dto);

    // Step 2: 插入 moments 主表
    Moment moment = new Moment();
    moment.setUserId(currentUserId());
    moment.setContent(dto.getContent());
    moment.setHasText(StringUtils.hasText(dto.getContent()));
    moment.setStatus(1); // 直接发布为正常状态
    moment.setVisibility(dto.getVisibility() != null ? dto.getVisibility() : 2);
    moment.setSource(1);
    momentService.save(moment);
    Long momentId = moment.getId();

    // Step 3: 关联文件
    if (ArrayUtil.isNotEmpty(dto.getFileIds())) {
        batchInsertMedia(momentId, dto.getFileIds());
        redundantUpdater.updateHasFlags(momentId);
    }

    // Step 4: 投递三方同步队列（仅当 status=1 时）
    if (userHasSyncEnabled(currentUserId())) {
        momentSocialSyncService.enqueueOutboundSync(momentId);
    }
}
```

### 1.3 提交审核

**Controller** `POST /moment/submitAudit`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| momentId | Long | 是 | 动态 ID |

**Service 核心事务流程：**

```
public void submitAudit(Long momentId) {

    // Step 1: 状态机校验 — 仅草稿(0)可提交审核
    Moment moment = momentService.getById(momentId);
    MomentStateMachine.guardTransition(moment.getStatus(), 2);

    // Step 2: 更新状态为审核中
    moment.setStatus(2);
    momentService.updateById(moment);

    // Step 3: 投递审核队列（异步）
    auditQueueService.send(momentId);
}
```

---

## 场景二：内容审核（后台管理）

### 2.1 审核通过

**Controller** `POST /admin/moment/audit/pass`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| momentId | Long | 是 | 动态 ID |
| skipReview | Boolean | 否 | 管理员跳过审核标志 |

**Service 流程：**

```
public void auditPass(Long momentId, boolean skipReview) {

    // Step 1: 乐观锁更新 — 仅 status=2 可审核
    int affected = momentMapper.updateStatusWithLock(
        momentId, 2, 1 // FROM status=2 TO status=1
    );
    if (affected == 0) {
        throw new BizException("该动态不可审核或已被处理");
    }

    // Step 2: 记录审核时间
    momentMapper.updateAuditedAt(momentId, LocalDateTime.now());

    // Step 3: 若为跳过审核（0→1），投递三方同步队列
    if (skipReview) {
        momentSocialSyncService.enqueueOutboundSync(momentId);
    }
}
```

### 2.2 审核驳回

**Controller** `POST /admin/moment/audit/reject`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| momentId | Long | 是 | 动态 ID |
| reason | String | 是 | 驳回原因 |

**Service 流程：**

```
public void auditReject(Long momentId, String reason) {

    // Step 1: 乐观锁更新
    int affected = momentMapper.updateStatusWithLock(
        momentId, 2, 3 // FROM status=2 TO status=3（违规下架）
    );
    if (affected == 0) {
        throw new BizException("该动态不可审核或已被处理");
    }

    // Step 2: 记录驳回原因和时间
    momentMapper.updateAuditInfo(momentId, reason, LocalDateTime.now());
}
```

### 2.3 审核 SQL（乐观锁）

```xml
<!-- MomentMapper.xml -->
<update id="updateStatusWithLock">
    UPDATE moments
    SET status = #{newStatus}, update_time = NOW()
    WHERE id = #{momentId}
      AND status = #{oldStatus}
      AND is_del = 0
</update>
```

---

## 场景三：动态列表查询

### 3.1 权限分层规则

| 请求来源 | SQL 条件拼接 | 说明 |
|----------|-------------|------|
| 未登录访客 | `is_del=0 AND status=1 AND visibility=2` | 仅公开且审核通过 |
| 登录用户（看自己） | `is_del=0 AND userId=? AND status IN (0,1,2,4)` | 草稿+正常+审核中+私密 |
| 登录用户（看他人） | `is_del=0 AND status=1 AND visibility=2` | 同访客 |
| 管理员（审核列表） | `is_del=0 AND status=2` | 只看待审核 |
| 管理员（回收站） | `is_del=1` | 查看所有已删除 |

### 3.2 Service 查询逻辑

```
public PageResult<Moment> listMoments(MomentListQuery query) {

    LambdaQueryWrapper<Moment> wrapper = new LambdaQueryWrapper<>();

    // 基础条件：永远过滤已删除
    wrapper.eq(Moment::getIsDel, 0);

    // 根据角色拼接条件
    if (query.isAdmin()) {
        // 管理员：按指定 status 筛选
        wrapper.eq(query.getStatus() != null, Moment::getStatus, query.getStatus());
    } else if (query.isSelf()) {
        // 本人：可见草稿、审核中、私密、正常
        wrapper.eq(Moment::getUserId, query.getUserId());
        wrapper.in(Moment::getStatus, 0, 1, 2);
    } else {
        // 访客/他人：仅公开+正常
        wrapper.eq(Moment::getStatus, 1);
        wrapper.eq(Moment::getVisibility, 2); // 公开
    }

    // 类型筛选（冗余字段，走索引）
    if (query.hasVideos()) {
        wrapper.eq(Moment::getHasVideos, true);
        wrapper.eq(Moment::getHasImages, false);
        wrapper.eq(Moment::getHasAudios, false);
    }

    // 排序：统一按创建时间倒序
    wrapper.orderByDesc(Moment::getCreateTime);

    // 分页
    return momentService.page(query.toPage(), wrapper);
}
```

### 3.3 索引策略

```
idx_user_del        → (user_id, is_del)           -- 个人动态列表
idx_del_status_time → (is_del, status, create_time DESC) -- 万能列表索引
idx_source_external → (source, source_external_id(128))  -- 导入去重
```

---

## 场景四：动态详情查询

### 4.1 Service 流程

```
public MomentDetailVO getDetail(Long momentId) {

    // Step 1: 查询主表
    Moment moment = momentService.getById(momentId);
    if (moment == null) {
        throw new BizException("动态不存在");
    }

    // Step 2: 权限校验
    checkVisibility(moment); // 私密的只能作者本人查看

    // Step 3: 查询文件列表
    List<MomentMedia> mediaList = momentMediaService
        .lambdaQuery()
        .eq(MomentMedia::getMomentId, momentId)
        .orderByAsc(MomentMedia::getSortOrder)
        .list();

    // Step 4: 关联 fileInfo 获取真实 URL
    enrichMediaUrls(mediaList);

    // Step 5: 浏览量自增（仅公开+非作者本人）
    if (moment.getVisibility() == 2 && !isAuthor(momentId, currentUserId())) {
        viewsIncrementService.asyncIncrement(momentId);
    }

    // Step 6: 组装 VO 返回
    return convertToDetailVO(moment, mediaList);
}
```

### 4.2 权限校验规则

```
private void checkVisibility(Moment moment) {
    if (moment.getIsDel() == 1) {
        // 已删除：仅作者或管理员可查看
        if (!isAuthorOrAdmin()) throw new BizException("该内容已删除");
    }
    if (moment.getStatus() == 3) {
        // 违规下架：仅作者或管理员可查看
        if (!isAuthorOrAdmin()) throw new BizException("该内容已被下架");
    }
    if (moment.getVisibility() == 0) {
        // 私密：仅作者可查看
        if (!isAuthor()) throw new BizException("该内容仅自己可见");
    }
}
```

---

## 场景五：编辑/更新动态

### 5.1 Service 流程

```
public void updateMoment(Long momentId, MomentCreateDTO dto) {

    Moment moment = momentService.getById(momentId);
    if (moment == null) {
        throw new BizException("动态不存在");
    }

    // Step 1: 状态机校验 — 仅草稿(0)和违规(3)可编辑
    MomentStateMachine.guardEditAllowed(moment.getStatus());

    // Step 2: 更新文本内容
    moment.setContent(dto.getContent());
    moment.setHasText(StringUtils.hasText(dto.getContent()));

    // Step 3: 替换文件关联
    // 3a. 删除旧关联（软删除，不删除 fileInfo）
    momentMediaService.remove(
        Wrappers.<MomentMedia>lambdaQuery()
            .eq(MomentMedia::getMomentId, momentId)
    );

    // 3b. 插入新关联
    if (ArrayUtil.isNotEmpty(dto.getFileIds())) {
        batchInsertMedia(momentId, dto.getFileIds());
        redundantUpdater.updateHasFlags(momentId);
    }

    // Step 4: 状态回退
    if (moment.getStatus() == 3) {
        // 违规下架 → 重新进入审核流程
        moment.setStatus(2);
    }

    momentService.updateById(moment);
}
```

### 5.2 状态机守卫

```
public class MomentStateMachine {

    // 允许的编辑状态
    private static final Set<Integer> EDITABLE_STATUSES = Set.of(0, 3);

    public static void guardEditAllowed(Integer currentStatus) {
        if (!EDITABLE_STATUSES.contains(currentStatus)) {
            throw new BizException("当前状态不允许编辑（已发布或审核中的内容不可修改）");
        }
    }

    // 允许的流转规则：status(old) → status(new)
    private static final Map<Integer, Set<Integer>> TRANSITIONS = Map.of(
        0, Set.of(0, 2),  // 草稿 → 草稿 / 提交审核
        2, Set.of(1, 3),  // 审核中 → 通过 / 驳回
        3, Set.of(2),     // 违规 → 重新审核（编辑后）
        1, Set.of()       // 正常 → 不可流转（只能软删除）
    );

    public static void guardTransition(Integer from, Integer to) {
        Set<Integer> allowed = TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new BizException("非法状态流转: " + from + " → " + to);
        }
    }
}
```

---

## 场景六：软删除、恢复与物理清理

### 6.1 软删除

```
public void softDelete(Long momentId) {
    Moment moment = momentService.getById(momentId);

    // 仅作者本人或管理员可删除
    if (!isAuthorOrAdmin(moment.getUserId())) {
        throw new BizException("无权删除该动态");
    }

    // 更新软删除标记
    moment.setIsDel(1);
    moment.setDelTime(LocalDateTime.now());
    moment.setDelBy(currentUserId());
    momentService.updateById(moment);

    // 异步投递：通知三方平台删除
    momentSocialSyncService.enqueueDeleteSync(momentId);
}
```

### 6.2 恢复

```
public void restoreMoment(Long momentId) {
    Moment moment = momentService.getById(momentId);

    if (moment.getIsDel() != 1) {
        throw new BizException("该动态未被删除");
    }

    moment.setIsDel(0);
    moment.setDelTime(null);
    momentService.updateById(moment);

    // 状态恢复：若原 status=1 且未过审核时效，直接恢复
    // 若 status=2 或 3，保持原状（仍在审核/违规中）
}
```

### 6.3 物理清理定时任务

```
@Scheduled(cron = "0 0 3 * * ?") // 每天凌晨 3 点
public void physicalCleanup() {
    // 删除超过 30 天的已删除动态
    LocalDateTime cutoff = LocalDateTime.now().minusDays(30);

    // Step 1: 查询待清理的 moment_ids
    List<Long> ids = momentService.lambdaQuery()
        .eq(Moment::getIsDel, 1)
        .lt(Moment::getDelTime, cutoff)
        .select(Moment::getId)
        .list()
        .stream()
        .map(Moment::getId)
        .collect(Collectors.toList());

    if (ids.isEmpty()) return;

    // Step 2: 级联删除 moment_media（解绑，不删除 fileInfo）
    momentMediaService.remove(
        Wrappers.<MomentMedia>lambdaQuery()
            .in(MomentMedia::getMomentId, ids)
    );

    // Step 3: 物理删除 moments 主表记录
    momentService.removeByIds(ids);
}
```

---

## 场景七：多平台同步

### 7.1 出向同步（本站 → 微信/微博/QQ）

**异步 Job 监听：**

```
@Scheduled(fixedDelay = 5000) // 每 5 秒轮询
public void outboundSyncJob() {

    // 查询待同步记录
    List<MomentSocialSync> pendingSyncs = momentSocialSyncService
        .lambdaQuery()
        .eq(MomentSocialSync::getDirection, 1) // 出向
        .eq(MomentSocialSync::getSyncStatus, 0) // 待同步
        .last("LIMIT 10")
        .list();

    for (MomentSocialSync sync : pendingSyncs) {
        try {
            // 更新为同步中
            sync.setSyncStatus(1);
            momentSocialSyncService.updateById(sync);

            // 查询动态主表和文件列表
            Moment moment = momentService.getById(sync.getMomentId());
            List<MomentMedia> mediaList = momentMediaService
                .lambdaQuery()
                .eq(MomentMedia::getMomentId, sync.getMomentId())
                .list();

            // 调用三方 API
            SyncResponse response = syncClient.sync(
                sync.getPlatform(),
                moment.getContent(),
                mediaList
            );

            // 同步成功
            sync.setExternalId(response.getExternalId());
            sync.setExternalUrl(response.getExternalUrl());
            sync.setSyncStatus(2); // 成功
            sync.setSyncedAt(LocalDateTime.now());
            momentSocialSyncService.updateById(sync);

        } catch (Exception e) {
            // 同步失败：重试
            sync.setRetryCount(sync.getRetryCount() + 1);
            if (sync.getRetryCount() >= 3) {
                sync.setSyncStatus(3); // 最终失败
                sync.setErrorMessage(e.getMessage());
            }
            momentSocialSyncService.updateById(sync);
        }
    }
}
```

### 7.2 入向导入（微信/QQ → 本站）

**Controller（三方回调接口）：**

```
@PostMapping("/callback/{platform}")
public R inboundCallback(@PathVariable String platform,
                         @RequestBody SyncCallbackDTO dto) {

    // Step 1: 去重校验
    Moment existing = momentService.lambdaQuery()
        .eq(Moment::getSource, platformToSourceCode(platform))
        .eq(Moment::getSourceExternalId, dto.getExternalId())
        .one();

    if (existing != null) {
        return R.success("已存在，跳过导入");
    }

    // Step 2: 转换内容并插入
    Moment moment = new Moment();
    moment.setUserId(dto.getUserId());
    moment.setContent(dto.getContent());
    moment.setHasText(StringUtils.hasText(dto.getContent()));
    moment.setStatus(1); // 入向默认免审
    moment.setSource(platformToSourceCode(platform));
    moment.setSourceExternalId(dto.getExternalId());
    moment.setSourceUserId(dto.getExternalUserId());
    momentService.save(moment);

    // Step 3: 导入文件（下载→fileInfo→moment_media）
    if (CollUtil.isNotEmpty(dto.getMediaList())) {
        importMediaFiles(moment.getId(), dto.getMediaList(), platform);
    }

    // Step 4: 记录同步日志
    MomentSocialSync sync = new MomentSocialSync();
    sync.setMomentId(moment.getId());
    sync.setDirection(2); // 入向
    sync.setPlatform(platform);
    sync.setSyncStatus(2); // 成功
    sync.setExternalId(dto.getExternalId());
    momentSocialSyncService.save(sync);

    return R.success();
}
```

---

## 附录 A：枚举定义

### MomentStatusEnum

```java
public enum MomentStatusEnum implements BaseEnum<Integer> {
    DRAFT(0, "草稿"),
    NORMAL(1, "正常（已发布）"),
    AUDITING(2, "审核中"),
    VIOLATION(3, "违规下架");

    private final Integer code;
    private final String desc;
}
```

### MomentSourceEnum

```java
public enum MomentSourceEnum implements BaseEnum<Integer> {
    OWN(1, "本站原创"),
    WECHAT(2, "微信"),
    QQ(3, "QQ空间"),
    WEIBO(4, "微博"),
    DOUYIN(5, "抖音");

    private final Integer code;
    private final String desc;
}
```

### MomentMediaTypeEnum

```java
public enum MomentMediaTypeEnum implements BaseEnum<Integer> {
    IMAGE(1, "图片"),
    VIDEO(2, "视频"),
    AUDIO(3, "音频");

    private final Integer code;
    private final String desc;
}
```

### MomentDirectionEnum

```java
public enum MomentDirectionEnum implements BaseEnum<Integer> {
    OUTBOUND(1, "出向：本站发往三方"),
    INBOUND(2, "入向：三方拉取至本站");

    private final Integer code;
    private final String desc;
}
```

---

## 附录 B：三方同步状态机

```
moment_social_sync.sync_status 流转：

  0-待同步 ──────→ 1-同步中 ──────→ 2-成功
    ↑                                  │
    │                                  ↓
    └──────────── 3-失败 ◄──────── 重试 ≤ 3

重试超过 3 次 → 最终失败，需人工介入
```

---

## 附录 C：状态流转总览

```
moments.status 流转图：

  草稿(0) ──提交审核──→ 审核中(2) ──通过──→ 正常(1)
    │                      │                    │
    │ 编辑                  └─驳回──→ 违规(3) ←──┘
    │                      ▲   │
    └──────────────────────┴───┘
           编辑违规内容自动回退到审核中
```

---

## 附录 D：待实现清单

| 功能 | 优先级 | 说明 |
|------|--------|------|
| AdminController | P1 | 管理端：审核列表、违规下架、回收站 |
| 定时发布(publish_at) | P2 | 增加 publish_at 字段，未到时间对非作者隐藏 |
| 评论联动 | P2 | 评论模块的 biz_type 需支持 MOMENT 类型 |
| 点赞/收藏联动 | P2 | 行为模块需支持 MOMENT 实体的点赞收藏 |
| OpenAPI 注解 | P3 | 为 Controller 补充 @Operation/@Tag |
| 单元测试 | P3 | 核心 Service 方法覆盖 |
