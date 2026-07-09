# Chat System

基于 Spring Boot + WebSocket + MyBatis-Plus 的实时聊天系统。

## 强制规范

> **本项目所有开发规范、代码风格、架构约定均以 `skills/` 目录下的文件为准。**
> 在修改或新增任何代码之前，必须优先阅读并遵守 `skills/` 中定义的规范。

- `skills/` 目录包含本项目的技能定义和开发规范，是最高优先级的行为准则
- 任何与 `skills/` 中规范冲突的通用习惯或外部指南，均以本项目 `skills/` 为准
- 新增功能或重构时，必须先检查 `skills/` 中是否有相关规范文件，有则必须遵循
- `skills/` 目录应持续维护，随项目演进更新

## 技术栈

- **Spring Boot 3.5.16** - 应用框架
- **Spring Security** - 认证授权
- **Spring WebSocket** - 实时通信
- **MyBatis-Plus** - ORM 框架
- **PostgreSQL** - 数据库
- **Redis** - 缓存/发布订阅
- **JWT (jjwt)** - 身份认证
- **Lombok** - 代码简化
- **Java 21**

## 项目结构

```
src/main/java/com/zjj/chatsystem/
├── ChatSystemApplication.java           # 启动类
├── common/
│   ├── constant/
│   │   └── RedisKeys.java              # 缓存 Key 常量
│   ├── exception/
│   │   ├── BusinessException.java      # 业务异常
│   │   ├── ErrorCode.java             # 错误码枚举
│   │   └── GlobalExceptionHandler.java # 全局异常处理
│   └── result/
│       ├── PageResult.java            # 分页返回封装
│       └── Result.java                # 统一返回封装
├── config/
│   ├── JacksonConfig.java             # Jackson 配置
│   ├── MyBatisPlusConfig.java         # MyBatis-Plus 配置
│   ├── RedisConfig.java               # Redis 配置
│   ├── SecurityConfig.java            # Spring Security 配置
│   └── WebSocketConfig.java           # WebSocket 配置
├── controller/
│   ├── AuthController.java            # 认证接口（登录/注册）
│   └── ChatController.java            # 聊天相关 REST 接口
├── domain/
│   ├── dto/
│   │   ├── UserLoginRequest.java      # 登录请求 DTO
│   │   ├── UserRegisterRequest.java   # 注册请求 DTO
│   │   └── UserVO.java               # 用户视图 DTO
│   ├── entity/
│   │   ├── User.java                  # 用户实体
│   │   └── ChatMessage.java           # 消息实体
│   └── query/
│       └── UserPageQuery.java        # 用户分页查询参数
├── mapper/
│   ├── UserMapper.java               # 用户 Mapper
│   └── xml/UserMapper.xml            # 用户 Mapper XML
├── service/
│   ├── UserService.java              # 用户服务接口
│   └── impl/
│       └── UserServiceImpl.java      # 用户服务实现
├── websocket/
│   └── ChatHandler.java              # WebSocket 消息处理器
└── utils/
    ├── JwtUtil.java                   # JWT 工具类
    └── RedisService.java              # Redis 操作封装
```

## 接口

### REST API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/register | 用户注册 |
| POST | /api/auth/login | 用户登录 |
| GET  | /api/chat/users | 获取当前用户信息 |
| GET  | /api/chat/online-users | 获取在线用户列表 |

### WebSocket

| 端点 | 说明 |
|------|------|
| /ws/chat?token={jwt} | 聊天 WebSocket 连接 |

## 构建与运行

```bash
# 构建
./mvnw clean package

# 运行
./mvnw spring-boot:run
```

## 配置说明

参考 `.env.example`，需要配置：
- 数据库连接（PostgreSQL）
- Redis 连接
- JWT secret
