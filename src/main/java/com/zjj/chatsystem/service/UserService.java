package com.zjj.chatsystem.service;

import com.zjj.chatsystem.common.result.PageResult;
import com.zjj.chatsystem.domain.dto.UserLoginRequest;
import com.zjj.chatsystem.domain.dto.UserRegisterRequest;
import com.zjj.chatsystem.domain.dto.UserVO;
import com.zjj.chatsystem.domain.query.UserPageQuery;

import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     */
    UserVO register(UserRegisterRequest request);

    /**
     * 用户登录
     */
    Map<String, Object> login(UserLoginRequest request);

    /**
     * 根据 ID 获取用户
     */
    UserVO getUserById(Long id);

    /**
     * 根据用户名获取用户
     */
    UserVO getUserByUsername(String username);

    /**
     * 根据用户名获取用户 ID
     */
    Long getUserIdByUsername(String username);

    /**
     * 分页查询用户列表
     */
    PageResult<UserVO> listUsers(UserPageQuery query);

    /**
     * 更新用户状态
     */
    void updateStatus(Long userId, int status);
}
