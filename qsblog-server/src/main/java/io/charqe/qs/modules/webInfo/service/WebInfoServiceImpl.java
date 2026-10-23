package io.charqe.qs.modules.webInfo.service;

import com.alibaba.fastjson2.JSON;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.modules.webInfo.domain.dao.WebInfoMapper;
import io.charqe.qs.modules.webInfo.domain.pojo.WebInfo;
import io.charqe.qs.utils.cache.QingSiCache;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Service
public class WebInfoServiceImpl extends CoreServiceImpl<WebInfoMapper, WebInfo> implements WebInfoService {

    @Resource
    private QingSiCache qingSiCache;

    @Override
    public WebInfo getSiteInfo() {
        String cached = qingSiCache.get(CommonConst.WEB_INFO);
        if (StringUtils.hasText(cached)) {
            return JSON.parseObject(cached, WebInfo.class);
        }

        List<WebInfo> list = list();
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        WebInfo webInfo = list.get(0);
        qingSiCache.put(CommonConst.WEB_INFO, webInfo);
        return webInfo;
    }
}


