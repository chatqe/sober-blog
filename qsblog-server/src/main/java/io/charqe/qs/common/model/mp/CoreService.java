package io.charqe.qs.common.model.mp;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import io.charqe.qs.common.model.PageQuery;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 基础Service接口，定义通用业务逻辑方法
 * <p>
 * 设计原则：
 * 1. 保持与MyBatis-Plus IService API兼容
 * 2. 提供更便捷的业务方法
 * 3. 统一分页查询接口
 * 4. 优化批量操作性能
 * 5. 支持幂等操作
 * 6. 提供Optional返回类型
 *
 * @author Joy
 * @since 2025-12-16 14:55
 */
public interface CoreService<T> extends IService<T> {
    
    /* ==================== 单条记录操作 ==================== */
    
    /**
     * 根据ID查询，返回Optional
     *
     * @param id 主键ID
     * @return Optional<T>
     */
    Optional<T> getByIdOpt(Serializable id);
    
    /**
     * 根据条件查询单条记录，返回Optional
     *
     * @param queryWrapper 查询条件
     * @return Optional<T>
     */
    Optional<T> getOneOpt(Wrapper<T> queryWrapper);
    
    /**
     * 根据条件查询唯一记录，多条则抛出异常
     *
     * @param queryWrapper 查询条件
     * @return T
     */
    T getUnique(Wrapper<T> queryWrapper);
    
    /**
     * 根据条件查询第一条记录
     *
     * @param queryWrapper 查询条件
     * @return T
     */
    T getFirst(Wrapper<T> queryWrapper);
    
    /* ==================== 批量操作 ==================== */
    
    /**
     * 高性能批量插入
     * <p>
     * 1. 支持批量插入，自动分批（默认500条/批）
     * 2. 支持事务管理
     * 3. 自动预插入处理（设置时间、状态等）
     *
     * @param entityList 实体列表
     * @return 是否成功
     */
    boolean saveBatchOptimize(Collection<T> entityList);
    
    /**
     * 高性能批量更新（根据ID）
     * <p>
     * 1. 支持批量更新，自动分批（默认500条/批）
     * 2. 支持事务管理
     * 3. 自动预更新处理（设置时间等）
     *
     * @param entityList 实体列表
     * @return 是否成功
     */
    boolean updateBatchByIdOptimize(Collection<T> entityList);
    
    /**
     * 批量更新（根据条件）
     * <p>
     * 1. 支持批量更新
     * 2. 支持事务管理
     * 3. 自动预更新处理（设置时间等）
     *
     * @param entity       实体对象
     * @param updateWrapper 更新条件
     * @return 是否成功
     */
    boolean updateBatch(T entity, Wrapper<T> updateWrapper);
    
    /* ==================== 幂等操作 ==================== */
    
    /**
     * 幂等新增
     * <p>
     * 1. 根据唯一索引判断是否已存在
     * 2. 不存在则插入，存在则返回false
     *
     * @param entity 实体对象
     * @return 是否插入成功
     */
    boolean saveIfAbsent(T entity);
    
    /**
     * 幂等新增或更新
     * <p>
     * 1. 根据ID判断
     * 2. 无ID则插入，有ID则更新
     *
     * @param entity 实体对象
     * @return 是否成功
     */
    boolean saveOrUpdateIdempotent(T entity);
    
    /* ==================== 分页查询 ==================== */
    
    /**
     * 统一分页查询
     * <p>
     * 1. 标准化分页参数
     * 2. 统一分页结果格式
     * 3. 支持排序
     * 4. 支持总数查询
     *
     * @param pageQuery   分页参数
     * @param queryWrapper 查询条件
     * @return 分页结果
     */
    IPage<T> page(PageQuery pageQuery, Wrapper<T> queryWrapper);
    
    /**
     * 统一分页查询（带总数）
     *
     * @param pageQuery   分页参数
     * @param queryWrapper 查询条件
     * @return 分页结果
     */
    IPage<T> pageWithCount(PageQuery pageQuery, Wrapper<T> queryWrapper);
    
    /**
     * 统一分页查询（不带总数）
     *
     * @param pageQuery   分页参数
     * @param queryWrapper 查询条件
     * @return 分页结果
     */
    IPage<T> pageWithoutCount(PageQuery pageQuery, Wrapper<T> queryWrapper);
    
    /* ==================== 条件查询 ==================== */
    
    /**
     * 根据条件查询列表，返回List
     *
     * @param queryWrapper 查询条件
     * @return 实体列表
     */
    List<T> list(Wrapper<T> queryWrapper);
    
    /**
     * 根据条件查询列表，返回List，支持排序
     *
     * @param queryWrapper 查询条件
     * @param sortField    排序字段
     * @param isAsc        是否升序
     * @return 实体列表
     */
    List<T> list(Wrapper<T> queryWrapper, String sortField, boolean isAsc);
    
    /**
     * 根据条件查询Map列表
     *
     * @param queryWrapper 查询条件
     * @return Map列表
     */
    List<Map<String, Object>> listMaps(Wrapper<T> queryWrapper);
    
    /* ==================== 计数查询 ==================== */
    
    /**
     * 根据条件查询记录数，返回long类型
     *
     * @param queryWrapper 查询条件
     * @return 记录数
     */
    long countLong(Wrapper<T> queryWrapper);
    
    /**
     * 根据条件判断记录是否存在
     *
     * @param queryWrapper 查询条件
     * @return 是否存在
     */
    boolean exists(Wrapper<T> queryWrapper);
    
    /* ==================== 数据校验 ==================== */
    
    /**
     * 数据校验
     * <p>
     * 1. 校验必填字段
     * 2. 校验字段格式
     * 3. 校验业务规则
     *
     * @param entity 实体对象
     * @return 是否校验通过
     */
    boolean validate(T entity);
    
    /**
     * 批量数据校验
     *
     * @param entityList 实体列表
     * @return 是否校验通过
     */
    boolean validateBatch(Collection<T> entityList);
    
    /* ==================== 扩展方法 ==================== */
    
    /**
     * 获取当前Service对应的Mapper
     *
     * @return CoreMapper<T>
     */
    CoreMapper<T> getBaseMapper();
    
    /**
     * 预插入处理
     * <p>
     * 1. 设置创建时间
     * 2. 设置创建人
     * 3. 设置状态默认值
     *
     * @param entity 实体对象
     */
    void preInsert(T entity);
    
    /**
     * 预更新处理
     * <p>
     * 1. 设置更新时间
     * 2. 设置更新人
     *
     * @param entity 实体对象
     */
    void preUpdate(T entity);
}


