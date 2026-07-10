-- ============================================================
-- chat_messages: 聊天消息表
-- 对应实体: com.zjj.chatsystem.domain.entity.ChatMessage
-- ============================================================

CREATE TABLE IF NOT EXISTS chat_messages (
    id            BIGINT PRIMARY KEY,                -- 雪花算法 ID
    sender_id     BIGINT NOT NULL,                   -- 发送者 ID
    receiver_id   BIGINT NOT NULL,                   -- 接收者 ID
    content       TEXT NOT NULL,                     -- 消息内容
    message_type  VARCHAR(16) NOT NULL DEFAULT 'TEXT', -- 消息类型: TEXT, IMAGE, FILE, SYSTEM
    status        VARCHAR(16) NOT NULL DEFAULT 'SENT', -- 状态: SENT, DELIVERED, READ
    group_id      BIGINT,                            -- 群聊 ID（预留）
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),  -- 发送时间
    updated_at    TIMESTAMP                           -- 更新时间（预留）
);

-- 索引：按接收者查询消息（收件箱）
CREATE INDEX idx_chat_messages_receiver ON chat_messages (receiver_id, created_at DESC);

-- 索引：按发送者查询消息（发件箱）
CREATE INDEX idx_chat_messages_sender ON chat_messages (sender_id, created_at DESC);

-- 索引：按会话查询（两人之间）（预留多消息扩展）
CREATE INDEX idx_chat_messages_conversation ON chat_messages (LEAST(sender_id, receiver_id), GREATEST(sender_id, receiver_id), created_at DESC);

COMMENT ON TABLE chat_messages IS '聊天消息表';
COMMENT ON COLUMN chat_messages.id IS '主键，雪花算法';
COMMENT ON COLUMN chat_messages.sender_id IS '发送者用户 ID';
COMMENT ON COLUMN chat_messages.receiver_id IS '接收者用户 ID';
COMMENT ON COLUMN chat_messages.content IS '消息内容';
COMMENT ON COLUMN chat_messages.message_type IS '消息类型: TEXT, IMAGE, FILE, SYSTEM';
COMMENT ON COLUMN chat_messages.status IS '消息状态: SENT, DELIVERED, READ';
COMMENT ON COLUMN chat_messages.group_id IS '群聊 ID（预留）';
COMMENT ON COLUMN chat_messages.created_at IS '消息发送时间';
COMMENT ON COLUMN chat_messages.updated_at IS '消息更新时间（预留）';
