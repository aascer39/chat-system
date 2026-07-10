package com.zjj.chatsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zjj.chatsystem.domain.entity.FriendRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FriendRequestMapper extends BaseMapper<FriendRequest> {

    /**
     * 查询发给某用户的待处理请求
     */
    @Select("SELECT * FROM friend_requests WHERE to_user_id = #{userId} AND status = 0 AND deleted = 0 ORDER BY created_at DESC")
    List<FriendRequest> findPendingByUserId(@Param("userId") Long userId);

    /**
     * 查询某用户发起的待处理请求
     */
    @Select("SELECT * FROM friend_requests WHERE from_user_id = #{userId} AND status = 0 AND deleted = 0 ORDER BY created_at DESC")
    List<FriendRequest> findSentByUserId(@Param("userId") Long userId);

    /**
     * 查询两个用户之间的待处理请求
     */
    @Select("SELECT * FROM friend_requests WHERE from_user_id = #{fromUserId} AND to_user_id = #{toUserId} AND status = 0 AND deleted = 0")
    FriendRequest findPendingBetween(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId);
}
