package com.zjj.chatsystem.service;

import com.zjj.chatsystem.common.result.PageResult;
import com.zjj.chatsystem.domain.entity.ChatMessage;

/**
 * 聊天消息服务接口
 */
public interface ChatMessageService {

    /**
     * 保存聊天消息到数据库（同步，返回后 message.getId() 可用）
     */
    void saveMessage(ChatMessage message);

    /**
     * 更新消息状态
     */
    void updateStatus(Long id, String status);

    /**
     * 根据 ID 获取消息
     */
    ChatMessage getById(Long id);

    /**
     * 分页查询聊天历史（按时间倒序，最新的在前）
     *
     * @param otherUserId 聊天对方用户 ID（单聊时传）
     * @param groupId     群组 ID（群聊时传）
     * @param page        页码
     * @param size        每页条数
     */
    PageResult<ChatMessage> getHistory(Long otherUserId, Long groupId, int page, int size);
}
