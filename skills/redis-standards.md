---
name: redis-standards
description: Spring Boot 项目 Redis 使用规范 — 配置注入、RedisTemplate、缓存 Key 命名、失效策略、分布式锁
metadata:
  type: reference
  level: mandatory
  appliesTo: spring-boot
---

# Redis 使用规范

## Skill Name

`redis-standards`

## Purpose

统一 Spring Boot 项目中 Redis 的配置、API 调用、缓存 Key 命名、失效策略和分布式锁使用方式，避免硬编码、Key 冲突、内存泄漏和并发安全问题。

## Scope

- **适用**: 所有使用 Spring Boot + Redis（Lettuce 客户端）的后端项目
- **覆盖**: 配置注入 | RedisTemplate 配置与使用 | Key 命名规范 | TTL 策略 | 分布式锁 | 缓存穿透/击穿/雪崩防护
- **不适用**: 非 Spring Boot 项目、使用 Jedis 客户端的遗留项目

## Rules

### R1: 配置注入（强制）

所有 Redis 连接信息必须通过环境变量注入，禁止在代码或配置文件中硬编码密码。

**.env**:
```env
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0
```

**.env.example**:
```env
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0
```

**application.yaml**:
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}
      timeout: 3000ms
      lettuce:
        pool:
          max-active: ${REDIS_POOL_MAX_ACTIVE:16}
          max-idle: ${REDIS_POOL_MAX_IDLE:8}
          min-idle: ${REDIS_POOL_MIN_IDLE:4}
          max-wait: ${REDIS_POOL_MAX_WAIT:3000ms}
```

### R2: RedisTemplate 配置（强制）

统一配置 `RedisTemplate` Bean，禁止在业务代码中各自创建或使用 `StringRedisTemplate` 以外的裸模板。

```java
@Configuration
public class RedisConfig {

    /**
     * 通用 RedisTemplate，Key 序列化用 String，Value 用 JSON。
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Key 序列化 — 一律用 String
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);

        // Value 序列化 — 使用 Jackson 2 JSON
        GenericJackson2JsonRedisSerializer valueSerializer =
                new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * String 专用模板，存简单字符串场景。
     * Spring Boot 已自动配置，这里直接声明别名方便注入。
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
```

> **注意**: `GenericJackson2JsonRedisSerializer` 会在 JSON 中写入 `@class` 类型信息，反序列化时自动还原类型。如果不需要跨语言读取缓存数据，这是最安全的方式。如果需要跨语言消费，换成 `Jackson2JsonRedisSerializer` 指定具体类型。

### R3: 缓存 Key 命名规范（强制）

禁止裸字符串 Key，必须按以下格式：

```
{项目}:{业务域}:{功能}:{标识}
```

**规则**:
1. **冒号 `:` 分隔** — Redis 官方推荐的分隔风格，在命令行中可按前缀扫描和删除
2. **项目缩写** — 如 `cs`(chat-system)、`oms`、`crm`
3. **业务域** — 如 `user`、`msg`、`session`
4. **功能** — 如 `detail`、`list`、`token`
5. **标识** — 用户 ID、UUID、唯一键

**示例**:
```
cs:user:token:a1b2c3d4          → 用户 Token 信息
cs:user:detail:9527             → 用户详情缓存
cs:msg:list:123:1               → 用户 123 的第一页消息列表
cs:lock:order:create:888        → 分布式锁 Key
```

**工具类**:
```java
/**
 * Redis Key 构建工具。
 * 所有 Key 必须通过此工具生成，禁止在代码中直接拼接字符串。
 */
public final class RedisKeys {

    private static final String PREFIX = "cs";  // 项目前缀

    public static String userToken(String token) {
        return join("user", "token", token);
    }

    public static String userDetail(Long userId) {
        return join("user", "detail", String.valueOf(userId));
    }

    public static String msgList(Long userId, Integer page) {
        return join("msg", "list", String.valueOf(userId), String.valueOf(page));
    }

    public static String lockKey(String domain, String bizId) {
        return join("lock", domain, bizId);
    }

    public static String join(String... parts) {
        return PREFIX + ":" + String.join(":", parts);
    }

