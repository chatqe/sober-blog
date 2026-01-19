package io.charqe.qs.modules.comment.domain.dao;

import io.charqe.qs.modules.comment.domain.pojo.Comment;
import io.charqe.qs.common.model.mp.CoreMapper;
import org.apache.ibatis.annotations.Mapper;


/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Mapper
public interface CommentMapper extends CoreMapper<Comment> {

}


