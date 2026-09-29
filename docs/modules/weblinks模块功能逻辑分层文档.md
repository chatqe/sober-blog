# Weblinks 模块功能逻辑分层文档

> 生成日期: 2026-06-21
> 技术栈: Spring Boot 2.7 + MyBatis-Plus（无 Service 层）

---

## 模块概览

```
weblinks/
  ├── controller/
  │   ├── web/
  │   │   ├── WeblinksController.java             # 友链/通用资源管理
  │   │   ├── CollectController.java              # 收藏夹
  │   │   ├── MusicController.java                # 音乐
  │   │   ├── PictureController.java              # 爱情相册
  │   │   ├── QiniuController.java                # 七牛云直传（与 file 模块重复）
  │   │   └── ResourceAggregationController.java  # 资源聚合（与 WeblinksController 重复）
  │   └── admin/
  │       └── AdminWeblinksController.java        # （如有）管理端
  ├── domain/
  │   ├── pojo/Weblinks.java                      # 资源链接实体
  │   ├── dao/WeblinksMapper.java                 # 继承 CoreMapper<Weblinks>
  │   └── convert/WeblinksConvert.java            # MapStruct 转换器
  └── controller/web/vo/
      └── WeblinksVO.java                         # 前端传输对象
```

**核心特征：所有 Controller 直接注入 Mapper 操作数据库，没有 Service 层。** 这是项目中已知的架构缺陷。

所有资源共用一张数据库表 `weblinks`，通过 `type` 字段区分不同业务场景。

---

## 场景一：用户提交友链申请

**Controller** `POST /weblinks/save`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 是 | 友链标题 |
| url | String | 是 | 链接地址 |
| cover | String | 是 | 封面图 |
| desc | String | 是 | 友链描述 |
| classify | String | 否 | 分类 |

**注解：** `@LoginCheck`、`@SaveCheck`

**Service 流程：**

```
@PostMapping("/save")
@SaveCheck
public R save(@RequestBody WeblinksVO weblinksVO) {

    // Step 1: 构建实体
    Weblinks weblinks = new Weblinks();
    weblinks.setTitle(weblinksVO.getTitle());
    weblinks.setUrl(weblinksVO.getUrl());
    weblinks.setCover(weblinksVO.getCover());
    weblinks.setDesc(weblinksVO.getDesc());

    // Step 2: 自动设置分类和类型
    weblinks.setClassify("DEFAULT_FRIEND");
    weblinks.setType(CommonConst.RESOURCE_PATH_TYPE_FRIEND);  // "friendUrl"

    // Step 3: 状态设为 false（待审核）
    weblinks.setStatus(false);

    // Step 4: 保存
    weblinksMapper.insert(weblinks);

    return R.success();
}
```

---

## 场景二：查询已审核友链列表

**Controller** `GET /weblinks/list`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 无 | - | - | 公开接口 |

**Service 流程：**

```
@GetMapping("/list")
public R<Map<String, List<WeblinksVO>>> list() {

    // Step 1: 查询所有已审核通过的友链
    List<Weblinks> weblinks = new LambdaQueryChainWrapper<>(weblinksMapper)
        .eq(Weblinks::getType, CommonConst.RESOURCE_PATH_TYPE_FRIEND)
        .eq(Weblinks::getStatus, true)
        .list();

    // Step 2: 按 classify 分组
    Map<String, List<WeblinksVO>> grouped = weblinks.stream()
        .collect(Collectors.groupingBy(
            w -> w.getClassify() != null ? w.getClassify() : "默认",
            Collectors.mapping(w -> convertToVO(w), Collectors.toList())
        ));

    return R.success(grouped);
}
```

---

## 场景三：保存通用资源

**Controller** `POST /weblinks/saveWeblinks`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 是 | 资源标题 |
| type | String | 是 | 资源类型 |
| url | String | 是 | 链接地址 |
| cover | String | 否 | 封面图 |
| classify | String | 否 | 分类 |
| desc | String | 否 | 描述 |
| sort | Integer | 否 | 排序 |

