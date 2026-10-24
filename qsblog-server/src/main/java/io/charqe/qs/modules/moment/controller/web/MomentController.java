package io.charqe.qs.modules.moment.controller.web;

import io.charqe.qs.modules.user.auth.SessionScope;

import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.behavior.controller.web.dto.BehDTO;
import io.charqe.qs.modules.behavior.service.comment.CommentActionService;
import io.charqe.qs.modules.behavior.service.like.LikeService;
import io.charqe.qs.modules.behavior.service.view.ViewService;
import io.charqe.qs.modules.comment.controller.web.vo.CommentVO;
import io.charqe.qs.modules.comment.service.CommentService;
import io.charqe.qs.modules.file.enums.BizTypeEnum;
import io.charqe.qs.modules.moment.controller.web.dto.MomentSaveReqDTO;
import io.charqe.qs.modules.moment.controller.web.dto.MomentSyncReqDTO;
import io.charqe.qs.modules.moment.controller.web.dto.TimelineQueryDTO;
import io.charqe.qs.modules.moment.controller.web.vo.*;
import io.charqe.qs.modules.moment.service.MomentService;
import io.charqe.qs.utils.BlogUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 动态管理控制器（Web端）
 *
 * @author Joy
 * @since 2026-06-25
 */
@RestController
@RequestMapping("/moment")
@Tag(name = "moment", description = "微言/动态 Web 端接口")
public class MomentController {

    @Resource
    private MomentService momentService;
    @Resource
    private LikeService likeService;
    @Resource
    private ViewService viewService;
    @Resource
    private CommentService commentService;
    @Resource
    private CommentActionService commentActionService;

    // ── 时间线 ──────────────────────────────────────────────────

    @Operation(summary = "公开动态时间线")
    @ApiResponse(responseCode = "200", description = "成功返回分页列表")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @PostMapping("/timeline")
    public R<PageResult<MomentListVO>> timeline(
            @Parameter(name = "query", description = "分页查询参数", required = true)
            @RequestBody TimelineQueryDTO query) {
        return R.success(momentService.timeline(query, BlogUtil.getUserId()));
    }

    @Operation(summary = "当前用户自己的动态时间线")
    @ApiResponse(responseCode = "200", description = "成功返回分页列表")
    @ApiResponse(responseCode = "401", description = "未登录")
    @LoginCheck(scope = SessionScope.WEB)
    @PostMapping("/my/timeline")
    public R<PageResult<MomentListVO>> myTimeline(
            @Parameter(name = "query", description = "分页查询参数", required = true)
            @RequestBody TimelineQueryDTO query) {
        return R.success(momentService.myTimeline(query, BlogUtil.getUserId()));
    }

    @Operation(summary = "指定用户的公开动态时间线")
    @ApiResponse(responseCode = "200", description = "成功返回分页列表")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @GetMapping("/{userId}/timeline")
    public R<PageResult<MomentListVO>> userTimeline(
            @Parameter(name = "userId", description = "用户ID", required = true)
            @PathVariable("userId") Long userId,
            @Parameter(name = "pageNum", description = "页码，默认1")
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @Parameter(name = "pageSize", description = "每页条数，默认10，最大50")
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        TimelineQueryDTO query = new TimelineQueryDTO();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        return R.success(momentService.userTimeline(userId, query, BlogUtil.getUserId()));
    }

    // ── 详情 ────────────────────────────────────────────────────

    @Operation(summary = "获取动态详情")
    @ApiResponse(responseCode = "200", description = "成功返回详情")
    @ApiResponse(responseCode = "404", description = "动态不存在或不可见")
    @GetMapping("/{id}")
    public R<MomentDetailVO> getDetail(
            @Parameter(name = "id", description = "动态ID", required = true)
            @PathVariable("id") Long id) {
        return R.success(momentService.getWebDetail(id, BlogUtil.getUserId()));
    }

    // ── 发布 / 更新 / 删除 ─────────────────────────────────────

