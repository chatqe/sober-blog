package io.charqe.qs.modules.comment.controller.web;

import io.charqe.qs.modules.user.auth.SessionScope;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.aop.SaveCheck;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.R;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.comment.controller.web.dto.AuthorCommentQueryDTO;
import io.charqe.qs.modules.comment.controller.web.vo.AuthorCommentListVO;
import io.charqe.qs.modules.comment.controller.web.vo.CommentVO;
import io.charqe.qs.modules.comment.service.CommentService;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.CommonQuery;
import io.charqe.qs.utils.StrUtil;
import io.charqe.qs.utils.cache.QingSiCache;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;


/**
 * 评论控制
 *
 * @author Upjqs
 * @since 2025-12-31 19:57
 */

@RestController
@RequestMapping("/comment")
@Tag(name = "comment", description = "负责评论的增删改查、评论数量统计等功能")
public class CommentController {


    @Resource
    private CommentService commentService;

    @Resource
    private CommonQuery commonQuery;

    @Resource
    private QingSiCache qingSiCache;


    /**
     * 保存评论
     * 
     * 用于创建新评论，包括评论内容、来源、类型等信息
     * 
     * @param commentVO 评论信息
     * @return 保存结果
     */
    @Operation(summary = "保存评论", description = "用于创建新评论，包括评论内容、来源、类型等信息。评论内容会自动过滤HTML标签，创建成功后会清除相关缓存。")
    @ApiResponse(responseCode = "200", description = "保存成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误，如评论内容不合法")
    @ApiResponse(responseCode = "401", description = "未授权，需要登录后操作")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @PostMapping("/save")
    @LoginCheck
    @SaveCheck
    public R saveComment(
        @Parameter(name = "commentVO", description = "评论信息对象", required = true, example = "{\"content\":\"测试评论\",\"bizId\":1,\"bizType\":1}")
        @Validated @RequestBody CommentVO commentVO) {
        String content = StrUtil.removeHtml(commentVO.getContent());
        if (!StringUtils.hasText(content)) {
            return R.fail("评论内容不合法！");
        }
        commentVO.setContent(content);

        qingSiCache.del(CommonConst.COMMENT_COUNT_CACHE + commentVO.getBizId().toString() + "_" + commentVO.getBizType());
        return commentService.saveComment(commentVO);
    }


    /**
     * 删除评论
     * 
     * 用于删除指定ID的评论
     * 
     * @param id 评论ID
     * @return 删除结果
     */
    @Operation(summary = "删除评论", description = "用于删除指定ID的评论。只有评论作者或管理员才能删除评论。")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误，如评论ID为空")
    @ApiResponse(responseCode = "401", description = "未授权，需要登录后操作")
    @ApiResponse(responseCode = "403", description = "无权限删除该评论")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @GetMapping("/del")
    @LoginCheck
    public R delComment(
        @Parameter(name = "id", description = "评论ID", required = true, example = "1")
        @RequestParam("id") Long id) {
        return commentService.deleteComment(id);
    }


    /**
     * 查询评论数量
     * 
     * 用于查询指定来源和类型的评论数量
     * 
     * @param source 来源ID，如文章ID、树洞ID等
     * @param type 来源类型，如article、treehole等
     * @return 评论数量
     */
    @Operation(summary = "查询评论数量", description = "用于查询指定来源和类型的评论数量。支持查询文章、树洞等不同类型内容的评论数。")
    @ApiResponse(responseCode = "200", description = "查询成功，返回评论数量")
    @ApiResponse(responseCode = "400", description = "请求参数错误，如来源ID或类型为空")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @GetMapping("/getCount")
    public R<Integer> getCommentCount(
        @Parameter(name = "bizId", description = "来源ID，如文章ID、树洞ID等", required = true, example = "1")
        @RequestParam("bizId") Long bizId,
        @Parameter(name = "type", description = "来源类型，如article、treehole等", required = true, example = "article")
        @RequestParam("type") String type) {
        return R.success(commonQuery.getCommentCount(bizId, type));
    }


    /**
     * 查询评论列表
     * 
     * 用于获取指定条件的评论列表，支持分页、排序等功能
     * 
     * @param baseReqVO 分页查询参数
     * @return 评论列表
     */
    @Operation(summary = "查询评论列表", description = "用于获取指定条件的评论列表，支持分页、排序等功能。返回结果包含评论内容、作者信息、创建时间等。")
    @ApiResponse(responseCode = "200", description = "查询成功，返回评论列表")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @PostMapping("/list")
    public R<PageResult> listComment(
        @Parameter(name = "baseReqVO", description = "分页查询参数", required = true, example = "{\"pageNum\":1,\"pageSize\":10}") 
        @RequestBody BaseReqVO baseReqVO) {
        return commentService.listComment(baseReqVO);
    }




    /**
     * 作者查询自己文章下的评论（新版）
     *
     * 支持按评论状态过滤和内容关键词搜索，返回结构化的分页结果
     */
    @Operation(summary = "作者查询自己文章下的评论", description = "当前登录用户查看自己发布文章收到的评论列表，支持分页、状态过滤和关键词搜索")
    @ApiResponse(responseCode = "200", description = "查询成功，返回评论分页列表")
    @ApiResponse(responseCode = "401", description = "未授权，需要登录后操作")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @PostMapping("/author/list")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R<AuthorCommentListVO> listAuthorComments(
        @Parameter(name = "dto", description = "查询参数", required = true, example = "{\"pageNum\":1,\"pageSize\":10,\"status\":1,\"searchKey\":\"很棒\"}")
        @RequestBody AuthorCommentQueryDTO dto) {
        return R.success(commentService.listAuthorComments(dto, BlogUtil.getUserId()));
    }
}