**注解：** `@LoginCheck(0)`

**Service 流程：**

```
@PostMapping("/saveWeblinks")
public R saveWeblinks(@RequestBody WeblinksVO weblinksVO) {

    // Step 1: 校验 title + type 非空
    if (!StringUtils.hasText(weblinksVO.getTitle()) ||
        !StringUtils.hasText(weblinksVO.getType())) {
        return R.fail("标题和资源类型不能为空");
    }

    // Step 2: lovePhoto 类型自动设置 remark 为管理员 ID
    if (CommonConst.RESOURCE_PATH_TYPE_LOVE_PHOTO.equals(weblinksVO.getType())) {
        weblinksVO.setRemark(String.valueOf(BlogUtil.getAdminUser().getId()));
    }

    // Step 3: 转换并保存
    Weblinks weblinks = weblinksConvert.convert(weblinksVO);
    weblinksMapper.insert(weblinks);

    return R.success();
}
```

---

## 场景四：删除资源

**Controller** `GET /weblinks/deleteWeblinks?id={id}`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Integer | 是 | 资源 ID |

**注解：** `@LoginCheck(0)`

**Service 流程：**

```
@GetMapping("/deleteWeblinks")
public R deleteWeblinks(@RequestParam("id") Integer id) {
    weblinksMapper.deleteById(id);
    return R.success();
}
```

---

## 场景五：更新资源

**Controller** `POST /weblinks/update`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Integer | 是 | 资源 ID |
| title | String | 是 | 资源标题 |
| type | String | 是 | 资源类型 |
| 其他字段 | - | 否 | 需要更新的字段 |

**注解：** `@LoginCheck(0)`

**Service 流程：**

```
@PostMapping("/update")
public R update(@RequestBody WeblinksVO weblinksVO) {

    // Step 1: 校验必要字段
    if (!StringUtils.hasText(weblinksVO.getId().toString()) ||
        !StringUtils.hasText(weblinksVO.getTitle()) ||
        !StringUtils.hasText(weblinksVO.getType())) {
        return R.fail("ID、标题和资源类型不能为空");
    }

    // Step 2: 转换并更新
    Weblinks weblinks = weblinksConvert.convert(weblinksVO);
    weblinksMapper.updateById(weblinks);

    return R.success();
}
```

---

## 场景六：管理端分页查询资源列表

**Controller** `POST /weblinks/listWeblinks`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 是 | 页码 |
| pageSize | Integer | 是 | 每页条数 |
| type | String | 否 | 按类型过滤 |
| classify | String | 否 | 按分类过滤 |
| status | Boolean | 否 | 按状态过滤 |

**注解：** 无（公开接口）

**Service 流程：**

```
@PostMapping("/listWeblinks")
public R<Page> listWeblinks(@RequestBody BaseReqVO baseReqVO) {

    LambdaQueryWrapper<Weblinks> wrapper = new LambdaQueryWrapper<>();

    // 类型过滤
    if (StringUtils.hasText(baseReqVO.getType())) {
        wrapper.eq(Weblinks::getType, baseReqVO.getType());
    }

    // 分类过滤
    if (StringUtils.hasText(baseReqVO.getClassify())) {
        wrapper.eq(Weblinks::getClassify, baseReqVO.getClassify());
    }

    // 状态过滤
    if (baseReqVO.getStatus() != null) {
        wrapper.eq(Weblinks::getStatus, baseReqVO.getStatus());
    } else {
        // 未指定状态：管理员可见全部，普通用户只看 status=true
        Long adminId = BlogUtil.getAdminUser().getId();
        Long userId = BlogUtil.getUserId();
        if (!adminId.equals(userId)) {
            wrapper.eq(Weblinks::getStatus, true);
        }
    }

    // 分页查询
    Page<Weblinks> page = weblinksMapper.selectPage(baseReqVO.toPage(), wrapper);

    return R.success(page);
}
```

---

## 场景七：收藏夹查询

