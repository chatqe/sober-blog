# Category 模块功能逻辑分层文档

> 生成日期: 2026-06-21
> 技术栈: Spring Boot 2.7 + MyBatis-Plus + Redis 缓存 + 行为模块订阅

---

## 模块概览

```
category/
  ├── controller/web/
  │   ├── CategoryTagController.java    # 分类-标签合并控制器
  │   └── TagController.java            # 标签独立控制器（缺少 @RestController 注解）
  ├── service/
  │   ├── CategoryService.java          # 继承 CoreService<Category>
  │   └── CategoryServiceImpl.java      # 继承 CoreServiceImpl，无自定义逻辑
  ├── domain/
  │   ├── pojo/Category.java            # 分类实体
  │   ├── pojo/Tag.java                 # 标签实体
  │   ├── dao/CategoryMapper.java       # 继承 CoreMapper<Category>
  │   └── dao/TagMapper.java            # 继承 CoreMapper<Tag>
  └── enums/
      └── CategoryTypeEnum.java         # 分类类型枚举
```

该模块同时管理**分类 (Category)** 和**标签 (Tag)** 两个实体，共用一套缓存机制和权限体系。

---

## 场景一：获取分类信息（缓存优先）

**Controller** `GET /category/getCategoryInfo`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 无 | - | - | 公开接口，无需登录 |

**Service 核心逻辑（委托给 CommonQuery）：**

```
public List<Category> getCategoryInfo() {

    // Step 1: 尝试从 Redis 缓存读取
    String cacheKey = CommonConst.SORT_INFO;  // "sortInfo"
    List<Category> cached = qingSiCache.get(cacheKey, List.class);
    if (cached != null && !cached.isEmpty()) {
        return cached;
    }

    // Step 2: 双重检查锁
    synchronized (cacheKey.intern()) {
        cached = qingSiCache.get(cacheKey, List.class);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        // Step 3: 查询所有未删除的分类
        List<Category> categories = categoryMapper.selectList(
            new LambdaQueryWrapper<Category>().eq(Category::getIsDel, 0)
        );

        // Step 4: 统计每个分类关联的文章数
        for (Category cat : categories) {
            long articleCount = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>()
                    .eq(Article::getSortId, cat.getId())
                    .eq(Article::getIsDel, 0)
            );
            cat.setStatus(Math.toIntExact(articleCount));  // 复用 status 字段存文章数
        }

        // Step 5: 写入缓存（TTL = CommonConst.EXPIRE = 1800 秒）
        qingSiCache.put(cacheKey, categories, CommonConst.EXPIRE);

        return categories;
    }
}
```

---

## 场景二：订阅/取消订阅分类

**Controller** `GET /category/subscribe?categoryId={id}&flag={true/false}`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| categoryId | Integer | 是 | 分类 ID（参数名 labelId，实际传 categoryId） |
| flag | Boolean | 是 | true=订阅，false=取消订阅 |

**注解：** `@LoginCheck`（需要登录）

**Service 流程：**

```
@GetMapping("/subscribe")
public R<UserVO> subscribe(@RequestParam("categoryId") Integer labelId,
                           @RequestParam("flag") Boolean flag) {

    // Step 1: 清除用户缓存（确保下次读取最新订阅状态）
    Long userId = BlogUtil.getUserId();
    qingSiCache.del(CommonConst.USER_CACHE + userId);

    // Step 2: 从 UserContext 获取当前用户
    User user = UserContext.getCurrentUser();
    if (user == null) {
        return R.fail(CodeMsg.NOT_LOGIN);
    }

    // Step 3: 构建行为 DTO
    BehDTO behDTO = new BehDTO();
    behDTO.setBizId(labelId);                          // 分类 ID
    behDTO.setBizType(BizTypeEnum.CATEGORY.getCode()); // 6 = 专栏

    // Step 4: 根据 flag 调用订阅或取消订阅
    if (flag) {
        subscribeService.subscribe(behDTO);
    } else {
        subscribeService.unsubscribe(behDTO);
    }

    return R.success();
}
```

**行为类型映射：** `BizTypeEnum.CATEGORY` 编码为 `6`，描述为"专栏"。

---

## 场景三：保存分类（管理员）

**Controller** `POST /category/save`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 分类名称 |
| desc | String | 是 | 分类描述 |
| sort | Integer | 是 | 排序号 |
| icon | String | 否 | 分类图标 |
| type | Integer | 否 | 分类类型 |

**注解：** `@LoginCheck(0)`（管理员级别）

**Service 流程：**

```
@PostMapping("/save")
public R saveCategory(@RequestBody Category category) {

    // Step 1: 参数校验
    if (!StringUtils.hasText(category.getName())) {
        return R.fail("分类名称不能为空");
    }
    if (!StringUtils.hasText(category.getDesc())) {
        return R.fail("分类描述不能为空");
    }
    if (category.getSort() == null) {
        return R.fail("排序号不能为空");
    }

    // Step 2: 插入数据库
    categoryMapper.insert(category);

    // Step 3: 清除排序缓存
    qingSiCache.del(CommonConst.SORT_INFO);

    return R.success();
}
```

---

## 场景四：删除分类

**Controller** `GET /category/del?id={id}`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Integer | 是 | 分类 ID |

**注解：** `@LoginCheck(0)`

**Service 流程：**

```
@GetMapping("/del")
public R deleteCategory(@RequestParam("id") Integer id) {
    categoryMapper.deleteById(id);
    qingSiCache.del(CommonConst.SORT_INFO);
    return R.success();
}
```

---

## 场景五：更新分类

