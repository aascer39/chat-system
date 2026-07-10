-- File: V1__create_friends_tables.sql
-- Description: 创建好友关系与好友请求表

-- 好友关系表
CREATE TABLE IF NOT EXISTS friends (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users(id),
    friend_id     BIGINT NOT NULL REFERENCES users(id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted       SMALLINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uidx_friends_pair
    ON friends(user_id, friend_id) WHERE deleted = 0;

COMMENT ON TABLE friends IS '好友关系';
COMMENT ON COLUMN friends.user_id IS '用户 ID';
COMMENT ON COLUMN friends.friend_id IS '好友 ID';

-- 好友请求表
CREATE TABLE IF NOT EXISTS friend_requests (
    id            BIGSERIAL PRIMARY KEY,
    from_user_id  BIGINT NOT NULL REFERENCES users(id),
    to_user_id    BIGINT NOT NULL REFERENCES users(id),
    status        SMALLINT NOT NULL DEFAULT 0,   -- 0-pending, 1-accepted, 2-rejected
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted       SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_friend_requests_to_user
    ON friend_requests(to_user_id, status) WHERE deleted = 0;

CREATE UNIQUE INDEX IF NOT EXISTS uidx_friend_request_pair
    ON friend_requests(from_user_id, to_user_id) WHERE deleted = 0 AND status = 0;

COMMENT ON TABLE friend_requests IS '好友请求';
COMMENT ON COLUMN friend_requests.from_user_id IS '请求发起方';
COMMENT ON COLUMN friend_requests.to_user_id IS '请求接收方';
COMMENT ON COLUMN friend_requests.status IS '状态: 0-待处理, 1-已接受, 2-已拒绝';
