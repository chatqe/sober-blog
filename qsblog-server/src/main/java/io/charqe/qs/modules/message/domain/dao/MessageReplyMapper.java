package io.charqe.qs.modules.message.domain.dao;

import io.charqe.qs.common.model.mp.CoreMapper;
import io.charqe.qs.modules.message.domain.pojo.MessageReply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MessageReplyMapper extends CoreMapper<MessageReply> {

    @Update("UPDATE message_reply SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(@Param("id") Long id);

    @Update("UPDATE message_reply SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{id}")
    int decrementLikeCount(@Param("id") Long id);

    @Update("UPDATE message_reply SET dislike_count = dislike_count + 1 WHERE id = #{id}")
    int incrementDislikeCount(@Param("id") Long id);

    @Update("UPDATE message_reply SET dislike_count = GREATEST(dislike_count - 1, 0) WHERE id = #{id}")
    int decrementDislikeCount(@Param("id") Long id);

    @Update("UPDATE message_reply SET child_cnt = child_cnt + 1 WHERE id = #{parentId}")
    int incrementChildCount(@Param("parentId") Long parentId);

    @Update("UPDATE message_reply SET child_cnt = GREATEST(child_cnt - 1, 0) WHERE id = #{parentId}")
    int decrementChildCount(@Param("parentId") Long parentId);

    @Update("UPDATE message SET reply_count = reply_count + 1, last_reply_time = NOW() WHERE id = #{messageId}")
    int incrementReplyCount(@Param("messageId") Long messageId);

    @Update("UPDATE message SET reply_count = GREATEST(reply_count - 1, 0) WHERE id = #{messageId}")
    int decrementReplyCount(@Param("messageId") Long messageId);
}
