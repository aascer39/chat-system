请根据以下需求生成一个 Claude Code skill，严格按照 Meta-Skill 格式输出：

需求：
创建一个用于 Spring Boot 后端项目的 PostgreSQL 数据库开发规范 skill。

要求：
- 数据库统一使用 PostgreSQL。
- ORM 支持 MyBatis-Plus。
- 所有数据库配置（用户名、密码、地址等敏感信息）必须通过 .env 注入。
- .env 必须加入 .gitignore，并提供 .env.example。
- 规范 PostgreSQL 表设计、字段命名、主键策略、时间类型、SQL 编写规范。
- 规范 Flyway/Liquibase 数据库迁移流程。
- 规范 Entity、Mapper、Service 分层。
- 禁止使用 MySQL 特有语法。
- skill 必须适用于多个 Spring Boot 项目。
- 输出必须包含：Skill Name、Purpose、Scope、Rules、Implementation Steps、Code Patterns、Edge Cases、Output Format Rules、Example。
