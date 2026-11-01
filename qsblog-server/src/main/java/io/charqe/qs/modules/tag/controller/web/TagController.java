package io.charqe.qs.modules.tag.controller.web;

import io.charqe.qs.common.model.OptionVO;
import io.charqe.qs.common.model.R;
import io.charqe.qs.common.enums.OptionScopeEnum;
import io.charqe.qs.modules.sysdict.controller.web.vo.DictVO;
import io.charqe.qs.modules.tag.controller.web.vo.TagCloudVO;
import io.charqe.qs.modules.tag.controller.web.vo.TagDictVO;
import io.charqe.qs.modules.tag.controller.web.vo.TagWebVO;
import io.charqe.qs.modules.tag.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
@RequestMapping("/tag")
@Tag(name = "tag", description = "标签公开接口")
public class TagController {

    @Resource
    private TagService tagService;

    @Operation(summary = "获取标签列表", description = "返回所有未删除标签，支持按分类过滤")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/list")
    public R<List<TagWebVO>> list(
            @Parameter(name = "categoryId", description = "分类ID过滤") @RequestParam(required = false) Long categoryId) {
        return R.success(tagService.listPublic(categoryId));
    }

    @Operation(summary = "获取标签下拉选项", description = "返回表单下拉与级联所需的精简选项（id/label/value/categoryId/sort），id 与 categoryId 为字符串；"
            + "标签表无状态列，返回全部未删除标签；后台表单请改用 /admin/tag/options（出于权限口径要求，两者数据一致）")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/options")
    public R<List<OptionVO>> options() {
        return R.success(tagService.listOptions(OptionScopeEnum.PUBLIC));
    }

    /**
     * @deprecated value 返回的是标签描述而非主键，请改用 /tag/options
     */
    @Deprecated
    @Operation(summary = "获取标签下拉字典", deprecated = true, description = "已废弃：value 实际返回 tagDesc（标签描述）而非 id，"
            + "cateId 又受陈旧缓存影响可能为空，请改用 /tag/options；本接口仅为外部兼容保留")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @GetMapping("/dict")
    public R<List<TagDictVO>> dict() {
        return R.success(tagService.getDictList());
    }

    @Operation(summary = "获取标签云数据", description = "返回所有未删除标签的 id 与名称，按 sort、id 升序，供首页标签云展示与点击跳转")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/cloud")
    public R<List<TagCloudVO>> cloud() {
        return R.success(tagService.getCloud());
    }
}
