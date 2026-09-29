package io.charqe.qs.modules.behavior.service;

import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.modules.behavior.domain.dao.BehaviorCntMapper;
import io.charqe.qs.modules.behavior.domain.pojo.BehaviorCnt;
import io.charqe.qs.modules.behavior.service.BehaviorCntService;
import org.springframework.stereotype.Service;

/**
 * 用户行为统计表 服务实现类
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
@Service
public class BehaviorCntServiceImpl extends CoreServiceImpl<BehaviorCntMapper, BehaviorCnt> implements BehaviorCntService {

}
