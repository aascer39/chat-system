package com.zjj.chatsystem.controller;

import com.zjj.chatsystem.common.result.Result;
import com.zjj.chatsystem.domain.dto.UserVO;
import com.zjj.chatsystem.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 聊天相关 REST 接口
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final UserService userService;

    public ChatController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/users")
    public Result<UserVO> getCurrentUser(Authentication authentication) {
        // 当前暂从 Authentication 获取 username，后续可改为从 token 解析完整用户信息
        String currentUser = authentication.getName();
        return Result.success(userService.getUserById(1L)); // TODO: 替换为真实用户 ID 查询
    }

    /**
     * 获取在线用户列表
     */
    @GetMapping("/online-users")
    public Result<List<UserVO>> getOnlineUsers() {
        // 在线用户由 WebSocket 维护，此处返回空列表
        return Result.success(List.of());
    }
}
