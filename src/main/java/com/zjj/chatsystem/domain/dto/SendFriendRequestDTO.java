package com.zjj.chatsystem.domain.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 发送好友请求 DTO
 */
public class SendFriendRequestDTO {

    @NotNull(message = "用户 ID 不能为空")
    private Long toUserId;

    public Long getToUserId() { return toUserId; }
    public void setToUserId(Long toUserId) { this.toUserId = toUserId; }
}
