package com.zjj.chatsystem.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群成员实体
 */
@Data
@TableName("group_members")
public class GroupMember {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long groupId;

    private Long userId;

    /** 角色: OWNER, ADMIN, MEMBER */
    private String role;

    private LocalDateTime joinedAt;
}
