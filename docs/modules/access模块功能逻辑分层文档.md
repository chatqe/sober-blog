# Access 模块功能逻辑分层文档

> 生成日期: 2026-06-21
> 技术栈: Spring Boot 2.7 + MyBatis-Plus + Redis 去重 + ip2region + @Async 异步持久化

---

## 模块概览

```
access/
  ├── controller/admin/
  │   └── AdminAccessLogController.java      # 管理端：获取网站统计数据
  ├── service/
  │   ├── AccessLogService.java              # 含统计查询方法
  │   ├── AccessLogServiceImpl.java          # 实现类
  │   └── AccessLogAsyncService.java         # 异步保存服务
  └── domain/
      ├── pojo/AccessLog.java                # 访问日志实体
      └── dao/AccessLogMapper.java           # 继承 CoreMapper<AccessLog>
```

**关联文件（非本模块但参与访问统计流程）：**
- `utils/CommonQuery.java` — IP 去重 + 地理定位核心逻辑
- `framework/filter/YouthFilter.java` — 第一层过滤器（同步）
- `framework/filter/BizFilter.java` — 第二层过滤器（异步）
- `utils/IPUtil.java` — 客户端真实 IP 提取
- `framework/config/AsyncThreadPoolConfig.java` — 异步线程池配置

---

## 场景一：访问记录触发（双过滤器机制）

### 第一层：YouthFilter（同步阻塞）

每个非 OPTIONS 请求都会经过此过滤器：

```
public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {

    // Step 1: 提取客户端真实 IP
    HttpServletRequest request = (HttpServletRequest) req;
    String ip = IPUtil.getRealIp(request);

    // Step 2: 同步调用保存访问记录
    commonQuery.saveHistory(ip);

    // Step 3: 继续过滤器链
    chain.doFilter(req, res);
}
```

### 第二层：BizFilter（异步非阻塞）

```
public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {

    // Step 1: 记录请求基本信息到日志
    HttpServletRequest request = (HttpServletRequest) req;
    String ip = IPUtil.getRealIp(request);
    long startTime = System.currentTimeMillis();

    // Step 2: 继续过滤器链
    chain.doFilter(req, res);

    // Step 3: 异步保存访问日志
    long respTime = System.currentTimeMillis() - startTime;
    accessLogAsyncService.saveAccessLogAsync(ip, request.getRequestURI(),
        request.getMethod(), null, respTime);

    // Step 4: 记录到日志文件
    log.info("ip={}, uri={}, method={}, status={}, time={}ms",
        ip, request.getRequestURI(), request.getMethod(),
        getStatusCode(res), respTime);
}
```

---

## 场景二：IP 去重与地理定位（核心逻辑）

**位置：** `CommonQuery.saveHistory(String ip)`

```
public void saveHistory(String ip) {

    // Step 1: 获取当前登录用户 ID
    Long userId = BlogUtil.getUserId();  // 从 Cookie/Token 解析

    // Step 2: 构造唯一标识（ip_userId 或纯 ip）
    String ipUser = ip + "_" + (userId != null ? userId : "anonymous");

    // Step 3: 从 Redis 读取去重 Set
    CopyOnWriteArraySet<String> ipHistory = qingSiCache.get(
        CommonConst.IP_HISTORY, CopyOnWriteArraySet.class);

    if (ipHistory == null) {
        ipHistory = new CopyOnWriteArraySet<>();
    }

    // Step 4: 去重检查
    if (!ipHistory.contains(ipUser)) {

        // Step 5: 双重检查锁（防止并发重复写入）
        synchronized (ipUser.intern()) {
            if (!ipHistory.contains(ipUser)) {

                // Step 6: 添加去重记录
                ipHistory.add(ipUser);

                // Step 7: ip2region 地理定位
                Region region = ip2region.search(ip);
                String country = region != null ? region.getCountry() : "未知";
                String province = region != null ? region.getProvince() : "未知";
                String city = region != null ? region.getCity() : "未知";

                // Step 8: 构建访问日志实体
                AccessLog accessLog = new AccessLog();
                accessLog.setIp(ip);
                accessLog.setUserId(userId);
                accessLog.setCountry(country);
                accessLog.setProvince(province);
                accessLog.setCity(city);

                // Step 9: 写入数据库
                accessLogService.save(accessLog);

                // Step 10: 写回 Redis 去重集合
                qingSiCache.put(CommonConst.IP_HISTORY, ipHistory);
            }
        }
    }
}
```

**IP 提取优先级（IPUtil.getRealIp）：**
```
X-Forwarded-For > X-Real-IP > Proxy-Client-IP > WL-Proxy-Client-IP > HTTP_CLIENT_IP > HTTP_X_FORWARDED_FOR > request.getRemoteAddr()
```

---

## 场景三：管理端获取网站统计数据

