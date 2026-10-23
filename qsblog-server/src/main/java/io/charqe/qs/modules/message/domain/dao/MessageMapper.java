package io.charqe.qs.modules.message.domain.dao;

import io.charqe.qs.common.model.mp.CoreMapper;
import io.charqe.qs.modules.message.domain.pojo.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MessageMapper extends CoreMapper<Message> {

    @Update("UPDATE message SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(@Param("id") Long id);

    @Update("UPDATE message SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{id}")
    int decrementLikeCount(@Param("id") Long id);

    @Update("UPDATE message SET dislike_count = dislike_count + 1 WHERE id = #{id}")
    int incrementDislikeCount(@Param("id") Long id);

    @Update("UPDATE message SET dislike_count = GREATEST(dislike_count - 1, 0) WHERE id = #{id}")
    int decrementDislikeCount(@Param("id") Long id);

    @Update("UPDATE message SET reply_count = reply_count + 1, last_reply_time = NOW() WHERE id = #{messageId}")
    int incrementReplyCount(@Param("messageId") Long messageId);

    @Update("UPDATE message SET reply_count = GREATEST(reply_count - 1, 0) WHERE id = #{messageId}")
    int decrementReplyCount(@Param("messageId") Long messageId);
}
