package com.zjj.chatsystem.controller;

import com.zjj.chatsystem.common.result.PageResult;
import com.zjj.chatsystem.common.result.Result;
import com.zjj.chatsystem.domain.dto.UserVO;
import com.zjj.chatsystem.domain.entity.ChatMessage;
import com.zjj.chatsystem.mapper.ChatMessageMapper;
import com.zjj.chatsystem.service.ChatMessageService;
import com.zjj.chatsystem.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 聊天相关 REST 接口
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final UserService userService;
    private final ChatMessageService chatMessageService;
    private final ChatMessageMapper chatMessageMapper;

    public ChatController(UserService userService, ChatMessageService chatMessageService, ChatMessageMapper chatMessageMapper) {
        this.userService = userService;
        this.chatMessageService = chatMessageService;
        this.chatMessageMapper = chatMessageMapper;
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/users")
    public Result<UserVO> getCurrentUser() {
        return Result.success(userService.getCurrentUser());
    }

    /**
     * 获取在线用户列表
     */
    @GetMapping("/online-users")
    public Result<List<UserVO>> getOnlineUsers() {
        // 在线用户由 WebSocket 维护，此处返回空列表
        return Result.success(List.of());
    }

    /**
     * 获取聊天历史（分页，按时间倒序）
     *
     * @param userId  聊天对方用户 ID（单聊时传）
     * @param groupId 群组 ID（群聊时传）
     * @param page    页码，从 1 开始
     * @param size    每页条数，默认 20
     */
    @GetMapping("/history")
    public Result<PageResult<ChatMessage>> getHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(chatMessageService.getHistory(userId, groupId, page, size));
    }

    /**
     * 获取当前用户的未读消息数（按好友分组）
     */
    @GetMapping("/unread-counts")
    public Result<java.util.Map<Long, Long>> getUnreadCounts() {
        Long userId = userService.getCurrentUser().getId();
        return Result.success(chatMessageMapper.countUnreadByReceiver(userId));
    }
}
