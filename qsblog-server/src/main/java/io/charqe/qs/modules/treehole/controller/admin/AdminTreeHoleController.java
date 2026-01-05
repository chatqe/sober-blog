package io.charqe.qs.modules.treehole.controller.admin;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.treehole.domain.dao.TreeHoleMapper;
import io.charqe.qs.modules.treehole.domain.pojo.TreeHole;
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
 * 管理后台-树洞管理
 *
 * @author sjq
 * @since 2025-12-31 20:13
 */
@RestController
@RequestMapping("/admin/treeHole")
@Tag(name = "adminTreeHole", description = "管理后台-树洞管理")
public class AdminTreeHoleController {


    @Resource
    private TreeHoleMapper treeHoleMapper;

    /**
     * 管理员查询树洞列表
     *
     * @param baseReqVO 查询参数
     * @return 树洞分页列表
     */
    @Operation(summary = "管理员查询树洞列表", description = "用于管理员在管理后台查询树洞列表，支持分页和排序")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功，返回树洞分页列表"),
        @ApiResponse(responseCode = "401", description = "未授权，需要管理员权限"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/boss/list")
    @LoginCheck(0)
    public R<Page> listBossTreeHole(
        @Parameter(name = "baseReqVO", description = "查询参数", required = true, example = "{\"page\":1,\"pageSize\":10}")
        @RequestBody BaseReqVO baseReqVO) {
        LambdaQueryChainWrapper<TreeHole> wrapper = new LambdaQueryChainWrapper<>(treeHoleMapper);
        wrapper.orderByDesc(TreeHole::getCreateTime).page(baseReqVO);
        return R.success(baseReqVO);
    }
}
