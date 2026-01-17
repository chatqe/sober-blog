package io.charqe.qs.common.model.mp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

/**
 * 强制规约：只准用 LambdaWrapper，禁止字符串列名 统一封装 Wrappers.lambdaXXX() 工具，禁止出现 .eq("status", val)；
 *
 * @author Joy
 * @since 2025-12-16 14:51
 */
public final class Lb {
    /**
     * 强制用 Lambda 条件构造器
     */
    public static <T> LambdaQueryWrapper<T> q() {
        return Wrappers.lambdaQuery();
    }

    /**
     * 强制用 Lambda 更新构造器
     */
    public static <T> LambdaUpdateWrapper<T> u() {
        return Wrappers.lambdaUpdate();
    }
}


