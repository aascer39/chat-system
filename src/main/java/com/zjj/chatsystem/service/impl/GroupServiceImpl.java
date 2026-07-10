package com.zjj.chatsystem.service.impl;

import com.zjj.chatsystem.common.exception.BusinessException;
import com.zjj.chatsystem.common.exception.ErrorCode;
import com.zjj.chatsystem.domain.dto.GroupVO;
import com.zjj.chatsystem.domain.entity.Group;
import com.zjj.chatsystem.domain.entity.GroupMember;
import com.zjj.chatsystem.mapper.GroupMapper;
import com.zjj.chatsystem.mapper.GroupMemberMapper;
import com.zjj.chatsystem.service.GroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class GroupServiceImpl implements GroupService {

    private final GroupMapper groupMapper;
    private final GroupMemberMapper groupMemberMapper;

    public GroupServiceImpl(GroupMapper groupMapper, GroupMemberMapper groupMemberMapper) {
        this.groupMapper = groupMapper;
        this.groupMemberMapper = groupMemberMapper;
    }

    @Override
    public GroupVO create(String name, String description, Long ownerId) {
        Group group = new Group();
        group.setName(name);
        group.setDescription(description);
        group.setOwnerId(ownerId);
        group.setMemberCount(1);
        group.setCreatedAt(LocalDateTime.now());
        groupMapper.insert(group);

        // 创建者自动成为群主
        addMember(group.getId(), ownerId, "OWNER");

        GroupVO vo = GroupVO.fromEntity(group);
        vo.setRole("OWNER");
        return vo;
    }

    @Override
    public void addMember(Long groupId, Long userId, String role) {
        // 校验群存在
        Group group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 校验是否已是成员
        if (groupMemberMapper.findByGroupIdAndUserId(groupId, userId).isPresent()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(role != null ? role : "MEMBER");
        member.setJoinedAt(LocalDateTime.now());
        groupMemberMapper.insert(member);

        // 更新成员计数
        group.setMemberCount(group.getMemberCount() + 1);
        group.setUpdatedAt(LocalDateTime.now());
        groupMapper.updateById(group);
    }

    @Override
    public List<GroupVO> listMyGroups(Long userId) {
        // 查询用户加入的所有群
        List<GroupMember> memberships = groupMemberMapper.findByUserId(userId);
        if (memberships.isEmpty()) {
            return List.of();
        }

        List<GroupVO> result = new ArrayList<>();
        for (GroupMember membership : memberships) {
            Group group = groupMapper.selectById(membership.getGroupId());
            if (group != null) {
                GroupVO vo = GroupVO.fromEntity(group);
                vo.setRole(membership.getRole());
                result.add(vo);
            }
        }
        return result;
    }

    @Override
    public GroupVO getById(Long groupId) {
        Group group = groupMapper.selectById(groupId);
        return GroupVO.fromEntity(group);
    }

    @Override
    public List<GroupMember> getMembers(Long groupId) {
        return groupMemberMapper.findByGroupId(groupId);
    }

    @Override
    public boolean isMember(Long groupId, Long userId) {
        return groupMemberMapper.findByGroupIdAndUserId(groupId, userId).isPresent();
    }
}
