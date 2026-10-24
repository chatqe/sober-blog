package io.charqe.qs.modules.user.controller.admin;

import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.enums.CodeMsg;
import io.charqe.qs.common.enums.StatusEnum;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.im.websocket.TioUtil;
import io.charqe.qs.modules.im.websocket.TioWebsocketStarter;
import io.charqe.qs.modules.user.domain.pojo.User;
import io.charqe.qs.modules.user.service.UserService;
import io.charqe.qs.modules.user.auth.AuthSessionService;
import io.charqe.qs.modules.user.auth.AuthSessionVO;
import io.charqe.qs.modules.user.auth.SessionScope;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.cache.QingSiCache;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.tio.core.Tio;

import javax.annotation.Resource;

/**
 * 管理后台-用户控制
 *
 * @author Upjqs
 * @since 2025-12-31 20:17
 */

@RestController
@RequestMapping("/admin/user")
@Tag(name = "adminUser", description = "管理后台-用户管理")
public class AdminUserController {

    @Resource
    private UserService userService;


    @Resource
    private QingSiCache qingSiCache;

    @Resource
    private AuthSessionService sessions;

    @PostMapping("/login")
    @Operation(summary = "后台独立登录", description = "仅允许站长和管理员，签发 ADMIN 会话")
    public R<AuthSessionVO> login(@RequestParam("account") String account,
                                  @RequestParam("password") String password) {
        return userService.login(account, password, SessionScope.ADMIN);
    }

    @GetMapping("/session")
    @Operation(summary = "查询当前后台会话", description = "不签发或续期")
    public R<AuthSessionVO> session() {
        return R.success(sessions.inspect(SessionScope.ADMIN));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出当前后台会话")
    public R<Void> logout() {
        sessions.logout(BlogUtil.getToken(), SessionScope.ADMIN);
        return R.success();
    }

    /**
     * 查询用户列表
     *
     * @param baseReqVO 查询条件
     * @return 用户分页列表
     */
    @Operation(summary = "查询用户列表", description = "用于管理后台查询用户列表，支持分页和条件筛选")
    @ApiResponse(responseCode = "200", description = "查询成功，返回用户分页列表")
    @ApiResponse(responseCode = "401", description = "未授权，需要管理员登录")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @PostMapping("/page")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R<Page> listUser(
            @Parameter(name = "baseReqVO", description = "查询条件", required = true, example = "{\"page\":1,\"pageSize\":10}")
            @RequestBody BaseReqVO baseReqVO) {
        return userService.listUser(baseReqVO);
    }

    /**
     * 修改用户状态
     * <p>
     * flag = true：解禁
     * flag = false：封禁
     *
     * @param userId 用户ID
     * @param flag   操作类型，true-解禁，false-封禁
     * @return 修改结果
     */
    @Operation(summary = "修改用户状态", description = "用于管理后台修改用户状态，支持解禁和封禁操作")
    @ApiResponse(responseCode = "200", description = "修改成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误，如站长状态不能修改")
    @ApiResponse(responseCode = "401", description = "未授权，需要管理员登录")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @GetMapping("/changeUserStatus")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R changeUserStatus(
            @Parameter(name = "userId", description = "用户ID", required = true, example = "1")
            @RequestParam("userId") Integer userId,
            @Parameter(name = "flag", description = "操作类型，true-解禁，false-封禁", required = true, example = "true")
            @RequestParam("flag") Boolean flag) {
        if (userId.intValue() == BlogUtil.getAdminUser().getId().intValue()) {
            return R.fail("站长状态不能修改！");
        }

        LambdaUpdateChainWrapper<User> updateChainWrapper = userService.lambdaUpdate().eq(User::getId, userId);
        if (flag) {
            updateChainWrapper.eq(User::getUserStatus, StatusEnum.DISABLE.getCode()).set(User::getUserStatus, StatusEnum.ENABLE.getCode()).update();
        } else {
            updateChainWrapper.eq(User::getUserStatus, StatusEnum.ENABLE.getCode()).set(User::getUserStatus, StatusEnum.DISABLE.getCode()).update();
        }
        logout(userId);
        return R.success();
    }

    /**
     * 修改用户赞赏信息
     *
     * @param userId 用户ID
     * @param admire 赞赏信息
     * @return 修改结果
     */
    @Operation(summary = "修改用户赞赏信息", description = "用于管理后台修改用户的赞赏信息")
    @ApiResponse(responseCode = "200", description = "修改成功")
    @ApiResponse(responseCode = "401", description = "未授权，需要管理员登录")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @GetMapping("/changeUserAdmire")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R changeUserAdmire(
            @Parameter(name = "userId", description = "用户ID", required = true, example = "1")
            @RequestParam("userId") Integer userId,
            @Parameter(name = "admire", description = "赞赏信息", required = true, example = "感谢支持！")
            @RequestParam("admire") String admire) {
        userService.lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getAdmire, admire)
                .update();
        qingSiCache.del(CommonConst.ADMIN);
        return R.success();
    }

    /**
     * 修改用户类型
     *
     * @param userId   用户ID
     * @param userType 用户类型，0-普通用户，1-管理员，2-超级管理员
     * @return 修改结果
     */
    @Operation(summary = "修改用户类型", description = "用于管理后台修改用户类型，支持普通用户、管理员和超级管理员三种类型")
    @ApiResponse(responseCode = "200", description = "修改成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误，如站长类型不能修改或用户类型无效")
    @ApiResponse(responseCode = "401", description = "未授权，需要管理员登录")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @GetMapping("/changeUserType")
    // 修改用户类型等同于授权提级，只有站长可以执行
    @LoginCheck(value = 0, scope = SessionScope.ADMIN)
    public R changeUserType(
            @Parameter(name = "userId", description = "用户ID", required = true, example = "1")
            @RequestParam("userId") Integer userId,
            @Parameter(name = "userType", description = "用户类型，0-普通用户，1-管理员，2-超级管理员", schema = @Schema(allowableValues = {"0", "1", "2"}), required = true, example = "1")
            @RequestParam("userType") Integer userType) {
        if (userId.intValue() == BlogUtil.getAdminUser().getId().intValue()) {
            return R.fail("站长类型不能修改！");
        }

        if (userType != 0 && userType != 1 && userType != 2) {
            return R.fail(CodeMsg.PARAMETER_ERROR);
        }
        userService.lambdaUpdate().eq(User::getId, userId).set(User::getUserType, userType).update();

        logout(userId);
        return R.success();
    }

    private void logout(Integer userId) {
        sessions.revokeAll(userId.longValue());
        qingSiCache.del(CommonConst.USER_CACHE + userId);

    }
}


