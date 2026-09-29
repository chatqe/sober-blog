# Family 模块功能逻辑分层文档

> 生成日期: 2026-06-21
> 技术栈: Spring Boot 2.7 + MyBatis-Plus + Redis 缓存

---

## 模块概览

```
family/
  ├── controller/
  │   └── admin/
  │       └── FamilyController.java          # 管理端：表白墙/恋爱计时 CRUD
  ├── service/
  │   ├── FamilyService.java                 # 继承 CoreService<Family>
  │   └── FamilyServiceImpl.java             # 继承 CoreServiceImpl，无自定义逻辑
  ├── domain/
  │   ├── pojo/Family.java                   # 家庭（表白墙）实体
  │   ├── dao/FamilyMapper.java              # 继承 CoreMapper<Family>
  │   └── enums/
  │       └── FamilyTypeEnum.java            # 家庭类型枚举
  └── controller/web/vo/
      └── FamilyVO.java                      # 前端传输对象
```

该模块管理"表白墙"功能，包含恋爱计时、倒计时、情侣头像等配置。

---

## 场景一：保存/更新家庭配置

**Controller** `POST /family/saveFamily`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| bgCover | String | 是 | 背景封面图 |
| manCover | String | 是 | 男生头像 |
| womanCover | String | 是 | 女生头像 |
| manName | String | 是 | 男生昵称 |
| womanName | String | 是 | 女生昵称 |
| timing | String | 是 | 计时信息 |
| countdownTitle | String | 否 | 倒计时标题 |
| countdownTime | String | 否 | 倒计时时间 |
| familyInfo | String | 否 | 额外信息 |
| likeCount | Integer | 否 | 点赞数 |

**注解：** `@LoginCheck`（需要登录）、`@Validated`

**Service 流程：**

```
@PostMapping("/saveFamily")
public R<Void> saveFamily(@Validated @RequestBody FamilyVO familyVO) {

    // Step 1: 获取当前登录用户 ID
    Long userId = BlogUtil.getUserId();

    // Step 2: 按 userId 查询是否已有记录
    Family existing = familyService.getOne(
        new LambdaQueryWrapper<Family>()
            .eq(Family::getUserId, userId)
    );

    // Step 3: 判断是否为管理员
    Long adminId = BlogUtil.getAdminUser().getId();
    boolean isAdmin = userId.equals(adminId);

    // Step 4: 构建或更新实体
    if (existing != null) {
        // 更新已有记录
        existing.setBgCover(familyVO.getBgCover());
        existing.setManCover(familyVO.getManCover());
        existing.setWomanCover(familyVO.getWomanCover());
        existing.setManName(familyVO.getManName());
        existing.setWomanName(familyVO.getWomanName());
        existing.setTiming(familyVO.getTiming());
        existing.setCountdownTitle(familyVO.getCountdownTitle());
        existing.setCountdownTime(familyVO.getCountdownTime());
        existing.setFamilyInfo(familyVO.getFamilyInfo());

        // 管理员强制启用
        existing.setStatus(isAdmin);

        familyService.updateById(existing);
    } else {
        // 新建记录
        Family family = new Family();
        family.setUserId(userId);
        family.setBgCover(familyVO.getBgCover());
        family.setManCover(familyVO.getManCover());
        family.setWomanCover(familyVO.getWomanCover());
        family.setManName(familyVO.getManName());
        family.setWomanName(familyVO.getWomanName());
        family.setTiming(familyVO.getTiming());
        family.setCountdownTitle(familyVO.getCountdownTitle());
        family.setCountdownTime(familyVO.getCountdownTime());
        family.setFamilyInfo(familyVO.getFamilyInfo());
        family.setStatus(isAdmin);  // 管理员直接启用，普通用户需审核

        familyService.save(family);
    }

    // Step 5: 管理员保存后写入缓存
    if (isAdmin) {
        qingSiCache.put(CommonConst.ADMIN_FAMILY, family);
    }

    // Step 6: 删除列表缓存（使随机列表失效）
    qingSiCache.del(CommonConst.FAMILY_LIST);

    return R.success();
}
```

---

## 场景二：删除家庭配置

**Controller** `GET /family/deleteFamily?id={id}`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 家庭记录 ID |