    @Operation(summary = "发布动态（仅站长）")
    @ApiResponse(responseCode = "200", description = "发布成功，返回动态ID")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "未登录")
    @ApiResponse(responseCode = "403", description = "仅站长可操作")
    @LoginCheck(value = 0, scope = SessionScope.EITHER)
    @PostMapping("/publish")
    public R<Long> publish(
            @Parameter(name = "dto", description = "动态内容", required = true)
            @Validated @RequestBody MomentSaveReqDTO dto) {
        return R.success(momentService.publish(BlogUtil.getUserId(), dto));
    }

    @Operation(summary = "更新动态（仅站长）")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误或无权操作")
    @ApiResponse(responseCode = "401", description = "未登录")
    @ApiResponse(responseCode = "403", description = "仅站长可操作")
    @LoginCheck(value = 0, scope = SessionScope.EITHER)
    @PutMapping("/{id}")
    public R<Void> update(
            @Parameter(name = "id", description = "动态ID", required = true)
            @PathVariable("id") Long id,
            @Parameter(name = "dto", description = "更新内容", required = true)
            @Validated @RequestBody MomentSaveReqDTO dto) {
        momentService.updateWeb(id, BlogUtil.getUserId(), dto);
        return R.success(null);
    }

    @Operation(summary = "删除动态（仅站长）")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "401", description = "未登录")
    @ApiResponse(responseCode = "403", description = "仅站长可操作")
    @LoginCheck(value = 0, scope = SessionScope.EITHER)
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @Parameter(name = "id", description = "动态ID", required = true)
            @PathVariable("id") Long id) {
        momentService.deleteWeb(id, BlogUtil.getUserId());
        return R.success(null);
    }

    // ── 点赞 ────────────────────────────────────────────────────

    @Operation(summary = "点赞动态")
    @ApiResponse(responseCode = "200", description = "点赞成功")
    @ApiResponse(responseCode = "401", description = "未登录")
    @LoginCheck(scope = SessionScope.WEB)
    @PostMapping("/{id}/like")
    public R<Void> like(
            @Parameter(name = "id", description = "动态ID", required = true)
            @PathVariable("id") Long id) {
        BehDTO behDTO = new BehDTO();
        behDTO.setBizId(id);
        behDTO.setBizType(BizTypeEnum.MOMENT.getCode());
        likeService.like(behDTO, BlogUtil.getUserId());
        return R.success(null);
    }

    @Operation(summary = "取消点赞动态")
    @ApiResponse(responseCode = "200", description = "取消成功")
    @ApiResponse(responseCode = "401", description = "未登录")
    @LoginCheck(scope = SessionScope.WEB)
    @DeleteMapping("/{id}/like")
    public R<Void> unlike(
            @Parameter(name = "id", description = "动态ID", required = true)
            @PathVariable("id") Long id) {
        BehDTO behDTO = new BehDTO();
        behDTO.setBizId(id);
        behDTO.setBizType(BizTypeEnum.MOMENT.getCode());
        likeService.unlike(behDTO, BlogUtil.getUserId());
        return R.success(null);
    }

    @Operation(summary = "获取点赞该动态的用户列表")
    @ApiResponse(responseCode = "200", description = "成功返回点赞用户列表")
    @GetMapping("/{id}/likes")
    public R<List<MomentLikeUserVO>> listLikes(
            @Parameter(name = "id", description = "动态ID", required = true)
            @PathVariable("id") Long id) {
        return R.success(momentService.listLikes(id));
    }

    // ── 评论 ────────────────────────────────────────────────────

    @Operation(summary = "发表评论")
    @ApiResponse(responseCode = "200", description = "评论成功")
    @ApiResponse(responseCode = "400", description = "评论内容不合法")
    @ApiResponse(responseCode = "401", description = "未登录")
    @LoginCheck(scope = SessionScope.WEB)
    @PostMapping("/{id}/comment")
    public R<Void> comment(
            @Parameter(name = "id", description = "动态ID", required = true)
            @PathVariable("id") Long id,
            @Parameter(name = "content", description = "评论内容", required = true)
            @RequestBody CommentVO commentVO) {
        // bizId/bizType 由服务端根据路径补全，不能交给 @Validated 先行校验
        if (!StringUtils.hasText(commentVO.getContent())) {
            throw new BizException("评论内容不能为空");
        }
        commentVO.setBizId(id);
        commentVO.setBizType(BizTypeEnum.MOMENT.getCode());
        commentService.saveComment(commentVO);
        BehDTO behDTO = new BehDTO();
        behDTO.setBizId(id);
        behDTO.setBizType(BizTypeEnum.MOMENT.getCode());
        commentActionService.comment(behDTO, BlogUtil.getUserId());
        return R.success(null);
    }

    @Operation(summary = "获取动态评论列表")
    @ApiResponse(responseCode = "200", description = "成功返回评论列表")
    @GetMapping("/{id}/comments")
    public R<List<CommentVO>> listComments(
            @Parameter(name = "id", description = "动态ID", required = true)
            @PathVariable("id") Long id) {
        return R.success(commentService.listCommentByBiz(id, BizTypeEnum.MOMENT.getCode()));
    }

    @Operation(summary = "删除评论")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "401", description = "未登录")
    @LoginCheck(scope = SessionScope.WEB)
    @DeleteMapping("/comment/{commentId}")
    public R<Void> deleteComment(
            @Parameter(name = "commentId", description = "评论ID", required = true)
            @PathVariable("commentId") Long commentId) {
        return commentService.deleteComment(commentId);
    }

    @Operation(summary = "点赞评论")
    @ApiResponse(responseCode = "200", description = "点赞成功")
    @ApiResponse(responseCode = "401", description = "未登录")
    @LoginCheck(scope = SessionScope.WEB)
    @PostMapping("/comment/{commentId}/like")
    public R<Void> likeComment(
            @Parameter(name = "commentId", description = "评论ID", required = true)
            @PathVariable("commentId") Long commentId) {
        BehDTO behDTO = new BehDTO();
        behDTO.setBizId(commentId);
        behDTO.setBizType(BizTypeEnum.COMMENT.getCode());
        likeService.like(behDTO, BlogUtil.getUserId());
        return R.success(null);
    }

    @Operation(summary = "取消点赞评论")
    @ApiResponse(responseCode = "200", description = "取消成功")
    @ApiResponse(responseCode = "401", description = "未登录")
    @LoginCheck(scope = SessionScope.WEB)
    @DeleteMapping("/comment/{commentId}/like")
    public R<Void> unlikeComment(
            @Parameter(name = "commentId", description = "评论ID", required = true)
            @PathVariable("commentId") Long commentId) {
        BehDTO behDTO = new BehDTO();
        behDTO.setBizId(commentId);
        behDTO.setBizType(BizTypeEnum.COMMENT.getCode());
        likeService.unlike(behDTO, BlogUtil.getUserId());
        return R.success(null);
    }

    @Operation(summary = "获取评论点赞用户列表")
    @ApiResponse(responseCode = "200", description = "成功返回点赞用户列表")
    @GetMapping("/comment/{commentId}/likes")
    public R<List<MomentLikeUserVO>> listCommentLikes(
            @Parameter(name = "commentId", description = "评论ID", required = true)
            @PathVariable("commentId") Long commentId) {
        return R.success(momentService.listCommentLikes(commentId, BizTypeEnum.COMMENT.getCode()));
    }

    // ── 浏览 ────────────────────────────────────────────────────

    @Operation(summary = "记录浏览（幂等，匿名直接忽略）")
    @ApiResponse(responseCode = "200", description = "记录成功")
    @PostMapping("/{id}/view")
    public R<Void> view(
            @Parameter(name = "id", description = "动态ID", required = true)
            @PathVariable("id") Long id) {
        BehDTO behDTO = new BehDTO();
        behDTO.setBizId(id);
        behDTO.setBizType(BizTypeEnum.MOMENT.getCode());
        Long userId = BlogUtil.getUserId();
        if (userId == null) {
            return R.success(null);
        }
        viewService.view(behDTO, userId);
        return R.success(null);
    }

    // ── 同步 ────────────────────────────────────────────────────

    @Operation(summary = "同步到多平台（仅站长）")
    @ApiResponse(responseCode = "200", description = "同步结果")
    @ApiResponse(responseCode = "401", description = "未登录")
    @ApiResponse(responseCode = "403", description = "仅站长可操作")
    @LoginCheck(value = 0, scope = SessionScope.EITHER)
    @PostMapping("/sync")
    public R<MomentSyncVO> sync(
            @Parameter(name = "dto", description = "同步请求", required = true)
            @Validated @RequestBody MomentSyncReqDTO dto) {
        return R.success(momentService.sync(dto, BlogUtil.getUserId()));
    }

    @Operation(summary = "获取同步记录列表")
    @ApiResponse(responseCode = "200", description = "成功返回同步记录")
    @GetMapping("/sync/records")
    public R<List<MomentSyncRecordVO>> listSyncRecords(
            @Parameter(name = "momentId", description = "动态ID", required = true)
            @RequestParam("momentId") Long momentId) {
        return R.success(momentService.listSyncRecords(momentId));
    }
}
