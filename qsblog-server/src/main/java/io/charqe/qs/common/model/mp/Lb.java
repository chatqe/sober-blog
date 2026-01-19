package io.charqe.qs.common.model.mp;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.io.Serializable;
import java.util.Collection;

/**
 * 条件构造器封装，强制使用Lambda表达式，提高代码的类型安全性和可读性
 * <p>
 * 设计原则：
 * 1. 强制使用Lambda表达式，避免字符串硬编码
 * 2. 提供更便捷的API，简化查询条件的构建
 * 3. 支持链式调用，提高代码可读性
 * 4. 提供常用查询条件的快捷方法
 * 5. 保持与MyBatis-Plus条件构造器的兼容性
 *
 * @author Joy
 * @since 2025-12-16 14:51
 */
public final class Lb {
    
    /**
     * 私有构造函数，防止实例化
     */
    private Lb() {
        throw new UnsupportedOperationException("Lb工具类不能实例化");
    }
    
    /* ==================== 查询条件构造器 ==================== */
    
    /**
     * 创建Lambda查询条件构造器
     *
     * @param <T> 实体类型
     * @return LambdaQueryWrapper<T>
     */
    public static <T> LambdaQueryWrapper<T> q() {
        return Wrappers.lambdaQuery();
    }
    
    /**
     * 创建Lambda查询条件构造器，并指定实体类
     *
     * @param entity 实体对象
     * @param <T>    实体类型
     * @return LambdaQueryWrapper<T>
     */
    public static <T> LambdaQueryWrapper<T> q(T entity) {
        return Wrappers.lambdaQuery(entity);
    }
    
    /**
     * 创建Lambda查询条件构造器，并指定实体类
     *
     * @param entityClass 实体类
     * @param <T>         实体类型
     * @return LambdaQueryWrapper<T>
     */
    public static <T> LambdaQueryWrapper<T> q(Class<T> entityClass) {
        return Wrappers.lambdaQuery(entityClass);
    }
    
    /* ==================== 更新条件构造器 ==================== */
    
    /**
     * 创建Lambda更新条件构造器
     *
     * @param <T> 实体类型
     * @return LambdaUpdateWrapper<T>
     */
    public static <T> LambdaUpdateWrapper<T> u() {
        return Wrappers.lambdaUpdate();
    }
    
    /**
     * 创建Lambda更新条件构造器，并指定实体类
     *
     * @param entity 实体对象
     * @param <T>    实体类型
     * @return LambdaUpdateWrapper<T>
     */
    public static <T> LambdaUpdateWrapper<T> u(T entity) {
        return Wrappers.lambdaUpdate(entity);
    }
    
    /**
     * 创建Lambda更新条件构造器，并指定实体类
     *
     * @param entityClass 实体类
     * @param <T>         实体类型
     * @return LambdaUpdateWrapper<T>
     */
    public static <T> LambdaUpdateWrapper<T> u(Class<T> entityClass) {
        return Wrappers.lambdaUpdate(entityClass);
    }
    
    /* ==================== 禁用非Lambda构造器 ==================== */
    
    /**
     * 禁止使用QueryWrapper（非Lambda）
     * <p>
     * 该方法被标记为已弃用，并且会抛出UnsupportedOperationException，强制使用Lambda表达式
     *
     * @param <T> 实体类型
     * @return QueryWrapper<T>
     * @deprecated 禁止使用非Lambda构造器，请使用Lb.q()代替
     */
    @Deprecated
    public static <T> QueryWrapper<T> query() {
        throw new UnsupportedOperationException("禁止使用非Lambda构造器，请使用Lb.q()代替");
    }
    
    /**
     * 禁止使用UpdateWrapper（非Lambda）
     * <p>
     * 该方法被标记为已弃用，并且会抛出UnsupportedOperationException，强制使用Lambda表达式
     *
     * @param <T> 实体类型
     * @return UpdateWrapper<T>
     * @deprecated 禁止使用非Lambda构造器，请使用Lb.u()代替
     */
    @Deprecated
    public static <T> UpdateWrapper<T> update() {
        throw new UnsupportedOperationException("禁止使用非Lambda构造器，请使用Lb.u()代替");
    }
    
