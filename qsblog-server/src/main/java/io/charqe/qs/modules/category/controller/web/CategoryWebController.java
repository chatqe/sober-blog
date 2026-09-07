package io.charqe.qs.modules.category.controller.web;

import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.category.controller.web.vo.CategoryWebVO;
import io.charqe.qs.modules.category.controller.web.vo.HomeStatsVO;
import io.charqe.qs.modules.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 分类控制器（Web端公开）
 *
 * @author Joy
 * @since 2026-06-22
 */
@RestController
@RequestMapping("/api/categories")
@Tag(name = "category", description = "分类公开接口")
public class CategoryWebController {

    @Resource
    private CategoryService categoryService;

    @Operation(summary = "获取公开分类列表", description = "返回启用且展示的分类，支持按类型过滤")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/list")
    public R<List<CategoryWebVO>> list(
            @Parameter(name = "type", description = "分类类型过滤: 0-导航栏分类, 1-普通分类") @RequestParam(required = false) Integer type) {
        return R.success(categoryService.listPublic(type));
    }

    @Operation(summary = "获取首页分类统计", description = "聚合返回分类列表（含每类文章数）和文章总数，供侧边栏展示")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/homeStats")
    public R<HomeStatsVO> homeStats() {
        return R.success(categoryService.getHomeStats());
    }
}
