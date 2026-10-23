package io.charqe.qs.modules.portal.service;

import io.charqe.qs.modules.portal.controller.web.vo.PortalOverviewVO;

/**
 * 门户聚合服务
 * 跨域编排站点、文章、分类的只读统计数据，不承载任何领域写逻辑
 *
 * @author Joy
 * @since 2026-10-23
 */
public interface PortalService {

    /**
     * 获取站点概览统计数据
     *
     * @return 站点概览
     */
    PortalOverviewVO getOverview();
}