    // 构造函数私有，禁止实例化
    private RedisKeys() {}
}
```

### R4: TTL 失效策略（强制）

**原则**: 所有缓存必须设置 TTL，禁止永不失效的缓存（除非有明确且受控的理由）。

| 数据类型 | 推荐 TTL | 说明 |
|---------|---------|------|
| 用户 Token/会话 | Token 有效期一致 | Redis TTL === JWT 过期时间 |
| 用户详情 | 30 ~ 60 分钟 | 低频变化数据 |
| 消息列表 | 5 ~ 15 分钟 | 中频变化数据 |
| 验证码 | 5 分钟 | 严格对齐验证码有效期 |
| 分布式锁 | 自动续期（见 R6） | 非固定 TTL |
| 配置/字典 | 1 ~ 24 小时 | 极少变化数据 |

**代码规范**:
```java
// ✅ 正确：显式设置 TTL
ValueOperations<String, Object> ops = redisTemplate.opsForValue();
ops.set(key, user, 30, TimeUnit.MINUTES);

// ❌ 错误：无 TTL
ops.set(key, user);
```

**批量删除**（更新业务数据时）:
```java
// 使用 scan + 通配符删除，禁止使用 keys *
Set<String> keys = redisTemplate.keys("cs:user:detail:*");
if (keys != null && !keys.isEmpty()) {
    redisTemplate.delete(keys);
}
```

> 使用 `keys *` 在 Key 数量多时会导致 Redis 阻塞。生产环境推荐使用 `SCAN` 命令，或直接记录 Key 并在更新时精确删除。

### R5: 缓存穿透 / 击穿 / 雪崩防护（建议）

```java
@Service
public class UserCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserService userService;

    /**
     * 查询用户详情 — 旁路缓存模式 + 互斥锁防击穿。
     */
    public User getUserDetail(Long userId) {
        String key = RedisKeys.userDetail(userId);
        String lockKey = RedisKeys.lockKey("user", "detail:" + userId);

        // 1. 查缓存
        User cached = (User) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }

        // 2. 缓存未命中，尝试获取分布式锁
        if (tryLock(lockKey, 5, TimeUnit.SECONDS)) {
            try {
                // 双重检查 — 防止并发时上一个线程已回填
                User u = (User) redisTemplate.opsForValue().get(key);
                if (u != null) {
                    return u;
                }

                // 3. 查询数据库
                User user = userService.findById(userId);
                if (user == null) {
                    // 防缓存穿透：空值也缓存，TTL 缩短（60 秒）
                    redisTemplate.opsForValue().set(key, new User(), 60, TimeUnit.SECONDS);
                    return null;
                }

                // 4. 回填缓存
                redisTemplate.opsForValue().set(key, user, 30, TimeUnit.MINUTES);
                return user;

            } finally {
                unlock(lockKey);
            }
        }

        // 5. 未获取到锁，短暂等待后重试或返回兜底数据
        TimeUnit.MILLISECONDS.sleep(100);
        return getUserDetail(userId); // 递归重试（注意栈深度）
    }

    private boolean tryLock(String key, long timeout, TimeUnit unit) {
        // 见 R6 分布式锁实现
        return redisTemplate.opsForValue()
                .setIfAbsent(key, "locked", timeout, unit);
    }

    private void unlock(String key) {
        redisTemplate.delete(key);
    }
}
```

**防雪崩**（缓存同时大量失效）:
- TTL 添加随机偏移: `baseTTL + random(0~300)` 秒
- 多级缓存：Redis（主）+ 本地 Caffeine（副）

**防穿透**（查询不存在的数据）:
- 缓存空值（如上述代码），TTL 60 秒
- 布隆过滤器（Bloom Filter）前置拦截

### R6: 分布式锁规范（强制）

#### 6.1 锁 Key 命名
遵循 R3 规范：
```
cs:lock:{业务域}:{资源标识}
```

#### 6.2 锁工具类

```java
@Component
public class RedisDistributedLock {

    private final StringRedisTemplate stringRedisTemplate;

    private static final long DEFAULT_TIMEOUT = 30;  // 默认持有时间（秒）

