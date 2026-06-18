---
name: current-optimization-points
description: 项目当前识别出的优化点清单，用户决定暂不处理
metadata:
  type: project
---

# 当前优化点（暂不处理）

2026-06-17 识别出以下优化点，用户决定**暂不处理**：

1. **HistoryInfo → AccessLog 迁移收尾** — 旧模块目录未删、注释代码残留、缓存解析类型错误、SQL 缺失、ID 类型不匹配
2. **行为模块待完成** — 统计聚合接口、乐观锁、单元测试、dead code 清理
3. **数据库表 ID 类型审计** — fix_id_type.sql、fix_is_del.sql 已准备好但未执行
4. **字典表/用户实体修正** — SysDictData、User 等有未提交修改
5. **废弃代码清理** — AESUtil.encrypt() deprecated、多处吞异常
6. **OpenAPI 文档规范** — 有实施计划但未落地
7. **枚举设计** — 已完成拆分，需确认引用迁移
8. **异步写入可靠性** — @Async 缺乏失败重试和死信机制
9. **UserServiceImpl 928 行** — 过重有拆分空间

用户明确说"暂时不用处理"，后续如需推进再参考此记录。
