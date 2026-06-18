---
name: historyinfo-to-accesslog-migration
description: historyInfo 到 AccessLog 实体迁移计划和现状
metadata:
  type: project
---

# HistoryInfo → AccessLog 迁移

## 现状

项目已处于迁移过渡期，新旧两套体系并存：

| 维度 | historyInfo（旧） | AccessLog（新） |
|------|------------------|-----------------|
| 表名 | `history_info` | `access_log` |
| 实体字段 | `id, userId, ip, nation, province, city, createTime` | 更多：traceId, ipLocation, referer, ua, browser, os, deviceType, reqUrl, reqMethod, respStatus, respTime, extInfo |
| Mapper | 4 个 `@Select` 查询 | 继承 `CoreMapper`，Service 层用 QueryWrapper |
| Service | 空接口+空实现 | 有完整查询方法（Top10省、Top10IP、24h数据、统计） |
| Controller | 无独立 Controller | `AdminAccessLogController` 提供管理端接口 |
| 写入方式 | 同步写入 | `@Async` 异步线程池 (`accessLogExecutor`) |

## 未清理的废弃代码

### 可删除文件
- `modules/historyInfo/domain/pojo/HistoryInfo.java`
- `modules/historyInfo/domain/dao/HistoryInfoMapper.java`（4 个 `@Select` 已被 AccessLogService 替代）
- `modules/historyInfo/service/HistoryInfoService.java`（空接口）
- `modules/historyInfo/service/HistoryInfoServiceImpl.java`（空实现）
- 整个 `modules/historyInfo/` 目录

### 需清理的注释代码
| 文件 | 内容 |
|------|------|
| `CommonQuery.java` | 注释掉的 `HistoryInfo accessLog = new HistoryInfo()` + `historyInfoMapper.insert()` |
| `TaskSchedule.java` | 注释掉的 `historyInfoMapper` 调用 |
| `YouthApplicationRunner.java` | 注释掉的 `historyInfoMapper` 调用 |
| `ScheduleTask.java` | 注释掉的 `historyInfoMapper` 调用 |

### 需修复的活跃引用
| 文件 | 问题 |
|------|------|
| `WebInfoController.java` | 第 196 行 `TypeReference<List<HistoryInfo>>()` 解析缓存，需改为 `AccessLog` |
| `DatabaseTests.java` | `HistoryInfoService` 注入和数据迁移测试方法 |

### 其他
- `CommonConst.java` 和 `CacheKeyConst.java` 中 IP 历史常量重复定义
- `access_log` 表未在 `qsblog.sql` 中有 CREATE TABLE 语句
- `history_info` 表 SQL 中 `id` 是 `int AUTO_INCREMENT`，与 Java 实体 `Long + ASSIGN_ID` 类型不匹配

## 迁移计划

### Step 1：清理废弃注释代码（低风险）
- 移除 `CommonQuery.java`、`TaskSchedule.java`、`YouthApplicationRunner.java`、`ScheduleTask.java` 中注释掉的 HistoryInfo 代码
- 删除 `modules/historyInfo/` 整个目录

### Step 2：修复活跃引用（低风险）
- `WebInfoController.java` 第 196 行改为 `AccessLog`
- `DatabaseTests.java` 移除对 `HistoryInfo` 的依赖

### Step 3：删除旧模块（中风险）
- 删除 `HistoryInfoMapper.java`、`HistoryInfoService.java`、`HistoryInfoServiceImpl.java`
- 清理 `CommonConst` 和 `CacheKeyConst` 中重复的 IP 历史常量
- 补充 `access_log` 表 CREATE TABLE 到 `qsblog.sql`
- **前置条件**：确认 `history_info` 表数据已全部迁入 `access_log`，再 DROP 旧表
