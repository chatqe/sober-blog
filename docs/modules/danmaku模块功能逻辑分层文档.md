# Danmaku 模块功能逻辑分层文档

> 生成日期: 2026-06-21
> 技术栈: Spring Boot 2.7 + MyBatis-Plus + @SaveCheck 防刷

---

## 模块概览

```
danmaku/
  ├── controller/
  │   ├── web/
  │   │   └── DanmakuController.java          # 公开：保存/列表/最新/批量
  │   └── admin/
  │       └── AdminDanmakuController.java      # 管理端：分页查询
  ├── service/
  │   ├── DanmakuService.java                 # 含 saveList/racdomList/latestDanmaku
  │   └── DanmakuServiceImpl.java             # 实现类
  └── domain/
      ├── pojo/Danmaku.java                   # 弹幕实体
      └── dao/DanmakuMapper.java              # 继承 CoreMapper<Danmaku>
```

---

## 场景一：保存弹幕（防刷）

**Controller** `POST /danmaku/save`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| avatar | String | 否 | 头像 URL |
| message | String | 是 | 留言内容 |

**注解：** `@PostMapping("/save")`、`@SaveCheck`（防重复提交/限流）

**Service 流程：**

```
@PostMapping("/save")
@SaveCheck
public R<Danmaku> save(@RequestBody Danmaku danmaku) {

    // Step 1: 校验 message 非空
    if (!StringUtils.hasText(danmaku.getMessage())) {
        return R.fail("留言内容不能为空");
    }

    // Step 2: 若 avatar 为空，分配随机头像
    if (!StringUtils.hasText(danmaku.getAvatar())) {
        String avatar = QingsiUtil.getRandomAvatar(null);
        danmaku.setAvatar(avatar);
    }

    // Step 3: 保存数据库
    danmakuService.save(danmaku);

    return R.success(danmaku);
}
```

**@SaveCheck 注解作用：** 基于 Redis 的防刷限流，防止同一用户短时间内重复提交弹幕。

---

## 场景二：删除弹幕（管理员）

**Controller** `GET /danmaku/del?id={id}`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Integer | 是 | 弹幕 ID |

**注解：** `@LoginCheck(0)`

**Service 流程：**

```
@GetMapping("/del")
public R del(@RequestParam("id") Integer id) {
    danmakuMapper.deleteById(id);
    return R.success();
}
```

---

## 场景三：随机获取弹幕列表

**Controller** `GET /danmaku/list`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 无 | - | - | 公开接口 |

**注解：** `@GetMapping("/list")`

**Service 流程（从 100 条中随机采样）：**

```
@GetMapping("/list")
public R<List<Danmaku>> list() {
    return R.success(danmakuService.racdomList());
}
```

**DanmakuServiceImpl.racdomList() 核心逻辑：**

```
public List<Danmaku> racdomList() {

    // Step 1: 获取总记录数
    long count = danmakuService.count();

    List<Danmaku> result;

    if (count > CommonConst.TREE_HOLE_COUNT) {  // TREE_HOLE_COUNT = 100
        // Step 2: 总数超过 100，随机偏移分页
        Random random = new Random();
        int offset = random.nextInt((int)(count - 100));

        // Step 3: 分页查询 100 条（只查必要字段）
        result = danmakuMapper.selectList(
            Wrappers.<Danmaku>lambdaQuery()
                .select(Danmaku::getId, Danmaku::getAvatar,
                        Danmaku::getMessage, Danmaku::getCreateTime)
                .last("limit " + offset + ", 100")
        );
    } else {
        // Step 4: 总数不超过 100，直接返回全部
        result = danmakuService.list();
    }

    // Step 5: 补全缺失的头像
    for (Danmaku d : result) {
        if (!StringUtils.hasText(d.getAvatar())) {
            d.setAvatar(BlogUtil.getRandomAvatar(d.getId().toString()));
        }
    }

    return result;
}
```

---

## 场景四：获取最新 20 条弹幕

**Controller** `GET /danmaku/latestDanmaku`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 无 | - | - | 公开接口 |

**注解：** `@GetMapping("/latestDanmaku")`

**Service 流程：**

