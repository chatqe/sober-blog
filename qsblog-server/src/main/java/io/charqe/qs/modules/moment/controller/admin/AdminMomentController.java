package io.charqe.qs.modules.moment.controller.admin;

import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.moment.controller.admin.dto.MomentPageQueryDTO;
import io.charqe.qs.modules.moment.controller.admin.dto.MomentSaveReqDTO;
import io.charqe.qs.modules.moment.controller.admin.vo.MomentDetailVO;
import io.charqe.qs.modules.moment.controller.admin.vo.MomentListVO;
import io.charqe.qs.modules.moment.service.MomentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 动态管理控制器（后台管理）
 *
 * @author Joy
 * @since 2026-06-25
 */
@RestController
@RequestMapping("/admin/moment")
@Tag(name = "adminMoment", description = "管理后台-动态管理")
public class AdminMomentController {

    @Resource
    private MomentService momentService;

    @Operation(summary = "分页查询动态列表", description = "支持按用户、状态、可见性、来源筛选")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/page")
    @LoginCheck
    public R<PageResult<MomentListVO>> pageList(MomentPageQueryDTO query) {
        return R.success(momentService.pageList(query));
    }

    @Operation(summary = "获取动态详情", description = "返回动态完整信息及关联媒体文件")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/{id}")
    @LoginCheck
    public R<MomentDetailVO> getDetail(@Parameter(name = "id", description = "动态ID", required = true) @PathVariable("id") Long id) {
        return R.success(momentService.getDetail(id));
    }

    @Operation(summary = "新增动态", description = "管理员可创建动态")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "新增成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PostMapping
    @LoginCheck
    public R save(@RequestBody MomentSaveReqDTO dto) {
        momentService.save(dto);
        return R.success();
    }

    @Operation(summary = "更新动态", description = "更新动态内容及状态")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PutMapping("/{id}")
    @LoginCheck
    public R update(@Parameter(name = "id", description = "动态ID", required = true) @PathVariable("id") Long id,
                    @RequestBody MomentSaveReqDTO dto) {
        dto.setId(id);
        momentService.update(dto);
        return R.success();
    }

    @Operation(summary = "删除动态", description = "物理删除动态及关联媒体")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @DeleteMapping("/{id}")
    @LoginCheck
    public R delete(@Parameter(name = "id", description = "动态ID", required = true) @PathVariable("id") Long id) {
        momentService.delete(id);
        return R.success();
    }

    @Operation(summary = "批量删除动态", description = "批量物理删除动态及关联媒体")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @DeleteMapping("/batch")
    @LoginCheck
    public R batchDelete(@RequestBody List<Long> ids) {
        momentService.batchDelete(ids);
        return R.success();
    }

    @Operation(summary = "审核动态", description = "管理员审核动态，通过或驳回")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "审核成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PatchMapping("/{id}/audit")
    @LoginCheck
    public R audit(@Parameter(name = "id", description = "动态ID", required = true) @PathVariable("id") Long id,
                   @Parameter(name = "status", description = "审核状态: 1-通过 2-驳回", required = true) @RequestParam Integer status,
                   @Parameter(name = "auditReason", description = "审核原因（驳回时必填") @RequestParam(required = false) String auditReason) {
        momentService.audit(id, status, auditReason);
        return R.success();
    }
}
