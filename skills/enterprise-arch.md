---
name: enterprise-arch
description: Spring Boot 企业级项目架构规范 — 分层架构、统一异常处理、DTO 隔离、统一返回、多环境配置、容器化
metadata:
  type: reference
  level: mandatory
  appliesTo: spring-boot
---

# 企业级项目架构规范

## Skill Name

`enterprise-arch`

## Purpose

建立 Spring Boot 后端项目的企业级架构规范，统一分层模式、接口隔离、异常处理、统一返回格式和环境配置，确保项目具备可维护性、可测试性和可扩展性。

## Scope

- **适用**: 所有基于 Spring Boot 3 + Java 21+ 的企业级后端项目
- **覆盖**: 分层架构 | DTO | 统一返回 | 全局异常处理 | 环境配置 | 容器化部署
- **不适用**: 非 Spring Boot 项目、单体 JSP 项目、纯 API 网关项目

## Rules

### R0: 完整项目结构

```
src/main/java/com/{company}/{project}/
├── ChatSystemApplication.java           # 启动类
│
├── common/                              # 公共模块
│   ├── result/
│   │   ├── Result.java                  # 统一返回封装
│   │   └── PageResult.java             # 分页返回封装
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java  # 全局异常处理
│   │   ├── BusinessException.java       # 业务异常
│   │   └── ErrorCode.java              # 错误码枚举
│   └── constant/
│       └── RedisKeys.java              # 缓存 Key 常量
│
├── config/                              # 配置类
│   ├── SecurityConfig.java
│   ├── WebSocketConfig.java
│   ├── RedisConfig.java
│   ├── MyBatisPlusConfig.java
│   └── JacksonConfig.java
│
├── controller/                          # 控制器层
│   ├── AuthController.java
│   └── UserController.java
│
├── domain/                              # 领域模型
│   ├── dto/
│   │   ├── UserLoginRequest.java        # 请求 DTO
│   │   ├── UserRegisterRequest.java
│   │   └── UserVO.java                  # 视图 DTO（响应）
│   ├── entity/
│   │   ├── User.java
│   │   └── ChatMessage.java
│   └── query/
│       └── UserPageQuery.java          # 分页查询参数
│
├── mapper/                              # 数据访问层
│   ├── UserMapper.java
│   └── xml/
│       └── UserMapper.xml
│
├── service/                             # 业务逻辑层
│   ├── UserService.java
│   └── impl/
│       └── UserServiceImpl.java
│
└── utils/                               # 工具类
    ├── JwtUtil.java
    └── RedisService.java
```

### R1: 分层职责与依赖规则（强制）

| 层 | 命名规范 | 职责 | 可调用的层 |
|----|---------|------|-----------|
| **Controller** | `XxxController` | 请求接收、参数校验、调用 Service、返回 Result | Service（不可直接调用 Mapper） |
| **Service** | `XxxService` / `XxxServiceImpl` | 业务编排、事务管理、缓存调用 | Mapper、Utils、其他 Service |
| **Mapper** | `XxxMapper` | 数据库 CRUD、SQL 映射 | 仅 Entity |
| **Entity** | `Xxx` | 数据库表一对一映射 | 无业务逻辑 |
| **DTO** | `XxxRequest` / `XxxVO` / `XxxDTO` | 接口入参/出参隔离 | 纯 POJO，无业务逻辑 |

**强制约束**:
- ❌ Controller 中禁止出现 `@Autowired Mapper`、`@Autowired SqlSession`
- ❌ Controller 中禁止直接返回 Entity（必须通过 DTO/VO 隔离）
- ❌ Service 中禁止处理 `HttpServletRequest` / `HttpServletResponse`
- ❌ Entity 中禁止包含 `@Schema`、`@ApiModel` 等接口文档注解（这些属于 DTO 层）
- ✅ Controller 只做"接收→校验→调 Service→返回"四件事

### R2: 统一返回 Result（强制）

所有接口响应必须使用 `Result<T>` 封装，禁止直接返回 Entity 或 `Map<String, Object>`。

```java
@Data
public class Result<T> {

    private int code;
    private String message;
    private T data;
    private long timestamp;

    private Result() {
        this.timestamp = System.currentTimeMillis();
    }

    // ========== 成功 ==========

    public static <T> Result<T> success() {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        return r;
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    // ========== 失败 ==========

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public static <T> Result<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> Result<T> error(ErrorCode errorCode, Object... args) {
        return error(errorCode.getCode(), String.format(errorCode.getMessage(), args));
    }
}
```

