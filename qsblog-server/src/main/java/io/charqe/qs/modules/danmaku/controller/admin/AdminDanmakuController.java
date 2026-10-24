package io.charqe.qs.modules.danmaku.controller.admin;

import io.charqe.qs.modules.user.auth.SessionScope;

import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.danmaku.controller.admin.dto.AdminDanmakuPageReqDTO;
import io.charqe.qs.modules.danmaku.controller.admin.vo.AdminDanmakuVO;
import io.charqe.qs.modules.danmaku.service.DanmakuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 管理后台-弹幕管理
 *
 * @author sjq
 * @since 2025-12-31 20:13
 */
@RestController
@RequestMapping("/admin/danmaku")
@Tag(name = "adminDanmaku", description = "管理后台-弹幕管理")
public class AdminDanmakuController {

    @Resource
    private DanmakuService danmakuService;

    /**
     * 管理员查询弹幕列表
     *
     * @param dto 查询参数
     * @return 弹幕分页列表
     */
    @Operation(summary = "管理员查询弹幕列表", description = "用于管理员在管理后台查询弹幕列表，支持分页、关键字和时间范围过滤")
    @ApiResponse(responseCode = "200", description = "查询成功，返回弹幕分页列表")
    @ApiResponse(responseCode = "401", description = "未授权，需要管理员权限")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @PostMapping("/list")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R<PageResult<AdminDanmakuVO>> list(
        @Parameter(name = "dto", description = "查询参数", required = true, example = "{\"pageNum\":1,\"pageSize\":10,\"keyword\":\"测试\"}")
        @RequestBody AdminDanmakuPageReqDTO dto) {
        return R.success(danmakuService.pageList(dto));
    }
}
