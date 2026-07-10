package com.zjj.chatsystem.service;

import com.zjj.chatsystem.domain.dto.GroupVO;
import com.zjj.chatsystem.domain.entity.GroupMember;

import java.util.List;

/**
 * 群组服务接口
 */
public interface GroupService {

    /**
     * 创建群组，创建者自动成为群主
     */
    GroupVO create(String name, String description, Long ownerId);

    /**
     * 添加成员到群组
     */
    void addMember(Long groupId, Long userId, String role);

    /**
     * 获取当前用户的群组列表（含角色信息）
     */
    List<GroupVO> listMyGroups(Long userId);

    /**
     * 根据 ID 获取群组
     */
    GroupVO getById(Long groupId);

    /**
     * 获取群组所有成员
     */
    List<GroupMember> getMembers(Long groupId);

    /**
     * 校验用户是否为群成员
     */
    boolean isMember(Long groupId, Long userId);
}
