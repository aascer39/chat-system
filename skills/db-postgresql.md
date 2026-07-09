---
name: db-postgresql
description: Spring Boot 项目 PostgreSQL 数据库开发规范 — 表设计、MyBatis-Plus ORM、配置注入、迁移流程
metadata:
  type: reference
  level: mandatory
  appliesTo: spring-boot
---

# PostgreSQL 数据库开发规范

## Skill Name

`db-postgresql`

## Purpose

统一 Spring Boot 项目中 PostgreSQL 数据库的设计、编码、迁移和配置规范，确保所有模块遵循一致的最佳实践，避免 MySQL 方言污染，降低维护成本。

## Scope

- **适用**: 所有使用 PostgreSQL + MyBatis-Plus + Spring Boot 的后端项目
- **覆盖**: 表结构设计 | 字段命名 | 主键策略 | 时间类型 | SQL 编写 | 配置注入 | 数据库迁移 | 分层代码规范
- **不适用**: 非 PostgreSQL 数据库项目、非 MyBatis-Plus 项目

## Rules

### R1: 配置注入（强制）

所有数据库连接信息必须通过环境变量注入，禁止在 `application.yaml` 中硬编码。

**.env** (加入 `.gitignore`):
```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=chat_system
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

**.env.example** (提交到仓库):
```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=my_app
DB_USERNAME=postgres
DB_PASSWORD=change_me
```

**application.yaml** 配置方式（通过 `env` 占位符，禁止使用 `@Value` 逐字段注入 Datasource）:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
```

> 注意：`${VAR:default}` 语法中的 `:default` 是 fallback 默认值，仅用于本地开发环境。生产环境必须通过真实环境变量覆盖。

### R2: 表设计规范（强制）

1. **表名**: `snake_case` 复数形式，如 `users`、`chat_messages`、`user_roles`
2. **每表必备字段**:
   ```sql
   id            BIGSERIAL PRIMARY KEY,              -- 主键（见 R3）
   created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),  -- 创建时间
   updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),  -- 更新时间
   deleted       SMALLINT NOT NULL DEFAULT 0,         -- 逻辑删除（0-未删, 1-已删）
   ```
3. **关联表名**: 按字母顺序，如 `user_role`（非 `role_user`）
4. **索引命名**: `idx_{表名}_{字段名}`，唯一索引用 `uidx_{表名}_{字段名}`
5. **禁止使用 MySQL 方言**:
   - ❌ `ENGINE=InnoDB` / `AUTO_INCREMENT` / `CHARSET utf8` / `COLLATE` / `COMMENT='...'`
   - ❌ `` ` `` 反引号括表名/字段名（PostgreSQL 用双引号 `"` 或不用）
   - ❌ `AUTO_INCREMENT`（PostgreSQL 用 `BIGSERIAL` / `IDENTITY`）
   - ❌ `TINYINT(1)`（PostgreSQL 用 `BOOLEAN` 或 `SMALLINT`）
   - ❌ `VARCHAR(n)`（PostgreSQL 中 `VARCHAR` 和 `TEXT` 性能一致，按语义选）
   - ✅ 使用 `TEXT` / `BOOLEAN` / `TIMESTAMPTZ` / `BIGSERIAL`

### R3: 主键策略（强制）

所有表统一使用 **Snowflake 算法或数据库序列** 生成的 `BIGINT` 主键，禁止使用 UUID 字符串作为主键（性能原因）。

**MyBatis-Plus 配置**:
```yaml
mybatis-plus:
  global-config:
    db-config:
      id-type: ASSIGN_ID    # Snowflake 算法
```

**实体类**:
```java
@Data
@TableName("users")
public class User {
    @TableId(type = IdType.ASSIGN_ID)  // 全局已配置时可省略
    private Long id;
    // ...
}
```

**特殊情况**: 纯关联表（多对多中间表）可使用联合主键，但推荐仍用单列自增 `BIGSERIAL` 主键 + 唯一索引。

### R4: 时间类型规范（强制）

| Java 类型 | PostgreSQL 类型 | 说明 |
|-----------|----------------|------|
| `LocalDateTime` | `TIMESTAMP` | 无时区业务时间 |
| `LocalDate` | `DATE` | 日期（生日、账单日） |
| `Instant` | `TIMESTAMPTZ` | 跨时区时间（推荐） |
| `LocalTime` | `TIME` | 仅时间 |