**Controller** `GET /collect/list`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 无 | - | - | 公开接口 |

**Service 流程：**

```
@GetMapping("/list")
public R<Map<String, List<WeblinksVO>>> list() {

    // 查询 type=favorites 且 status=true 的收藏
    List<Weblinks> weblinks = new LambdaQueryChainWrapper<>(weblinksMapper)
        .eq(Weblinks::getType, CommonConst.RESOURCE_PATH_TYPE_FAVORITES)
        .eq(Weblinks::getStatus, true)
        .list();

    // 按 classify 分组
    Map<String, List<WeblinksVO>> grouped = weblinks.stream()
        .collect(Collectors.groupingBy(
            w -> w.getClassify() != null ? w.getClassify() : "默认",
            Collectors.mapping(w -> convertToVO(w), Collectors.toList())
        ));

    return R.success(grouped);
}
```

---

## 场景八：音乐查询与保存

**Controller** `GET /music/listFunny`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 无 | - | - | 公开接口 |

**Service 流程：**

```
@GetMapping("/listFunny")
public R<List<Map<String, Object>>> listFunny() {

    // 查询 type=funny 且 status=true 的音乐
    List<Weblinks> weblinks = new LambdaQueryChainWrapper<>(weblinksMapper)
        .eq(Weblinks::getType, CommonConst.RESOURCE_PATH_TYPE_FUNNY)
        .eq(Weblinks::getStatus, true)
        .list();

    // 按 classify 分组统计数量
    List<Map<String, Object>> result = weblinks.stream()
        .collect(Collectors.groupingBy(
            w -> w.getClassify() != null ? w.getClassify() : "默认",
            Collectors.collectingAndThen(Collectors.toList(), list -> {
                Map<String, Object> map = new HashMap<>();
                map.put("classify", list.get(0).getClassify());
                map.put("count", list.size());
                map.put("items", list);
                return map;
            })
        ))
        .values()
        .stream()
        .collect(Collectors.toList());

    return R.success(result);
}
```

**Controller** `POST /music/saveFunny`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| classify | String | 是 | 分类 |
| cover | String | 是 | 封面图 |
| url | String | 是 | 音乐链接 |
| title | String | 是 | 音乐标题 |

**注解：** `@LoginCheck`、`@SaveCheck`

---

## 场景九：爱情相册查询与保存

**Controller** `GET /webInfo/listAdminLovePhoto`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 无 | - | - | 公开接口 |

**Service 流程：**

```
@GetMapping("/listAdminLovePhoto")
public R<List<Map<String, Object>>> listAdminLovePhoto() {

    // 查询 type=lovePhoto、status=true 且 remark=管理员ID
    Long adminId = BlogUtil.getAdminUser().getId();

    List<Weblinks> weblinks = new LambdaQueryChainWrapper<>(weblinksMapper)
        .eq(Weblinks::getType, CommonConst.RESOURCE_PATH_TYPE_LOVE_PHOTO)
        .eq(Weblinks::getStatus, true)
        .eq(Weblinks::getRemark, String.valueOf(adminId))
        .list();

    // 按 classify 分组
    List<Map<String, Object>> result = weblinks.stream()
        .collect(Collectors.groupingBy(
            w -> w.getClassify() != null ? w.getClassify() : "默认",
            Collectors.mapping(w -> convertToVO(w), Collectors.toList())
        ))
        .entrySet().stream()
        .map(e -> {
            Map<String, Object> map = new HashMap<>();
            map.put("classify", e.getKey());
            map.put("items", e.getValue());
            return map;
        })
        .collect(Collectors.toList());

    return R.success(result);
}
```

**Controller** `POST /webInfo/saveLovePhoto`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| classify | String | 是 | 分类 |
| cover | String | 是 | 封面图 |
| title | String | 是 | 相册标题 |

**注解：** `@LoginCheck`、`@SaveCheck`

---

## 场景十：资源聚合控制器（重复 CRUD）

**Controller 前缀：** `/resAgg`