**分页返回**:
```java
@Data
public class PageResult<T> {

    private int code;
    private String message;
    private List<T> records;
    private long total;
    private long page;
    private long pageSize;
    private long timestamp;

    public static <T> PageResult<T> success(IPage<T> page) {
        PageResult<T> r = new PageResult<>();
        r.code = 200;
        r.message = "success";
        r.records = page.getRecords();
        r.total = page.getTotal();
        r.page = page.getCurrent();
        r.pageSize = page.getSize();
        r.timestamp = System.currentTimeMillis();
        return r;
    }
}
```

**Controller 中的使用**:
```java
@GetMapping("/{id}")
public Result<UserVO> getUser(@PathVariable Long id) {
    UserVO user = userService.getUserById(id);
    return Result.success(user);
}

@PostMapping
public Result<Void> createUser(@Valid @RequestBody UserRegisterRequest request) {
    userService.createUser(request);
    return Result.success();
}
```

### R3: 错误码枚举（强制）

使用枚举统一定义所有业务错误码，禁止在代码中散落魔法数字。

```java
public enum ErrorCode {

    // ========== 通用 ==========
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或 Token 已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    // ========== 业务 ==========
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_EXISTS(1002, "用户已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    PASSWORD_WEAK(1004, "密码强度不足，需包含字母和数字，至少 8 位"),
    TOKEN_INVALID(1005, "Token 无效或已过期"),
    TOKEN_EXPIRED(1006, "Token 已过期，请重新登录"),
    SMS_CODE_ERROR(1007, "验证码错误"),
    SMS_CODE_EXPIRED(1008, "验证码已过期"),
    FILE_TOO_LARGE(1009, "文件大小超过限制"),
    FILE_TYPE_NOT_ALLOWED(1010, "文件类型不允许"),

    // ========== 业务：聊天 ==========
    MESSAGE_TOO_LONG(2001, "消息长度超过限制"),
    RECEIVER_NOT_FOUND(2002, "接收方不存在"),
    GROUP_NOT_FOUND(2003, "群组不存在"),
    NOT_GROUP_MEMBER(2004, "不是群组成员"),

    // ========== 系统 ==========
    DB_ERROR(5001, "数据库操作失败"),
    CACHE_ERROR(5002, "缓存服务异常"),
    LOCK_FAILED(5003, "获取锁失败，请稍后重试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
```

### R4: 全局异常处理（强制）

使用 `@RestControllerAdvice` 捕获所有异常，确保任何未预期错误都不泄露堆栈信息。

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ========== 业务异常 ==========

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    // ========== 参数校验异常 ==========

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), msg);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), "缺少必要参数: " + e.getParameterName());
    }

    // ========== 类型转换 ==========

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), "请求体格式错误");
    }

    @ExceptionHandler(TypeMismatchException.class)
    public Result<Void> handleTypeMismatch(TypeMismatchException e) {
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), "参数类型错误: " + e.getPropertyName());
    }

    // ========== 安全 ==========

    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDenied(AccessDeniedException e) {
        return Result.error(ErrorCode.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    public Result<Void> handleAuthentication(AuthenticationException e) {
        return Result.error(ErrorCode.UNAUTHORIZED);
    }

    // ========== 404 ==========

    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNoHandler(NoHandlerFoundException e) {
        return Result.error(ErrorCode.NOT_FOUND.getCode(), "接口不存在: " + e.getRequestURL());
    }

    // ========== HTTP 方法不支持 ==========

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.error(ErrorCode.METHOD_NOT_ALLOWED);
    }

    // ========== 限流 ==========

    @ExceptionHandler(TooManyRequestsException.class)
    public Result<Void> handleTooManyRequests(TooManyRequestsException e) {
        return Result.error(ErrorCode.TOO_MANY_REQUESTS);
    }

    // ========== 兜底：未捕获异常 ==========

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception e) {
        log.error("未捕获异常: ", e);
        return Result.error(ErrorCode.INTERNAL_ERROR);
    }
}
```

**业务异常类**:
```java
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessException extends RuntimeException {

    private final int code;
    private final String message;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    public BusinessException(ErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage(), args));
        this.code = errorCode.getCode();
        this.message = String.format(errorCode.getMessage(), args);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}
