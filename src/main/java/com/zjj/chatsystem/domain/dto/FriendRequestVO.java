package com.zjj.chatsystem.domain.dto;

import com.zjj.chatsystem.domain.entity.FriendRequest;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

/**
 * 好友请求视图 DTO（响应）
 */
public class FriendRequestVO {

    private Long id;
    private Long fromUserId;
    private String fromUsername;
    private String fromNickname;
    private String fromAvatar;
    private Long toUserId;
    private String toUsername;
    private String toNickname;
    private String toAvatar;
    private Integer status;
    private LocalDateTime createdAt;

    public static FriendRequestVO fromEntity(FriendRequest request) {
        if (request == null) return null;
        FriendRequestVO vo = new FriendRequestVO();
        BeanUtils.copyProperties(request, vo);
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFromUserId() { return fromUserId; }
    public void setFromUserId(Long fromUserId) { this.fromUserId = fromUserId; }

    public String getFromUsername() { return fromUsername; }
    public void setFromUsername(String fromUsername) { this.fromUsername = fromUsername; }

    public String getFromNickname() { return fromNickname; }
    public void setFromNickname(String fromNickname) { this.fromNickname = fromNickname; }

    public String getFromAvatar() { return fromAvatar; }
    public void setFromAvatar(String fromAvatar) { this.fromAvatar = fromAvatar; }

    public Long getToUserId() { return toUserId; }
    public void setToUserId(Long toUserId) { this.toUserId = toUserId; }

    public String getToUsername() { return toUsername; }
    public void setToUsername(String toUsername) { this.toUsername = toUsername; }

    public String getToNickname() { return toNickname; }
    public void setToNickname(String toNickname) { this.toNickname = toNickname; }

    public String getToAvatar() { return toAvatar; }
    public void setToAvatar(String toAvatar) { this.toAvatar = toAvatar; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
