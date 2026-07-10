package com.zjj.chatsystem.controller;

import com.zjj.chatsystem.common.result.Result;
import com.zjj.chatsystem.domain.dto.GroupMemberVO;
import com.zjj.chatsystem.domain.dto.GroupVO;
import com.zjj.chatsystem.service.GroupService;
import com.zjj.chatsystem.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 群组 REST 接口
 */
@RestController
@RequestMapping("/api/chat/groups")
public class GroupController {

    private final GroupService groupService;
    private final UserService userService;

    public GroupController(GroupService groupService, UserService userService) {
        this.groupService = groupService;
        this.userService = userService;
    }

    /**
     * 创建群组
     */
    @PostMapping
    public Result<GroupVO> create(@RequestParam String name,
                                  @RequestParam(required = false) String description) {
        Long userId = userService.getCurrentUser().getId();
        return Result.success(groupService.create(name, description, userId));
    }

    /**
     * 获取我的群组列表
     */
    @GetMapping
    public Result<List<GroupVO>> list() {
        Long userId = userService.getCurrentUser().getId();
        return Result.success(groupService.listMyGroups(userId));
    }

    /**
     * 获取群详情
     */
    @GetMapping("/{groupId}")
    public Result<GroupVO> detail(@PathVariable Long groupId) {
        return Result.success(groupService.getById(groupId));
    }

    /**
     * 添加成员到群组
     */
    @PostMapping("/{groupId}/members")
    public Result<Void> addMember(@PathVariable Long groupId,
                                  @RequestParam Long userId) {
        groupService.addMember(groupId, userId, "MEMBER");
        return Result.success(null);
    }

    /**
     * 获取群成员列表（含用户名、昵称、头像）
     */
    @GetMapping("/{groupId}/members")
    public Result<List<GroupMemberVO>> members(@PathVariable Long groupId) {
        return Result.success(groupService.getMemberVOs(groupId));
    }
}
