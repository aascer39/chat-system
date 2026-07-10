package com.zjj.chatsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zjj.chatsystem.domain.entity.Friend;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FriendMapper extends BaseMapper<Friend> {

    /**
     * 查询用户的好友 ID 列表
     */
    @Select("SELECT friend_id FROM user_friend WHERE user_id = #{userId} AND deleted = 0")
    List<Long> findFriendIdsByUserId(Long userId);

    /**
     * 查询双向好友关系
     */
    @Select("SELECT * FROM user_friend WHERE user_id = #{userId} AND friend_id = #{friendId} AND deleted = 0")
    Friend findByUserAndFriend(Long userId, Long friendId);
}
