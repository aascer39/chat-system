-- File: V2__add_deleted_to_user_friend.sql
-- Description: 给 user_friend 表添加逻辑删除字段

ALTER TABLE user_friend
    ADD COLUMN IF NOT EXISTS deleted SMALLINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN user_friend.deleted IS '逻辑删除: 0-未删, 1-已删';
