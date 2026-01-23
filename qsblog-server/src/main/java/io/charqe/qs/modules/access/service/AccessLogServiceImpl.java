package io.charqe.qs.modules.access.service;

import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.modules.access.domain.dao.AccessLogMapper;
import io.charqe.qs.modules.access.domain.pojo.AccessLog;
import io.charqe.qs.modules.behavior.domain.dao.BehaviorCntMapper;
import io.charqe.qs.modules.behavior.domain.pojo.BehaviorCnt;
import io.charqe.qs.modules.behavior.service.BehaviorCntService;
import org.springframework.stereotype.Service;

/**
 * 访问日志实现类
 *
 * @author Joy
 * @since 2026-01-23 20:52
 */
@Service
public class AccessLogServiceImpl extends CoreServiceImpl<AccessLogMapper, AccessLog> implements AccessLogService {

}