**Controller** `POST /category/update`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Integer | 是 | 分类 ID |
| name | String | 否 | 分类名称 |
| desc | String | 否 | 分类描述 |
| sort | Integer | 否 | 排序号 |
| icon | String | 否 | 分类图标 |
| type | Integer | 否 | 分类类型 |

**注解：** `@LoginCheck(0)`

**Service 流程：**

```
@PostMapping("/update")
public R updateCategory(@RequestBody Category category) {
    categoryMapper.updateById(category);
    qingSiCache.del(CommonConst.SORT_INFO);
    return R.success();
}
```

---

## 场景六：查询全部分类

**Controller** `GET /category/list`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 无 | - | - | 公开接口 |

**Service 流程：**

```
@GetMapping("/list")
public R<List<Category>> listCategory() {
    List<Category> categories = new LambdaQueryChainWrapper<>(categoryMapper)
        .list();
    return R.success(categories);
}
```

---

## 场景七：查询分类+标签组合列表

**Controller** `GET /category/listCategoryAndTag`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 无 | - | - | 公开接口 |

**Service 流程：**

```
@GetMapping("/listCategoryAndTag")
public R<Map> listCategoryAndTag() {
    Map<String, Object> result = new HashMap<>();

    // 查询全部分类
    List<Category> categories = new LambdaQueryChainWrapper<>(categoryMapper)
        .list();
    result.put("categories", categories);

    // 查询全部标签（直接通过 TagMapper 绕过 Service 层）
    List<Tag> tags = new LambdaQueryChainWrapper<>(tagMapper)
        .list();
    result.put("tags", tags);

    return R.success(result);
}
```

---

## 附录：实体字段文档

### Category 实体

**表名：** `category`
**继承：** `BaseDO`（雪花 ID、逻辑删除、自动填充时间）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 雪花算法主键 |
| isDel | Integer | 逻辑删除（0=未删，1=已删） |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |
| status | Integer | 状态（复用为文章计数） |
| isShow | Integer | 是否展示（0=否，1=是） |
| type | Integer | 分类类型：0=导航栏，1=普通分类 |
| name | String | 分类名称 |
| desc | String | 分类描述 |
| sort | Integer | 排序号 |
| icon | String | 分类图标 |
| updateBy | String | 最后修改人 |
| createBy | String | 创建人 |

### Tag 实体

**表名：** `tag`
**继承：** `BaseDO`

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 雪花算法主键 |
| isDel | Integer | 逻辑删除 |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |
| categoryId | Long | 所属分类 ID |
| name | String | 标签名称 |
| desc | String | 标签描述 |
| createBy | String | 创建人 |
| updateBy | String | 更新人 |

### CategoryTypeEnum 枚举

| 枚举常量 | code | desc |
|----------|------|------|
| BAR | 0 | 导航栏分类 |
| NORMAL | 1 | 普通分类 |

> **注意：** 实体 `Category.type` 字段注释写的是"1:普通分类 2:导航栏分类"，而枚举中 `BAR=0, NORMAL=1`，两者不一致。

---

## 约束与权限矩阵

| 操作 | 权限要求 | 校验规则 |
|------|---------|---------|
| 获取分类信息 | 公开 | 无 |
| 全部分类列表 | 公开 | 无 |
| 分类+标签组合列表 | 公开 | 无 |
| 订阅分类 | `@LoginCheck`（默认等级） | 用户已登录 |
| 取消订阅分类 | `@LoginCheck`（默认等级） | 用户已登录 |
| 保存分类 | `@LoginCheck(0)` | name/desc/sort 非空 |
| 删除分类 | `@LoginCheck(0)` | 管理员 |
| 更新分类 | `@LoginCheck(0)` | 管理员 |
| 保存标签 | `@LoginCheck(0)` | name/desc/categoryId 非空 |
| 删除标签 | `@LoginCheck(0)` | 管理员 |
| 更新标签 | `@LoginCheck(0)` | 管理员 |
| 标签列表 | 公开 | 无 |

---

## 缓存机制

| 缓存键 | 值 | 用途 | TTL | 失效时机 |
|--------|-----|------|-----|---------|
| `SORT_INFO` | `"sortInfo"` | 分类列表（含文章计数） | 1800 秒 | 分类增/删/改后 |
| `USER_CACHE + userId` | `"user_{userId}"` | 用户信息（含订阅状态） | - | 订阅/取消订阅后 |

缓存读取采用**双重检查锁**模式：先读缓存，未命中则在 `key.intern()` 上同步加锁，二次检查后查库并写缓存。

---

## 待实现清单

| 问题 | 优先级 | 说明 |
|------|--------|------|
| TagController 缺少 @RestController | P1 | 该类没有 @RestController 和 @RequestMapping 注解，可能无法被 Spring MVC 正确注册 |
| TagController 直接注入 ServiceImpl | P2 | 应注入接口而非实现类 |
| deleteTag 使用 GET 方法 | P2 | 不符合 REST 语义，应改用 DELETE |
| listCategoryAndTag 绕过 Service 层 | P2 | 标签查询直接通过 TagMapper，未走 Service |
| Category.type 与 CategoryTypeEnum 不一致 | P2 | 枚举 BAR=0 vs 注释说导航栏=2 |
| Tag.categoryId 类型 Long vs Category.id 类型 Integer | P3 | 外键关联类型不一致 |
| 缺少 OpenAPI 注解 | P2 | TagController 方法无 @Operation/@ApiResponses |
| 分类删除未检查关联文章 | P1 | 删除分类前应检查是否有文章引用该分类 |
