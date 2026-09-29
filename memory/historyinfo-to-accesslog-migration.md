---
name: historyinfo-to-accesslog-migration
description: historyInfo 到 AccessLog 实体迁移计划和现状
metadata:
  type: project
---

# HistoryInfo → AccessLog 迁移

## 现状

**迁移已于 2026-06-19 完成。** `modules/historyInfo/` 整个目录已删除，废弃代码已清理，编译通过。

### 已完成的清理

- 删除 `modules/historyInfo/` 整个目录（HistoryInfo.java, HistoryInfoMapper.java, HistoryInfoService.java, HistoryInfoServiceImpl.java）
- 移除 `CommonQuery.java`、`ScheduleTask.java`、`TaskSchedule.java` 中注释掉的 HistoryInfo 代码
- 移除 `YouthApplicationRunner.java`、`LoginCheckAspect.java`、`WebInfoHandlerInterceptor.java` 中废弃代码
- 移除 `CommonQuery.java`、`BlogUtil.java` 中不再使用的工具方法
- 移除 `MutexRegistry.java` 中的废弃互斥规则、`BehaviorCnt.java` 清理
- 适配 `UserServiceImpl.java`、`User.java`、`WebInfoController.java`、`FamilyController.java`、`CategoryTagController.java`、`TagController.java`、`AdminController.java`、`UserController.java`
- 修正 `WebInfoController.java` 中 Swagger import 路径（缺少 oas 层级导致编译失败）
- 补充 `AccessLogMapper.java` 缺失的方法声明

### 遗留问题（暂不处理）

- `access_log` 表未在 `qsblog.sql` 中有 CREATE TABLE 语句
- `history_info` 表 SQL 中 `id` 是 `int AUTO_INCREMENT`，与 Java 实体 `Long + ASSIGN_ID` 类型不匹配（`fix_id_type.sql`、`fix_is_del.sql` 已准备好但未执行）