**最佳实践**:
- **推荐使用 `TIMESTAMPTZ`**（即 `TIMESTAMP WITH TIME ZONE`），存储 UTC 时间，展示时由应用层转换时区
- `created_at` / `updated_at` 类型统一为 `TIMESTAMPTZ`
- Jackson 配置:
  ```yaml
  spring:
    jackson:
      date-format: yyyy-MM-dd HH:mm:ss
      time-zone: Asia/Shanghai
  ```

### R5: SQL 编写规范（建议）

1. **关键词大写**: `SELECT`, `FROM`, `WHERE`, `INSERT INTO`, `JOIN`
2. **使用 MyBatis-Plus 条件构造器优先**: `lambdaQuery()`, `lambdaUpdate()`
3. **复杂 SQL 写在 XML 中**，路径与 Mapper 接口对应:
   - `src/main/resources/mapper/UserMapper.xml`
4. **分页使用 MyBatis-Plus Pagination 插件**
5. **禁止 `SELECT *`**，明确列出需要的字段
6. **避免在 `WHERE` 条件中对字段使用函数**，如 `WHERE DATE(created_at) = '2024-01-01'`（会走全表扫描）

### R6: 数据库迁移工具（选择其一）

#### 方案 A: Flyway（推荐）

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

命名规范: `V{版本号}__{描述}.sql`
```
db/migration/
├── V1__init_schema.sql
├── V1_1__create_users_index.sql
├── V2__add_chat_messages.sql
```

#### 方案 B: Liquibase

```yaml
spring:
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml
```

**选型原则**:
- 新项目默认选 **Flyway**（轻量、SQL 原生、学习成本低）
- 需要复杂回滚、多环境差异管理的选 Liquibase
- 选定后在 CLAUDE.md 中记录，后续不随意切换

#### 迁移编写原则（强制）
- **增量变更，禁止修改已执行迁移文件** — 任何表结构变更都通过新的迁移文件完成
- 迁移文件原子化：一个文件只做一件事（如：`V2__add_column_avatar_to_users.sql`）
- 写一个对应的回滚注释或脚本（Flyway 不支持自动回滚，需手动编写 `undo` 脚本）

### R7: 分层规范（强制）

```
Entity  →  Mapper  →  Service
  ↓                      |
  ↓           Controller/Socket
Entity
```

| 层 | 职责 | 规范 |
|----|------|------|
| **Entity** | 数据库表映射 | `@TableName`、`@TableId`，字段与列一一对应，使用 Lombok `@Data` |
| **Mapper** | 数据库访问 | 继承 `BaseMapper<T>`，复杂查询用 XML 或 `@Select` 注解 |
| **Service** | 业务逻辑 | 调用 Mapper 完成业务，含事务注解 `@Transactional`，不得直接暴露 Mapper 给 Controller |

**禁止违规**: Controller 不得直接注入 `Mapper`，所有数据操作必须经过 `Service` 层。

### R8: MyBatis-Plus 配置总览

```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true    # 自动映射 snake_case → camelCase
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 开发环境开启 SQL 日志
  global-config:
    db-config:
      id-type: ASSIGN_ID
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  mapper-locations: classpath*:/mapper/**/*.xml
```

## Implementation Steps

1. **初始化项目**时，在 `pom.xml` 添加依赖（PostgreSQL driver + MyBatis-Plus + Flyway/Liquibase）
2. **配置 `.env` 和 `.env.example`**，将 `.env` 加入 `.gitignore`
3. **编写 `application.yaml`**，通过 `${}` 占位符引用环境变量，配置 MyBatis-Plus
4. **创建第一条 Migration**（如 `V1__init_schema.sql`），定义基础表
5. **编写 Entity**，标注 `@TableName`，`@TableId(type = IdType.ASSIGN_ID)`
6. **编写 Mapper**，继承 `BaseMapper<T>`，加 `@Mapper`
7. **编写 Service**，加 `@Service` + `@Transactional`，注入 Mapper 实现业务方法
8. **验证**: 启动应用，检查 Flyway/Liquibase 是否自动执行迁移，数据源是否正确连接

## Code Patterns

### 完整分层示例（User 模块）

**Entity**:
```java
@Data
@TableName("users")
public class User {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String email;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
```

