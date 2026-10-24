package io.charqe.qs.modules.message.controller.admin;

import io.charqe.qs.modules.user.auth.SessionScope;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.message.controller.admin.dto.AdminMessagePageReqDTO;
import io.charqe.qs.modules.message.controller.admin.vo.AdminMessageVO;
import io.charqe.qs.modules.message.controller.web.vo.MessageReplyVO;
import io.charqe.qs.modules.message.service.MessageService;
import io.charqe.qs.modules.message.service.MessageReplyService;
import io.charqe.qs.utils.IPUtil;
import io.charqe.qs.utils.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/message")
@Tag(name = "adminMessage", description = "管理后台-留言管理")
public class AdminMessageController {

    @Resource
    private MessageService messageService;

    @Resource
    private MessageReplyService messageReplyService;

    @Operation(summary = "管理员查询留言列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "401", description = "未授权")
    @PostMapping("/list")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R<PageResult<AdminMessageVO>> list(
            @Parameter(description = "分页参数")
            @RequestBody AdminMessagePageReqDTO dto) {
        return R.success(messageService.listAdminMessages(dto));
    }

    @Operation(summary = "通过留言（审核通过）")
    @ApiResponse(responseCode = "200", description = "操作成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "未授权")
    @GetMapping("/audit/pass")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R<Void> auditPass(
            @Parameter(description = "留言ID", required = true)
            @RequestParam("id") Long id) {
        messageService.auditMessage(id, 1);
        return R.success();
    }

    @Operation(summary = "拒绝留言（审核拒绝）")
    @ApiResponse(responseCode = "200", description = "操作成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "未授权")
    @GetMapping("/audit/reject")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R<Void> auditReject(
            @Parameter(description = "留言ID", required = true)
            @RequestParam("id") Long id) {
        messageService.auditMessage(id, 3);
        return R.success();
    }

    @Operation(summary = "置顶/取消置顶留言")
    @ApiResponse(responseCode = "200", description = "操作成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "未授权")
    @GetMapping("/top")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R<Void> toggleTop(
            @Parameter(description = "留言ID", required = true)
            @RequestParam("id") Long id) {
        messageService.toggleTop(id);
        return R.success();
    }

    @Operation(summary = "管理员删除留言")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "未授权")
    @GetMapping("/delete")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R<Void> delete(
            @Parameter(description = "留言ID", required = true)
            @RequestParam("id") Long id) {
        messageService.removeById(id);
        return R.success();
    }

    @Operation(summary = "查询留言回复列表（管理后台）")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "未授权")
    @GetMapping("/{id}/replies")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R<List<MessageReplyVO>> getReplies(
            @Parameter(description = "留言ID", required = true)
            @PathVariable("id") Long id) {
        return R.success(messageService.getReplies(id));
    }

    @Operation(summary = "管理员回复留言")
    @ApiResponse(responseCode = "200", description = "回复成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "未授权")
    @PostMapping("/reply")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R<Void> reply(
            @Parameter(description = "回复信息", required = true)
            @Validated @RequestBody AdminReplyDTO dto,
            HttpServletRequest request) {
        String ip = IPUtil.getRealIp(request);
        String ua = request.getHeader("User-Agent");
        String content = StrUtil.removeHtml(dto.getContent());
        messageReplyService.adminReply(dto.getMessageId(), dto.getParentId(), dto.getNickname(), content, ip, ua);
        return R.success();
    }

    @Operation(summary = "管理员删除回复")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "未授权")
    @GetMapping("/reply/delete")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R<Void> deleteReply(
            @Parameter(description = "回复ID", required = true)
            @RequestParam("id") Long id) {
        messageReplyService.removeById(id);
        return R.success();
    }

    /**
     * 管理员回复 DTO
     */
    @lombok.Data
    public static class AdminReplyDTO {
        @Parameter(description = "留言ID", required = true)
        @javax.validation.constraints.NotNull
        private Long messageId;

        @Parameter(description = "父回复ID，0表示顶级回复")
        private Long parentId;

        @Parameter(description = "昵称", required = true)
        @javax.validation.constraints.NotBlank
        private String nickname;

        @Parameter(description = "回复内容", required = true)
        @javax.validation.constraints.NotBlank
        private String content;
    }
}
