package com.joy.qs.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joy.qs.common.R;

import com.joy.qs.model.BaseReqVO;
import com.joy.qs.model.comment.pojo.Comment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.joy.qs.model.comment.vo.CommentVO;


/**
 * <p>
 * 文章评论表 服务类
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
public interface CommentService extends IService<Comment> {

    R saveComment(CommentVO commentVO);

    R deleteComment(Integer id);

    R<BaseReqVO> listComment(BaseReqVO baseReqVO);

    R<Page> listAdminComment(BaseReqVO baseReqVO, Boolean isBoss);
}
