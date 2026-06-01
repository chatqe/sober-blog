package io.charqe.qs.modules.category.controller.web;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.enums.CodeMsg;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.behavior.controller.web.dto.BehDTO;
import io.charqe.qs.modules.behavior.enums.BehActionType;
import io.charqe.qs.modules.behavior.service.BehaviorStateService;
import io.charqe.qs.modules.file.enums.BizTypeEnum;
import io.charqe.qs.modules.tag.domain.dao.TagMapper;
import io.charqe.qs.modules.category.domain.dao.CategoryMapper;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.category.domain.pojo.Category;
import io.charqe.qs.modules.user.controller.web.dto.LoginUserInfo;
import io.charqe.qs.modules.user.controller.web.vo.UserVO;
import io.charqe.qs.modules.user.service.UserService;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.CommonQuery;
import io.charqe.qs.utils.UserContext;
import io.charqe.qs.utils.cache.QingSiCache;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Slf4j
@RestController
@RequestMapping("/category")
public class CategoryTagController {

    @Resource
    private UserService userService;

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private TagMapper tagMapper;

    @Resource
    private CommonQuery commonQuery;

    @Resource
    private QingSiCache qingSiCache;

    @Resource
    private BehaviorStateService behaviorStateService;

    /**
     * 获取分类标签信息
     */
    @GetMapping("/getCategoryInfo")
    public R<List<Category>> getCategoryInfo() {
        return R.success(commonQuery.getCategoryInfo());
    }


    /**
     * 订阅/取消订阅专栏（标签）
     * <p>
     * flag = true：订阅
     * flag = false：取消订阅
     *
     * @param labelId 标签ID
     * @param flag 订阅状态，true-订阅，false-取消订阅
     * @return 更新结果，包含更新后的用户信息
     */
    @Operation(summary = "订阅/取消订阅专栏", description = "用于订阅或取消订阅专栏（标签）。订阅成功后，用户将收到该专栏的最新文章通知。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功，返回更新后的用户信息"),
            @ApiResponse(responseCode = "400", description = "请求参数错误，如标签ID为空"),
            @ApiResponse(responseCode = "401", description = "未授权，需要登录后操作"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/subscribe")
    @LoginCheck
    public R<UserVO> subscribe(
            @Parameter(name = "categoryId", description = "专栏ID", required = true, example = "1")
            @RequestParam("categoryId") Integer labelId,
            @Parameter(name = "flag", description = "订阅状态，true-订阅，false-取消订阅", required = true, example = "true")
            @RequestParam("flag") Boolean flag) {
        qingSiCache.del(CommonConst.USER_CACHE + BlogUtil.getUserId());
        LoginUserInfo userInfo = UserContext.get();
        if (Objects.isNull(userInfo)||Objects.isNull(userInfo.getId())) {
            return R.fail(CodeMsg.NOT_LOGIN);
        }
        // return userService.subscribe(labelId, flag);
        BehDTO behDTO = new BehDTO();
        behDTO.setUserId(userInfo.getId());
        behDTO.setBizId(labelId.longValue());
        behDTO.setBizType(BizTypeEnum.CATEGORY.code());
        behDTO.setAction(Boolean.TRUE.equals(flag) ? BehActionType.SUBSCRIBE.code() : BehActionType.UN_SUBSCRIBE.code());
        behaviorStateService.handleBeh(behDTO,flag);
        return null;
    }


    /**
     * 保存
     */
    @PostMapping("/save")
    @LoginCheck(0)
    public R saveCategory(@RequestBody Category category) {
        if (!StringUtils.hasText(category.getCategoryName()) || !StringUtils.hasText(category.getDescription())) {
            return R.fail("分类名称和分类描述不能为空！");
        }

        if (category.getSort() == null) {
            return R.fail("分类必须配置优先级！");
        }

        categoryMapper.insert(category);
        // PoetryCache.remove(CommonConst.SORT_INFO);
        qingSiCache.del(CommonConst.SORT_INFO);
        return R.success();
    }


    /**
     * 删除
     */
    @GetMapping("/del")
    @LoginCheck(0)
    public R deleteCategory(@RequestParam("id") Integer id) {
        categoryMapper.deleteById(id);
        // PoetryCache.remove(CommonConst.SORT_INFO);
        qingSiCache.del(CommonConst.SORT_INFO);
        return R.success();
    }


    /**
     * 更新
     */
    @PostMapping("/update")
    @LoginCheck(0)
    public R updateCategory(@RequestBody Category category) {
        categoryMapper.updateById(category);
        // PoetryCache.remove(CommonConst.SORT_INFO);
        qingSiCache.del(CommonConst.SORT_INFO);
        return R.success();
    }



    /**
     * 查询List
     */
    @GetMapping("/list")
    public R<List<Category>> listCategory() {
        return R.success(new LambdaQueryChainWrapper<>(categoryMapper).list());
    }





    /**
     * 查询List
     */
    @GetMapping("/listCategoryAndTag")
    public R<Map> listCategoryAndTag() {
        Map<String, List> map = new HashMap<>();
        map.put("categories", new LambdaQueryChainWrapper<>(categoryMapper).list());
        map.put("tags", new LambdaQueryChainWrapper<>(tagMapper).list());
        return R.success(map);
    }
}
