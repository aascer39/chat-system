package com.zjj.chatsystem.controller;

import com.zjj.chatsystem.common.result.Result;
import com.zjj.chatsystem.domain.dto.UserLoginRequest;
import com.zjj.chatsystem.domain.dto.UserRegisterRequest;
import com.zjj.chatsystem.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody UserRegisterRequest request) {
        return Result.success(userService.register(request));
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody UserLoginRequest request) {
        return Result.success(userService.login(request));
    }
}
