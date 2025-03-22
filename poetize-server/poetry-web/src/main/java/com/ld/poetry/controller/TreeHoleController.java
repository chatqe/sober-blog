package com.ld.poetry.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.ld.poetry.aop.LoginCheck;
import com.ld.poetry.aop.SaveCheck;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.constants.CommonConst;
import com.ld.poetry.dao.TreeHoleMapper;
import com.ld.poetry.entity.TreeHole;
import com.ld.poetry.service.TreeHoleService;
import com.ld.poetry.utils.PoetryUtil;
import com.ld.poetry.utils.QingsiUtil;
import com.ld.poetry.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Random;

/**
 * <p>
 * 弹幕 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@Slf4j
@RestController
@RequestMapping("/webInfo")
public class TreeHoleController {

    @Autowired
    private TreeHoleMapper treeHoleMapper;

    @Resource
    private TreeHoleService treeHoleService;


    @Resource
    private QingsiUtil qingsiUtil;

    /**
     * 保存
     */
    @PostMapping("/saveTreeHole")
    @SaveCheck
    public PoetryResult<TreeHole> saveTreeHole(@RequestBody TreeHole treeHole) {
        if (!StringUtils.hasText(treeHole.getMessage())) {
            return PoetryResult.fail("留言不能为空！");
        }
        if (!StringUtils.hasText(treeHole.getAvatar())) {
            treeHole.setAvatar(qingsiUtil.getRandomAvatar());
        }
        treeHoleMapper.insert(treeHole);
        return PoetryResult.success(treeHole);
    }


    /**
     * 删除
     */
    @GetMapping("/deleteTreeHole")
    @LoginCheck(0)
    public PoetryResult deleteTreeHole(@RequestParam("id") Integer id) {
        treeHoleMapper.deleteById(id);
        return PoetryResult.success();
    }


    /**
     * 查询List
     */
    @GetMapping("/listTreeHole")
    public PoetryResult<List<TreeHole>> listTreeHole() {
        List<TreeHole> treeHoles;
        Integer count = new LambdaQueryChainWrapper<>(treeHoleMapper).count();
        if (count > CommonConst.TREE_HOLE_COUNT) {
            int i = new Random().nextInt(count + 1 - CommonConst.TREE_HOLE_COUNT);
            treeHoles = treeHoleMapper.queryAllByLimit(i, CommonConst.TREE_HOLE_COUNT);
        } else {
            treeHoles = new LambdaQueryChainWrapper<>(treeHoleMapper).list();
        }

        treeHoles.forEach(treeHole -> {
            if (!StringUtils.hasText(treeHole.getAvatar())) {
                treeHole.setAvatar(PoetryUtil.getRandomAvatar(treeHole.getId().toString()));
            }
        });
        return PoetryResult.success(treeHoles);
    }

    @PostMapping("/saveList")
    public PoetryResult saveList(@RequestBody List<TreeHole> treeHoles) {


        return PoetryResult.success(treeHoleService.saveList(treeHoles));
    }


    /**
     * 获取最新的20条
     * @param
     */
    @GetMapping("/latestTreeHole")
    public PoetryResult<List<TreeHole>> latestTreeHole() {

        return PoetryResult.success(treeHoleService.latestTreeHole());
    }


    public static void main(String[] args) {


        // TreeHoleService.
    }

}
