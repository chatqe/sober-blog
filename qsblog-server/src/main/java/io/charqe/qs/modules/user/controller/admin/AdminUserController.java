package io.charqe.qs.modules.user.controller.admin;

import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.model.BaseReqVO;
import io.charqe.qs.common.model.PageResult;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.user.controller.admin.vo.AdminUserUpdateReq;
import io.charqe.qs.modules.user.service.UserService;
import io.charqe.qs.modules.user.auth.AuthSessionService;
import io.charqe.qs.modules.user.auth.AuthSessionVO;
import io.charqe.qs.modules.user.auth.SessionScope;
import io.charqe.qs.utils.BlogUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

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
    public R<PageResult> listUser(
            @Parameter(name = "baseReqVO", description = "查询条件", required = true, example = "{\"page\":1,\"pageSize\":10}")
            @RequestBody BaseReqVO baseReqVO) {
        return userService.listUser(baseReqVO);
    }

    @PostMapping("/update")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    @Operation(summary = "编辑用户", description = "Boss 可编辑所有用户；Admin 仅可编辑非 Boss 用户且不能授予 Boss 角色。返回是否撤销目标用户会话")
    public R<Boolean> updateAdminUser(@Valid @RequestBody AdminUserUpdateReq request) {
        return userService.updateAdminUser(request);
    }

    @DeleteMapping("/{userId}")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    @Operation(summary = "删除用户", description = "逻辑删除并撤销会话；Boss 可操作所有用户，Admin 不能删除 Boss")
    public R<Void> deleteAdminUser(@PathVariable("userId") Long userId) {
        return userService.deleteAdminUser(userId);
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
    @ApiResponse(responseCode = "400", description = "请求参数错误")
    @ApiResponse(responseCode = "401", description = "未授权，需要管理员登录")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @GetMapping("/changeUserStatus")
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R changeUserStatus(
            @Parameter(name = "userId", description = "用户ID", required = true, example = "1")
            @RequestParam("userId") Long userId,
            @Parameter(name = "flag", description = "操作类型，true-解禁，false-封禁", required = true, example = "true")
            @RequestParam("flag") Boolean flag) {
        AdminUserUpdateReq request = new AdminUserUpdateReq();
        request.setId(userId);
        request.setUserStatus(Boolean.TRUE.equals(flag) ? 1 : 0);
        return userService.updateAdminUser(request);
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
            @RequestParam("userId") Long userId,
            @Parameter(name = "admire", description = "赞赏信息", required = true, example = "感谢支持！")
            @RequestParam("admire") String admire) {
        AdminUserUpdateReq request = new AdminUserUpdateReq();
        request.setId(userId);
        request.setAdmire(admire);
        return userService.updateAdminUser(request);
    }

    /**
     * 修改用户类型
     *
     * @param userId   用户ID
     * @param userType 用户类型，0-Boss（站长），1-Admin（管理员），2-Normal（用户）
     * @return 修改结果
     */
    @Operation(summary = "修改用户类型", description = "用于管理后台修改用户类型，支持 Boss（站长）、Admin（管理员）和 Normal（用户）三种类型")
    @ApiResponse(responseCode = "200", description = "修改成功")
    @ApiResponse(responseCode = "400", description = "请求参数错误，如用户类型无效")
    @ApiResponse(responseCode = "401", description = "未授权，需要管理员登录")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @GetMapping("/changeUserType")
    // Admin 仅可在非 Boss 用户之间调整角色，目标和新角色由服务层统一校验。
    @LoginCheck(value = 1, scope = SessionScope.ADMIN)
    public R changeUserType(
            @Parameter(name = "userId", description = "用户ID", required = true, example = "1")
            @RequestParam("userId") Long userId,
            @Parameter(name = "userType", description = "用户类型，0-Boss（站长），1-Admin（管理员），2-Normal（用户）", schema = @Schema(allowableValues = {"0", "1", "2"}), required = true, example = "1")
            @RequestParam("userType") Integer userType) {
        AdminUserUpdateReq request = new AdminUserUpdateReq();
        request.setId(userId);
        request.setUserType(userType);
        return userService.updateAdminUser(request);
    }
}


