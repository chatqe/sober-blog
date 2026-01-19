package io.charqe.qs.modules.webInfo.service;

import io.charqe.qs.modules.webInfo.domain.dao.WebInfoMapper;
import io.charqe.qs.modules.webInfo.domain.pojo.WebInfo;
import io.charqe.qs.modules.webInfo.service.WebInfoService;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import org.springframework.stereotype.Service;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Service
public class WebInfoServiceImpl extends CoreServiceImpl<WebInfoMapper, WebInfo> implements WebInfoService {

}


