package io.charqe.qs.modules.behavior.controller.web;

import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.behavior.controller.web.dto.BehDTO;
import io.charqe.qs.modules.behavior.enums.BehActionType;
import io.charqe.qs.modules.behavior.service.BehaviorStateService;
import io.charqe.qs.modules.behavior.service.comment.CommentActionService;
import io.charqe.qs.modules.behavior.service.dislike.DislikeService;
import io.charqe.qs.modules.behavior.service.share.ShareService;
import io.charqe.qs.modules.behavior.service.like.LikeService;
import io.charqe.qs.modules.behavior.service.subscribe.SubscribeService;
import io.charqe.qs.modules.behavior.service.view.ViewService;
import io.charqe.qs.utils.BlogUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 统一行为控制器
 * 按行为类型拆分端点，使用 RESTful 方法区分正向/反向操作
 *
 * @author Joy
 * @since 2026-01-27 21:42
 */
@Slf4j
@RestController
@RequestMapping("/behavior")
@Tag(name = "behavior", description = "统一行为操作：点赞/点踩/订阅/转发/评论/浏览")
public class BehaviorController {

    @Resource
    private LikeService likeService;

    @Resource
    private DislikeService dislikeService;

    @Resource
    private SubscribeService subscribeService;

    @Resource
    private ShareService shareService;

    @Resource
    private CommentActionService commentActionService;

    @Resource
    private ViewService viewService;

    @Resource
    private BehaviorStateService behaviorStateService;

    // ==================== 点赞 ====================

    @Operation(summary = "点赞")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "点赞成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @PostMapping("/like")
    @LoginCheck
    public R<Void> like(
            @Parameter(description = "bizId=业务ID, bizType=业务类型", required = true)
            @RequestBody BehDTO dto) {
        likeService.like(dto, BlogUtil.getUserId());
        return R.success();
    }

    @Operation(summary = "取消点赞")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "取消点赞成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @DeleteMapping("/like")
    @LoginCheck
    public R<Void> unlike(
            @Parameter(description = "bizId=业务ID, bizType=业务类型", required = true)
            @RequestBody BehDTO dto) {
        likeService.unlike(dto, BlogUtil.getUserId());
        return R.success();
    }

    // ==================== 点踩 ====================

    @Operation(summary = "点踩")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "点踩成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @PostMapping("/dislike")
    @LoginCheck
    public R<Void> dislike(
            @Parameter(description = "bizId=业务ID, bizType=业务类型", required = true)
            @RequestBody BehDTO dto) {
        dislikeService.dislike(dto, BlogUtil.getUserId());
        return R.success();
    }

    @Operation(summary = "取消点踩")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "取消点踩成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @DeleteMapping("/dislike")
    @LoginCheck
    public R<Void> undislike(
            @Parameter(description = "bizId=业务ID, bizType=业务类型", required = true)
            @RequestBody BehDTO dto) {
        dislikeService.undislike(dto, BlogUtil.getUserId());
        return R.success();
    }

    // ==================== 订阅 ====================

    @Operation(summary = "订阅")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "订阅成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @PostMapping("/subscribe")
    @LoginCheck
    public R<Void> subscribe(
            @Parameter(description = "bizId=专栏/标签ID, bizType=业务类型", required = true)
            @RequestBody BehDTO dto) {
        subscribeService.subscribe(dto, BlogUtil.getUserId());
        return R.success();
    }

    @Operation(summary = "取消订阅")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "取消订阅成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @DeleteMapping("/subscribe")
    @LoginCheck
    public R<Void> unsubscribe(
            @Parameter(description = "bizId=专栏/标签ID, bizType=业务类型", required = true)
            @RequestBody BehDTO dto) {
        subscribeService.unsubscribe(dto, BlogUtil.getUserId());
        return R.success();
    }

    // ==================== 转发 ====================

    @Operation(summary = "转发")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "转发成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @PostMapping("/forward")
    @LoginCheck
    public R<Void> forward(
            @Parameter(description = "bizId=业务ID, bizType=业务类型", required = true)
            @RequestBody BehDTO dto) {
        shareService.share(dto, BlogUtil.getUserId());
        return R.success();
    }

    @Operation(summary = "取消转发")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "取消转发成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @DeleteMapping("/forward")
    @LoginCheck
    public R<Void> unforward(
            @Parameter(description = "bizId=业务ID, bizType=业务类型", required = true)
            @RequestBody BehDTO dto) {
        shareService.unShare(dto, BlogUtil.getUserId());
        return R.success();
    }

    // ==================== 评论行为记录 ====================

    @Operation(summary = "记录评论行为（评论内容创建由CommentController负责）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "记录成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @PostMapping("/comment")
    @LoginCheck
    public R<Void> comment(
            @Parameter(description = "bizId=业务ID, bizType=业务类型", required = true)
            @RequestBody BehDTO dto) {
        commentActionService.comment(dto, BlogUtil.getUserId());
        return R.success();
    }

    @Operation(summary = "取消评论记录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "取消记录成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @DeleteMapping("/comment")
    @LoginCheck
    public R<Void> uncomment(
            @Parameter(description = "bizId=业务ID, bizType=业务类型", required = true)
            @RequestBody BehDTO dto) {
        commentActionService.uncomment(dto, BlogUtil.getUserId());
        return R.success();
    }

    // ==================== 浏览 ====================

    @Operation(summary = "记录浏览（异步处理，登录可选）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "记录成功")
    })
    @PostMapping("/view")
    public R<Void> view(
            @Parameter(description = "bizId=业务ID, bizType=业务类型", required = true)
            @RequestBody BehDTO dto) {
        viewService.view(dto, BlogUtil.getUserId());
        return R.success();
    }

    // ==================== 状态查询 ====================

    @Operation(summary = "查询用户行为状态（点赞/点踩/订阅/转发/评论）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @GetMapping("/state")
    public R<Boolean> getState(
            @Parameter(description = "业务ID", required = true) @RequestParam Long bizId,
            @Parameter(description = "业务类型(1=文章,2=评论,6=分类,7=标签)", required = true) @RequestParam Integer bizType,
            @Parameter(description = "行为类型(1=点赞,2=点踩,3=订阅,4=转发,5=评论)", required = true) @RequestParam Integer action) {
        if (!BehActionType.contains(action)) {
            return R.fail("无效的行为类型: " + action);
        }
        Long userId = BlogUtil.getUserId();
        if (userId == null) {
            return R.fail("请先登录");
        }
        boolean hasState = behaviorStateService.checkHasState(userId, bizId, bizType, action);
        return R.success(hasState);
    }
}
