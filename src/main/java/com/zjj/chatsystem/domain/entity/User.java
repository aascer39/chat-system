package com.zjj.chatsystem.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 *
 * @TableName users
 */
@Data
@TableName("users")
public class User {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String avatar;

    private String email;

    /** 状态: 0-离线, 1-在线 */
    private Integer status;

    /** 逻辑删除: 0-未删, 1-已删 */
    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
