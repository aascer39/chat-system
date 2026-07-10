package com.zjj.chatsystem.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友关系实体
 *
 * @TableName user_friend
 */
@Data
@TableName("user_friend")
public class Friend {

    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 好友 ID */
    private Long friendId;

    private LocalDateTime createdAt;

    /** 软删除 */
    private Integer deleted;
}
