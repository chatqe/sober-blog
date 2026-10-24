package io.charqe.qs.modules.portal.controller.web;

import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.portal.controller.web.vo.PortalOverviewVO;
import io.charqe.qs.modules.portal.controller.web.vo.PortalOwnerVO;
import io.charqe.qs.modules.portal.service.PortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 门户控制器（Web端公开）
 * 承载首页等展示场景的跨域聚合读接口
 *
 * @author Joy
 * @since 2026-10-23
 */
@RestController
@RequestMapping("/portal")
@Tag(name = "portal", description = "门户聚合接口")
public class PortalController {

    @Resource
    private PortalService portalService;

    @Operation(summary = "获取站点概览", description = "聚合站点信息与公开文章总数、分类总数、累计浏览量、累计点赞数，供首页侧边栏展示")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @GetMapping("/overview")
    public R<PortalOverviewVO> overview() {
        return R.success(portalService.getOverview());
    }

    @Operation(summary = "获取站长信息", description = "返回站长（userType=0）的用户名与头像，供前台匿名展示作者信息")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @GetMapping("/owner")
    public R<PortalOwnerVO> owner() {
        return R.success(portalService.getOwner());
    }
}
