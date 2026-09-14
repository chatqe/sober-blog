package io.charqe.qs.modules.tag.controller.web;

import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.tag.controller.web.vo.TagWebVO;
import io.charqe.qs.modules.tag.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 标签控制器（Web端公开）
 *
 * @author Joy
 * @since 2026-06-22
 */
@RestController
@RequestMapping("/api/tags")
@Tag(name = "tag", description = "标签公开接口")
public class TagWebController {

    @Resource
    private TagService tagService;

    @Operation(summary = "获取标签列表", description = "返回所有未删除标签，支持按分类过滤")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/list")
    public R<List<TagWebVO>> list(
            @Parameter(name = "categoryId", description = "分类ID过滤") @RequestParam(required = false) Long categoryId) {
        return R.success(tagService.listPublic(categoryId));
    }
}
