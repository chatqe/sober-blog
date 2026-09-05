# WebInfo 模块功能逻辑分层文档

> 生成日期: 2026-06-21
> 技术栈: Spring Boot 2.7 + MyBatis-Plus + Redis 缓存 + @Async 访问量统计

---

## 模块概览

```
webInfo/
  ├── controller/
  │   ├── web/
  │   │   └── WebInfoController.java      # 前端：获取网站信息/看板娘/赞赏
  │   └── admin/
  │       └── AdminWebInfoController.java  # 管理端：获取网站配置
  ├── service/
  │   ├── WebInfoService.java             # 继承 CoreService<WebInfo>
  │   └── WebInfoServiceImpl.java         # 继承 CoreServiceImpl，无自定义逻辑
  └── domain/
      ├── pojo/WebInfo.java               # 网站信息实体
      ├── dao/WebInfoMapper.java           # 继承 CoreMapper<WebInfo>
      └── convert/
          └── WebInfoConvert.java          # MapStruct 转换器
```

---

## 场景一：获取网站信息（缓存优先）

**Controller** `GET /webInfo/get`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 无 | - | - | 公开接口 |

**Service 核心流程：**

```
@GetMapping("/get")
public R<WebInfo> getWebInfo() {

    // Step 1: 从 Redis 读取缓存
    String cacheKey = CommonConst.WEB_INFO;  // "webInfo"
    WebInfo cached = qingSiCache.get(cacheKey, WebInfo.class);

    if (cached != null) {
        // Step 2: 拷贝新对象，清除敏感字段
        WebInfo result = BeanUtil.copyProperties(cached, WebInfo.class);
        result.setRandomAvatar(null);   // 清除随机头像
        result.setRandomName(null);     // 清除随机名称
        result.setWaifuJson(null);      // 清除看板娘配置

        // Step 3: 追加访问量统计
        getHistoryCache(result);

        return R.success(result);
    }

    // Step 4: 缓存未命中，查数据库
    WebInfo webInfo = webInfoService.list().get(0);

    // Step 5: 写入缓存
    qingSiCache.put(cacheKey, webInfo);

    // Step 6: 追加访问量统计
    getHistoryCache(webInfo);

    return R.success(webInfo);
}
```

---

## 场景二：更新网站信息（管理员）

**Controller** `POST /webInfo/update`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| webName | String | 否 | 网站名称 |
| webTitle | String | 否 | 网站标题 |
| notices | String | 否 | 公告内容 |
| footer | String | 否 | 页脚信息 |
| backgroundImage | String | 否 | 背景图 |
| avatar | String | 否 | 头像 |
| status | Boolean | 否 | 是否启用 |

**注解：** `@LoginCheck(0)`（管理员）

**Service 流程：**

```
@PostMapping("/update")
public R<WebInfo> updateWebInfo(@RequestBody WebInfo webInfo) {

    // Step 1: 更新数据库
    webInfoService.updateById(webInfo);

    // Step 2: 查询全表取第一条（单记录模式）
    WebInfo updated = webInfoService.list().get(0);

    // Step 3: 设置默认商店类型（来自配置文件 ${store.type}）
    updated.setDefaultStoreType(storeType);

    // Step 4: 写入 Redis 缓存
    qingSiCache.put(CommonConst.WEB_INFO, updated);

    return R.success();
}
```

---

## 场景三：获取访问量统计（历史缓存）

**私有方法** `getHistoryCache(WebInfo webInfo)`

```
private void getHistoryCache(WebInfo webInfo) {

    // Step 1: 从 Redis 读取 IP 访问统计
    String cacheKey = CommonConst.IP_HISTORY_STATISTICS;  // "ip_history_statistics"
    Map<String, Object> historyMap = qingSiCache.get(cacheKey, Map.class);

    // Step 2: 若缓存为空，重新计算并写入
    if (historyMap == null || historyMap.isEmpty()) {
        accessLogService.addIPCache(historyMap);
        historyMap = qingSiCache.get(cacheKey, Map.class);
    }

    // Step 3: 写入 WebInfo 对象
    if (historyMap != null) {
        // 总访问量 = 数据库计数 + 1168（历史基数）
        Long allCount = (Long) historyMap.get(CommonConst.IP_HISTORY_COUNT);
        webInfo.setHistoryAllCount(String.valueOf(allCount + 1168));

        // 24小时访问量
        List<Map<String, Object>> hourList =
            (List<Map<String, Object>>) historyMap.get(CommonConst.IP_HISTORY_HOUR);
        webInfo.setHistoryDayCount(String.valueOf(hourList != null ? hourList.size() : 0 + 168));
    }
}
```

---

## 场景四：获取赞赏用户信息

**Controller** `GET /webInfo/getAdmire`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 无 | - | - | 公开接口 |

**Service 流程（委托 CommonQuery）：**

