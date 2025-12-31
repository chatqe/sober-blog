package io.charqe.qs.modules.treehole.controller.web;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.aop.SaveCheck;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.treehole.domain.dao.TreeHoleMapper;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.treehole.domain.pojo.TreeHole;
import io.charqe.qs.modules.treehole.service.TreeHoleService;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.QingsiUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "树洞管理",description = "树洞")
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
    public R<TreeHole> saveTreeHole(@RequestBody TreeHole treeHole) {
        if (!StringUtils.hasText(treeHole.getMessage())) {
            return R.fail("留言不能为空！");
        }
        if (!StringUtils.hasText(treeHole.getAvatar())) {
            treeHole.setAvatar(qingsiUtil.getRandomAvatar());
        }
        treeHoleMapper.insert(treeHole);
        return R.success(treeHole);
    }


    /**
     * 删除
     */
    @GetMapping("/deleteTreeHole")
    @LoginCheck(0)
    public R deleteTreeHole(@RequestParam("id") Integer id) {
        treeHoleMapper.deleteById(id);
        return R.success();
    }


    /**
     * 查询List
     */
    @GetMapping("/listTreeHole")
    public R<List<TreeHole>> listTreeHole() {
        List<TreeHole> treeHoles;
        Integer count = Math.toIntExact(new LambdaQueryChainWrapper<>(treeHoleMapper).count());
        if (count > CommonConst.TREE_HOLE_COUNT) {
            int i = new Random().nextInt(count + 1 - CommonConst.TREE_HOLE_COUNT);
            treeHoles = treeHoleMapper.queryAllByLimit(i, CommonConst.TREE_HOLE_COUNT);
        } else {
            treeHoles = new LambdaQueryChainWrapper<>(treeHoleMapper).list();
        }

        treeHoles.forEach(treeHole -> {
            if (!StringUtils.hasText(treeHole.getAvatar())) {
                treeHole.setAvatar(BlogUtil.getRandomAvatar(treeHole.getId().toString()));
            }
        });
        return R.success(treeHoles);
    }

    @PostMapping("/saveList")
    public R saveList(@RequestBody List<TreeHole> treeHoles) {


        return R.success(treeHoleService.saveList(treeHoles));
    }


    /**
     * 获取最新的20条
     * @param
     */
    @GetMapping("/latestTreeHole")
    public R<List<TreeHole>> latestTreeHole() {

        return R.success(treeHoleService.latestTreeHole());
    }


    public static void main(String[] args) {


        // TreeHoleService.
    }

}


