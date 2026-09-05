# SysConfig 模块功能逻辑分层文档

> 生成日期: 2026-06-21
> 技术栈: Spring Boot 2.7 + MyBatis-Plus

---

## 模块概览

```
sysconfig/
  ├── controller/admin/
  │   └── SysConfigController.java          # 系统配置管理（仅后台）
  ├── service/
  │   ├── SysConfigService.java             # 继承 CoreService<SysConfig>
  │   └── SysConfigServiceImpl.java         # 继承 CoreServiceImpl，无自定义逻辑
  └── domain/
      ├── pojo/SysConfig.java               # 系统配置实体
      └── dao/SysConfigMapper.java          # 继承 CoreMapper<SysConfig>
```

该模块管理博客系统的公共配置参数（如网站名称、描述、Logo 等），所有接口均在 admin 子目录下。

---

## 场景一：查询公开系统参数

**Controller** `GET /sysConfig/listSysConfig`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 无 | - | - | 公开接口 |

**注解：** `@Operation(summary = "查询系统参数")`、`@GetMapping("/listSysConfig")`

**Service 流程：**

```
@GetMapping("/listSysConfig")
public R<Map<String, String>> listSysConfig(HttpServletRequest request) {

    // Step 1: 查询所有公开配置（configType = "2" 即 PUBLIC）
    List<SysConfig> configs = new LambdaQueryChainWrapper<>(sysConfigMapper)
        .eq(SysConfig::getConfigType, "2")  // PUBLIC 公开配置
        .list();

    // Step 2: 转换为 Map<configKey, configValue>
    Map<String, String> result = configs.stream()
        .collect(Collectors.toMap(SysConfig::getConfigKey, SysConfig::getConfigValue));

    return R.success(result);
}
```

**用途：** 前端获取公共配置（如网站名称、Logo、描述等），用于页面渲染。

---

## 场景二：保存或更新系统配置

**Controller** `POST /sysConfig/saveOrUpdateConfig`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| configName | String | 是 | 配置名称 |
| configKey | String | 是 | 配置键名 |
| configValue | String | 是 | 配置键值 |
| configType | String | 是 | 配置类型：1=私有，2=公开 |

**注解：** `@LoginCheck(0)`

**Service 流程：**

```
@PostMapping("/saveOrUpdateConfig")
public R saveConfig(@RequestBody SysConfig sysConfig) {

    // Step 1: 校验必填字段
    if (!StringUtils.hasText(sysConfig.getConfigName())) {
        return R.fail("请完善所有配置信息！");
    }
    if (!StringUtils.hasText(sysConfig.getConfigKey())) {
        return R.fail("请完善所有配置信息！");
    }
    if (!StringUtils.hasText(sysConfig.getConfigType())) {
        return R.fail("请完善所有配置信息！");
    }

    // Step 2: 校验配置类型
    if (!"1".equals(sysConfig.getConfigType()) && !"2".equals(sysConfig.getConfigType())) {
        return R.fail("配置类型不正确！");
    }

    // Step 3: 执行保存或更新（根据 id 是否存在决定）
    sysConfigService.saveOrUpdate(sysConfig);

    return R.success();
}
```

---

## 场景三：删除系统配置

**Controller** `GET /sysConfig/deleteConfig?id={id}`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Integer | 是 | 配置 ID |

**注解：** `@LoginCheck(0)`

**Service 流程：**

```
@GetMapping("/deleteConfig")
public R deleteConfig(@RequestParam("id") Integer id) {
    sysConfigService.removeById(id);
    return R.success();
}
```

---

## 场景四：查询所有系统配置

**Controller** `GET /sysConfig/listConfig`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| 无 | - | - | 需管理员登录 |

**注解：** `@LoginCheck(0)`

**Service 流程：**

```
@GetMapping("/listConfig")
public R<List<SysConfig>> listConfig() {
    // 全量查询（未过滤 isDel）
    List<SysConfig> configs = new LambdaQueryChainWrapper<>(sysConfigMapper)
        .list();
    return R.success(configs);
}
```

---

## 附录：实体字段文档

### SysConfig 实体

**表名：** `sys_config`
**继承：** `BaseDO`

| 字段名 | 类型 | 数据库列名 | 说明 |
|--------|------|-----------|------|
| id | Long | id | 雪花算法主键（`@JsonFormat(shape = STRING)` 防前端精度丢失） |
| isDel | Integer | is_del | 逻辑删除 |
| createTime | LocalDateTime | create_time | 创建时间（自动填充） |
| updateTime | LocalDateTime | update_time | 更新时间（自动填充） |
| configName | String | config_name | 配置名称 |
| configKey | String | config_key | 配置键名 |
| configValue | String | config_value | 配置键值 |
| configType | String | config_type | 配置类型（String 存储枚举 code） |

### SysConfigVisibilityEnum 枚举

| 枚举常量 | code | desc |
|----------|------|------|
| PRIVATE | 1 | 私有 |
| PUBLIC | 2 | 公开 |

> **注意：** 枚举 `code` 是 `Integer` 类型，但实体 `configType` 字段是 `String` 类型。Controller 中通过 `Integer.toString()` 与枚举值做字符串比较。

---

## 约束与权限矩阵

| 操作 | 权限要求 | 校验规则 |
|------|---------|---------|
| 查询公开配置 | 公开 | 过滤 configType = "2" |
| 保存/更新配置 | `@LoginCheck(0)` | configName/configKey/configType 非空 |
| 删除配置 | `@LoginCheck(0)` | 管理员 |
| 查询全部配置 | `@LoginCheck(0)` | 管理员 |

---

## 待实现清单

| 问题 | 优先级 | 说明 |
|------|--------|------|
| deleteConfig id 参数类型错误 | P1 | 参数为 Integer，但实体 id 是 Long（雪花 ID 超 Integer 范围） |
| configType 类型不一致 | P2 | 枚举 code 是 int，实体字段是 String |
| listConfig 未过滤逻辑删除 | P2 | 应过滤 isDel = 0 |
| saveOrUpdateConfig 缺少 configKey 唯一性校验 | P1 | 重复 key 会导致 toMap 冲突 |
| listSysConfig 绕过 Service 层 | P2 | 直接使用 LambdaQueryChainWrapper 操作 Mapper |
| deleteConfig 使用 GET 方法 | P2 | 应改用 @DeleteMapping |
| 缺少配置热更新机制 | P3 | 修改配置后未刷新缓存 |
