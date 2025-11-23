package com.joy.qs.utils;


import com.joy.qs.model.user.dto.LoginUserInfo;

/**
 * @author Upjqs
 * app端用户登录局部变量类
 */
public class ThreadLocalUtil {

    private final static ThreadLocal<LoginUserInfo> WM_USER_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 添加用户
     * @param
     */
    public static void  setUser(LoginUserInfo loginUserInfo){
        WM_USER_THREAD_LOCAL.set(loginUserInfo);
    }

    /**
     * 获取用户
     */
    public static LoginUserInfo getUser(){
        return WM_USER_THREAD_LOCAL.get();
    }

    /**
     * 清理用户
     */
    public static void clear(){
        WM_USER_THREAD_LOCAL.remove();
    }
}