| 方法 | 路径 | 说明 |
|------|------|------|
| saveWeblinks | POST /resAgg/saveWeblinks | 与 WeblinksController.saveWeblinks 完全重复 |
| deleteWeblinks | GET /resAgg/deleteWeblinks | 与 WeblinksController.deleteWeblinks 完全重复 |
| updateWeblinks | POST /resAgg/updateWeblinks | 与 WeblinksController.update 完全重复 |
| listWeblinks | POST /resAgg/listWeblinks | 与 WeblinksController.listWeblinks 完全重复 |

---

## 附录：实体字段文档

### Weblinks 实体

**表名：** `weblinks`
**继承：** `BaseDO`

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 雪花算法主键 |
| isDel | Integer | 逻辑删除 |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |
| title | String | 标题 |
| classify | String | 分类 |
| url | String | 链接地址 |
| cover | String | 封面图 |
| desc | String | 描述 |
| remark | String | 备注（存储用户 ID 或管理员 ID） |
| type | String | 资源类型 |
| sort | Integer | 排序 |
| status | Boolean | 状态（0=未审核/禁用，1=已审核/启用） |
| updateBy | String | 最终修改人 |

### WeblinksVO 前端传输对象

与 Weblinks 基本一致，额外包含 `introduction` 字段。
**注意：** `id` 类型为 `Integer`（与实体的 `Long` 不一致）。

---

## 资源类型常量

| 常量 | 值 | 用途 |
|------|-----|------|
| `RESOURCE_PATH_TYPE_FRIEND` | `"friendUrl"` | 友链 |
| `RESOURCE_PATH_TYPE_FUNNY` | `"funny"` | 音乐/搞笑 |
| `RESOURCE_PATH_TYPE_FAVORITES` | `"favorites"` | 收藏夹 |
| `RESOURCE_PATH_TYPE_LOVE_PHOTO` | `"lovePhoto"` | 爱情相册 |

---

## 约束与权限矩阵

| 操作 | 权限要求 | 校验规则 |
|------|---------|---------|
| 提交友链申请 | `@LoginCheck` + `@SaveCheck` | 自动设 status=false（待审核） |
| 查询友链列表 | 公开 | 过滤 status=true |
| 保存通用资源 | `@LoginCheck(0)` | title + type 非空 |
| 删除资源 | `@LoginCheck(0)` | 管理员 |
| 更新资源 | `@LoginCheck(0)` | 管理员 |
| 管理端分页查询 | 公开 | 管理员可见全部，普通用户只看已审核 |
| 收藏夹查询 | 公开 | type=favorites + status=true |
| 音乐列表 | 公开 | type=funny + status=true |
| 音乐保存 | `@LoginCheck` + `@SaveCheck` | classify + cover + url + title 非空 |
| 爱情相册列表 | 公开 | type=lovePhoto + status=true + remark=管理员ID |
| 爱情相册保存 | `@LoginCheck` + `@SaveCheck` | classify + cover + title 非空 |
| 七牛云直传凭证 | `@LoginCheck` + `@SaveCheck` | key 非空 |

---

## 待实现清单

| 问题 | 优先级 | 说明 |
|------|--------|------|
| 无 Service 层 | P1 | 所有 Controller 直接操作 Mapper，违反分层架构 |
| ResourceAggregationController 重复 WeblinksController | P1 | 四个端点完全重复，应合并 |
| WeblinksVO.id 类型不匹配 | P2 | VO 中 id 是 Integer，实体是 Long |
| deleteWeblinks 使用 GET 方法 | P2 | 应改用 @DeleteMapping |
| listAdminLovePhoto 未登录时异常 | P2 | BlogUtil.getAdminUser() 未登录返回 null |
| 缺少审核流程 | P1 | 友链/音乐/相册提交后 status=false，缺少管理员审核接口 |
| 缺少 OpenAPI 注解 | P2 | 大部分端点无 @Operation/@ApiResponses |
| @SaveCheck 误用在公开接口 | P3 | /save 友链申请应是公开提交，不应要求登录 |
