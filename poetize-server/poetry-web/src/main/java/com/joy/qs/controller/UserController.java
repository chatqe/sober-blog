package com.joy.qs.controller;


import com.joy.qs.aop.LoginCheck;
import com.joy.qs.aop.SaveCheck;
import com.joy.qs.common.R;
import com.joy.qs.constants.CommonConst;
import com.joy.qs.model.user.vo.UserVO;
import com.joy.qs.service.UserService;
import com.joy.qs.utils.PoetryUtil;
import com.joy.qs.utils.cache.PoetryCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 用户信息表 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-08-12
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;


    /**
     * 用户名/密码注册
     */
    @PostMapping("/regist")
    public R<UserVO> regist(@Validated @RequestBody UserVO user) {
        return userService.regist(user);
    }


    /**
     * 用户名、邮箱、手机号/密码登录
     */
    @PostMapping("/login")
    public R<UserVO> login(@RequestParam("account") String account,
                           @RequestParam("password") String password,
                           @RequestParam(value = "isAdmin", defaultValue = "false") Boolean isAdmin) {
        return userService.login(account, password, isAdmin);
    }


    /**
     * Token登录
     */
    @PostMapping("/token")
    public R<UserVO> login(@RequestParam("userToken") String userToken) {
        return userService.token(userToken);
    }


    /**
     * 退出
     */
    @GetMapping("/logout")
    @LoginCheck
    public R exit() {
        return userService.exit();
    }


    /**
     * 更新用户信息
     */
    @PostMapping("/updateUserInfo")
    @LoginCheck
    public R<UserVO> updateUserInfo(@RequestBody UserVO user) {
        PoetryCache.remove(CommonConst.USER_CACHE + PoetryUtil.getUserId().toString());
        return userService.updateUserInfo(user);
    }

    /**
     * 获取验证码
     * <p>
     * 1 手机号
     * 2 邮箱
     */
    @GetMapping("/getCode")
    @LoginCheck
    @SaveCheck
    public R getCode(@RequestParam("flag") Integer flag) {
        return userService.getCode(flag);
    }

    /**
     * 忘记密码 获取验证码
     * <p>
     * 1 手机号
     * 2 邮箱
     */
    @GetMapping("/getCodeByRegister")
    @SaveCheck
    public R getCodeByRegister(@RequestParam("place") String place, @RequestParam("flag") Integer flag) {
        return userService.getCodeByRegister(place, flag);
    }


    /**
     * 绑定手机号或者邮箱
     * <p>
     * 1 手机号
     * 2 邮箱
     */
    @GetMapping("/getCodeForBind")
    @LoginCheck
    @SaveCheck
    public R getCodeForBind(@RequestParam("place") String place, @RequestParam("flag") Integer flag) {
        return userService.getCodeForBind(place, flag);
    }

    /**
     * 更新邮箱、手机号、密码
     * <p>
     * 1 手机号
     * 2 邮箱
     * 3 密码：place=老密码&password=新密码
     */
    @PostMapping("/updateSecretInfo")
    @LoginCheck
    public R<UserVO> updateSecretInfo(@RequestParam("place") String place, @RequestParam("flag") Integer flag, @RequestParam(value = "code", required = false) String code, @RequestParam("password") String password) {
        PoetryCache.remove(CommonConst.USER_CACHE + PoetryUtil.getUserId().toString());
        return userService.updateSecretInfo(place, flag, code, password);
    }

    /**
     * 忘记密码 获取验证码
     * <p>
     * 1 手机号
     * 2 邮箱
     */
    @GetMapping("/getCodeForForgetPassword")
    @SaveCheck
    public R getCodeForForgetPassword(@RequestParam("place") String place, @RequestParam("flag") Integer flag) {
        return userService.getCodeForForgetPassword(place, flag);
    }


    /**
     * 忘记密码 更新密码
     * <p>
     * 1 手机号
     * 2 邮箱
     */
    @PostMapping("/updateForForgetPassword")
    public R updateForForgetPassword(@RequestParam("place") String place, @RequestParam("flag") Integer flag, @RequestParam("code") String code, @RequestParam("password") String password) {
        return userService.updateForForgetPassword(place, flag, code, password);
    }

    /**
     * 根据用户名查找用户信息
     */
    @GetMapping("/getUserByUsername")
    @LoginCheck
    public R<List<UserVO>> getUserByUsername(@RequestParam("username") String username) {
        return userService.getUserByUsername(username);
    }

    /**
     * 订阅/取消订阅专栏（标签）
     * <p>
     * flag = true：订阅
     * flag = false：取消订阅
     */
    @GetMapping("/subscribe")
    @LoginCheck
    public R<UserVO> subscribe(@RequestParam("labelId") Integer labelId, @RequestParam("flag") Boolean flag) {
        PoetryCache.remove(CommonConst.USER_CACHE + PoetryUtil.getUserId().toString());
        return userService.subscribe(labelId, flag);
    }


    /**
     * 注册获取验证码
     *
     * @param flag
     * @param email
     * @return
     */
    // @GetMapping("/captcha")
    // public R<String> getCaptcha(@RequestParam("flag") Boolean flag, @RequestParam("email") String email) {
    //     return R.success(userService.getCaptcha(flag, email));
    // }
    @GetMapping("/captcha")
    public R<Map<String, String>> getCaptcha(@RequestParam("flag") Boolean flag, @RequestParam("email") String email) {
        return R.success(userService.getCaptcha(flag, email));
    }


    /**
     * 验证码校验
     * @param uuid
     * @param code
     * @return
     */
    @PostMapping("/captchaCheck")
    public R<Boolean> checkCaptcha(@RequestParam String uuid, @RequestParam String code) {
        return R.success(userService.checkCaptcha(uuid, code));
    }
}

