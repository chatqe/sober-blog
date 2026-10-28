package io.charqe.qs.modules.comment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.R;

import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.modules.comment.domain.pojo.Comment;
import io.charqe.qs.common.model.mp.CoreService;
import io.charqe.qs.modules.comment.controller.web.dto.AuthorCommentQueryDTO;
import io.charqe.qs.modules.comment.controller.web.vo.AuthorCommentListVO;
import io.charqe.qs.modules.comment.controller.web.vo.CommentVO;

import java.util.List;

public interface CommentService extends CoreService<Comment> {

    R saveComment(CommentVO commentVO);

    R deleteComment(Long id);

    R<PageResult> listComment(BaseReqVO baseReqVO);

    R<PageResult> listAdminComment(BaseReqVO baseReqVO, Boolean isBoss);

    AuthorCommentListVO listAuthorComments(AuthorCommentQueryDTO dto, Long currentUserId);

    List<CommentVO> listCommentByBiz(Long bizId, Integer bizType);
}