    /* ==================== 快捷方法 ==================== */
    
    /**
     * 创建一个空的Lambda查询条件构造器，用于查询所有记录
     *
     * @param <T> 实体类型
     * @return LambdaQueryWrapper<T>
     */
    public static <T> LambdaQueryWrapper<T> all() {
        return Wrappers.lambdaQuery();
    }
    
    /**
     * 创建一个查询条件构造器，用于根据ID查询
     *
     * @param idProperty ID属性的Lambda表达式
     * @param id         ID值
     * @param <T>        实体类型
     * @return LambdaQueryWrapper<T>
     */
    public static <T, R> LambdaQueryWrapper<T> byId(SFunction<T, R> idProperty, Serializable id) {
        LambdaQueryWrapper<T> queryWrapper = Wrappers.lambdaQuery();
        return queryWrapper.eq(idProperty, id);
    }
    
    /**
     * 创建一个查询条件构造器，用于根据状态查询
     *
     * @param statusProperty 状态属性的Lambda表达式
     * @param status         状态值
     * @param <T>            实体类型
     * @return LambdaQueryWrapper<T>
     */
    public static <T, R> LambdaQueryWrapper<T> byStatus(SFunction<T, R> statusProperty, R status) {
        LambdaQueryWrapper<T> queryWrapper = Wrappers.lambdaQuery();
        return queryWrapper.eq(statusProperty, status);
    }
    
    /**
     * 创建一个查询条件构造器，用于根据多个ID查询
     *
     * @param idProperty ID属性的Lambda表达式
     * @param ids        ID集合
     * @param <T>        实体类型
     * @return LambdaQueryWrapper<T>
     */
    public static <T, R> LambdaQueryWrapper<T> byIds(SFunction<T, R> idProperty, Collection<? extends Serializable> ids) {
        LambdaQueryWrapper<T> queryWrapper = Wrappers.lambdaQuery();
        return queryWrapper.in(idProperty, ids);
    }
    
    /**
     * 创建一个更新条件构造器，用于根据ID更新
     *
     * @param idProperty ID属性的Lambda表达式
     * @param id         ID值
     * @param <T>        实体类型
     * @return LambdaUpdateWrapper<T>
     */
    public static <T, R> LambdaUpdateWrapper<T> updateById(SFunction<T, R> idProperty, Serializable id) {
        LambdaUpdateWrapper<T> updateWrapper = Wrappers.lambdaUpdate();
        return updateWrapper.eq(idProperty, id);
    }
    
    /**
     * 创建一个更新条件构造器，用于根据状态更新
     *
     * @param statusProperty 状态属性的Lambda表达式
     * @param status         状态值
     * @param <T>            实体类型
     * @return LambdaUpdateWrapper<T>
     */
    public static <T, R> LambdaUpdateWrapper<T> updateByStatus(SFunction<T, R> statusProperty, R status) {
        LambdaUpdateWrapper<T> updateWrapper = Wrappers.lambdaUpdate();
        return updateWrapper.eq(statusProperty, status);
    }
    
    /* ==================== 工具方法 ==================== */
    
    /**
     * 检查条件构造器是否有条件
     *
     * @param wrapper 条件构造器
     * @param <T>     实体类型
     * @return boolean
     */
    public static <T> boolean hasCondition(AbstractWrapper<T, ?, ?> wrapper) {
        return wrapper != null && wrapper.getExpression() != null;
    }
    
    /**
     * 清空条件构造器的条件
     *
     * @param wrapper 条件构造器
     * @param <T>     实体类型
     * @return 清空条件后的构造器
     */
    public static <T> Wrapper<T> clearCondition(Wrapper<T> wrapper) {
        if (wrapper instanceof AbstractWrapper) {
            ((AbstractWrapper<?, ?, ?>) wrapper).clear();
        }
        return wrapper;
    }
}