**注解：** `@LoginCheck(0)`

**Service 流程：**

```
@GetMapping("/deleteFamily")
public R<Void> deleteFamily(@RequestParam("id") Long id) {
    familyService.removeById(id);
    qingSiCache.del(CommonConst.FAMILY_LIST);
    return R.success();
}
```

---

## 场景三：获取当前用户家庭配置

**Controller** `GET /family/getFamily`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 无 | - | - | 需登录 |

**注解：** `@LoginCheck`

**Service 流程：**

```
@GetMapping("/getFamily")
public R<FamilyVO> getFamily() {
    Long userId = BlogUtil.getUserId();

    Family family = familyService.getOne(
        new LambdaQueryWrapper<Family>()
            .eq(Family::getUserId, userId)
    );

    if (family == null) {
        return R.success(new FamilyVO());  // 返回空对象
    }

    // 转换为 VO 返回
    FamilyVO vo = BeanUtil.copyProperties(family, FamilyVO.class);
    return R.success(vo);
}
```

---

## 场景四：获取管理员家庭配置（公开）

**Controller** `GET /family/getAdminFamily`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 无 | - | - | 公开接口 |

**注解：** 无（无需登录）

**Service 流程（Redis 缓存优先）：**

```
@GetMapping("/getAdminFamily")
public R<FamilyVO> getAdminFamily() {

    // Step 1: 从 Redis 读取管理员家庭数据
    Family cached = qingSiCache.get(CommonConst.ADMIN_FAMILY, Family.class);

    if (cached != null) {
        return R.success(convertToVO(cached));
    }

    // Step 2: 缓存未命中，查数据库
    Long adminId = BlogUtil.getAdminUser().getId();
    Family family = familyService.getOne(
        new LambdaQueryWrapper<Family>()
            .eq(Family::getUserId, adminId)
    );

    if (family == null) {
        return R.fail("请初始化表白墙");
    }

    // Step 3: 写入缓存
    qingSiCache.put(CommonConst.ADMIN_FAMILY, family);

    return R.success(convertToVO(family));
}
```

---

## 场景五：随机获取家庭列表

**Controller** `GET /family/listRandomFamily?size={n}`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| size | Integer | 否 | 返回数量，默认 10 |

**注解：** 无（公开接口）

**Service 流程：**

```
@GetMapping("/listRandomFamily")
public R<List<FamilyVO>> listRandomFamily(
        @RequestParam(value = "size", defaultValue = "10") Integer size) {

    // Step 1: 调用 CommonQuery.getFamilyList() 获取全部已启用家庭
    List<Family> families = commonQuery.getFamilyList();

    // Step 2: 随机采样
    if (families.size() > size) {
        Collections.shuffle(families);
        families = families.subList(0, size);
    }

    // Step 3: 转换为 VO
    List<FamilyVO> result = families.stream()
        .map(f -> BeanUtil.copyProperties(f, FamilyVO.class))
        .collect(Collectors.toList());

    return R.success(result);
}
```

**CommonQuery.getFamilyList() 缓存逻辑：**

```
public List<Family> getFamilyList() {
    // Step 1: 尝试从缓存读取
    List<Family> cached = qingSiCache.get(CommonConst.FAMILY_LIST, List.class);
    if (cached != null) {
        return cached;
    }

    // Step 2: 双重检查锁
    synchronized (CommonConst.FAMILY_LIST.intern()) {
        cached = qingSiCache.get(CommonConst.FAMILY_LIST, List.class);
        if (cached != null) {
            return cached;
        }

        // Step 3: 查询所有已启用的家庭
        List<Family> families = familyMapper.selectList(
            new LambdaQueryWrapper<Family>()
                .eq(Family::getStatus, true)
        );

        // Step 4: 写回缓存
        qingSiCache.put(CommonConst.FAMILY_LIST, families);

        return families;
    }
}
```

---

## 场景六：管理端分页查询家庭列表

**Controller** `POST /family/listFamily`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 是 | 页码 |
| pageSize | Integer | 是 | 每页条数 |
| status | Boolean | 否 | 按状态过滤 |

**注解：** `@LoginCheck(0)`

**Service 流程：**

