package io.charqe.qs.modules.category.controller.admin;

import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.category.controller.admin.dto.CategoryBatchSortDTO;
import io.charqe.qs.modules.category.controller.admin.dto.CategorySaveReqDTO;
import io.charqe.qs.modules.category.controller.admin.vo.CategoryDetailVO;
import io.charqe.qs.modules.category.controller.admin.vo.CategoryListVO;
import io.charqe.qs.modules.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 分类管理控制器（后台管理）
 *
 * @author Joy
 * @since 2026-06-22
 */
@RestController
@RequestMapping("/admin/category")
@Tag(name = "adminCategory", description = "管理后台-分类管理")
public class AdminCategoryController {

    @Resource
    private CategoryService categoryService;

    @Operation(summary = "分页查询分类列表", description = "支持按关键词、类型、状态筛选")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/page")
    @LoginCheck
    public R<PageResult<CategoryListVO>> pageList(
            @Parameter(name = "keyword", description = "关键词（模糊匹配名称）") @RequestParam(required = false) String keyword,
            @Parameter(name = "type", description = "分类类型: 0-导航栏分类, 1-普通分类") @RequestParam(required = false) Integer type,
            @Parameter(name = "status", description = "状态: 0-禁用, 1-启用") @RequestParam(required = false) Integer status,
            @Parameter(name = "pageNum", description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(name = "pageSize", description = "每页条数") @RequestParam(defaultValue = "10") int pageSize) {
        return R.success(categoryService.pageList(keyword, type, status, pageNum, pageSize));
    }

    @Operation(summary = "获取分类详情", description = "返回分类的完整信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/{id}")
    @LoginCheck
    public R<CategoryDetailVO> getDetail(@Parameter(name = "id", description = "分类ID", required = true) @PathVariable("id") Long id) {
        return R.success(categoryService.getDetail(id));
    }

    @Operation(summary = "新增分类", description = "创建新的分类，需保证名称唯一")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "新增成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PostMapping
    @LoginCheck
    public R save(@RequestBody CategorySaveReqDTO dto) {
        categoryService.save(dto);
        return R.success();
    }

    @Operation(summary = "更新分类", description = "更新分类信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PutMapping("/{id}")
    @LoginCheck
    public R update(@PathVariable("id") Long id, @RequestBody CategorySaveReqDTO dto) {
        dto.setId(id);
        categoryService.update(dto);
        return R.success();
    }

    @Operation(summary = "删除分类", description = "软删除分类")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @DeleteMapping("/{id}")
    @LoginCheck
    public R delete(@Parameter(name = "id", description = "分类ID", required = true) @PathVariable("id") Long id) {
        categoryService.delete(id);
        return R.success();
    }

    @Operation(summary = "批量排序", description = "批量更新分类排序")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "排序成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PutMapping("/batchSort")
    @LoginCheck
    public R batchSort(@RequestBody CategoryBatchSortDTO dto) {
        categoryService.batchSort(dto);
        return R.success();
    }
}
