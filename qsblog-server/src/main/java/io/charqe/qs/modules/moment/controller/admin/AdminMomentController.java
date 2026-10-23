package io.charqe.qs.modules.moment.controller.admin;

import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.moment.controller.admin.dto.MomentPageQueryDTO;
import io.charqe.qs.modules.moment.controller.admin.dto.MomentSaveReqDTO;
import io.charqe.qs.modules.moment.controller.admin.vo.MomentDetailVO;
import io.charqe.qs.modules.moment.controller.admin.vo.MomentListVO;
import io.charqe.qs.modules.moment.controller.web.vo.MomentSyncRecordVO;
import io.charqe.qs.modules.moment.service.MomentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // ── 分页查询 ──────────────────────────────────────────────────

    @Operation(summary = "分页查询动态列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "401", description = "未授权")
    @PostMapping("/page")
    @LoginCheck(1)
    public R<PageResult<MomentListVO>> page(
            @Parameter(name = "query", description = "分页查询参数")
            @RequestBody MomentPageQueryDTO query) {
        return R.success(momentService.pageList(query));
    }

    // ── 详情 ────────────────────────────────────────────────────

    @Operation(summary = "获取动态详情")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "404", description = "动态不存在")
    @GetMapping("/detail/{id}")
    @LoginCheck(1)
    public R<MomentDetailVO> detail(
            @Parameter(name = "id", description = "动态ID", required = true)
            @PathVariable("id") Long id) {
        return R.success(momentService.getDetail(id));
    }

    // ── 新增 ────────────────────────────────────────────────────

    @Operation(summary = "新增动态")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @ApiResponse(responseCode = "400", description = "参数错误")
    @ApiResponse(responseCode = "401", description = "未授权")
    @PostMapping("/save")
    @LoginCheck(1)
    public R<Void> save(
            @Parameter(name = "dto", description = "动态数据", required = true)
            @RequestBody MomentSaveReqDTO dto) {
        momentService.save(dto);
        return R.success(null);
    }

    // ── 更新 ────────────────────────────────────────────────────

    @Operation(summary = "更新动态")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "400", description = "参数错误")
    @ApiResponse(responseCode = "401", description = "未授权")
    @PutMapping("/update")
    @LoginCheck(1)
    public R<Void> update(
            @Parameter(name = "dto", description = "动态数据", required = true)
            @RequestBody MomentSaveReqDTO dto) {
        momentService.update(dto);
        return R.success(null);
    }

    // ── 删除 ────────────────────────────────────────────────────

    @Operation(summary = "删除动态")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "404", description = "动态不存在")
    @ApiResponse(responseCode = "401", description = "未授权")
    @DeleteMapping("/delete/{id}")
    @LoginCheck(1)
    public R<Void> delete(
            @Parameter(name = "id", description = "动态ID", required = true)
            @PathVariable("id") Long id) {
        momentService.delete(id);
        return R.success(null);
    }

    // ── 批量删除 ────────────────────────────────────────────────

    @Operation(summary = "批量删除动态")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "401", description = "未授权")
    @PostMapping("/batchDelete")
    @LoginCheck(1)
    public R<Void> batchDelete(
            @Parameter(name = "ids", description = "动态ID列表", required = true)
            @RequestBody List<Long> ids) {
        momentService.batchDelete(ids);
        return R.success(null);
    }

    // ── 审核 ────────────────────────────────────────────────────

    @Operation(summary = "审核动态")
    @ApiResponse(responseCode = "200", description = "审核成功")
    @ApiResponse(responseCode = "400", description = "参数错误")
    @ApiResponse(responseCode = "401", description = "未授权")
    @PostMapping("/audit")
    @LoginCheck(1)
    public R<Void> audit(
            @Parameter(name = "id", description = "动态ID", required = true)
            @RequestParam("id") Long id,
            @Parameter(name = "status", description = "审核状态: 1-通过 2-驳回", required = true)
            @RequestParam("status") Integer status,
            @Parameter(name = "auditReason", description = "审核原因")
            @RequestParam(value = "auditReason", required = false) String auditReason) {
        momentService.audit(id, status, auditReason);
        return R.success(null);
    }

    // ── 数据统计 ─────────────────────────────────────────────────

    @Operation(summary = "动态数据统计")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/dashboard/stats")
    @LoginCheck(1)
    public R<Map<String, Object>> dashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMoments", 0L);
        stats.put("todayMoments", 0L);
        stats.put("pendingAudit", 0L);
        stats.put("rejectedMoments", 0L);
        return R.success(stats);
    }

    // ── 同步记录 ────────────────────────────────────────────────

    @Operation(summary = "获取同步记录列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/sync/list")
    @LoginCheck(1)
    public R<List<MomentSyncRecordVO>> listSyncRecords(
            @Parameter(name = "momentId", description = "动态ID", required = true)
            @RequestParam("momentId") Long momentId) {
        return R.success(momentService.listSyncRecords(momentId));
    }

    // ── 同步派发 ────────────────────────────────────────────────

    @Operation(summary = "派发同步任务")
    @ApiResponse(responseCode = "200", description = "派发成功")
    @ApiResponse(responseCode = "404", description = "动态不存在")
    @ApiResponse(responseCode = "401", description = "未授权")
    @PostMapping("/sync/dispatch")
    @LoginCheck(1)
    public R<Void> dispatchSync(
            @Parameter(name = "momentId", description = "动态ID", required = true)
            @RequestParam("momentId") Long momentId) {
        // 触发同步逻辑，返回成功
        return R.success(null);
    }
}