**Controller** `GET /admin/accessLog/getData`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 无 | - | - | 需管理员登录 |

**注解：** `@LoginCheck(0)`

**Service 流程：**

```
@GetMapping("/getData")
public R<Map<String, Object>> getAccessLogData() {

    Map<String, Object> result = new HashMap<>();

    // Step 1: 从 Redis 读取历史统计缓存
    Map<String, Object> historyMap = qingSiCache.get(
        CommonConst.IP_HISTORY_STATISTICS, Map.class);

    // Step 2: 查询今天的访问记录
    LocalDateTime todayStart = LocalDate.now().atStartOfDay();
    List<AccessLog> todayLogs = accessLogService.list(
        new LambdaQueryWrapper<AccessLog>()
            .ge(AccessLog::getCreateTime, todayStart)
    );

    // Step 3: 计算昨日访问数
    List<Map<String, Object>> yesterdayData = new ArrayList<>();
    if (historyMap != null) {
        List<Map<String, Object>> hourList =
            (List<Map<String, Object>>) historyMap.get(CommonConst.IP_HISTORY_HOUR);
        if (hourList != null) {
            yesterdayData = hourList;
        }
    }

    // Step 4: 计算今日访问数
    Map<String, Object> todayStats = new HashMap<>();
    Set<String> todayIps = new HashSet<>();
    List<UserVO> todayUsers = new ArrayList<>();

    for (AccessLog log : todayLogs) {
        todayIps.add(log.getIp());
        if (log.getUserId() != null) {
            User user = commonQuery.getUser(log.getUserId());
            if (user != null) {
                todayUsers.add(convertToVO(user));
            }
        }
    }
    todayStats.put("ipCount", todayIps.size());
    todayStats.put("userList", todayUsers);

    // Step 5: 计算昨日 IP 统计
    Map<String, Object> yesterdayStats = new HashMap<>();
    Set<String> yesterdayIps = new HashSet<>();
    for (Map<String, Object> item : yesterdayData) {
        String ip = (String) item.get("ip");
        if (ip != null) {
            yesterdayIps.add(ip);
        }
    }
    yesterdayStats.put("ipCount", yesterdayIps.size());

    // Step 6: 计算今日访问省份统计
    Map<String, Integer> provinceToday = new HashMap<>();
    for (AccessLog log : todayLogs) {
        String province = log.getProvince();
        provinceToday.merge(province, 1, Integer::sum);
    }
    List<Map<String, Object>> provinceList = provinceToday.entrySet().stream()
        .map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("province", e.getKey());
            m.put("count", e.getValue());
            return m;
        })
        .sorted((a, b) -> ((Integer) b.get("count")).compareTo((Integer) a.get("count")))
        .collect(Collectors.toList());

    result.put("yesterdayData", yesterdayData);
    result.put("todayData", todayStats);
    result.put("provinceToday", provinceList);
    result.put("ipDetailToday", null);  // 固定返回 null

    return R.success(result);
}
```

---

## 场景四：统计查询方法（AccessLogService）

### 4.1 获取 Top 10 省份

```
public List<Map<String, Object>> getTop10ProvincesByIP() {
    // 原生 SQL：按国家和省份分组，统计各省份独立IP数
    String sql = "SELECT country, province, count(distinct ip) as num " +
                 "FROM access_log GROUP BY country, province ORDER BY num DESC LIMIT 10";
    return baseMapper.selectMaps(SqlHelper.wrapSql(sql));
}
```

### 4.2 获取 Top 10 IP

```
public List<Map<String, Object>> getTop10IPs() {
    // 按 IP 分组统计访问次数
    String sql = "SELECT ip, count(*) as num " +
                 "FROM access_log GROUP BY ip ORDER BY num DESC LIMIT 10";
    return baseMapper.selectMaps(SqlHelper.wrapSql(sql));
}
```

### 4.3 获取最近 24 小时数据

```
public List<Map<String, Object>> getLast24HoursData() {
    // 查询最近 24 小时的访问记录
    return baseMapper.selectMaps(
        Wrappers.<AccessLog>lambdaQuery()
            .select(AccessLog::getIp, AccessLog::getUserId,
                    AccessLog::getCountry, AccessLog::getProvince)
            .ge(AccessLog::getCreateTime, LocalDateTime.now().minusHours(24))
            .orderByAsc(AccessLog::getCreateTime)
    );
}
```

### 4.4 获取总访问量

```
public Long allAccessCount() {
    // 返回 access_log 表总记录数
    return baseMapper.count();
}
```

### 4.5 写入访问统计缓存

