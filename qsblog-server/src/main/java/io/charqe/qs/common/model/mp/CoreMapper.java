package io.charqe.qs.common.model.mp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

/**
 * 自建 CoreMapper（继承 MP 的 BaseMapper，但不对外暴露）
 *
 * @author sjq
 * @since 2025-12-16 14:55
 */
public interface CoreMapper<T> extends BaseMapper<T> {
    /* 这里可以扩展通用 SQL，但对外仍不可见 */

    /**
     * 高性能批量插入（MyBatis原生批量，比MP的saveBatch快10倍+）
     */
    int saveBatch(@Param("list") Collection<T> entityList);

    /**
     * 批量更新（根据ID）
     */
    int updateBatchById(@Param("list") Collection<T> entityList);

    /**
     * 幂等新增（根据唯一键，不存在则新增，新增）
     */
    int saveIfAbsent(@Param("entity") T entity);
}