```
@GetMapping("/getAdmire")
public R<List<User>> getAdmire() {
    return R.success(commonQuery.getAdmire());
}
```

`CommonQuery.getAdmire()` 实现双重检查锁 + Redis 缓存：
1. 先读 `CommonConst.ADMIRE` 缓存
2. 未命中则在 `ADMIRE.intern()` 上同步加锁
3. 查数据库获取赞赏用户列表
4. 写回缓存

---

## 场景五：获取看板娘 JSON 配置

**Controller** `GET /webInfo/getWaifuJson`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 无 | - | - | 公开接口 |

**Service 流程：**

```
@GetMapping("/getWaifuJson")
public String getWaifuJson() {
    // 从 Redis 读取 WEB_INFO 缓存
    WebInfo cached = qingSiCache.get(CommonConst.WEB_INFO, WebInfo.class);
    if (cached != null && StringUtils.hasText(cached.getWaifuJson())) {
        return cached.getWaifuJson();
    }
    return "{}";  // 空 JSON
}
```

---

## 场景六：管理端获取网站信息

**Controller** `GET /admin/webInfo/get`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 无 | - | - | 需登录 |

**注解：** `@LoginCheck(0)`

**Service 流程（不走缓存，直接查库）：**

```
@GetMapping("/get")
public R<WebInfo> getWebInfo() {
    List<WebInfo> list = webInfoService.list();
    if (list.isEmpty()) {
        return R.success(new WebInfo());  // 返回空对象
    }
    return R.success(list.get(0));
}
```

---

## 附录：实体字段文档

### WebInfo 实体

**表名：** `web_info`
**继承：** `BaseDO`
**注意：** WebInfo 仍然继承 BaseDO，但 `createTime`/`updateTime` 字段被注释掉了，由 `DataAutoFill` 处理器自动填充。

| 字段名 | 类型 | 数据库列名 | 说明 |
|--------|------|-----------|------|
| id | Long | id | 雪花算法主键 |
| isDel | Integer | is_del | 逻辑删除 |
| createTime | LocalDateTime | create_time | 自动填充 |
| updateTime | LocalDateTime | update_time | 自动填充 |
| webName | String | web_name | 网站名称 |
| webTitle | String | web_title | 网站标题/信息 |
| notices | String | notices | 公告内容 |
| footer | String | footer | 页脚信息 |
| backgroundImage | String | background_image | 背景图 |
| avatar | String | avatar | 头像 |
| randomAvatar | String | random_avatar | 随机头像（敏感字段，不对外暴露） |
| randomName | String | random_name | 随机名称（敏感字段） |
| randomCover | String | random_cover | 随机封面 |
| waifuJson | String | waifu_json | 看板娘 JSON 配置（敏感字段） |
| status | Boolean | status | 是否启用 |

### 非表字段（动态统计）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| historyAllCount | String | 总访问量（非表字段，`@TableField(exist=false)`） |
| historyDayCount | String | 24小时访问量（非表字段） |
| defaultStoreType | String | 默认商店类型（非表字段，来自配置文件） |

---

## 缓存机制

| 缓存键 | 值 | 用途 | TTL | 失效时机 |
|--------|-----|------|-----|---------|
| `WEB_INFO` | `"webInfo"` | 网站信息完整对象 | 永久（手动清除） | `update` 接口写入时覆盖 |
| `IP_HISTORY_STATISTICS` | `"ip_history_statistics"` | 访问统计 Map | 永久 | 手动调用 `addIPCache()` 更新 |
| `ADMIRE` | `"admire"` | 赞赏用户列表 | 永久 | 赞赏数据变更时 |

---

## 约束与权限矩阵

| 操作 | 权限要求 | 校验规则 |
|------|---------|---------|
| 获取网站信息（前端） | 公开 | 优先读 Redis 缓存 |
| 更新网站信息 | `@LoginCheck(0)` | 管理员 |
| 获取赞赏用户 | 公开 | 双重检查锁 + 缓存 |
| 获取看板娘 JSON | 公开 | 读缓存 |
| 管理端获取网站信息 | `@LoginCheck(0)` | 直接查库，不走缓存 |

---

## 待实现清单

| 问题 | 优先级 | 说明 |
|------|--------|------|
| updateWebInfo 重复查询 | P2 | `webInfoService.list().get(0)` 被调用两次，可复用第一次结果 |
| historyAllCount 硬编码基数 | P3 | `+1168` 和 `+168` 是硬编码的历史偏移量，应改为可配置 |
| AdminWebInfoController 不走缓存 | P3 | 管理端接口直接查库，与前端缓存接口不一致 |
| WebInfo 继承 BaseDO 但未使用 isDel | P3 | web_info 表只有一条记录，逻辑删除无实际意义 |
| IP 统计缓存永不失效 | P3 | `IP_HISTORY_STATISTICS` 无 TTL，依赖手动更新 |
