package io.charqe.qs.modules.comment.domain.dao;

import io.charqe.qs.modules.comment.domain.pojo.Comment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;


/**
 * <p>
 * 文章评论表 Mapper 接口
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

}


