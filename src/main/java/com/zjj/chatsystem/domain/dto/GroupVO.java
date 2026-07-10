package com.zjj.chatsystem.domain.dto;

import com.zjj.chatsystem.domain.entity.Group;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

/**
 * 群组视图 DTO
 */
public class GroupVO {

    private Long id;
    private String name;
    private String avatar;
    private String description;
    private Long ownerId;
    private Integer memberCount;
    private String role;           // 当前登录用户在该群的角色
    private LocalDateTime createdAt;

    public static GroupVO fromEntity(Group group) {
        if (group == null) return null;
        GroupVO vo = new GroupVO();
        BeanUtils.copyProperties(group, vo);
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public Integer getMemberCount() { return memberCount; }
    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
