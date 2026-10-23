package io.charqe.qs.modules.family.controller.admin;

import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.family.controller.admin.dto.FamilyPageQueryDTO;
import io.charqe.qs.modules.family.controller.admin.vo.FamilyDetailVO;
import io.charqe.qs.modules.family.controller.admin.vo.FamilyListVO;
import io.charqe.qs.modules.family.domain.pojo.Family;
import io.charqe.qs.modules.family.service.FamilyService;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.cache.QingSiCache;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 家庭卡片管理控制器（后台管理）
 *
 * 注意：本控制器同时服务于 C 端表白墙，普通登录用户需要维护自己的卡片，
 * 因此统一显式保持 @LoginCheck(2)（仅要求登录），不能收紧为管理员级别。
 *
 * @author Joy
 * @since 2026-06-22
 */
@RestController
@RequestMapping("/admin/families")
@Tag(name = "adminFamily", description = "管理后台-表白墙管理")
public class AdminFamilyController {

    @Resource
    private FamilyService familyService;

    @Resource
    private QingSiCache qingSiCache;

    @Operation(summary = "分页查询家庭列表", description = "支持按状态筛选的分页查询")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "未授权")
    @GetMapping("/page")
    @LoginCheck(2)
    public R<PageResult<FamilyListVO>> pageList(FamilyPageQueryDTO query) {
        return R.success(familyService.pageList(query));
    }

    @Operation(summary = "获取家庭详情", description = "返回家庭卡片的完整信息")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "未授权")
    @GetMapping("/{id}")
    @LoginCheck(2)
    public R<FamilyDetailVO> getDetail(@Parameter(name = "id", description = "家庭卡片ID", required = true) @PathVariable("id") Long id) {
        return R.success(familyService.getDetail(id));
    }

    @Operation(summary = "新增/更新家庭卡片", description = "每个用户只能有一条家庭卡片记录")
    @ApiResponse(responseCode = "200", description = "操作成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "未授权")
    @PostMapping
    @LoginCheck(2)
    public R save(@RequestBody Family family) {
        Long userId = BlogUtil.getUserId();
        family.setUserId(userId);
        // 管理员自动启用，普通用户待审核
        if (userId.equals(BlogUtil.getAdminUser().getId())) {
            family.setStatus(true);
        } else {
            family.setStatus(false);
        }
        familyService.saveOrUpdate(family);
        return R.success();
    }

    @Operation(summary = "软删除家庭卡片", description = "修改 is_del = 1")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "未授权")
    @DeleteMapping("/{id}")
    @LoginCheck(2)
    public R delete(@Parameter(name = "id", description = "家庭卡片ID", required = true) @PathVariable("id") Long id) {
        familyService.softDelete(id);
        return R.success();
    }

    @Operation(summary = "切换启用状态", description = "切换家庭卡片的启用/禁用状态")
    @ApiResponse(responseCode = "200", description = "操作成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "未授权")
    @PatchMapping("/{id}/status")
    @LoginCheck(2)
    public R toggleStatus(@Parameter(name = "id", description = "家庭卡片ID", required = true) @PathVariable("id") Long id) {
        familyService.toggleStatus(id);
        return R.success();
    }
}
