package io.charqe.qs.modules.sysdict.controller.admin;

import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.sysdict.controller.admin.dto.SysDictDataReq;
import io.charqe.qs.modules.sysdict.controller.admin.dto.SysDictTypeReq;
import io.charqe.qs.modules.sysdict.domain.pojo.SysDictData;
import io.charqe.qs.modules.sysdict.domain.pojo.SysDictType;
import io.charqe.qs.modules.sysdict.service.SysDictDataService;
import io.charqe.qs.modules.sysdict.service.SysDictTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 字典管理控制器（后台管理）
 *
 * @author Joy
 * @since 2026-06-22
 */
@RestController
@RequestMapping("/admin/sysDict")
@Tag(name = "字典管理", description = "字典类型和字典数据的后台管理接口")
public class AdminSysDictController {

    @Resource
    private SysDictTypeService sysDictTypeService;

    @Resource
    private SysDictDataService sysDictDataService;

    // ==================== 字典类型管理 ====================

    @Operation(summary = "分页查询字典类型", description = "支持按类型名称、状态筛选的分页查询")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/type/list")
    @LoginCheck(0)
    public R<PageResult<SysDictType>> listType(
            @Parameter(name = "typeName", description = "类型名称（模糊查询）") @RequestParam(required = false) String typeName,
            @Parameter(name = "status", description = "状态：0-禁用, 1-启用") @RequestParam(required = false) Integer status,
            @Parameter(name = "pageNum", description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(name = "pageSize", description = "每页条数") @RequestParam(defaultValue = "10") int pageSize) {
        return R.success(sysDictTypeService.page(typeName, status, pageNum, pageSize));
    }

    @Operation(summary = "新增字典类型", description = "创建新的字典类型，需保证typeCode全局唯一")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "新增成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误，如编码重复或为空"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PostMapping("/type/save")
    @LoginCheck(0)
    public R saveType(@RequestBody SysDictTypeReq req) {
        sysDictTypeService.save(req);
        return R.success();
    }

    @Operation(summary = "更新字典类型", description = "更新字典类型信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PostMapping("/type/update")
    @LoginCheck(0)
    public R updateType(@RequestBody SysDictTypeReq req) {
        sysDictTypeService.update(req);
        return R.success();
    }

    @Operation(summary = "删除字典类型", description = "批量删除字典类型及其关联的字典数据")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @DeleteMapping("/type/batchDelete")
    @LoginCheck(0)
    public R batchDeleteType(@RequestBody Long[] ids) {
        sysDictTypeService.batchDelete(ids);
        return R.success();
    }

    @Operation(summary = "获取所有启用的字典类型", description = "返回所有状态为启用的字典类型列表，按sort排序")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/type/listEnabled")
    @LoginCheck(0)
    public R<List<SysDictType>> listEnabledType() {
        return R.success(sysDictTypeService.listEnabled());
    }

    // ==================== 字典数据管理 ====================

    @Operation(summary = "根据类型编码查询字典数据", description = "查询指定字典类型下的所有启用数据，按sort排序")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    @GetMapping("/data/listByTypeCode")
    @LoginCheck(0)
    public R<List<SysDictData>> listDataByTypeCode(
            @Parameter(name = "typeCode", description = "字典类型编码", required = true, example = "gender")
            @RequestParam("typeCode") String typeCode) {
        return R.success(sysDictDataService.lambdaQuery()
                .eq(SysDictData::getTypeCode, typeCode)
                .eq(SysDictData::getStatus, 1)
                .orderByAsc(SysDictData::getSort)
                .list());
    }

    @Operation(summary = "新增字典数据", description = "创建新的字典数据项")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "新增成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误，如类型编码不存在或编码重复"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PostMapping("/data/save")
    @LoginCheck(0)
    public R saveData(@RequestBody SysDictDataReq req) {
        sysDictDataService.save(req);
        return R.success();
    }

    @Operation(summary = "更新字典数据", description = "更新字典数据信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PostMapping("/data/update")
    @LoginCheck(0)
    public R updateData(@RequestBody SysDictDataReq req) {
        sysDictDataService.update(req);
        return R.success();
    }

    @Operation(summary = "批量删除字典数据", description = "批量删除字典数据项")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @DeleteMapping("/data/batchDelete")
    @LoginCheck(0)
    public R batchDeleteData(@RequestBody Long[] ids) {
        sysDictDataService.batchDelete(ids);
        return R.success();
    }
}
