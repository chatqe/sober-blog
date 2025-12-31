package io.charqe.qs.modules.treehole.controller.admin;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.treehole.domain.dao.TreeHoleMapper;
import io.charqe.qs.modules.treehole.domain.pojo.TreeHole;
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
@Tag(name = "管理后台-树洞管理", description = "管理后台-树洞管理")
public class AdminTreeHoleController {


    @Resource
    private TreeHoleMapper treeHoleMapper;

    /**
     * Boss查询树洞
     */
    @PostMapping("/boss/list")
    @LoginCheck(0)
    public R<Page> listBossTreeHole(@RequestBody BaseReqVO baseReqVO) {
        LambdaQueryChainWrapper<TreeHole> wrapper = new LambdaQueryChainWrapper<>(treeHoleMapper);
        wrapper.orderByDesc(TreeHole::getCreateTime).page(baseReqVO);
        return R.success(baseReqVO);
    }
}
