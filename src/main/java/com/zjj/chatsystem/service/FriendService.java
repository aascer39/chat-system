package com.zjj.chatsystem.service;

import com.zjj.chatsystem.domain.dto.FriendRequestVO;
import com.zjj.chatsystem.domain.dto.FriendVO;
import com.zjj.chatsystem.domain.dto.UserVO;

import java.util.List;

/**
 * 好友服务接口
 */
public interface FriendService {

    /**
     * 发送好友请求
     */
    FriendRequestVO sendRequest(Long fromUserId, Long toUserId);

    /**
     * 接受好友请求
     */
    FriendVO acceptRequest(Long requestId, Long userId);

    /**
     * 拒绝好友请求
     */
    void rejectRequest(Long requestId, Long userId);

    /**
     * 获取收到的待处理好友请求
     */
    List<FriendRequestVO> getPendingRequests(Long userId);

    /**
     * 获取已发送的待处理好友请求
     */
    List<FriendRequestVO> getSentRequests(Long userId);

    /**
     * 获取好友列表
     */
    List<FriendVO> getFriendList(Long userId);

    /**
     * 搜索用户（用于添加好友）
     */
    List<UserVO> searchUsers(String keyword, Long currentUserId);

    /**
     * 删除好友
     */
    void removeFriend(Long userId, Long friendUserId);
}
