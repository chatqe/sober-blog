package io.charqe.qs.modules.behavior.service;

import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.modules.behavior.controller.web.dto.BehaviorReqDTO;
import io.charqe.qs.modules.behavior.domain.dao.BehaviorMapper;
import io.charqe.qs.modules.behavior.domain.pojo.Behavior;
import org.springframework.stereotype.Service;

/**
 * 用户行为表 服务实现类
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
@Service
public class BehaviorServiceImpl extends CoreServiceImpl<BehaviorMapper, Behavior> implements BehaviorService {

    @Override
    public Object handleBehavior(BehaviorReqDTO dto) {

        return null;
    }
}
