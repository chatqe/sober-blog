package io.charqe.qs.modules.comment.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.common.model.R;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.article.domain.dao.ArticleMapper;
import io.charqe.qs.modules.comment.domain.dao.CommentMapper;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.modules.comment.controller.web.vo.CommentVO;
import io.charqe.qs.modules.article.domain.pojo.Article;
import io.charqe.qs.modules.comment.domain.pojo.Comment;
import io.charqe.qs.modules.user.domain.pojo.User;
import io.charqe.qs.common.enums.CodeMsg;
import io.charqe.qs.common.enums.CommentTypeEnum;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.CommonQuery;
import io.charqe.qs.infra.thirdparty.mail.MailSendUtil;
import org.springframework.beans.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Service
@Slf4j
public class CommentServiceImpl extends CoreServiceImpl<CommentMapper, Comment> implements CommentService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CommonQuery commonQuery;

    @Autowired
    private MailSendUtil mailSendUtil;

    @Override
    public R saveComment(CommentVO commentVO) {
        if (CommentTypeEnum.getByCode(commentVO.getType()) == null) {
            return R.fail("评论来源类型不存在！");
        }
        Article one = null;
        if (CommentTypeEnum.ARTICLE.getCode().equals(commentVO.getType())) {
            LambdaQueryChainWrapper<Article> articleWrapper = new LambdaQueryChainWrapper<>(articleMapper);
            one = articleWrapper.eq(Article::getId, commentVO.getSource()).select(Article::getUserId, Article::getArticleTitle, Article::getCommentStatus).one();

            if (one == null) {
                return R.fail("文章不存在");
            } else {
                if (!one.getCommentStatus()) {
                    return R.fail("评论功能已关闭！");
                }
            }
        }


        Comment comment = new Comment();
        comment.setSource(commentVO.getSource());
        comment.setType(commentVO.getType());
        comment.setCommentContent(commentVO.getCommentContent());
        comment.setParentCommentId(commentVO.getParentCommentId());
        comment.setFloorCommentId(commentVO.getFloorCommentId());
        comment.setParentUserId(commentVO.getParentUserId());
        comment.setUserId(BlogUtil.getUserId());
        if (StringUtils.hasText(commentVO.getCommentInfo())) {
            comment.setCommentInfo(commentVO.getCommentInfo());
        }
        save(comment);

        try {
            mailSendUtil.sendCommentMail(commentVO, one, this);
        } catch (Exception e) {
            log.error("发送评论邮件失败：", e);
        }

        return R.success();
    }

    @Override
    public R deleteComment(Long id) {
        Long userId = BlogUtil.getUserId();
        lambdaUpdate().eq(Comment::getId, id)
                .eq(Comment::getUserId, userId)
                .remove();
        return R.success();
    }

    @Override
    public R<BaseReqVO> listComment(BaseReqVO baseReqVO) {
        if (baseReqVO.getSource() == null || !StringUtils.hasText(baseReqVO.getCommentType())) {
            return R.fail(CodeMsg.PARAMETER_ERROR);
        }

        if (CommentTypeEnum.ARTICLE.getCode().equals(baseReqVO.getCommentType())) {
            LambdaQueryChainWrapper<Article> articleWrapper = new LambdaQueryChainWrapper<>(articleMapper);
            Article one = articleWrapper.eq(Article::getId, baseReqVO.getSource()).select(Article::getCommentStatus).one();

            if (one != null && !one.getCommentStatus()) {
                return R.fail("评论功能已关闭！");
            }
        }


        if (baseReqVO.getFloorCommentId() == null) {
            lambdaQuery().eq(Comment::getSource, baseReqVO.getSource()).eq(Comment::getType, baseReqVO.getCommentType()).eq(Comment::getParentCommentId, CommonConst.FIRST_COMMENT).orderByDesc(Comment::getCreateTime).page(baseReqVO);
            List<Comment> comments = baseReqVO.getRecords();
            if (CollectionUtils.isEmpty(comments)) {
                return R.success(baseReqVO);
            }
            List<CommentVO> commentVOs = comments.stream().map(c -> {
                CommentVO commentVO = buildCommentVO(c);
                Page page = new Page(1, 5);
                lambdaQuery().eq(Comment::getSource, baseReqVO.getSource()).eq(Comment::getType, baseReqVO.getCommentType()).eq(Comment::getFloorCommentId, c.getId()).orderByAsc(Comment::getCreateTime).page(page);
                List<Comment> childComments = page.getRecords();
                if (childComments != null) {
                    List<CommentVO> ccVO = childComments.stream().map(cc -> buildCommentVO(cc)).collect(Collectors.toList());
                    page.setRecords(ccVO);
                }
                commentVO.setChildComments(page);
                return commentVO;
            }).collect(Collectors.toList());
            baseReqVO.setRecords(commentVOs);
        } else {
            lambdaQuery().eq(Comment::getSource, baseReqVO.getSource()).eq(Comment::getType, baseReqVO.getCommentType()).eq(Comment::getFloorCommentId, baseReqVO.getFloorCommentId()).orderByAsc(Comment::getCreateTime).page(baseReqVO);
            List<Comment> childComments = baseReqVO.getRecords();
            if (CollectionUtils.isEmpty(childComments)) {
                return R.success(baseReqVO);
            }
            List<CommentVO> ccVO = childComments.stream().map(cc -> buildCommentVO(cc)).collect(Collectors.toList());
            baseReqVO.setRecords(ccVO);
        }
        return R.success(baseReqVO);
    }

    @Override
    public R<Page> listAdminComment(BaseReqVO baseReqVO, Boolean isBoss) {
        LambdaQueryChainWrapper<Comment> wrapper = lambdaQuery();
        if (isBoss) {
            if (baseReqVO.getSource() != null) {
                wrapper.eq(Comment::getSource, baseReqVO.getSource());
            }
            if (StringUtils.hasText(baseReqVO.getCommentType())) {
                wrapper.eq(Comment::getType, baseReqVO.getCommentType());
            }
            wrapper.orderByDesc(Comment::getCreateTime).page(baseReqVO);
        } else {
            List<Long> userArticleIds = commonQuery.getUserArticleIds(BlogUtil.getUserId());
            if (CollectionUtils.isEmpty(userArticleIds)) {
                baseReqVO.setTotal(0);
                baseReqVO.setRecords(new ArrayList());
            } else {
                if (baseReqVO.getSource() != null) {
                    wrapper.eq(Comment::getSource, baseReqVO.getSource()).eq(Comment::getType, CommentTypeEnum.ARTICLE.getCode());
                } else {
                    wrapper.eq(Comment::getType, CommentTypeEnum.ARTICLE.getCode()).in(Comment::getSource, userArticleIds);
                }
                wrapper.orderByDesc(Comment::getCreateTime).page(baseReqVO);
            }
        }
        return R.success(baseReqVO);
    }

    private CommentVO buildCommentVO(Comment c) {
        CommentVO commentVO = new CommentVO();
        BeanUtils.copyProperties(c, commentVO);

        User user = commonQuery.getUser(commentVO.getUserId().longValue());
        if (user != null) {
            commentVO.setAvatar(user.getAvatar());
            commentVO.setUsername(user.getUsername());
        }

        if (!StringUtils.hasText(commentVO.getUsername())) {
            commentVO.setUsername(BlogUtil.getRandomName(commentVO.getUserId().toString()));
        }

        if (commentVO.getParentUserId() != null) {
            User u = commonQuery.getUser(commentVO.getParentUserId().longValue());
            if (u != null) {
                commentVO.setParentUsername(u.getUsername());
            }
            if (!StringUtils.hasText(commentVO.getParentUsername())) {
                commentVO.setParentUsername(BlogUtil.getRandomName(commentVO.getParentUserId().toString()));
            }
        }
        return commentVO;
    }
}


