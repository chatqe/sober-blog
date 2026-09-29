package io.charqe.qs.modules.danmaku.controller.admin;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.danmaku.domain.dao.DanmakuMapper;
import io.charqe.qs.modules.danmaku.domain.pojo.Danmaku;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 管理后台-弹幕管理
 *
 * @author sjq
 * @since 2025-12-31 20:13
 */
@RestController
@RequestMapping("/admin/danmaku")
@Tag(name = "adminDanmaku", description = "管理后台-弹幕管理")
public class AdminDanmakuController {


    @Resource
    private DanmakuMapper danmakuMapper;

    /**
     * 管理员查询弹幕列表
     *
     * @param baseReqVO 查询参数
     * @return 弹幕分页列表
     */
    @Operation(summary = "管理员查询弹幕列表", description = "用于管理员在管理后台查询弹幕列表，支持分页和排序")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功，返回弹幕分页列表"),
        @ApiResponse(responseCode = "401", description = "未授权，需要管理员权限"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/boss/list")
    @LoginCheck(0)
    public R<Page> listBossDanmaku(
        @Parameter(name = "baseReqVO", description = "查询参数", required = true, example = "{\"page\":1,\"pageSize\":10}")
        @RequestBody BaseReqVO baseReqVO) {
        LambdaQueryChainWrapper<Danmaku> wrapper = new LambdaQueryChainWrapper<>(danmakuMapper);
        wrapper.orderByDesc(Danmaku::getCreateTime).page(baseReqVO);
        return R.success(baseReqVO);
    }
}