```

### R5: DTO 接口隔离（强制）

**原则**: Controller 的入参/出参必须与 Entity 分离，严禁直接将 Entity 暴露给客户端。

#### 请求 DTO (`XxxRequest`)

```java
@Data
public class UserRegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 32, message = "用户名长度 4~32 位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度 8~64 位")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码需包含字母和数字")
    private String password;

    @Size(max = 32, message = "昵称长度不超过 32 位")
    private String nickname;
}
```

#### 响应 DTO (`XxxVO`)

```java
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private Integer status;
    private LocalDateTime createdAt;

    /**
     * Entity → VO 转换。
     * 禁止使用 MapStruct 以外的转换工具，或手动 getter/setter 逐字段赋值。
     */
    public static UserVO fromEntity(User user) {
        if (user == null) return null;
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
```

> 敏感字段（如 `password`、`deleted`）不出现在 VO 中，从源头隔离。

#### Service 层使用 DTO

```java
// Service 接口
public interface UserService {
    UserVO getUserById(Long id);
    Long createUser(UserRegisterRequest request);
}

// Service 实现
@Service
public class UserServiceImpl implements UserService {

    @Override
    public UserVO getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return UserVO.fromEntity(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserRegisterRequest request) {
        // 校验唯一性
        if (userMapper.findByUsername(request.getUsername()).isPresent()) {
            throw new BusinessException(ErrorCode.USER_EXISTS);
        }

        // 创建用户
        User user = new User();
        BeanUtils.copyProperties(request, user);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        userMapper.insert(user);
        return user.getId();
    }
}
```

#### 转换规范

| 转换方向 | 方式 | 说明 |
|---------|------|------|
| Entity → VO | `XxxVO.fromEntity(e)` 静态工厂或 MapStruct | 视图层只读 |
| Request → Entity | `BeanUtils.copyProperties` 或手动构造 | 入参转数据 |
| Entity → DTO (跨服务) | MapStruct | 防止字段遗漏 |

**禁止**:
- ❌ Controller 直接返回 `User` Entity
- ❌ VO 中包含 `password`、`deleted` 等内部字段
- ❌ Request 中直接使用 Entity 的验证注解

### R6: .env 配置注入（强制）

所有敏感信息和环境差异配置通过 `.env` 注入，`.env` 加入 `.gitignore`，提供 `.env.example`。

**.env**（加入 `.gitignore`）:
```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=chat_system
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0

# JWT
JWT_SECRET=your-256-bit-secret-key-here-change-in-production
JWT_EXPIRATION=86400000

# Server
SERVER_PORT=8080
```

**.env.example**（提交到仓库）:
```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=my_app
DB_USERNAME=postgres
DB_PASSWORD=change_me

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0

# JWT
JWT_SECRET=replace-with-your-secret-key
JWT_EXPIRATION=86400000

# Server
SERVER_PORT=8080
```

**application.yaml** — 统一引用环境变量：
```yaml
server:
  port: ${SERVER_PORT:8080}

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 4
          max-wait: 3000ms

jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION:86400000}
```

**多环境 profile**（可选）：

```yaml
# application-dev.yaml — 覆盖开发环境配置
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME}
```

```yaml
# application-prod.yaml — 生产环境强化
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
```

### R7: 容器化部署（建议）

#### 多阶段 Dockerfile

```dockerfile
# ========== 构建阶段 ==========
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw package -DskipTests -B

# ========== 运行阶段 ==========
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 非 root 用户运行
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

# .env 通过 -e 或 Docker Compose 注入，不打包进镜像
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### docker-compose.yml

```yaml
version: "3.9"

services:
  postgres:
    image: postgres:16-alpine
    container_name: ${APP_NAME}-postgres
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "${DB_PORT}:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME} -d ${DB_NAME}"]
      interval: 5s
      timeout: 3s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: ${APP_NAME}-redis
    ports:
      - "${REDIS_PORT}:6379"
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5

  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: ${APP_NAME}
    ports:
      - "${SERVER_PORT}:8080"
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: ${DB_NAME}
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      REDIS_DATABASE: ${REDIS_DATABASE}
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRATION: ${JWT_EXPIRATION}
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    restart: unless-stopped

volumes:
  postgres-data:
  redis-data:
```

#### .gitignore 补充

```gitignore
.env
.env.local
*.log
target/
build/
.idea/
*.iml
.vscode/
```

## Implementation Steps

1. **初始化项目脚手架** — 创建上述完整目录结构（参见 R0）
2. **配置环境变量** — 创建 `.env`（加入 `.gitignore`）+ `.env.example`
3. **编写 `common` 模块** — `ErrorCode` → `BusinessException` → `Result` / `PageResult` → `GlobalExceptionHandler`
4. **配置基础设施** — `RedisConfig`、`MyBatisPlusConfig`、`JacksonConfig`、`SecurityConfig`
5. **按业务模块编码** — 遵循 Entity → Mapper → Service(Impl) → Controller + DTO 的从上到下顺序
6. **编写 Dockerfile + docker-compose.yml** — 确保本地一条命令启动全部依赖
7. **验证** — 运行 `docker compose up`，测试注册/登录/异常链路

## Code Patterns

### 完整链路示例：用户注册

**Request DTO**:
```java
@Data
public class UserRegisterRequest {
    @NotBlank @Size(min = 4, max = 32)
    private String username;
    @NotBlank @Size(min = 8, max = 64)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$")
    private String password;
    @Size(max = 32)
    private String nickname;
}
```

