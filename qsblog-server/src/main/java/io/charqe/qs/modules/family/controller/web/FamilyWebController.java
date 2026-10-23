package io.charqe.qs.modules.family.controller.web;

import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.family.controller.web.dto.FamilyLikeReqDTO;
import io.charqe.qs.modules.family.controller.web.vo.FamilyWebVO;
import io.charqe.qs.modules.family.service.FamilyService;
import io.charqe.qs.utils.BlogUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 家庭卡片控制器（Web端公开）
 *
 * @author Joy
 * @since 2026-06-22
 */
@RestController
@RequestMapping("/api/families")
@Tag(name = "family", description = "表白墙公开接口")
public class FamilyWebController {

    @Resource
    private FamilyService familyService;

    @Operation(summary = "获取公开家庭列表", description = "只返回启用状态的家庭卡片，支持随机排序")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/list")
    public R<List<FamilyWebVO>> list(@Parameter(name = "size", description = "返回条数") @RequestParam(defaultValue = "10") Integer size) {
        List<FamilyWebVO> list = familyService.listPublic();
        if (list.size() > size) {
            java.util.Collections.shuffle(list);
            list = list.subList(0, size);
        }
        return R.success(list);
    }

    @Operation(summary = "获取单个家庭详情", description = "返回指定ID的家庭卡片详情")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "404", description = "家庭卡片不存在或未启用")
    @GetMapping("/{id}")
    public R<FamilyWebVO> getDetail(@Parameter(name = "id", description = "家庭卡片ID", required = true) @PathVariable("id") Long id) {
        return R.success(familyService.getPublicDetail(id));
    }

    @Operation(summary = "点赞", description = "用户对家庭卡片点赞，需登录，防重复")
    @ApiResponse(responseCode = "200", description = "点赞成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "未授权")
    @ApiResponse(responseCode = "429", description = "已点赞过，请一小时后再试")
    @PostMapping("/{id}/like")
    @LoginCheck
    public R like(@Parameter(name = "id", description = "家庭卡片ID", required = true) @PathVariable("id") Long id) {
        Long userId = BlogUtil.getUserId();
        familyService.like(id, userId);
        return R.success();
    }
}
