package com.zjj.chatsystem.controller;

import com.zjj.chatsystem.common.result.Result;
import com.zjj.chatsystem.domain.dto.FriendRequestVO;
import com.zjj.chatsystem.domain.dto.FriendVO;
import com.zjj.chatsystem.domain.dto.SendFriendRequestDTO;
import com.zjj.chatsystem.domain.dto.UserVO;
import com.zjj.chatsystem.service.FriendService;
import com.zjj.chatsystem.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 好友管理接口
 */
@RestController
@RequestMapping("/api/friend")
public class FriendController {

    private final FriendService friendService;
    private final UserService userService;

    public FriendController(FriendService friendService, UserService userService) {
        this.friendService = friendService;
        this.userService = userService;
    }

    /**
     * 发送好友请求
     */
    @PostMapping("/request")
    public Result<FriendRequestVO> sendRequest(Authentication auth,
                                                @Valid @RequestBody SendFriendRequestDTO dto) {
        Long fromUserId = getUserId(auth);
        return Result.success(friendService.sendRequest(fromUserId, dto.getToUserId()));
    }

    /**
     * 接受好友请求
     */
    @PostMapping("/accept/{requestId}")
    public Result<FriendVO> acceptRequest(Authentication auth, @PathVariable Long requestId) {
        Long userId = getUserId(auth);
        return Result.success(friendService.acceptRequest(requestId, userId));
    }

    /**
     * 拒绝好友请求
     */
    @PostMapping("/reject/{requestId}")
    public Result<Void> rejectRequest(Authentication auth, @PathVariable Long requestId) {
        Long userId = getUserId(auth);
        friendService.rejectRequest(requestId, userId);
        return Result.success();
    }

    /**
     * 获取收到的待处理好友请求
     */
    @GetMapping("/requests")
    public Result<List<FriendRequestVO>> getPendingRequests(Authentication auth) {
        Long userId = getUserId(auth);
        return Result.success(friendService.getPendingRequests(userId));
    }

    /**
     * 获取已发送的好友请求
     */
    @GetMapping("/requests/sent")
    public Result<List<FriendRequestVO>> getSentRequests(Authentication auth) {
        Long userId = getUserId(auth);
        return Result.success(friendService.getSentRequests(userId));
    }

    /**
     * 获取好友列表
     */
    @GetMapping("/list")
    public Result<List<FriendVO>> getFriendList(Authentication auth) {
        Long userId = getUserId(auth);
        return Result.success(friendService.getFriendList(userId));
    }

    /**
     * 搜索用户（用于添加好友）
     */
    @PostMapping("/search")
    public Result<List<UserVO>> searchUsers(Authentication auth, @RequestBody Map<String, String> body) {
        Long userId = getUserId(auth);
        String keyword = body.getOrDefault("keyword", "");
        return Result.success(friendService.searchUsers(keyword, userId));
    }

    /**
     * 删除好友
     */
    @DeleteMapping("/remove/{friendUserId}")
    public Result<Void> removeFriend(Authentication auth, @PathVariable Long friendUserId) {
        Long userId = getUserId(auth);
        friendService.removeFriend(userId, friendUserId);
        return Result.success();
    }

    /**
     * 从 Authentication 获取当前用户 ID
     */
    private Long getUserId(Authentication auth) {
        return userService.getUserIdByUsername(auth.getName());
    }
}
