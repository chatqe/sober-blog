---
name: development-workflow
description: 开发偏好和工作流程约定
metadata:
  type: project
---

# 开发工作流

## 代理使用策略
- **日常开发**：优先使用内置 Agent 类型（Explore、code-reviewer、general-purpose）
- **大规模重构/新功能**：按需使用 Workflow 编排多代理流水线
- 不盲目配置，按需触发

## 代码质量偏好
- 代码可读性优先，避免过度抽象
- 遵循项目现有代码风格和约定
- 新代码必须遵循 CLAUDE.md 中的架构规范
- 提交前主动审查（/code-review）变更

## 配置约定
- 项目级别文件放在 `memory/` 目录
- 项目记忆以 `.md` 文件存在，每个文件一个事实
- 不在项目中存放 spec-workflow 相关的 agent 配置文件
