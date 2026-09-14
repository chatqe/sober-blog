package io.charqe.qs.modules.behavior.service.dislike;

import io.charqe.qs.modules.behavior.controller.web.dto.BehDTO;
import io.charqe.qs.modules.behavior.domain.pojo.BehaviorState;
import io.charqe.qs.modules.behavior.enums.BehActionType;
import io.charqe.qs.modules.behavior.service.AbstractBehService;
import io.charqe.qs.modules.behavior.service.IActionService;
import io.charqe.qs.modules.behavior.service.MutexRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 点踩服务实现
 * 点踩与点赞互斥：点踩时自动取消已有的点赞状态
 *
 * @author Joy
 * @since 2026-06-12
 */
@Slf4j
@Service
public class DislikeServiceImpl extends AbstractBehService implements DislikeService, IActionService {

    @Resource
    private MutexRegistry mutexRegistry;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dislike(BehDTO dto, Long userId) {
        doAction(dto, userId, BehActionType.DISLIKE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void undislike(BehDTO dto, Long userId) {
        cancelAction(dto, userId, BehActionType.UN_DISLIKE);
    }

    /**
     * 点踩时自动取消已有的点赞（通过配置驱动）
     */
    @Override
    protected void handleMutex(BehDTO dto, Long userId, BehActionType actionType) {
        BehActionType mutexType = mutexRegistry.getMutexType(actionType);
        if (mutexType == null) {
            return;
        }
        // 查询该 bizId 下已存在的活跃行为（不区分 actionType）
        BehaviorState mutexState = getActiveState(userId, dto.getBizId(), dto.getBizType());
        if (mutexState != null && mutexState.getAction().equals(mutexType.code())) {
            mutexState.setStatus(BehaviorState.BEH_CANCELLED);
            mutexState.setUpdateTime(java.time.LocalDateTime.now());
            behaviorStateService.updateById(mutexState);
            // 减少互斥行为的计数
            decrementCount(dto, mutexType);
            syncAfterMutexCancel(dto, userId, mutexType);
            log.debug("互斥操作: {} 时自动取消 {} (userId={}, bizId={})",
                    actionType.desc(), mutexType.desc(), userId, dto.getBizId());
        }
    }
}
