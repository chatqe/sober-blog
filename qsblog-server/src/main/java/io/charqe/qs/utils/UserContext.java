package io.charqe.qs.utils;


import io.charqe.qs.modules.user.controller.web.dto.LoginUserInfo;

/**
 * @author Upjqs
 * 用户登录局部变量类
 */
public class UserContext {

    private final static ThreadLocal<LoginUserInfo> WM_USER_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 添加用户
     *
     * @param
     */
    public static void set(LoginUserInfo loginUserInfo) {
        WM_USER_THREAD_LOCAL.set(loginUserInfo);
    }

    /**
     * 获取用户
     */
    public static LoginUserInfo get() {
        return WM_USER_THREAD_LOCAL.get();
    }

    /**
     * 清理用户
     */
    public static void clear() {
        WM_USER_THREAD_LOCAL.remove();
    }
}

