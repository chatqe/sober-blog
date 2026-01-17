package io.charqe.qs.common.model.mp;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import io.charqe.qs.common.model.PageQuery;

import java.util.Collection;

/**
 * 自建 CoreService（业务唯一入口）
 *
 * @author sjq
 * @since 2025-12-16 14:55
 */
public interface CoreService<T> extends IService<T> {
    /* 分页、批量、乐观锁、多租户全部在这里统一装配 */
    /**
     * 幂等新增（你的方案）
     */
    boolean saveIfAbsent(T entity);

    /**
     * 统一分页查询（你的方案+参数标准化）
     */
    IPage<T> page(PageQuery pageQuery, Wrapper<T> wrapper);

    /**
     * 高性能批量插入（我的方案）
     */
    boolean saveBatchOptimize(Collection<T> entityList);

    /**
     * 高性能批量插入（自定义分批大小）
     */
    boolean saveBatchOptimize(Collection<T> entityList, int batchSize);

    /**
     * 查询唯一数据（多条抛异常，我的方案）
     */
    T selectUnique(Wrapper<T> wrapper);

    /**
     * 批量更新（分批执行，我的方案）
     */
    boolean updateBatchByIdOptimize(Collection<T> entityList);
}


