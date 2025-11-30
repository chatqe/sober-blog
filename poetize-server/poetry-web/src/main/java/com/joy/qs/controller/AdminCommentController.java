package com.joy.qs.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joy.qs.aop.LoginCheck;
import com.joy.qs.common.R;
import com.joy.qs.model.BaseReqVO;
import com.joy.qs.model.article.pojo.Article;
import com.joy.qs.model.comment.pojo.Comment;
import com.joy.qs.enums.CommentTypeEnum;
import com.joy.qs.service.ArticleService;
import com.joy.qs.service.CommentService;
import com.joy.qs.utils.BlogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
public class AdminCommentController {

    @Autowired
    private ArticleService articleService;

    @Autowired
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
