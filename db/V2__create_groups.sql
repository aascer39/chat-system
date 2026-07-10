-- ============================================================
-- groups: 群组表
-- ============================================================

CREATE TABLE IF NOT EXISTS groups (
    id           BIGINT PRIMARY KEY,                -- 雪花 ID
    name         VARCHAR(64) NOT NULL,              -- 群名称
    avatar       VARCHAR(256),                      -- 群头像
    description  VARCHAR(256),                      -- 群简介
    owner_id     BIGINT NOT NULL,                   -- 群主用户 ID
    member_count INT NOT NULL DEFAULT 0,            -- 成员数（冗余字段）
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP
);

CREATE INDEX idx_groups_owner ON groups (owner_id);

COMMENT ON TABLE groups IS '群组表';
COMMENT ON COLUMN groups.id IS '主键，雪花算法';
COMMENT ON COLUMN groups.name IS '群名称';
COMMENT ON COLUMN groups.avatar IS '群头像 URL';
COMMENT ON COLUMN groups.description IS '群简介';
COMMENT ON COLUMN groups.owner_id IS '群主用户 ID';
COMMENT ON COLUMN groups.member_count IS '成员数（冗余，避免频繁 COUNT）';
COMMENT ON COLUMN groups.created_at IS '创建时间';
COMMENT ON COLUMN groups.updated_at IS '更新时间';

-- ============================================================
-- group_members: 群成员表
-- ============================================================

CREATE TABLE IF NOT EXISTS group_members (
    id          BIGINT PRIMARY KEY,                -- 雪花 ID
    group_id    BIGINT NOT NULL,                   -- 群 ID
    user_id     BIGINT NOT NULL,                   -- 用户 ID
    role        VARCHAR(16) NOT NULL DEFAULT 'MEMBER', -- 角色: OWNER, ADMIN, MEMBER
    joined_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (group_id, user_id)
);

CREATE INDEX idx_group_members_user ON group_members (user_id);
CREATE INDEX idx_group_members_group ON group_members (group_id);

COMMENT ON TABLE group_members IS '群成员表';
COMMENT ON COLUMN group_members.group_id IS '群 ID';
COMMENT ON COLUMN group_members.user_id IS '用户 ID';
COMMENT ON COLUMN group_members.role IS '角色: OWNER=群主, ADMIN=管理员, MEMBER=普通成员';
COMMENT ON COLUMN group_members.joined_at IS '加入时间';
