package com.zjj.chatsystem.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友请求实体
 *
 * @TableName friend_requests
 */
@Data
@TableName("friend_requests")
public class FriendRequest {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 请求发起方 ID */
    private Long fromUserId;

    /** 请求接收方 ID */
    private Long toUserId;

    /** 状态: 0-pending, 1-accepted, 2-rejected */
    private Integer status;

    /** 逻辑删除: 0-未删, 1-已删 */
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