```
@PostMapping("/listFamily")
public R<Page> listFamily(@RequestBody BaseReqVO baseReqVO) {

    LambdaQueryWrapper<Family> wrapper = new LambdaQueryWrapper<>();

    // 状态过滤
    if (baseReqVO.getStatus() != null) {
        wrapper.eq(Family::getStatus, baseReqVO.getStatus());
    }

    // 按创建时间降序排列
    wrapper.orderByDesc(Family::getCreateTime);

    // 分页查询
    Page<Family> page = familyService.page(baseReqVO.toPage(), wrapper);

    return R.success(page);
}
```

---

## 场景七：切换家庭启用状态

**Controller** `GET /family/changeLoveStatus?id={id}&flag={true/false}`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Integer | 是 | 家庭记录 ID |
| flag | Boolean | 是 | true=启用，false=禁用 |

**注解：** `@LoginCheck(0)`

**Service 流程：**

```
@GetMapping("/changeLoveStatus")
public R<Void> changeLoveStatus(
        @RequestParam("id") Integer id,
        @RequestParam("flag") Boolean flag) {

    Family family = new Family();
    family.setId(id.longValue());
    family.setStatus(flag);
    familyService.updateById(family);

    // 清除列表缓存
    qingSiCache.del(CommonConst.FAMILY_LIST);

    return R.success();
}
```

---

## 附录：实体字段文档

### Family 实体

**表名：** `family`
**继承：** `BaseDO`

| 字段名 | 类型 | 数据库列名 | 说明 |
|--------|------|-----------|------|
| userId | Long | user_id | 用户 ID |
| bgCover | String | bg_cover | 背景封面图 |
| manCover | String | man_cover | 男生头像 |
| womanCover | String | woman_cover | 女生头像 |
| manName | String | man_name | 男生昵称 |
| womanName | String | woman_name | 女生昵称 |
| timing | String | timing | 计时信息 |
| countdownTitle | String | countdown_title | 倒计时标题 |
| status | Boolean | status | 是否启用（0=否，1=是） |
| countdownTime | String | countdown_time | 倒计时时间 |
| familyInfo | String | family_info | 额外信息 |
| likeCount | Integer | like_count | 点赞数 |

### FamilyVO 前端传输对象

与 Family 实体基本一致，额外包含：
- `createTime` / `updateTime`：LocalDateTime 类型，带 `@JsonFormat` 和 `@DateTimeFormat` 注解（格式 `yyyy-MM-dd HH:mm:ss`）

**校验约束（`@NotBlank`）：** bgCover、manCover、womanCover、manName、womanName、timing 为必填项。

---

## 缓存机制

| 缓存键 | 值 | 用途 | 失效时机 |
|--------|-----|------|---------|
| `ADMIN_FAMILY` | `"adminFamily"` | 管理员家庭配置数据 | save/delete/status 变更时 |
| `FAMILY_LIST` | `"familyList"` | 所有已启用家庭列表 | save/delete/status 变更时 |

缓存读取均采用**双重检查锁 + Redis**模式。

---

## 约束与权限矩阵

| 操作 | 权限要求 | 校验规则 |
|------|---------|---------|
| 保存家庭配置 | `@LoginCheck` | @Validated 校验必填字段 |
| 删除家庭配置 | `@LoginCheck(0)` | 管理员 |
| 获取当前用户家庭 | `@LoginCheck` | 按 userId 查询 |
| 获取管理员家庭 | 公开 | 缓存优先，数据库兜底 |
| 随机家庭列表 | 公开 | 从缓存获取已启用列表 |
| 管理端分页列表 | `@LoginCheck(0)` | 管理员 |
| 切换启用状态 | `@LoginCheck(0)` | 管理员 |

---

## 待实现清单

| 问题 | 优先级 | 说明 |
|------|--------|------|
| 普通用户保存后 status=false 需审核 | P2 | 非管理员创建的数据默认禁用，缺少审核流程 |
| FamilyVO 校验过严 | P3 | bgCover/manCover/womanCover 等为 @NotBlank，但首次创建时可能没有头像 |
| listFamily 返回类型是 BaseReqVO | P3 | 应返回 Page<FamilyVO> 而非原始请求参数 |
| 缺少家庭类型枚举使用 | P3 | FamilyTypeEnum 存在但未在业务中使用 |
