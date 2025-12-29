package io.charqe.qs.common.mp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 自建 BaseDao（继承 MP 的 BaseMapper，但不对外暴露）
 *
 * @author sjq
 * @since 2025-12-16 14:55
 */
public interface BaseDao<T> extends BaseMapper<T> {
    /* 这里可以扩展通用 SQL，但对外仍不可见 */
}