**Mapper**:
```java
@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM users WHERE username = #{username} AND deleted = 0")
    Optional<User> findByUsername(String username);
}
```

**Service**:
```java
@Service
@Transactional(rollbackFor = Exception.class)
public class UserService {

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    public List<User> searchByName(String keyword) {
        return userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .like(User::getNickname, keyword)
                        .eq(User::getDeleted, 0)
        );
    }
}
```

### 分页查询

```java
// Controller 或 Service 中
Page<User> page = new Page<>(currentPage, pageSize);
Page<User> result = userMapper.selectPage(page,
        new LambdaQueryWrapper<User>()
                .eq(User::getDeleted, 0)
                .orderByDesc(User::getCreatedAt)
);
// result.getRecords(), result.getTotal(), result.getPages()
```

## Edge Cases

1. **连接池耗尽**: 配置 HikariCP 连接池上限，生产环境建议 `maximum-pool-size: 10-20`
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 10
         minimum-idle: 5
         connection-timeout: 30000
   ```
2. **Flyway 校验失败**: 生产环境不要随意修改已执行的迁移文件，若 checksum 不匹配，需创建新的修复迁移文件而非修改旧的
3. **多数据源**: 本项目不推荐使用多数据源。如需跨库查询，考虑应用层聚合或 PostgreSQL FDW（外部数据包装器）
4. **N+1 查询**: Service 层中循环调用 Mapper 的单条查询会导致 N+1 问题，改用 `selectBatchIds` 或 `IN` 查询一次批量获取
5. **大字段延迟加载**: 超过 1KB 的 `TEXT`/`JSONB` 字段在查询时若不需要，不应包含在 `SELECT` 列中，或使用 `select(columns...)` 排除
6. **时区陷阱**: 应用中始终使用 UTC 时间进行计算和存储，仅在展示层转换为用户时区。多个服务间传递时间用 `Instant` 或时间戳
7. **迁移回滚**: Flyway 不支持自动回滚。需要回滚时，创建新的迁移文件执行反向操作（如 `ALTER TABLE ... DROP COLUMN`），并标注 `-- rollback: ...` 注释

## Output Format Rules

当生成数据库相关代码时，必须遵守以下输出格式规则：

1. **生成 SQL 时** — 开头注明迁移文件名（如 `-- File: V3__add_email_to_users.sql`），PostgreSQL 方言，避免 MySQL 特有语法
2. **生成 Entity 时** — 末尾附加一行 `-- Table: {表名}` 注释
3. **生成 Mapper XML 时** — 使用 `<include refid="Base_Column_List"/>`，明确每个字段映射
4. **配置相关输出** — 标注配置来源（`application.yaml` / `.env`），敏感值使用 `${...}` 引用而非硬编码

## Example

### 场景：新增 `user_profiles` 表存储用户扩展信息

1. 创建迁移文件 `V3__create_user_profiles_table.sql`:
```sql
-- File: V3__create_user_profiles_table.sql
-- Description: 创建用户扩展信息表

CREATE TABLE user_profiles (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    avatar_url    TEXT,
    bio           TEXT,
    phone         VARCHAR(20),
    gender        SMALLINT DEFAULT 0,   -- 0-未知, 1-男, 2-女
    birthday      DATE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted       SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_user_profiles_user_id ON user_profiles(user_id);
CREATE UNIQUE INDEX uidx_user_profiles_user_id ON user_profiles(user_id) WHERE deleted = 0;

COMMENT ON TABLE user_profiles IS '用户扩展信息';
COMMENT ON COLUMN user_profiles.gender IS '性别: 0-未知, 1-男, 2-女';
```

2. 编写 Entity:
```java
@Data
@TableName("user_profiles")
public class UserProfile {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String avatarUrl;
    private String bio;
    private String phone;
    private Integer gender;
    private LocalDate birthday;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
```

3. 编写 Mapper:
```java
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {
}
```

4. 编写 Service:
```java
@Service
@Transactional(rollbackFor = Exception.class)
public class UserProfileService {

    private final UserProfileMapper userProfileMapper;

    public UserProfileService(UserProfileMapper userProfileMapper) {
        this.userProfileMapper = userProfileMapper;
    }

    public UserProfile getByUserId(Long userId) {
        return userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>()
                        .eq(UserProfile::getUserId, userId)
                        .eq(UserProfile::getDeleted, 0)
        );
    }
}
```