    public RedisDistributedLock(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 尝试获取锁（非阻塞）。
     *
     * @param key    锁 Key
     * @param expire 自动释放时间（秒）
     * @return true 获取成功
     */
    public boolean tryLock(String key, long expire) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForValue()
                        .setIfAbsent(key, getLockValue(), expire, TimeUnit.SECONDS)
        );
    }

    /**
     * 阻塞式获取锁。
     */
    public boolean lock(String key, long expire, long retryMillis, int maxRetries) {
        int attempts = 0;
        while (attempts < maxRetries) {
            if (tryLock(key, expire)) {
                return true;
            }
            attempts++;
            if (attempts < maxRetries) {
                try {
                    TimeUnit.MILLISECONDS.sleep(retryMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return false;
    }

    /**
     * 释放锁。
     * 必须使用 Lua 脚本确保「仅持有者释放」的原子性。
     */
    public void unlock(String key) {
        String script = """
                if redis.call("GET", KEYS[1]) == ARGV[1] then
                    return redis.call("DEL", KEYS[1])
                else
                    return 0
                end
                """;
        stringRedisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                List.of(key),
                getLockValue()
        );
    }

    /**
     * 带自动续期的执行器 — 防止业务执行超过锁 TTL。
     */
    public <T> T executeWithLock(String lockKey, long leaseTimeSeconds, Supplier<T> action) {
        if (!tryLock(lockKey, leaseTimeSeconds)) {
            throw new RuntimeException("获取锁失败: " + lockKey);
        }

        // 启动续期线程（看门狗）
        AtomicBoolean keepAlive = new AtomicBoolean(true);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            if (keepAlive.get()) {
                // 续期为原始 TTL（简化实现，生产建议用 Redisson）
                stringRedisTemplate.expire(lockKey, leaseTimeSeconds, TimeUnit.SECONDS);
            }
        }, leaseTimeSeconds / 3, leaseTimeSeconds / 3, TimeUnit.SECONDS);

        try {
            return action.get();
        } finally {
            keepAlive.set(false);
            scheduler.shutdown();
            unlock(lockKey);
        }
    }

    private String getLockValue() {
        // 使用 UUID + 线程 ID 作为锁标识，防止误删其他线程持有的锁
        return UUID.randomUUID().toString() + ":" + Thread.currentThread().getId();
    }
}
```

#### 6.3 使用示例

```java
// 场景：创建订单防重复提交
String lockKey = RedisKeys.lockKey("order", "create:" + userId);
boolean acquired = redisDistributedLock.tryLock(lockKey, 10);
if (!acquired) {
    return ResponseEntity.status(429).body("操作过于频繁，请稍后重试");
}
try {
    // 执行业务逻辑
    orderService.createOrder(orderDTO);
    return ResponseEntity.ok("下单成功");
} finally {
    redisDistributedLock.unlock(lockKey);
}
```

#### 6.4 锁使用原则

1. **TTL 不可过长**：默认 10~30 秒，超过说明业务需要拆分
2. **必须释放**：`try { ... } finally { unlock(); }`
3. **非必须不用锁**：优先考虑乐观锁（CAS）或队列串行化
4. **锁粒度最小化**：锁资源 ID，不锁整个业务域
5. **看门狗自动续期**：业务执行超过 TTL 时自动续期（参考上面的 `executeWithLock`），生产环境推荐使用 Redisson 的 `RLock`

> **生产建议**: 上述实现展示了分布式锁的核心原理。复杂场景（红锁、读写锁、公平锁、自动续期）推荐直接使用 **Redisson**，其 `RLock` 提供了开箱即用的看门狗机制。

## Implementation Steps

1. **添加依赖** — `spring-boot-starter-data-redis`（Lettuce 客户端，Spring Boot 默认）
2. **配置 `.env` 和 `.env.example`** — 加入 Redis 连接信息和连接池参数
3. **编写 `RedisConfig`** — 配置 `RedisTemplate` 的 Key/Value 序列化策略
4. **编写 `RedisKeys` 工具类** — 集中管理所有缓存 Key
5. **编写 `RedisDistributedLock` 工具类** — 分布式锁的统一入口
6. **按业务封装 RedisService** — 如 `UserCacheService`，在此层处理缓存穿透/击穿/雪崩以及空值缓存
7. **全局搜索现有硬编码 Key** — 所有 `redisTemplate.opsForValue().set("xxx"` 改为通过 `RedisKeys` 生成

## Code Patterns

### 封装 RedisService 基类

```java
@Service
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void set(String key, Object value, long ttl, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, ttl, unit);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    public boolean expire(String key, long ttl, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, ttl, unit));
    }

    // Hash 操作
    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    public <T> T hGet(String key, String field) {
        return (T) redisTemplate.opsForHash().get(key, field);
    }

    // List 操作
    public <T> void lPush(String key, T value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    public <T> List<T> lRange(String key, long start, long end) {
        return (List<T>) (List<?>) redisTemplate.opsForList().range(key, start, end);
    }

    // Set 操作
    public <T> void sAdd(String key, T... values) {
        redisTemplate.opsForSet().add(key, values);
    }

    public Boolean sIsMember(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }
}
```

### @Cacheable 注解（合理使用，谨慎）

```java
// 适用于读多写少的配置类数据
@Cacheable(
    value = "cs:config",
    key = "#configKey",
    unless = "#result == null",
    cacheManager = "redisCacheManager"
)
public String getConfig(String configKey) {
    return configMapper.findValueByKey(configKey);
}
```

> **注意**: Spring `@Cacheable` 注解适用于简单场景。复杂缓存逻辑（穿透防护、分布式锁回填、多级缓存）建议使用 `RedisService` 手动编码。

## Edge Cases

1. **Key 太长** — Redis Key 越短性能越好（内存 + 网络传输）。`{项目}:{业务}:{功能}:{标识}` 平均 30~60 字符为合理范围，超过 128 字符需要精简
2. **大 Value** — 单个 Value 超过 10MB 会阻塞 Redis。超过 1MB 的数据考虑压缩（如 Gzip）或换用其他存储
3. **连接池泄漏** — 每次操作后正确归还连接。Lettuce 基于 Netty 异步，一般不会泄漏，但连接池配置的 `max-wait` 不能太长（超过 5 秒会导致应用线程堆积）
4. **密码含特殊字符** — `.env` 中密码若有 `#`、`=`、`!` 等特殊字符，值用双引号括起来：`REDIS_PASSWORD="p@ss#word"`
5. **多环境 Key 混淆** — 开发/测试/生产共用同一 Redis 实例时，务必在 Key 前缀中加入环境标识：`cs:dev:user:detail:1`、`cs:prod:user:detail:1`，或直接隔离数据库索引（`database: 0/1/2`）
6. **事务中使用 Redis** — Redis 事务（`MULTI`/`EXEC`）不支持回滚，不要与数据库事务混合使用。分布式锁内部不要包含数据库事务操作（先释放锁，再提交事务）
7. **Redis 连接中断** — 配置 Lettuce 重试机制，避免短暂网络抖动导致缓存不可用：
   ```yaml
   spring:
     data:
       redis:
         lettuce:
           shutdown-timeout: 200ms
   ```