**Controller**:
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody UserRegisterRequest request) {
        UserVO user = userService.createUser(request);
        return Result.success(user);
    }
}
```

**Service**:
```java
@Service
public class UserServiceImpl implements UserService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO createUser(UserRegisterRequest request) {
        if (userMapper.findByUsername(request.getUsername()).isPresent()) {
            throw new BusinessException(ErrorCode.USER_EXISTS);
        }
        User user = new User();
        BeanUtils.copyProperties(request, user);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userMapper.insert(user);
        return UserVO.fromEntity(user);
    }
}
```

**响应（统一格式）**:
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "id": 1827354617923584,
        "username": "zhangsan",
        "nickname": "张三",
        "avatar": null,
        "email": null,
        "status": 0,
        "createdAt": "2026-07-10 10:30:00"
    },
    "timestamp": 1720583400000
}
```

**异常响应示例**:
```json
{
    "code": 1002,
    "message": "用户已存在",
    "data": null,
    "timestamp": 1720583400123
}
```

## Output Format Rules

当生成架构相关代码时:

1. **新增业务模块** — 必须产出完整的 Entity → Mapper → Service → Controller + DTO 五层代码，缺层视为不完整
2. **异常处理** — 业务异常使用 `BusinessException(ErrorCode.XXX)` 抛出，统一由 `GlobalExceptionHandler` 捕获，禁止 Controller 中写 `try-catch`
3. **返回格式** — 增删改查统一使用 `Result.success(data)` 或 `Result.success()`，错误使用 `Result.error(...)`
4. **配置信息** — 所有环境差异值必须通过 `${VAR}` 引用，禁止硬编码 IP、端口、密码
5. **新增技术组件** — 同步更新 `docker-compose.yml` 对应服务定义

## Edge Cases

1. **超大请求体** — 超过 10MB 的请求体会耗尽内存。配置 `spring.servlet.multipart.max-file-size: 10MB`，由全局异常处理器返回规范错误
2. **枚举值越界** — `ErrorCode` 中的 `code` 不允许重复，新增时严格自增。建议单元测试验证 `ErrorCode.values()` 无重复 code
3. **DTO 泄露密码** — 审核 VO 时重点检查 password、hashedPassword、salt 等敏感字段是否被意外映射到 VO 中
4. **事务失效常见原因** — `@Transactional` 注解标记在 private/protected 方法上、同类中方法直接调用（不走代理）、catch 了异常未抛出——这些场景事务不会生效
5. **JSON 循环引用** — Entity 之间存在双向关联（如 User ↔ Role）时，在关联字段上使用 `@JsonIgnore`，或在 VO 层做扁平化处理，避免无限递归
6. **Docker 多平台构建** — Dockerfile 中 `FROM eclipse-temurin:21-jdk-alpine` 在 ARM Mac 上可正常构建，无需额外平台参数

## Example

### 场景：新增「获取用户列表」接口

1. **创建分页查询参数 DTO**:
```java
@Data
public class UserPageQuery {
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小为 1")
    private Integer page = 1;

    @NotNull(message = "每页数量不能为空")
    @Min(value = 1, message = "每页数量最小为 1")
    @Max(value = 100, message = "每页数量最大为 100")
    private Integer pageSize = 10;

    private String keyword;  // 可选搜索关键词
}
```

2. **新增 VO**:
```java
@Data
public class UserListItemVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private Integer status;
    private LocalDateTime createdAt;
}
```

3. **Controller 新增方法**:
```java
@GetMapping
public PageResult<UserListItemVO> listUsers(@Valid UserPageQuery query) {
    return userService.listUsers(query);
}
```

4. **Service 实现**:
```java
@Override
public PageResult<UserListItemVO> listUsers(UserPageQuery query) {
    Page<User> page = new Page<>(query.getPage(), query.getPageSize());
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
            .eq(User::getDeleted, 0)
            .like(StrUtil.isNotBlank(query.getKeyword()), User::getNickname, query.getKeyword())
            .orderByDesc(User::getCreatedAt);

    Page<User> userPage = userMapper.selectPage(page, wrapper);

    // Entity → VO 转换
    List<UserListItemVO> voList = userPage.getRecords().stream()
            .map(user -> {
                UserListItemVO vo = new UserListItemVO();
                BeanUtils.copyProperties(user, vo);
                return vo;
            })
            .toList();

    return PageResult.success(userPage);
}
```

5. **响应**:
```json
{
    "code": 200,
    "message": "success",
    "records": [
        {
            "id": 1827354617923584,
            "username": "zhangsan",
            "nickname": "张三",
            "avatar": null,
            "status": 1,
            "createdAt": "2026-07-10 10:30:00"
        }
    ],
    "total": 42,
    "page": 1,
    "pageSize": 10,
    "timestamp": 1720583500000
}
```
