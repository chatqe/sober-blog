package io.charqe.qs.modules.comment.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.R;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.modules.article.domain.pojo.Article;
import io.charqe.qs.modules.comment.domain.pojo.Comment;
import io.charqe.qs.common.enums.CommentTypeEnum;
import io.charqe.qs.modules.article.service.ArticleService;
import io.charqe.qs.modules.comment.service.CommentService;
import io.charqe.qs.utils.BlogUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * 后台评论 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
@RestController
@RequestMapping("/admin")
@Tag(name = "管理后台-评论管理", description = "后台评论相关")
public class AdminCommentController {

    @Resource
    private ArticleService articleService;

    @Resource
    private CommentService commentService;

    /**
     * 作者删除评论
     */
    @GetMapping("/comment/user/deleteComment")
    @LoginCheck(1)
    public R userDeleteComment(@RequestParam("id") Integer id) {
        Comment comment = commentService.lambdaQuery().select(Comment::getSource, Comment::getType).eq(Comment::getId, id).one();
        if (comment == null) {
            return R.success();
        }
        if (!CommentTypeEnum.COMMENT_TYPE_ARTICLE.getCode().equals(comment.getType())) {
            return R.fail("权限不足！");
        }
        Article one = articleService.lambdaQuery().eq(Article::getId, comment.getSource()).select(Article::getUserId).one();
        if (one == null || (BlogUtil.getUserId().intValue() != one.getUserId().intValue())) {
            return R.fail("权限不足！");
        }
        commentService.removeById(id);
        return R.success();
    }

    /**
     * Boss删除评论
     */
    @GetMapping("/comment/boss/deleteComment")
    @LoginCheck(0)
    public R bossDeleteComment(@RequestParam("id") Integer id) {
        commentService.removeById(id);
        return R.success();
    }

    /**
     * 用户查询评论
     */
    @PostMapping("/comment/user/list")
    @LoginCheck(1)
    public R<Page> listUserComment(@RequestBody BaseReqVO baseReqVO) {
        return commentService.listAdminComment(baseReqVO, false);
    }

    /**
     * Boss查询评论
     */
    @PostMapping("/comment/boss/list")
    @LoginCheck(0)
    public R<Page> listBossComment(@RequestBody BaseReqVO baseReqVO) {
        return commentService.listAdminComment(baseReqVO, true);
    }
}

