package com.zjj.chatsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zjj.chatsystem.domain.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM users WHERE username = #{username}")
    Optional<User> findByUsername(String username);
}
