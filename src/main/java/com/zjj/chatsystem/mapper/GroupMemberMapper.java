package com.zjj.chatsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zjj.chatsystem.domain.entity.GroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {

    @Select("SELECT * FROM group_members WHERE group_id = #{groupId} ORDER BY joined_at ASC")
    List<GroupMember> findByGroupId(Long groupId);

    @Select("SELECT * FROM group_members WHERE user_id = #{userId} ORDER BY joined_at DESC")
    List<GroupMember> findByUserId(Long userId);

    @Select("SELECT * FROM group_members WHERE group_id = #{groupId} AND user_id = #{userId}")
    java.util.Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
}
