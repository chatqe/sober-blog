package io.charqe.qs.common.model.mp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;

/**
 * 基础Mapper封装，继承自MyBatis-Plus的BaseMapper，提供通用CRUD操作及扩展功能
 * <p>
 * 设计原则：
 * 1. 保持与MyBatis-Plus API兼容
 * 2. 提供更便捷的查询方法
 * 3. 支持Optional返回类型
 * 4. 预留扩展点
 *
 * @author Joy
 * @since 2025-12-16 14:55
 */
public interface CoreMapper<T> extends BaseMapper<T> {
    
    /**
     * 根据ID查询，返回Optional
     *
     * @param id 主键ID
     * @return Optional<T>
     */
    default Optional<T> selectByIdOpt(Serializable id) {
        return Optional.ofNullable(selectById(id));
    }
    
    /**
     * 根据条件查询单条记录，返回Optional
     *
     * @param queryWrapper 查询条件
     * @return Optional<T>
     */
    default Optional<T> selectOneOpt(Wrapper<T> queryWrapper) {
        return Optional.ofNullable(selectOne(queryWrapper));
    }
    
    /**
     * 根据条件查询唯一记录，多条则抛出异常
     *
     * @param queryWrapper 查询条件
     * @return T
     */
    T selectUnique(Wrapper<T> queryWrapper);
    
    /**
     * 批量插入（高性能）
     *
     * @param entityList 实体列表
     * @return 影响行数
     */
    int insertBatchSomeColumn(Collection<T> entityList);
    
    /**
     * 批量更新（根据ID）
     *
     * @param entityList 实体列表
     * @return 影响行数
     */
    int updateBatchById(@Param("list") Collection<T> entityList);
    
    /**
     * 批量更新（根据条件）
     *
     * @param entity 实体对象
     * @param updateWrapper 更新条件
     * @return 影响行数
     */
    int updateBatch(@Param("et") T entity, @Param("ew") Wrapper<T> updateWrapper);
    
    /**
     * 批量删除（根据ID）
     *
     * @param idList ID列表
     * @return 影响行数
     */
    int deleteBatchIds(@Param("coll") Collection<?> idList);
    
    /**
     * 根据条件查询记录数，返回long类型
     *
     * @param queryWrapper 查询条件
     * @return 记录数
     */
    default long selectCountLong(Wrapper<T> queryWrapper) {
        return selectCount(queryWrapper);
    }
    
    /**
     * 根据条件查询，返回第一条记录
     *
     * @param queryWrapper 查询条件
     * @return T
     */
    T selectFirst(Wrapper<T> queryWrapper);
}


