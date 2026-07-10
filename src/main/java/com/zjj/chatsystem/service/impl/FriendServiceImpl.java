package com.zjj.chatsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zjj.chatsystem.common.exception.BusinessException;
import com.zjj.chatsystem.common.exception.ErrorCode;
import com.zjj.chatsystem.domain.dto.FriendRequestVO;
import com.zjj.chatsystem.domain.dto.FriendVO;
import com.zjj.chatsystem.domain.dto.UserVO;
import com.zjj.chatsystem.domain.entity.Friend;
import com.zjj.chatsystem.domain.entity.FriendRequest;
import com.zjj.chatsystem.domain.entity.User;
import com.zjj.chatsystem.mapper.FriendMapper;
import com.zjj.chatsystem.mapper.FriendRequestMapper;
import com.zjj.chatsystem.mapper.UserMapper;
import com.zjj.chatsystem.service.FriendService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class FriendServiceImpl implements FriendService {

    private final FriendMapper friendMapper;
    private final FriendRequestMapper friendRequestMapper;
    private final UserMapper userMapper;

    public FriendServiceImpl(FriendMapper friendMapper,
                             FriendRequestMapper friendRequestMapper,
                             UserMapper userMapper) {
        this.friendMapper = friendMapper;
        this.friendRequestMapper = friendRequestMapper;
        this.userMapper = userMapper;
    }

    @Override
    public FriendRequestVO sendRequest(Long fromUserId, Long toUserId) {
        if (fromUserId.equals(toUserId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "不能添加自己为好友");
        }

        // 校验对方存在
        User toUser = userMapper.selectById(toUserId);
        if (toUser == null || (toUser.getDeleted() != null && toUser.getDeleted() == 1)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 检查是否已是好友
        Friend existing = friendMapper.findByUserAndFriend(fromUserId, toUserId);
        if (existing != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "对方已经是您的好友");
        }

        // 检查反向好友关系（对方已加你）
        Friend reverse = friendMapper.findByUserAndFriend(toUserId, fromUserId);
        if (reverse != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "对方已经是您的好友");
        }

        // 检查是否有待处理的请求
        FriendRequest pending = friendRequestMapper.findPendingBetween(fromUserId, toUserId);
        if (pending != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "已发送过好友请求，请等待对方处理");
        }

        // 检查对方是否已向你发送请求（如有，自动接受）
        FriendRequest reversePending = friendRequestMapper.findPendingBetween(toUserId, fromUserId);
        if (reversePending != null) {
            // 自动接受
            reversePending.setStatus(1);
            reversePending.setUpdatedAt(LocalDateTime.now());
            friendRequestMapper.updateById(reversePending);

            // 建立双向好友关系
            createFriendship(fromUserId, toUserId);

            return buildRequestVO(reversePending, fromUserId, toUserId);
        }

        // 创建新请求
        FriendRequest request = new FriendRequest();
        request.setFromUserId(fromUserId);
        request.setToUserId(toUserId);
        request.setStatus(0);
        request.setDeleted(0);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        friendRequestMapper.insert(request);

        return buildRequestVO(request, fromUserId, toUserId);
    }

    @Override
    public FriendVO acceptRequest(Long requestId, Long userId) {
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null || request.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "好友请求不存在");
        }
        if (!request.getToUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "无权操作此请求");
        }
        if (request.getStatus() != 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "该请求已被处理");
        }

        // 更新请求状态
        request.setStatus(1);
        request.setUpdatedAt(LocalDateTime.now());
        friendRequestMapper.updateById(request);

        // 建立双向好友关系
        createFriendship(request.getFromUserId(), request.getToUserId());

        // 返回好友信息
        User friendUser = userMapper.selectById(request.getFromUserId());
        FriendVO vo = new FriendVO();
        vo.setUserId(friendUser.getId());
        vo.setUsername(friendUser.getUsername());
        vo.setNickname(friendUser.getNickname());
        vo.setAvatar(friendUser.getAvatar());
        vo.setStatus(friendUser.getStatus());
        vo.setCreatedAt(LocalDateTime.now());
        return vo;
    }

    @Override
    public void rejectRequest(Long requestId, Long userId) {
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null || request.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "好友请求不存在");
        }
        if (!request.getToUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "无权操作此请求");
        }
        if (request.getStatus() != 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "该请求已被处理");
        }

        request.setStatus(2);
        request.setUpdatedAt(LocalDateTime.now());
        friendRequestMapper.updateById(request);
    }

    @Override
    public List<FriendRequestVO> getPendingRequests(Long userId) {
        List<FriendRequest> requests = friendRequestMapper.findPendingByUserId(userId);
        return requests.stream()
                .map(r -> buildRequestVO(r, r.getFromUserId(), r.getToUserId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<FriendRequestVO> getSentRequests(Long userId) {
        List<FriendRequest> requests = friendRequestMapper.findSentByUserId(userId);
        return requests.stream()
                .map(r -> buildRequestVO(r, r.getFromUserId(), r.getToUserId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<FriendVO> getFriendList(Long userId) {
        List<Long> friendIds = friendMapper.findFriendIdsByUserId(userId);
        if (friendIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<User> friends = userMapper.selectBatchIds(friendIds);
        return friends.stream()
                .filter(u -> u.getDeleted() == null || u.getDeleted() == 0)
                .map(u -> {
                    FriendVO vo = new FriendVO();
                    vo.setUserId(u.getId());
                    vo.setUsername(u.getUsername());
                    vo.setNickname(u.getNickname());
                    vo.setAvatar(u.getAvatar());
                    vo.setStatus(u.getStatus());
                    // 从好友关系表获取创建时间
                    Friend f = friendMapper.findByUserAndFriend(userId, u.getId());
                    vo.setCreatedAt(f != null ? f.getCreatedAt() : null);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<UserVO> searchUsers(String keyword, Long currentUserId) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }

        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .ne(User::getId, currentUserId)
                        .and(w -> w.like(User::getUsername, keyword)
                                .or()
                                .like(User::getNickname, keyword))
                        .eq(User::getDeleted, 0)
                        .last("LIMIT 20")
        );

        return users.stream()
                .map(UserVO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void removeFriend(Long userId, Long friendUserId) {
        friendMapper.delete(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getUserId, userId)
                        .eq(Friend::getFriendId, friendUserId)
        );
        friendMapper.delete(
                new LambdaQueryWrapper<Friend>()
                        .eq(Friend::getUserId, friendUserId)
                        .eq(Friend::getFriendId, userId)
        );
    }

    // ========== 私有方法 ==========

    private void createFriendship(Long user1, Long user2) {
        // user1 → user2
        Friend f1 = new Friend();
        f1.setUserId(user1);
        f1.setFriendId(user2);
        f1.setDeleted(0);
        f1.setCreatedAt(LocalDateTime.now());
        friendMapper.insert(f1);

        // user2 → user1
        Friend f2 = new Friend();
        f2.setUserId(user2);
        f2.setFriendId(user1);
        f2.setDeleted(0);
        f2.setCreatedAt(LocalDateTime.now());
        friendMapper.insert(f2);
    }

    private FriendRequestVO buildRequestVO(FriendRequest request, Long fromUserId, Long toUserId) {
        FriendRequestVO vo = FriendRequestVO.fromEntity(request);

        User fromUser = userMapper.selectById(fromUserId);
        if (fromUser != null) {
            vo.setFromUsername(fromUser.getUsername());
            vo.setFromNickname(fromUser.getNickname());
            vo.setFromAvatar(fromUser.getAvatar());
        }

        User toUser = userMapper.selectById(toUserId);
        if (toUser != null) {
            vo.setToUsername(toUser.getUsername());
            vo.setToNickname(toUser.getNickname());
            vo.setToAvatar(toUser.getAvatar());
        }

        return vo;
    }
}
