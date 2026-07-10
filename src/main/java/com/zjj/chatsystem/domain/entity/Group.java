package com.zjj.chatsystem.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群组实体
 */
@Data
@TableName("groups")
public class Group {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    private String avatar;

    private String description;

    private Long ownerId;

    private Integer memberCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