```
public void addIPCache(Map<String, Object> historyMap) {
    // 组装所有统计数据
    historyMap.put(IP_HISTORY_PROVINCE, getTop10ProvincesByIP());
    historyMap.put(IP_HISTORY_IP, getTop10IPs());
    historyMap.put(IP_HISTORY_HOUR, getLast24HoursData());
    historyMap.put(IP_HISTORY_COUNT, allAccessCount());

    // 写入 Redis
    qingSiCache.put(CommonConst.IP_HISTORY_STATISTICS, historyMap);
}
```

---

## 场景五：异步线程池配置

**位置：** `AsyncThreadPoolConfig`

| 配置项 | 值 | 说明 |
|--------|-----|------|
| Bean 名称 | `accessLogExecutor` | - |
| 核心线程数 | 5 | - |
| 最大线程数 | 10 | - |
| 队列容量 | 1000 | LinkedBlockingQueue |
| 拒绝策略 | CallerRunsPolicy | 调用者线程执行 |

---

## 附录：实体字段文档

### AccessLog 实体

**表名：** `access_log`
**继承：** `BaseDO`

| 字段名 | 类型 | 说明 |
|--------|------|------|
| traceId | String | 链路追踪 ID |
| userId | Long | 用户 ID（未登录时为 null） |
| accessTime | LocalDateTime | 访问时间 |
| ip | String | 访问 IP 地址 |
| ipLocation | String | IP 完整地理位置 |
| country | String | 国家 |
| province | String | 省份 |
| city | String | 市 |
| district | String | 区县 |
| isp | String | 运营商 |
| referer | String | 来源页 URL |
| ua | String | 用户代理 |
| browser | String | 浏览器类型 |
| os | String | 操作系统 |
| deviceType | String | 设备类型：PC/Mobile/Pad |
| reqUrl | String | 访问的 URL |
| reqMethod | String | 请求方式 |
| respStatus | Integer | 响应状态码 |
| respTime | Integer | 响应耗时（毫秒） |
| extInfo | String | 扩展字段（JSON） |

---

## 缓存机制

| 缓存键 | 值 | 用途 |
|--------|-----|------|
| `IP_HISTORY` | `"ip_history"` | IP 去重 Set（CopyOnWriteArraySet） |
| `IP_HISTORY_STATISTICS` | `"ip_history_statistics"` | 访问统计 Map（省份 TOP10 / IP TOP10 / 24h / 总量） |
| `IP_HISTORY_PROVINCE` | `"ip_history_province"` | Map 中省份 TOP10 的 key |
| `IP_HISTORY_IP` | `"ip_history_ip"` | Map 中 IP TOP10 的 key |
| `IP_HISTORY_HOUR` | `"ip_history_hour"` | Map 中 24h 数据的 key |
| `IP_HISTORY_COUNT` | `"ip_history_count"` | Map 中总量的 key |
| `IP_DETAIL_TODAY` | `"ip_detail_today"` | 今日 IP 详情 |
| `PROVINCE_TODAY` | `"province_today"` | 今日省份统计 |

---

## 约束与权限矩阵

| 操作 | 权限要求 | 校验规则 |
|------|---------|---------|
| 同步记录访问 | YouthFilter 自动触发 | IP 去重 |
| 异步记录访问 | BizFilter 自动触发 | IP 去重（与同步共享同一 Set） |
| 获取统计数据 | `@LoginCheck(0)` | 管理员 |
| 省份 TOP10 查询 | 内部方法 | 原生 SQL |
| IP TOP10 查询 | 内部方法 | 原生 SQL |
| 24h 数据查询 | 内部方法 | WHERE create_time >= NOW() - INTERVAL 24 HOUR |
| 总访问量查询 | 内部方法 | COUNT(*) |

---

## 关键设计发现

1. **双重记录机制**：YouthFilter 同步调用 `commonQuery.saveHistory()`，BizFilter 异步调用 `AccessLogAsyncService.saveAccessLogAsync()` 同样调用 `commonQuery.saveHistory()`。IP 去重逻辑保证不会重复入库。

2. **IP 去重粒度**：以 `ip_userId` 组合作为唯一标识，同一 IP 不同用户算不同访问。

3. **同步阻塞风险**：YouthFilter 在过滤器链中同步执行 Redis 读取、ip2region 解析和数据库写入，增加请求响应时间。

4. **硬编码偏移量**：`allAccessCount() + 1168` 存在硬编码的历史基数。

---

## 待实现清单

| 问题 | 优先级 | 说明 |
|------|--------|------|
| 同步阻塞过滤器 | P1 | YouthFilter 中 Redis + ip2region + DB 写入应全部异步化 |
| CopyOnWriteArraySet 高并发性能 | P2 | 应改用 Redis Set 做分布式去重 |
| 硬编码历史基数 1168 | P2 | 应从配置或数据库读取 |
| 缺少 IP 黑名单 | P3 | 恶意爬虫/IP 无过滤机制 |
| BizFilter 与 YouthFilter 逻辑重复 | P3 | 两者都调用 saveHistory，可合并 |
