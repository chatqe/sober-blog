package io.charqe.qs.modules.portal.service;

import com.alibaba.fastjson2.JSON;
import io.charqe.qs.modules.access.service.AccessLogService;
import io.charqe.qs.modules.article.domain.dto.ArticleStatsDTO;
import io.charqe.qs.modules.article.service.ArticleService;
import io.charqe.qs.modules.category.service.CategoryService;
import io.charqe.qs.modules.portal.constant.PortalConstant;
import io.charqe.qs.modules.portal.controller.web.vo.PortalOverviewVO;
import io.charqe.qs.modules.webInfo.domain.pojo.WebInfo;
import io.charqe.qs.modules.webInfo.service.WebInfoService;
import io.charqe.qs.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

/**
 * 门户聚合服务实现
 * 只做各域统计能力的编排与视图组装，不直接访问其他域的 Mapper
 *
 * @author Joy
 * @since 2026-10-23
 */
@Slf4j
@Service
public class PortalServiceImpl implements PortalService {

    @Resource
    private WebInfoService webInfoService;

    @Resource
    private ArticleService articleService;

    @Resource
    private CategoryService categoryService;

    @Resource
    private AccessLogService accessLogService;

    @Resource
    private QingSiCache qingSiCache;

    @Override
    public PortalOverviewVO getOverview() {
        String cached = qingSiCache.get(PortalConstant.OVERVIEW_CACHE);
        if (StringUtils.hasText(cached)) {
            try {
                return JSON.parseObject(cached, PortalOverviewVO.class);
            } catch (Exception e) {
                log.warn("站点概览缓存解析失败，回源重建：{}", e.getMessage());
                qingSiCache.del(PortalConstant.OVERVIEW_CACHE);
            }
        }

        PortalOverviewVO overview = buildOverview();
        qingSiCache.put(PortalConstant.OVERVIEW_CACHE, overview, PortalConstant.OVERVIEW_CACHE_SECONDS);
        return overview;
    }

    /**
     * 跨域组装站点概览
     */
    private PortalOverviewVO buildOverview() {
        PortalOverviewVO overview = new PortalOverviewVO();

        WebInfo siteInfo = webInfoService.getSiteInfo();
        if (siteInfo != null) {
            overview.setWebName(siteInfo.getWebName());
            overview.setWebTitle(siteInfo.getWebTitle());
            overview.setAvatar(siteInfo.getAvatar());
        }

        ArticleStatsDTO articleStats = articleService.getStats();
        overview.setArticleTotal(defaultZero(articleStats.getArticleTotal()));
        overview.setViewTotal(defaultZero(articleStats.getViewTotal()));
        overview.setLikeTotal(defaultZero(articleStats.getLikeTotal()));
        overview.setCategoryTotal(categoryService.countVisible());
        overview.setHistoryAllCount(accessLogService.getHistoryAllCount());

        return overview;
    }

    private long defaultZero(Long value) {
        return value != null ? value : 0L;
    }
}
