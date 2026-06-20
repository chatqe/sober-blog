---
name: current-optimization-points
description: 项目当前识别出的优化点清单，用户决定暂不处理
metadata:
  type: project
---

# 当前优化点（暂不处理）

2026-06-17 识别出以下优化点，用户决定**暂不处理**：

1. **行为模块待完成** — 统计聚合接口、乐观锁、单元测试、dead code 清理
2. **数据库表 ID 类型审计** — fix_id_type.sql、fix_is_del.sql 已准备好但未执行
3. **字典表/用户实体修正** — SysDictData、User 等有未提交修改
4. **废弃代码清理** — AESUtil.encrypt() deprecated、多处吞异常
5. **OpenAPI 文档规范** — 有实施计划，约 43% controller 已补全 @Operation/@Tag，剩余 ~17 个待处理
6. **枚举设计** — 已完成拆分，引用已全部迁移
7. **异步写入可靠性** — @Async 缺乏失败重试和死信机制
8. **UserServiceImpl 928 行** — 过重有拆分空间
9. **access_log 表 SQL 缺失** — qsblog.sql 中无 CREATE TABLE 语句

用户明确说"暂时不用处理"，后续如需推进再参考此记录。
