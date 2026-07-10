package com.zjj.chatsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zjj.chatsystem.domain.entity.ChatMessage;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * 聊天消息 Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 统计单聊中当前用户的未读消息数（按发送者分组）
     * 未读 = status 为 SENT 或 DELIVERED 且不是我发送的
     */
    @MapKey("sender_id")
    @Select("SELECT sender_id, COUNT(*) AS cnt FROM chat_messages " +
            "WHERE receiver_id = #{userId} AND group_id IS NULL " +
            "AND status IN ('SENT', 'DELIVERED') GROUP BY sender_id")
    Map<Long, Long> countUnreadByReceiver(Long userId);
}
