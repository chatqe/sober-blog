package io.charqe.qs.modules.message.controller.web;

import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.aop.SaveCheck;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.message.controller.web.dto.MessageQueryDTO;
import io.charqe.qs.modules.message.controller.web.dto.MessageSaveDTO;
import io.charqe.qs.modules.message.controller.web.vo.MessageReplyVO;
import io.charqe.qs.modules.message.controller.web.vo.MessageVO;
import io.charqe.qs.modules.message.service.MessageService;
import io.charqe.qs.utils.IPUtil;
import io.charqe.qs.utils.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/message")
@Tag(name = "message", description = "留言模块：留言列表、提交、回复、点赞点踩")
public class MessageController {

    @Resource
    private MessageService messageService;

    @Operation(summary = "保存留言")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "保存成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PostMapping("/save")
    @LoginCheck
    @SaveCheck
    public R<Void> save(
            @Parameter(description = "留言内容", required = true)
            @Validated @RequestBody MessageSaveDTO dto,
            HttpServletRequest request) {
        String ip = IPUtil.getRealIp(request);
        String ua = request.getHeader("User-Agent");
        String content = StrUtil.removeHtml(dto.getContent());
        if (!StringUtils.hasText(content)) {
            return R.fail("留言内容不合法！");
        }
        dto.setContent(content);
        messageService.saveMessage(dto, ip, ua);
        return R.success();
    }

    @Operation(summary = "查询留言列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    @PostMapping("/list")
    public R<PageResult<MessageVO>> list(
            @Parameter(description = "分页查询参数")
            @RequestBody MessageQueryDTO dto) {
        return R.success(messageService.listMessages(dto));
    }

    @Operation(summary = "查询留言回复列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    @GetMapping("/replies")
    public R<List<MessageReplyVO>> getReplies(
            @Parameter(description = "留言ID", required = true)
            @RequestParam("messageId") Long messageId) {
        List<MessageReplyVO> replies = messageService.getReplies(messageId);
        return R.success(replies);
    }
}