```
public List<Danmaku> latestDanmaku() {

    // Step 1: 按创建时间降序取前 20 条
    List<Danmaku> result = new LambdaQueryChainWrapper<>(danmakuMapper)
        .orderByDesc(Danmaku::getCreateTime)
        .last("limit 20")
        .list();

    // Step 2: 空结果返回空列表
    if (result == null || result.isEmpty()) {
        return new ArrayList<>();
    }

    // Step 3: 补全缺失的头像
    for (Danmaku d : result) {
        if (!StringUtils.hasText(d.getAvatar())) {
            d.setAvatar(BlogUtil.getRandomAvatar(d.getId().toString()));
        }
    }

    return result;
}
```

---

## 场景五：批量保存弹幕（存根）

**Controller** `POST /danmaku/saveList`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 列表 | List<Danmaku> | 是 | 弹幕列表 |

**注解：** `@PostMapping("/saveList")`

**Service 流程：**

```
@PostMapping("/saveList")
public R saveList(@RequestBody List<Danmaku> danmakus) {
    // 当前实际返回 null，是未实现的存根
    String result = danmakuService.saveList(danmakus);
    return R.success(result);
}
```

**DanmakuServiceImpl.saveList()：**

```
public String saveList(List<Danmaku> danmakus) {
    // TODO: 批量保存逻辑未实现，返回 null
    // 原注释代码：遍历分配随机头像后批量保存
    return null;
}
```

---

## 场景六：管理端分页查询弹幕

**Controller** `POST /admin/danmaku/boss/list`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 是 | 页码 |
| pageSize | Integer | 是 | 每页条数 |

**注解：** `@LoginCheck(0)`

**Service 流程（存在 Bug）：**

```
@PostMapping("/boss/list")
public R<Page> listBossDanmaku(@RequestBody BaseReqVO baseReqVO) {

    // Step 1: 构建查询条件
    LambdaQueryChainWrapper<Danmaku> wrapper = new LambdaQueryChainWrapper<>(danmakuMapper)
        .orderByDesc(Danmaku::getCreateTime);

    // BUG: 查询结果未赋值给变量，返回的是原始 baseReqVO
    wrapper.page(baseReqVO);

    return R.success(baseReqVO);  // 应该返回 wrapper.page() 的结果
}
```

---

## 附录：实体字段文档

### Danmaku 实体

**表名：** `danmaku`
**继承：** `BaseDO`

| 字段名 | 类型 | 数据库列名 | 说明 |
|--------|------|-----------|------|
| id | Long | id | 雪花算法主键 |
| isDel | Integer | is_del | 逻辑删除（0=未删，1=已删） |
| createTime | LocalDateTime | create_time | 创建时间（自动填充） |
| updateTime | LocalDateTime | update_time | 更新时间（自动填充） |
| avatar | String | avatar | 头像 URL |
| message | String | message | 留言内容 |
| createBy | String | create_by | 创建人 |

---

## 约束与权限矩阵

| 操作 | 权限要求 | 校验规则 |
|------|---------|---------|
| 保存弹幕 | `@SaveCheck` | message 非空 |
| 删除弹幕 | `@LoginCheck(0)` | 管理员 |
| 随机列表 | 公开 | 无 |
| 最新 20 条 | 公开 | 无 |
| 批量保存 | 公开 | 存根未实现 |
| 管理端分页 | `@LoginCheck(0)` | 管理员（有 Bug） |

---

## 待实现清单

| 问题 | 优先级 | 说明 |
|------|--------|------|
| saveList 批量保存未实现 | P1 | Service 层返回 null，Controller 层也未处理 |
| AdminDanmakuController 查询结果未返回 | P1 | page() 结果未赋值，始终返回原始请求参数 |
| racdomList 方法名拼写错误 | P3 | 应为 randomList 而非 racdomList |
| 头像分配逻辑分散 | P2 | 随机头像分配在多个方法中重复，应抽取为统一方法 |
| 缺少敏感词过滤 | P1 | 留言内容未做任何过滤，存在 XSS/敏感词风险 |
| @SaveCheck 未配置限流参数 | P2 | 当前使用默认限流，应明确配置频率限制 |
