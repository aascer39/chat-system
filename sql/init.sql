-- ============================================================
-- Chat System 数据库初始化脚本
-- 适用数据库: PostgreSQL (与 application.yaml 配置一致)
-- ============================================================

-- 创建数据库（如需独立数据库，取消注释并执行）
-- CREATE DATABASE chat_system WITH ENCODING 'UTF8' LC_COLLATE 'zh_CN.UTF-8' LC_CTYPE 'zh_CN.UTF-8';
-- \c chat_system;

-- ============================================================
-- 用户表（对应 @TableName("users")）
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL       PRIMARY KEY,
    username    VARCHAR(50)     NOT NULL UNIQUE,
    password    VARCHAR(255)    NOT NULL,
    nickname    VARCHAR(50),
    avatar      VARCHAR(255),
    email       VARCHAR(100),
    status      SMALLINT        NOT NULL DEFAULT 0,
    deleted     SMALLINT        NOT NULL DEFAULT 0,
    created_at  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  users      IS '用户表';
COMMENT ON COLUMN users.id          IS '用户 ID（雪花算法）';
COMMENT ON COLUMN users.username    IS '用户名';
COMMENT ON COLUMN users.password    IS '密码（BCrypt 加密）';
COMMENT ON COLUMN users.nickname    IS '昵称';
COMMENT ON COLUMN users.avatar      IS '头像 URL';
COMMENT ON COLUMN users.email       IS '邮箱';
COMMENT ON COLUMN users.status      IS '状态: 0-离线, 1-在线';
COMMENT ON COLUMN users.deleted     IS '逻辑删除: 0-未删, 1-已删';
COMMENT ON COLUMN users.created_at  IS '创建时间';
COMMENT ON COLUMN users.updated_at  IS '更新时间';

-- 用户表索引
CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);
CREATE INDEX IF NOT EXISTS idx_users_status   ON users (status);


-- ============================================================
-- 聊天消息表（对应 @TableName("chat_messages")）
-- ============================================================
CREATE TABLE IF NOT EXISTS chat_messages (
    id           BIGSERIAL       PRIMARY KEY,
    sender_id    BIGINT          NOT NULL,
    receiver_id  BIGINT,
    content      TEXT,
    message_type VARCHAR(20)     NOT NULL DEFAULT 'TEXT',
    status       VARCHAR(20)     NOT NULL DEFAULT 'SENT',
    group_id     BIGINT,
    created_at   TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE  chat_messages               IS '聊天消息表';
COMMENT ON COLUMN chat_messages.id             IS '消息 ID（雪花算法）';
COMMENT ON COLUMN chat_messages.sender_id      IS '发送者 ID';
COMMENT ON COLUMN chat_messages.receiver_id    IS '接收者 ID（私聊），群聊时为 NULL';
COMMENT ON COLUMN chat_messages.content        IS '消息内容';
COMMENT ON COLUMN chat_messages.message_type   IS '消息类型: TEXT, IMAGE, FILE, SYSTEM';
COMMENT ON COLUMN chat_messages.status         IS '消息状态: SENT, DELIVERED, READ';
COMMENT ON COLUMN chat_messages.group_id       IS '群组 ID（群聊），私聊时为 NULL';
COMMENT ON COLUMN chat_messages.created_at     IS '发送时间';

-- 消息表索引
CREATE INDEX IF NOT EXISTS idx_msg_sender   ON chat_messages (sender_id);
CREATE INDEX IF NOT EXISTS idx_msg_receiver ON chat_messages (receiver_id);
CREATE INDEX IF NOT EXISTS idx_msg_group    ON chat_messages (group_id);
CREATE INDEX IF NOT EXISTS idx_msg_time     ON chat_messages (created_at DESC);
-- 常用查询：两人的聊天记录
CREATE INDEX IF NOT EXISTS idx_msg_chat     ON chat_messages (LEAST(sender_id, receiver_id), GREATEST(sender_id, receiver_id), created_at DESC);


-- ============================================================
-- 好友关系表
-- ============================================================
CREATE TABLE IF NOT EXISTS user_friend (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    friend_id  BIGINT      NOT NULL,
    created_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_user_friend UNIQUE (user_id, friend_id),
    CONSTRAINT chk_user_friend CHECK (user_id <> friend_id)
);

COMMENT ON TABLE  user_friend              IS '好友关系表';
COMMENT ON COLUMN user_friend.id           IS '关系 ID';
COMMENT ON COLUMN user_friend.user_id      IS '用户 ID';
COMMENT ON COLUMN user_friend.friend_id    IS '好友 ID';
COMMENT ON COLUMN user_friend.created_at   IS '添加时间';

-- 好友表索引
CREATE INDEX IF NOT EXISTS idx_friend_user   ON user_friend (user_id);
CREATE INDEX IF NOT EXISTS idx_friend_friend ON user_friend (friend_id);


-- ============================================================
-- 外键约束（可选，业务层也保证完整性）
-- ============================================================
ALTER TABLE chat_messages
    ADD CONSTRAINT fk_msg_sender
    FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE user_friend
    ADD CONSTRAINT fk_friend_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE user_friend
    ADD CONSTRAINT fk_friend_friend
    FOREIGN KEY (friend_id) REFERENCES users (id) ON DELETE CASCADE;
