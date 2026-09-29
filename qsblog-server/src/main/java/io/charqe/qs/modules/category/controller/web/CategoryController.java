package io.charqe.qs.modules.category.controller.web;

import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.enums.CodeMsg;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.behavior.controller.web.dto.BehDTO;
import io.charqe.qs.modules.behavior.service.subscribe.SubscribeService;
import io.charqe.qs.modules.category.controller.web.vo.CategoryWebVO;
import io.charqe.qs.modules.category.controller.web.vo.HomeStatsVO;
import io.charqe.qs.modules.category.service.CategoryService;
import io.charqe.qs.modules.file.enums.BizTypeEnum;
import io.charqe.qs.modules.sysdict.controller.web.vo.DictVO;
import io.charqe.qs.modules.user.controller.web.dto.LoginUserInfo;
import io.charqe.qs.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * 分类控制器（Web端公开）
 *
 * @author Joy
 * @since 2026-06-22
 */
@RestController
@RequestMapping("/category")
@Tag(name = "category", description = "分类管理")
public class CategoryController {

    @Resource
    private CategoryService categoryService;
    @Resource
    private SubscribeService subscribeService;

    @Operation(summary = "获取导航栏分类列表", description = "返回启用且展示的分类，支持按类型过滤")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/listNavBar")
    public R<List<CategoryWebVO>> listNavBar() {
        return R.success(categoryService.listNavBar());
    }

    @Operation(summary = "获取首页分类统计", description = "聚合返回分类列表（含每类文章数）和文章总数，供侧边栏展示")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/homeStats")
    public R<HomeStatsVO> homeStats() {
        return R.success(categoryService.getHomeStats());
    }

    @Operation(summary = "获取分类下拉字典", description = "返回所有可用分类的 id/name 列表，用于表单下拉选择，结果带Redis缓存，数据变更时主动刷新")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/dict")
    public R<List<DictVO>> dict() {
        return R.success(categoryService.getDictList());
    }


    /**
     * 订阅/取消订阅专栏（标签
     * flag = true：订阅
     * flag = false：取消订阅
     *
     * @param categoryId 分类ID
     * @param flag       订阅状态，true-订阅，false-取消订阅
     * @return 操作结果
     */
    @Operation(summary = "订阅/取消订阅专栏", description = "用于订阅或取消订阅专栏（标签）。订阅成功后，用户将收到该专栏的最新文章通知。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误，如分类ID为空"),
            @ApiResponse(responseCode = "401", description = "未授权，需要登录后操作"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/subscribe")
    @LoginCheck
    public R subscribe(
            @Parameter(name = "categoryId", description = "专栏ID", required = true, example = "1")
            @RequestParam("categoryId") Long categoryId,
            @Parameter(name = "flag", description = "订阅状态，true-订阅，false-取消订阅", required = true, example = "true")
            @RequestParam("flag") Boolean flag) {
        LoginUserInfo userInfo = UserContext.get();
        if (Objects.isNull(userInfo) || Objects.isNull(userInfo.getId())) {
            return R.fail(CodeMsg.NOT_LOGIN);
        }

        // 验证分类是否存在
        if (categoryService.getById(categoryId) == null) {
            return R.fail("分类不存在");
        }

        BehDTO dto = new BehDTO();
        dto.setBizId(categoryId);
        dto.setBizType(BizTypeEnum.CATEGORY.getCode());
        if (Boolean.TRUE.equals(flag)) {
            subscribeService.subscribe(dto, userInfo.getId());
        } else {
            subscribeService.unsubscribe(dto, userInfo.getId());
        }
        return R.success();
    }
}
