package io.charqe.qs.modules.comment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.common.model.R;

import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.modules.comment.domain.pojo.Comment;
import com.baomidou.mybatisplus.extension.service.IService;
import io.charqe.qs.modules.comment.controller.web.vo.CommentVO;


/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
public interface CommentService extends IService<Comment> {

    R saveComment(CommentVO commentVO);

    R deleteComment(Integer id);

    R<BaseReqVO> listComment(BaseReqVO baseReqVO);

    R<Page> listAdminComment(BaseReqVO baseReqVO, Boolean isBoss);
}


