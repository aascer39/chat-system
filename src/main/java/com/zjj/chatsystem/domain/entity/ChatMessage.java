package com.zjj.chatsystem.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息实体
 */
@Data
@TableName("chat_messages")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long senderId;

    private Long receiverId;

    private String content;

    /** 消息类型: TEXT, IMAGE, FILE, SYSTEM */
    private String messageType;

    /** 消息状态: SENT, DELIVERED, READ */
    private String status;

    private Long groupId;

    private LocalDateTime createdAt;

    /** 发送者昵称（非持久化，仅用于群聊传输） */
    @TableField(exist = false)
    private String senderName;
}