## Output Format Rules

当生成 Redis 相关代码时:

1. **配置相关** — 敏感值必须使用 `${VAR}` 引用，标注来源（`application.yaml` / `.env`）
2. **Key 常量** — 必须通过 `RedisKeys` 工具类生成，禁止内联字符串
3. **锁代码** — `lock()` 与 `unlock()` 必须成对出现，`finally` 块释放，否则在代码评审中标记为 BUG
4. **缓存代码** — 生成缓存读写代码时，检查以下三点是否齐全：
   - TTL 是否显式设置
   - 空值穿透防护是否存在
   - 缓存删除/更新时机是否正确

## Example

### 场景：用户登出时清除 Token 缓存

不正确的方式：
```java
// ❌ Key 硬编码字符串、无 TTL 清理确认
redisTemplate.delete("token:" + token);
```

正确的方式：
```java
// 在 AuthService 中
public void logout(String token) {
    // 1. 构建规范的 Key
    String tokenKey = RedisKeys.userToken(token);

    // 2. 判断 Token 是否存在
    if (!redisService.hasKey(tokenKey)) {
        throw new RuntimeException("Token 已失效或不存在");
    }

    // 3. 删除 Token 缓存
    redisService.delete(tokenKey);

    // 4. 可选：将 Token 加入黑名单，防止并发使用
    String blacklistKey = RedisKeys.join("user", "blacklist", token);
    redisService.set(blacklistKey, "1", 24, TimeUnit.HOURS);

    // 5. 清除用户详情缓存（强制重新加载）
    // 从 Token 解析用户 ID（略）
    redisService.delete(RedisKeys.userDetail(userId));
}
```
