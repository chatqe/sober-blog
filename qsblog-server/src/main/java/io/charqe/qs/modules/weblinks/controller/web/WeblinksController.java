package io.charqe.qs.modules.weblinks.controller.web;

import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.aop.SaveCheck;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.weblinks.controller.web.vo.WeblinksVO;
import io.charqe.qs.modules.weblinks.service.WeblinksService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 资源链接控制器
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@RestController
@RequestMapping("/weblinks")
@Tag(name = "weblinks", description = "资源链接管理")
public class WeblinksController {

    @Resource
    private WeblinksService weblinksService;

    /**
     * 保存友链申请
     */
    @Operation(summary = "保存友链", description = "用于用户提交友链申请，需要登录后操作")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "保存成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误，如信息不全"),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录后操作"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @LoginCheck
    @PostMapping("/save")
    @SaveCheck
    public R saveFriend(
            @Parameter(name = "resourcePathVO", description = "友链信息对象", required = true,
                    example = "{\"title\":\"测试网站\",\"cover\":\"https://example.com/cover.jpg\",\"url\":\"https://example.com\",\"introduction\":\"测试网站介绍\"}")
            @RequestBody WeblinksVO vo) {
        weblinksService.saveFriend(vo);
        return R.success();
    }

    /**
     * 查询已审核通过的友链列表，按分类分组
     */
    @Operation(summary = "查询友链", description = "用于查询所有已审核通过的友链，按分类分组返回")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功，返回友链列表"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/list")
    public R<Map<String, List<WeblinksVO>>> listFriend() {
        return R.success(weblinksService.listFriend());
    }

    /**
     * 保存通用资源
     */
    @Operation(summary = "保存通用资源", description = "用于管理员保存通用资源链接")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "保存成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @LoginCheck(0)
    @PostMapping("/saveWeblinks")
    public R saveWeblinks(@RequestBody WeblinksVO vo) {
        weblinksService.saveWeblinks(vo);
        return R.success();
    }

    /**
     * 删除资源
     */
    @Operation(summary = "删除资源", description = "用于管理员删除资源链接")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @DeleteMapping("/deleteWeblinks")
    @LoginCheck(0)
    public R deleteWeblinks(@RequestParam("id") Long id) {
        weblinksService.deleteWeblinks(id);
        return R.success();
    }

    /**
     * 更新资源
     */
    @Operation(summary = "更新资源", description = "用于管理员更新资源链接信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PostMapping("/update")
    @LoginCheck(0)
    public R updateWeblinks(@RequestBody WeblinksVO vo) {
        weblinksService.updateWeblinks(vo);
        return R.success();
    }

    /**
     * 分页查询资源列表
     */
    @Operation(summary = "分页查询资源", description = "用于查询资源链接列表，支持分页和条件过滤")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/listWeblinks")
    public R<PageResult<WeblinksVO>> listWeblinks(
            @Parameter(name = "resourceType", description = "资源类型") @RequestParam(required = false) String resourceType,
            @Parameter(name = "classify", description = "分类") @RequestParam(required = false) String classify,
            @Parameter(name = "order", description = "排序字段") @RequestParam(defaultValue = "createTime") String order,
            @Parameter(name = "getDesc", description = "是否降序") @RequestParam(defaultValue = "true") boolean desc,
            @Parameter(name = "pageNum", description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(name = "pageSize", description = "每页条数") @RequestParam(defaultValue = "10") int pageSize) {
        return R.success(weblinksService.listWeblinks(resourceType, classify, order, desc, pageNum, pageSize));
    }
}
