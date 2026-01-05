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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
 * 树洞 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@Slf4j
@RestController
@RequestMapping("/treeHole")
@Tag(name = "treeHole", description = "树洞管理")
public class TreeHoleController {

    @Autowired
    private TreeHoleMapper treeHoleMapper;

    @Resource
    private TreeHoleService treeHoleService;


    @Resource
    private QingsiUtil qingsiUtil;

    /**
     * 保存树洞留言
     *
     * @param treeHole 树洞留言信息
     * @return 保存结果
     */
    @Operation(summary = "保存树洞留言", description = "用于保存用户的树洞留言，支持匿名发布。如果未提供头像，系统会自动分配随机头像。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "保存成功，返回树洞留言信息"),
        @ApiResponse(responseCode = "400", description = "请求参数错误，如留言内容为空"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/save")
    @SaveCheck
    public R<TreeHole> saveTreeHole(
        @Parameter(name = "treeHole", description = "树洞留言信息", required = true, example = "{\"message\":\"这是一条测试留言\",\"nickname\":\"匿名用户\"}")
        @RequestBody TreeHole treeHole) {
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
     * 删除树洞留言
     *
     * @param id 树洞留言ID
     * @return 删除结果
     */
    @Operation(summary = "删除树洞留言", description = "用于管理员删除树洞留言，需要管理员权限。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误，如留言ID为空"),
        @ApiResponse(responseCode = "401", description = "未授权，需要管理员权限"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/del")
    @LoginCheck(0)
    public R deleteTreeHole(
        @Parameter(name = "id", description = "树洞留言ID", required = true, example = "1")
        @RequestParam("id") Integer id) {
        treeHoleMapper.deleteById(id);
        return R.success();
    }


    /**
     * 查询树洞留言列表
     *
     * @return 树洞留言列表
     */
    @Operation(summary = "查询树洞留言列表", description = "用于获取树洞留言列表。当留言数量超过配置上限时，会随机返回指定数量的留言；否则返回所有留言。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功，返回树洞留言列表"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/list")
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

    /**
     * 批量保存树洞留言
     *
     * @param treeHoles 树洞留言列表
     * @return 保存结果
     */
    @Operation(summary = "批量保存树洞留言", description = "用于批量保存树洞留言，支持一次性保存多条留言。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "保存成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/saveList")
    public R saveList(
        @Parameter(name = "treeHoles", description = "树洞留言列表", required = true, example = "[{\"message\":\"留言1\"},{\"message\":\"留言2\"}]")
        @RequestBody List<TreeHole> treeHoles) {
        return R.success(treeHoleService.saveList(treeHoles));
    }


    /**
     * 获取最新的树洞留言
     *
     * @return 最新的树洞留言列表
     */
    @Operation(summary = "获取最新的树洞留言", description = "用于获取最新的树洞留言，默认返回最近的20条留言。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功，返回最新的树洞留言列表"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/latestTreeHole")
    public R<List<TreeHole>> latestTreeHole() {
        return R.success(treeHoleService.latestTreeHole());
    }


    public static void main(String[] args) {


        // TreeHoleService.
    }

}


