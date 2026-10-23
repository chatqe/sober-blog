package io.charqe.qs.modules.danmaku.controller.web;

import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.aop.SaveCheck;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.danmaku.controller.web.vo.DanmakuVO;
import io.charqe.qs.modules.danmaku.domain.dao.DanmakuMapper;
import io.charqe.qs.modules.danmaku.domain.pojo.Danmaku;
import io.charqe.qs.modules.danmaku.service.DanmakuService;
import io.charqe.qs.utils.QingsiUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Random;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Slf4j
@RestController
@RequestMapping("/danmaku")
@Tag(name = "danmaku", description = "弹幕管理")
public class DanmakuController {

    @Resource
    private DanmakuMapper danmakuMapper;

    @Resource
    private DanmakuService danmakuService;

    @Resource
    private QingsiUtil qingsiUtil;

    /**
     * 保存弹幕留言
     *
     * @param danmaku 弹幕留言信息
     * @return 保存结果
     */
    @Operation(summary = "保存弹幕留言", description = "用于保存用户的弹幕留言，支持匿名发布。如果未提供头像，系统会自动分配随机头像。")
    @ApiResponse(responseCode = "200", description = "保存成功，返回弹幕留言信息")
    @ApiResponse(responseCode = "400", description = "请求参数错误，如留言内容为空")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @PostMapping("/save")
    @SaveCheck
    public R<Danmaku> save(
        @Parameter(name = "danmaku", description = "弹幕留言信息", required = true, example = "{\"message\":\"这是一条测试弹幕\",\"nickname\":\"匿名用户\"}")
        @RequestBody Danmaku danmaku) {
        if (!StringUtils.hasText(danmaku.getMessage())) {
            return R.fail("弹幕不能为空！");
        }
        if (!StringUtils.hasText(danmaku.getAvatar())) {
            danmaku.setAvatar(qingsiUtil.getRandomAvatar());
        }
        danmakuService.save(danmaku);
        return R.success(danmaku);
    }


    /**
     * 删除弹幕留言
     *
     * @param id 弹幕留言ID
     * @return 删除结果
     */
    @Operation(summary = "删除弹幕留言", description = "用于管理员删除弹幕留言，需要管理员权限。")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误，如留言ID为空")
    @ApiResponse(responseCode = "401", description = "未授权，需要管理员权限")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @GetMapping("/del")
    @LoginCheck(0)
    public R del(
        @Parameter(name = "id", description = "弹幕留言ID", required = true, example = "1")
        @RequestParam("id") Integer id) {
        danmakuMapper.deleteById(id);
        return R.success();
    }


    /**
     * 查询弹幕留言列表
     *
     * @return 弹幕留言列表
     */
    @Operation(summary = "查询弹幕留言列表", description = "用于获取弹幕留言列表。当留言数量超过配置上限时，会随机返回指定数量的留言；否则返回所有留言。")
    @ApiResponse(responseCode = "200", description = "查询成功，返回弹幕留言列表")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @GetMapping("/list")
    public R<List<DanmakuVO>> list() {

        return R.success(danmakuService.randomList());
    }

    /**
     * 批量保存弹幕留言
     *
     * @param danmakus 弹幕留言列表
     * @return 保存结果
     */
    @Operation(summary = "批量保存弹幕留言", description = "用于批量保存弹幕留言，支持一次性保存多条留言。")
    @ApiResponse(responseCode = "200", description = "保存成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @PostMapping("/saveList")
    public R saveList(
        @Parameter(name = "danmakus", description = "弹幕留言列表", required = true, example = "[{\"message\":\"弹幕1\"},{\"message\":\"弹幕2\"}]")
        @RequestBody List<Danmaku> danmakus) {
        return R.success(danmakuService.saveList(danmakus));
    }


    /**
     * 获取最新的弹幕留言
     *
     * @return 最新的弹幕留言列表
     */
    @Operation(summary = "获取最新的弹幕留言", description = "用于获取最新的弹幕留言，默认返回最近的20条留言。")
    @ApiResponse(responseCode = "200", description = "查询成功，返回最新的弹幕留言列表")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @GetMapping("/latestDanmaku")
    public R<List<DanmakuVO>> latestDanmaku() {
        return R.success(danmakuService.latestDanmaku());
    }


}


