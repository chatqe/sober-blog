package io.charqe.qs.modules.tag.controller.admin;

import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.tag.controller.admin.dto.TagSaveReqDTO;
import io.charqe.qs.modules.tag.controller.admin.vo.TagDetailVO;
import io.charqe.qs.modules.tag.controller.admin.vo.TagListVO;
import io.charqe.qs.modules.tag.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 标签管理控制器（后台管理）
 *
 * @author Joy
 * @since 2026-06-22
 */
@RestController
@RequestMapping("/admin/tags")
@Tag(name = "adminTag", description = "管理后台-标签管理")
public class AdminTagController {

    @Resource
    private TagService tagService;

    @Operation(summary = "分页查询标签列表", description = "支持按分类ID、关键词筛选")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/page")
    @LoginCheck
    public R<PageResult<TagListVO>> pageList(
            @Parameter(name = "categoryId", description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(name = "keyword", description = "关键词（模糊匹配名称）") @RequestParam(required = false) String keyword,
            @Parameter(name = "pageNum", description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(name = "pageSize", description = "每页条数") @RequestParam(defaultValue = "10") int pageSize) {
        return R.success(tagService.pageList(categoryId, keyword, pageNum, pageSize));
    }

    @Operation(summary = "获取标签详情", description = "返回标签的完整信息，含分类名称")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/{id}")
    @LoginCheck
    public R<TagDetailVO> getDetail(@Parameter(name = "id", description = "标签ID", required = true) @PathVariable("id") Long id) {
        return R.success(tagService.getDetail(id));
    }

    @Operation(summary = "新增标签", description = "创建新标签，需保证同一分类下名称唯一")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "新增成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PostMapping
    @LoginCheck
    public R save(@RequestBody TagSaveReqDTO dto) {
        tagService.save(dto);
        return R.success();
    }

    @Operation(summary = "更新标签", description = "更新标签信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PutMapping("/{id}")
    @LoginCheck
    public R update(@PathVariable("id") Long id, @RequestBody TagSaveReqDTO dto) {
        dto.setId(id);
        tagService.update(dto);
        return R.success();
    }

    @Operation(summary = "删除标签", description = "软删除标签")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @DeleteMapping("/{id}")
    @LoginCheck
    public R delete(@Parameter(name = "id", description = "标签ID", required = true) @PathVariable("id") Long id) {
        tagService.delete(id);
        return R.success();
    }
}
