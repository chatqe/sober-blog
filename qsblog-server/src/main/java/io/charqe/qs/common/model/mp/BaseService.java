package io.charqe.qs.common.model.mp;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 自建 BaseService（业务唯一入口）
 *
 * @author sjq
 * @since 2025-12-16 14:55
 */
public interface BaseService<T> extends IService<T> {
    /* 分页、批量、乐观锁、多租户全部在这里统一装配 */
}


