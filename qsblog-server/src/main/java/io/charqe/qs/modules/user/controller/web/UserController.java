package io.charqe.qs.modules.user.controller.web;


import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.aop.SaveCheck;
import io.charqe.qs.common.model.R;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.user.controller.web.vo.UserVO;
import io.charqe.qs.modules.user.service.UserService;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.cache.QingSiCache;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@RestController
@RequestMapping("/user")
@Tag(name = "user", description = "负责用户注册、登录、信息更新、验证码获取等功能")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private QingSiCache qingSiCache;


    /**
     * 用户名/密码注册
     * 
     * 用于新用户注册，支持用户名、密码等基本信息的注册
     * 
     * @param user 用户注册信息
     * @return 注册结果
     */
    @Operation(summary = "用户注册", description = "用于新用户注册，支持用户名、密码等基本信息的注册")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "注册成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误，如用户名已存在、密码格式不正确等"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/register")
    public R<UserVO> regist(
        @Parameter(name = "user", description = "用户注册信息", required = true, example = "{\"username\":\"testuser\",\"password\":\"123456\"}")
        @Validated @RequestBody UserVO user) {
        return userService.regist(user);
    }


    /**
     * 用户名、邮箱、手机号/密码登录
     * 
     * 用于用户登录，支持用户名、邮箱、手机号三种登录方式
     * 
     * @param account 登录账号（用户名、邮箱或手机号）
     * @param password 登录密码
     * @param isAdmin 是否为管理员登录，默认为false
     * @return 登录结果，包含用户信息和Token
     */
    @Operation(summary = "用户登录", description = "用于用户登录，支持用户名、邮箱、手机号三种登录方式。登录成功后返回用户信息和Token。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登录成功，返回用户信息和Token"),
        @ApiResponse(responseCode = "400", description = "请求参数错误，如账号或密码为空"),
        @ApiResponse(responseCode = "401", description = "账号或密码错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/login")
    public R<UserVO> login(
        @Parameter(name = "account", description = "登录账号（用户名、邮箱或手机号）", required = true, example = "testuser") 
        @RequestParam("account") String account,
        @Parameter(name = "password", description = "登录密码", required = true, example = "123456") 
        @RequestParam("password") String password,
        @Parameter(name = "isAdmin", description = "是否为管理员登录，默认为false", required = false, example = "false") 
        @RequestParam(value = "isAdmin", defaultValue = "false") Boolean isAdmin) {
        return userService.login(account, password, isAdmin);
    }


    /**
     * Token登录
     * 
     * 用于通过Token直接登录，无需输入账号密码
     * 
     * @param userToken 用户Token
     * @return 登录结果，包含用户信息
     */
    @Operation(summary = "Token登录", description = "用于通过Token直接登录，无需输入账号密码。适用于已登录用户的会话保持。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登录成功，返回用户信息"),
        @ApiResponse(responseCode = "400", description = "请求参数错误，如Token为空"),
        @ApiResponse(responseCode = "401", description = "Token无效或已过期"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/token")
    public R<UserVO> login(
        @Parameter(name = "userToken", description = "用户Token", required = true, example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") 
        @RequestParam("userToken") String userToken) {
        return userService.token(userToken);
    }


    /**
     * 退出登录
     * 
     * 用于用户退出登录，清除当前会话
     * 
     * @return 退出结果
     */
    @Operation(summary = "退出登录", description = "用于用户退出登录，清除当前会话。退出成功后，当前Token将失效。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "退出成功"),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录后操作"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/logout")
    @LoginCheck
    public R exit() {
        return userService.exit();
    }


    /**
     * 更新用户信息
     * 
     * 用于更新用户的基本信息，如昵称、头像、简介等
     * 
     * @param user 用户信息
     * @return 更新结果，包含更新后的用户信息
     */
    @Operation(summary = "更新用户信息", description = "用于更新用户的基本信息，如昵称、头像、简介等。更新成功后会清除用户缓存，确保数据一致性。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功，返回更新后的用户信息"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录后操作"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/updateUserInfo")
    @LoginCheck
    public R<UserVO> updateUserInfo(
        @Parameter(name = "user", description = "用户信息", required = true, example = "{\"nickname\":\"测试用户\",\"avatar\":\"https://example.com/avatar.jpg\"}") 
        @RequestBody UserVO user) {
        qingSiCache.del(CommonConst.USER_CACHE + BlogUtil.getUserId().toString());
        return userService.updateUserInfo(user);
    }

    /**
     * 获取验证码
     * <p>
     * 1 手机号
     * 2 邮箱
     * 
     * @param flag 验证码类型，1-手机号验证码，2-邮箱验证码
     * @return 验证码发送结果
     */
    @Operation(summary = "获取验证码", description = "用于获取验证码，支持手机号和邮箱两种类型。验证码用于后续的安全操作，如修改密码、绑定手机号等。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "验证码发送成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误，如验证码类型无效"),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录后操作"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误或验证码发送失败")
    })
    @GetMapping("/getCode")
    @LoginCheck
    @SaveCheck
    public R getCode(
        @Parameter(name = "flag", description = "验证码类型，1-手机号验证码，2-邮箱验证码", required = true, example = "1") 
        @RequestParam("flag") Integer flag) {
        return userService.getCode(flag);
    }

    /**
     * 忘记密码 获取验证码
     * <p>
     * 1 手机号
     * 2 邮箱
     * 
     * @param place 手机号或邮箱
     * @param flag 验证码类型，1-手机号验证码，2-邮箱验证码
     * @return 验证码发送结果
     */
    @Operation(summary = "忘记密码获取验证码", description = "用于忘记密码时获取验证码，支持手机号和邮箱两种类型。验证码用于后续的密码重置操作。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "验证码发送成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误，如手机号或邮箱格式不正确"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误或验证码发送失败")
    })
    @GetMapping("/getCodeByRegister")
    @SaveCheck
    public R getCodeByRegister(
        @Parameter(name = "place", description = "手机号或邮箱", required = true, example = "test@example.com") 
        @RequestParam("place") String place, 
        @Parameter(name = "flag", description = "验证码类型，1-手机号验证码，2-邮箱验证码", required = true, example = "2") 
        @RequestParam("flag") Integer flag) {
        return userService.getCodeByRegister(place, flag);
    }


    /**
     * 绑定手机号或者邮箱获取验证码
     * <p>
     * 1 手机号
     * 2 邮箱
     * 
     * @param place 手机号或邮箱
     * @param flag 验证码类型，1-手机号验证码，2-邮箱验证码
     * @return 验证码发送结果
     */
    @Operation(summary = "绑定手机号或邮箱获取验证码", description = "用于绑定手机号或邮箱时获取验证码，支持手机号和邮箱两种类型。验证码用于后续的绑定操作。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "验证码发送成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误，如手机号或邮箱格式不正确"),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录后操作"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误或验证码发送失败")
    })
    @GetMapping("/getCodeForBind")
    @LoginCheck
    @SaveCheck
    public R getCodeForBind(
        @Parameter(name = "place", description = "手机号或邮箱", required = true, example = "test@example.com") 
        @RequestParam("place") String place, 
        @Parameter(name = "flag", description = "验证码类型，1-手机号验证码，2-邮箱验证码", required = true, example = "2") 
        @RequestParam("flag") Integer flag) {
        return userService.getCodeForBind(place, flag);
    }

    /**
     * 更新邮箱、手机号、密码
     * <p>
     * 1 手机号
     * 2 邮箱
     * 3 密码：place=老密码&password=新密码
     * 
     * @param place 原手机号、原邮箱或原密码
     * @param flag 更新类型，1-手机号，2-邮箱，3-密码
     * @param code 验证码（更新手机号或邮箱时必填）
     * @param password 新手机号、新邮箱或新密码
     * @return 更新结果，包含更新后的用户信息
     */
    @Operation(summary = "更新安全信息", description = "用于更新用户的安全信息，包括手机号、邮箱和密码。更新手机号或邮箱时需要验证码，更新密码时需要原密码。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功，返回更新后的用户信息"),
        @ApiResponse(responseCode = "400", description = "请求参数错误，如验证码无效、原密码错误等"),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录后操作"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/updateSecretInfo")
    @LoginCheck
    public R<UserVO> updateSecretInfo(
        @Parameter(name = "place", description = "原手机号、原邮箱或原密码", required = true, example = "oldpassword123") 
        @RequestParam("place") String place, 
        @Parameter(name = "flag", description = "更新类型，1-手机号，2-邮箱，3-密码", required = true, example = "3") 
        @RequestParam("flag") Integer flag, 
        @Parameter(name = "code", description = "验证码（更新手机号或邮箱时必填）", required = false, example = "123456") 
        @RequestParam(value = "code", required = false) String code, 
        @Parameter(name = "password", description = "新手机号、新邮箱或新密码", required = true, example = "newpassword456") 
        @RequestParam("password") String password) {
        qingSiCache.del(CommonConst.USER_CACHE + BlogUtil.getUserId().toString());
        return userService.updateSecretInfo(place, flag, code, password);
    }


    /**
     * 忘记密码 更新密码
     * <p>
     * 1 手机号
     * 2 邮箱
     * 
     * @param place 手机号或邮箱
     * @param flag 验证类型，1-手机号，2-邮箱
     * @param code 验证码
     * @param password 新密码
     * @return 更新结果
     */
    @Operation(summary = "忘记密码重置密码", description = "用于忘记密码时重置密码，支持手机号和邮箱两种验证方式。需要提供验证码和新密码。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "密码重置成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误，如验证码无效、新密码格式不正确等"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/updateForForgetPassword")
    public R updateForForgetPassword(
        @Parameter(name = "place", description = "手机号或邮箱", required = true, example = "test@example.com") 
        @RequestParam("place") String place, 
        @Parameter(name = "flag", description = "验证类型，1-手机号，2-邮箱", required = true, example = "2") 
        @RequestParam("flag") Integer flag, 
        @Parameter(name = "code", description = "验证码", required = true, example = "123456") 
        @RequestParam("code") String code, 
        @Parameter(name = "password", description = "新密码", required = true, example = "newpassword456") 
        @RequestParam("password") String password) {
        return userService.updateForForgetPassword(place, flag, code, password);
    }

    /**
     * 根据用户名查找用户信息
     * 
     * 用于根据用户名搜索用户信息
     * 
     * @param username 用户名
     * @return 符合条件的用户列表
     */
    @Operation(summary = "根据用户名查找用户", description = "用于根据用户名搜索用户信息。返回符合条件的用户列表。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功，返回符合条件的用户列表"),
        @ApiResponse(responseCode = "400", description = "请求参数错误，如用户名为空"),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录后操作"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/getUserByUsername")
    @LoginCheck
    public R<List<UserVO>> getUserByUsername(
        @Parameter(name = "username", description = "用户名", required = true, example = "test") 
        @RequestParam("username") String username) {
        return userService.getUserByUsername(username);
    }




    /**
     * 注册获取图形验证码
     *
     * @param flag 是否为注册，true-注册，false-其他
     * @param email 邮箱地址
     * @return 图形验证码信息，包含验证码ID和Base64编码的验证码图片
     */
    // @GetMapping("/captcha")
    // public R<String> getCaptcha(@RequestParam("flag") Boolean flag, @RequestParam("email") String email) {
    //     return R.success(userService.getCaptcha(flag, email));
    // }
    // @Idempotent(key = "#send")
    @Operation(summary = "获取图形验证码", description = "用于注册或其他需要图形验证码的场景。返回图形验证码的ID和Base64编码的图片。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功，返回图形验证码信息"),
        @ApiResponse(responseCode = "400", description = "请求参数错误，如邮箱格式不正确"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/captcha")
    public R<Map<String, String>> getCaptcha(
        @Parameter(name = "flag", description = "是否为注册，true-注册，false-其他", required = true, example = "true") 
        @RequestParam("flag") Boolean flag, 
        @Parameter(name = "email", description = "邮箱地址", required = true, example = "test@example.com") 
        @RequestParam("email") String email) {
        return R.success(userService.getCaptcha(flag, email));
    }


    /**
     * 图形验证码校验
     *
     * @param uuid 验证码ID
     * @param code 验证码内容
     * @return 校验结果，true-验证成功，false-验证失败
     */
    @Operation(summary = "校验图形验证码", description = "用于校验图形验证码的正确性。需要提供验证码ID和验证码内容。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "校验成功，返回校验结果"),
        @ApiResponse(responseCode = "400", description = "请求参数错误，如验证码ID或内容为空"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/captchaCheck")
    public R<Boolean> checkCaptcha(
        @Parameter(name = "uuid", description = "验证码ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000") 
        @RequestParam String uuid, 
        @Parameter(name = "code", description = "验证码内容", required = true, example = "abcd1234") 
        @RequestParam String code) {
        return R.success(userService.checkCaptcha(uuid, code));
    }

    /**
     * 发送邮箱验证码
     *
     * @param bizType 业务类型，如register、reset_password等
     * @param email 邮箱地址
     * @return 发送结果
     */
    @Operation(summary = "发送邮箱验证码", description = "用于发送邮箱验证码，支持不同的业务类型。验证码用于后续的业务操作验证。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "发送成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误，如邮箱格式不正确、业务类型无效"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误或验证码发送失败")
    })
    @GetMapping("/mailCode")
    public R mailCode(
        @Parameter(name = "bizType", description = "业务类型，如register、reset_password等", required = true, example = "register") 
        @RequestParam("bizType") String bizType, 
        @Parameter(name = "email", description = "邮箱地址", required = true, example = "test@example.com") 
        @RequestParam("email") String email) {
        return userService.mailCode(bizType, email);
    }


    /**
     * 忘记密码 更新密码
     * <p>
     * 1 手机号
     * 2 邮箱
     * 
     * @param email 邮箱地址
     * @param bizType 业务类型
     * @param code 验证码
     * @param password 新密码
     * @return 更新结果
     */
    @Operation(summary = "忘记密码重置密码（新接口）", description = "用于忘记密码时重置密码，通过邮箱验证码验证。需要提供邮箱、业务类型、验证码和新密码。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "密码重置成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误，如验证码无效、新密码格式不正确等"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/resetPwdForFgtPwd")
    public R resetPwdForFgtPwd(
        @Parameter(name = "email", description = "邮箱地址", required = true, example = "test@example.com") 
        @NotNull @RequestParam("email") String email, 
        @Parameter(name = "bizType", description = "业务类型", required = true, example = "reset_password") 
        @RequestParam("bizType") String bizType, 
        @Parameter(name = "code", description = "验证码", required = true, example = "123456") 
        @RequestParam("code") String code, 
        @Parameter(name = "password", description = "新密码", required = true, example = "newpassword456") 
        @RequestParam("password") String password) {
        return userService.resetPwdForFgtPwd(email, bizType, code, password);
    }
}



