package io.charqe.qs.modules.webInfo.service;

import io.charqe.qs.modules.webInfo.domain.pojo.WebInfo;
import io.charqe.qs.common.model.mp.CoreService;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
public interface WebInfoService extends CoreService<WebInfo> {

    /**
     * 获取站点信息（缓存优先，缓存未命中时回源并写入缓存）
     *
     * @return 站点信息，数据库无记录时返回 null
     */
    WebInfo getSiteInfo();
}


