package io.charqe.qs.modules.comment.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.common.model.R;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.article.domain.dao.ArticleMapper;
import io.charqe.qs.modules.comment.domain.dao.CommentMapper;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.modules.comment.controller.web.dto.AuthorCommentQueryDTO;
import io.charqe.qs.modules.comment.controller.web.vo.AuthorCommentListVO;
import io.charqe.qs.modules.comment.controller.web.vo.CommentVO;
import io.charqe.qs.modules.article.domain.pojo.Article;
import io.charqe.qs.modules.comment.domain.pojo.Comment;
import io.charqe.qs.modules.user.domain.dao.UserMapper;
import io.charqe.qs.modules.user.domain.pojo.User;
import io.charqe.qs.common.enums.CodeMsg;
import io.charqe.qs.modules.comment.enums.CommentTypeEnum;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.CommonQuery;
import io.charqe.qs.infra.thirdparty.mail.MailSendUtil;
import org.springframework.beans.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
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

    @Autowired
    private UserMapper userMapper;

    @Override
    public R saveComment(CommentVO commentVO) {
        if (CommentTypeEnum.getByCode(commentVO.getBizType()) == null) {
            return R.fail("评论来源类型不存在！");
        }
        Article one = null;
        if (CommentTypeEnum.ARTICLE.getCode().equals(commentVO.getBizType())) {
            LambdaQueryChainWrapper<Article> articleWrapper = new LambdaQueryChainWrapper<>(articleMapper);
            one = articleWrapper.eq(Article::getId, commentVO.getBizId()).select(Article::getUserId, Article::getArticleTitle, Article::getCommentStatus).one();

            if (one == null) {
                return R.fail("文章不存在");
            } else {
                if (!one.getCommentStatus()) {
                    return R.fail("评论功能已关闭！");
                }
            }
        }


        Comment comment = new Comment();
        comment.setBizId(commentVO.getBizId());
        comment.setBizType(commentVO.getBizType());
        comment.setContent(commentVO.getContent());
        comment.setParentId(commentVO.getParentId());
        comment.setRootId(commentVO.getRootId());
        comment.setReplyToUserId(commentVO.getReplyToUserId());
        Long authorId = BlogUtil.getUserId();
        comment.setAuthorId(authorId);
        if (authorId != null) {
            User author = commonQuery.getUser(authorId);
            if (author != null) {
                comment.setAuthorName(author.getUsername());
                comment.setAuthorAvatar(author.getAvatar());
            }
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
                .eq(Comment::getAuthorId, userId)
                .remove();
        return R.success();
    }

    @Override
    public R<BaseReqVO> listComment(BaseReqVO baseReqVO) {
        if (baseReqVO.getBizId() == null || !StringUtils.hasText(baseReqVO.getCommentType())) {
            return R.fail(CodeMsg.PARAMETER_ERROR);
        }

        Integer commentType = parseCommentType(baseReqVO.getCommentType());
        if (commentType == null) {
            return R.fail("评论来源类型不存在！");
        }

        if (CommentTypeEnum.ARTICLE.getCode().equals(commentType)) {
            LambdaQueryChainWrapper<Article> articleWrapper = new LambdaQueryChainWrapper<>(articleMapper);
            Article one = articleWrapper.eq(Article::getId, baseReqVO.getBizId()).select(Article::getCommentStatus).one();

            if (one != null && !one.getCommentStatus()) {
                return R.fail("评论功能已关闭！");
            }
        }


        if (baseReqVO.getRootId() == null) {
            lambdaQuery().eq(Comment::getBizId, baseReqVO.getBizId()).eq(Comment::getBizType, commentType).eq(Comment::getParentId, CommonConst.FIRST_COMMENT).orderByDesc(Comment::getCreateTime).page(baseReqVO);
            List<Comment> comments = baseReqVO.getRecords();
            if (CollectionUtils.isEmpty(comments)) {
                return R.success(baseReqVO);
            }
            List<CommentVO> commentVOs = comments.stream().map(c -> {
                CommentVO commentVO = buildCommentVO(c);
                Page page = new Page(1, 5);
                lambdaQuery().eq(Comment::getBizId, baseReqVO.getBizId()).eq(Comment::getBizType, commentType).eq(Comment::getRootId, c.getId()).orderByAsc(Comment::getCreateTime).page(page);
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
            lambdaQuery().eq(Comment::getBizId, baseReqVO.getBizId()).eq(Comment::getBizType, commentType).eq(Comment::getRootId, baseReqVO.getRootId()).orderByAsc(Comment::getCreateTime).page(baseReqVO);
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
            if (baseReqVO.getBizId() != null) {
                wrapper.eq(Comment::getBizId, baseReqVO.getBizId());
            }
            if (StringUtils.hasText(baseReqVO.getCommentType())) {
                Integer ct = parseCommentType(baseReqVO.getCommentType());
                if (ct != null) {
                    wrapper.eq(Comment::getBizType, ct);
                }
            }
            wrapper.orderByDesc(Comment::getCreateTime).page(baseReqVO);
        } else {
            List<Long> userArticleIds = commonQuery.getUserArticleIds(BlogUtil.getUserId());
            if (CollectionUtils.isEmpty(userArticleIds)) {
                baseReqVO.setTotal(0);
                baseReqVO.setRecords(new ArrayList());
            } else {
                if (baseReqVO.getBizId() != null) {
                    wrapper.eq(Comment::getBizId, baseReqVO.getBizId()).eq(Comment::getBizType, CommentTypeEnum.ARTICLE.getCode());
                } else {
                    wrapper.eq(Comment::getBizType, CommentTypeEnum.ARTICLE.getCode()).in(Comment::getBizId, userArticleIds);
                }
                wrapper.orderByDesc(Comment::getCreateTime).page(baseReqVO);
            }
        }
        return R.success(baseReqVO);
    }

    @Override
    public AuthorCommentListVO listAuthorComments(AuthorCommentQueryDTO dto, Long currentUserId) {
        List<Long> articleIds = commonQuery.getUserArticleIds(currentUserId);
        if (articleIds.isEmpty()) {
            return emptyResult();
        }

        LambdaQueryChainWrapper<Comment> wrapper = lambdaQuery()
                .in(Comment::getBizId, articleIds)
                .eq(Comment::getBizType, CommentTypeEnum.ARTICLE.getCode())
                .orderByDesc(Comment::getCreateTime);

        if (dto.getStatus() != null) {
            wrapper.eq(Comment::getStatus, dto.getStatus());
        }
        if (StringUtils.hasText(dto.getSearchKey())) {
            wrapper.like(Comment::getContent, dto.getSearchKey());
        }

        Page<Comment> page = wrapper.page(new Page<>(dto.getPageNum(), dto.getPageSize()));
        if (page.getRecords().isEmpty()) {
            return emptyResult();
        }

        Set<Long> authorIds = page.getRecords().stream()
                .map(Comment::getAuthorId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> replyIds = page.getRecords().stream()
                .map(Comment::getReplyToUserId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Set<Long> allIds = new java.util.HashSet<>();
        allIds.addAll(authorIds);
        allIds.addAll(replyIds);

        Map<Long, User> userMap = allIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(allIds).stream().collect(Collectors.toMap(User::getId, u -> u));

        List<AuthorCommentListVO.Item> records = page.getRecords().stream().map(c -> {
            AuthorCommentListVO.Item item = new AuthorCommentListVO.Item();
            item.setId(c.getId());
            item.setBizId(c.getBizId());
            item.setContent(c.getContent());
            item.setStatus(c.getStatus());
            item.setChildCnt(c.getChildCnt());
            item.setCreateTime(c.getCreateTime());
            if (c.getAuthorId() != null) {
                User author = userMap.get(c.getAuthorId());
                if (author != null) {
                    item.setUsername(author.getUsername());
                    item.setAvatar(author.getAvatar());
                } else {
                    item.setUsername(BlogUtil.getRandomName(c.getAuthorId().toString()));
                }
            }
            return item;
        }).collect(Collectors.toList());

        AuthorCommentListVO result = new AuthorCommentListVO();
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setPageNum((long) dto.getPageNum());
        result.setPageSize((long) dto.getPageSize());
        result.setRecords(records);
        return result;
    }

    private AuthorCommentListVO emptyResult() {
        AuthorCommentListVO result = new AuthorCommentListVO();
        result.setTotal(0L);
        result.setPages(0L);
        result.setPageNum(1L);
        result.setPageSize(0L);
        result.setRecords(Collections.emptyList());
        return result;
    }

    /**
     * 解析评论类型字符串为Integer编码
     * 支持数字字符串（如"1"）或描述字符串（如"文章评论"）
     */
    private Integer parseCommentType(String type) {
        try {
            return Integer.parseInt(type);
        } catch (NumberFormatException e) {
            CommentTypeEnum cte = CommentTypeEnum.getByDesc(type);
            return cte != null ? cte.getCode() : null;
        }
    }

    private CommentVO buildCommentVO(Comment c) {
        CommentVO commentVO = new CommentVO();
        BeanUtils.copyProperties(c, commentVO);

        User user = commonQuery.getUser(commentVO.getAuthorId().longValue());
        if (user != null) {
            commentVO.setAvatar(user.getAvatar());
            commentVO.setUsername(user.getUsername());
        }

        if (!StringUtils.hasText(commentVO.getUsername())) {
            commentVO.setUsername(BlogUtil.getRandomName(commentVO.getAuthorId().toString()));
        }

        if (commentVO.getReplyToUserId() != null) {
            User u = commonQuery.getUser(commentVO.getReplyToUserId().longValue());
            if (u != null) {
                commentVO.setParentUsername(u.getUsername());
            }
            if (!StringUtils.hasText(commentVO.getParentUsername())) {
                commentVO.setParentUsername(BlogUtil.getRandomName(commentVO.getReplyToUserId().toString()));
            }
        }
        return commentVO;
    }
}
