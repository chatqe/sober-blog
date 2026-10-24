package io.charqe.qs.modules.comment.controller.admin;

import io.charqe.qs.modules.user.auth.SessionScope;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.R;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.modules.article.domain.pojo.Article;
import io.charqe.qs.modules.comment.domain.pojo.Comment;
import io.charqe.qs.modules.comment.enums.CommentTypeEnum;
import io.charqe.qs.modules.article.service.ArticleService;
import io.charqe.qs.modules.comment.service.CommentService;
import io.charqe.qs.utils.BlogUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@RestController
@RequestMapping("/admin/comment")
@Tag(name = "adminComment", description = "管理后台-评论管理")
public class AdminCommentController {

    @Resource
    private ArticleService articleService;

    @Resource
    private CommentService commentService;

    /**
     * 作者删除评论
     *
     * @param id 评论ID
     * @return 删除结果
     */
    @Operation(summary = "作者删除评论", description = "用于文章作者删除自己文章下的评论")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误或权限不足")
    @ApiResponse(responseCode = "401", description = "未授权，需要登录后操作")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @GetMapping("/user/deleteComment")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R userDeleteComment(
        @Parameter(name = "id", description = "评论ID", required = true, example = "1")
        @RequestParam("id") Integer id) {
        Comment comment = commentService.lambdaQuery().select(Comment::getBizId, Comment::getBizType).eq(Comment::getId, id).one();
        if (comment == null) {
            return R.success();
        }
        if (!CommentTypeEnum.ARTICLE.getCode().equals(comment.getBizType())) {
            return R.fail("权限不足！");
        }
        Article one = articleService.lambdaQuery().eq(Article::getId, comment.getBizId()).select(Article::getUserId).one();
        if (one == null || (BlogUtil.getUserId().intValue() != one.getUserId().intValue())) {
            return R.fail("权限不足！");
        }
        commentService.removeById(id);
        return R.success();
    }

    /**
     * 管理员删除评论
     *
     * @param id 评论ID
     * @return 删除结果
     */
    @Operation(summary = "管理员删除评论", description = "用于管理员删除任意评论")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "未授权，需要管理员权限")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @GetMapping("/delComment")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R adminDelComment(
        @Parameter(name = "id", description = "评论ID", required = true, example = "1")
        @RequestParam("id") Integer id) {
        commentService.removeById(id);
        return R.success();
    }



    /**
     * 管理员查询评论
     *
     * @param baseReqVO 查询参数
     * @return 评论分页列表
     */
    @Operation(summary = "管理员查询评论", description = "用于管理员查询所有评论列表")
    @ApiResponse(responseCode = "200", description = "查询成功，返回评论分页列表")
    @ApiResponse(responseCode = "401", description = "未授权，需要管理员权限")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @PostMapping("/list")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R<Page> listBossComment(
        @Parameter(name = "baseReqVO", description = "查询参数", required = true, example = "{\"page\":1,\"pageSize\":10}")
        @RequestBody BaseReqVO baseReqVO) {
        return commentService.listAdminComment(baseReqVO, true);
    }
}

