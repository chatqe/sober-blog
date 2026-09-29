package io.charqe.qs.modules.behavior.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.common.model.mp.Lb;
import io.charqe.qs.modules.behavior.domain.dao.BehaviorStateMapper;
import io.charqe.qs.modules.behavior.domain.pojo.BehaviorState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 用户行为状态 CRUD 实现
 * 业务逻辑（点赞/点踩/订阅等）已拆分到各独立 Service
 *
 * @author Joy
 * @since 2026-03-15 20:05
 */
@Slf4j
@Service
public class BehaviorStateServiceImpl extends CoreServiceImpl<BehaviorStateMapper, BehaviorState> implements BehaviorStateService {

    @Override
    public boolean checkHasBeh(BehaviorState beh) {
        LambdaQueryWrapper<BehaviorState> wrapper = Lb.<BehaviorState>q()
                .eq(Objects.nonNull(beh.getUserId()), BehaviorState::getUserId, beh.getUserId())
                .eq(Objects.nonNull(beh.getBizId()), BehaviorState::getBizId, beh.getBizId())
                .eq(Objects.nonNull(beh.getBizType()), BehaviorState::getBizType, beh.getBizType())
                .orderByDesc(BehaviorState::getCreateTime)
                .last("limit 1");

        BehaviorState dbBeh = getOne(wrapper);
        return dbBeh != null && beh.getAction().equals(dbBeh.getAction());
    }

    @Override
    public boolean checkHasState(Long userId, Long bizId, Integer bizType, Integer action) {
        return count(Lb.<BehaviorState>q()
                .eq(BehaviorState::getUserId, userId)
                .eq(BehaviorState::getBizId, bizId)
                .eq(BehaviorState::getBizType, bizType)
                .eq(BehaviorState::getAction, action)
                .eq(BehaviorState::getStatus, BehaviorState.BEH_ACTIVE)) > 0;
    }
}
