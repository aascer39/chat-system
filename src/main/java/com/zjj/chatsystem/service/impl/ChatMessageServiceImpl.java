package com.zjj.chatsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjj.chatsystem.common.result.PageResult;
import com.zjj.chatsystem.domain.entity.ChatMessage;
import com.zjj.chatsystem.mapper.ChatMessageMapper;
import com.zjj.chatsystem.service.ChatMessageService;
import com.zjj.chatsystem.service.UserService;
import org.springframework.stereotype.Service;

/**
 * 聊天消息服务实现
 */
@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageMapper chatMessageMapper;
    private final UserService userService;

    public ChatMessageServiceImpl(ChatMessageMapper chatMessageMapper, UserService userService) {
        this.chatMessageMapper = chatMessageMapper;
        this.userService = userService;
    }

    @Override
    public void saveMessage(ChatMessage message) {
        chatMessageMapper.insert(message);
    }

    @Override
    public void updateStatus(Long id, String status) {
        ChatMessage msg = new ChatMessage();
        msg.setId(id);
        msg.setStatus(status);
        chatMessageMapper.updateById(msg);
    }

    @Override
    public ChatMessage getById(Long id) {
        return chatMessageMapper.selectById(id);
    }

    @Override
    public PageResult<ChatMessage> getHistory(Long otherUserId, Long groupId, int page, int size) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                .orderByDesc(ChatMessage::getCreatedAt);

        if (groupId != null) {
            // 群聊历史
            wrapper.eq(ChatMessage::getGroupId, groupId);
        } else {
            // 单聊历史
            Long currentUserId = userService.getCurrentUser().getId();
            wrapper.and(w -> w.eq(ChatMessage::getSenderId, currentUserId)
                            .eq(ChatMessage::getReceiverId, otherUserId))
                    .or(w -> w.eq(ChatMessage::getSenderId, otherUserId)
                            .eq(ChatMessage::getReceiverId, currentUserId));
        }

        Page<ChatMessage> mpPage = new Page<>(page, size);
        chatMessageMapper.selectPage(mpPage, wrapper);
        return PageResult.success(mpPage);
    }
}
