package com.joy.qs.controller;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joy.qs.aop.LoginCheck;
import com.joy.qs.common.R;
import com.joy.qs.dao.TreeHoleMapper;
import com.joy.qs.dao.WebInfoMapper;
import com.joy.qs.model.BaseReqVO;
import com.joy.qs.model.treehole.pojo.TreeHole;
import com.joy.qs.model.webinfo.pojo.WebInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 后台 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private WebInfoMapper webInfoMapper;

    @Autowired
    private TreeHoleMapper treeHoleMapper;

    /**
     * 获取网站信息
     */
    @GetMapping("/webInfo/getAdminWebInfo")
    @LoginCheck(0)
    public R<WebInfo> getWebInfo() {
        LambdaQueryChainWrapper<WebInfo> wrapper = new LambdaQueryChainWrapper<>(webInfoMapper);
        List<WebInfo> list = wrapper.list();
        if (!CollectionUtils.isEmpty(list)) {
            return R.success(list.get(0));
        } else {
            return R.success();
        }
    }

    /**
     * Boss查询树洞
     */
    @PostMapping("/treeHole/boss/list")
    @LoginCheck(0)
    public R<Page> listBossTreeHole(@RequestBody BaseReqVO baseReqVO) {
        LambdaQueryChainWrapper<TreeHole> wrapper = new LambdaQueryChainWrapper<>(treeHoleMapper);
        wrapper.orderByDesc(TreeHole::getCreateTime).page(baseReqVO);
        return R.success(baseReqVO);
    }
}
