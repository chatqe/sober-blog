package io.charqe.qs.modules.behavior.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.charqe.qs.common.model.mp.Lb;
import io.charqe.qs.modules.behavior.controller.web.dto.BehDTO;
import io.charqe.qs.modules.behavior.domain.pojo.BehaviorCnt;
import io.charqe.qs.modules.behavior.domain.pojo.BehaviorState;
import io.charqe.qs.modules.behavior.enums.BehActionType;
import io.charqe.qs.modules.behavior.event.BehActionEvent;
import io.charqe.qs.modules.behavior.handler.BehEntityHandler;
import io.charqe.qs.modules.behavior.handler.BehHandlerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

import javax.annotation.Resource;

/**
 * 行为Service抽象基类
 * 提供正向操作（doAction）和反向取消操作（cancelAction）的模板方法
 *
 * @author Joy
 * @since 2026-06-12
 */
@Slf4j
public abstract class AbstractBehService {

    @Resource
    protected BehaviorStateService behaviorStateService;

    @Resource
    protected BehaviorCntService behCntService;

    @Resource
    protected BehHandlerFactory handlerFactory;

    @Resource
    protected ApplicationEventPublisher eventPublisher;

    // ==================== 模板方法 ====================

    /**
     * 执行正向行为（点赞/点踩/订阅/转发等）
     *
     * @param dto        请求参数
     * @param userId     操作人ID
     * @param actionType 正向行为类型
     */
    protected void doAction(BehDTO dto, Long userId, BehActionType actionType) {
        // 1. 实体校验
        BehEntityHandler handler = handlerFactory.getHandler(dto.getBizType());
        handler.validate(dto.getBizId());

        // 2. 查询当前状态
        BehaviorState existingState = getExistingState(userId, dto.getBizId(), dto.getBizType(), actionType);

        if (existingState != null) {
            // 已存在相同行为，幂等返回
            if (BehaviorState.BEH_ACTIVE == stateStatus(existingState)) {
                log.debug("行为已存在，幂等跳过: userId={}, bizId={}, action={}", userId, dto.getBizId(), actionType.desc());
                return;
            }
            // 之前取消过，先处理互斥再重新激活
            handleMutex(dto, userId, actionType);
            existingState.setStatus(BehaviorState.BEH_ACTIVE);
            existingState.setUpdateTime(java.time.LocalDateTime.now());
            behaviorStateService.updateById(existingState);
        } else {
            // 3. 互斥处理（子类覆写）
            handleMutex(dto, userId, actionType);

            // 4. 保存状态
            BehaviorState state = new BehaviorState();
            state.setUserId(userId);
            state.setBizId(dto.getBizId());
            state.setBizType(dto.getBizType());
            state.setAction(actionType.code());
            state.setStatus(BehaviorState.BEH_ACTIVE);
            state.setCreateTime(java.time.LocalDateTime.now());
            behaviorStateService.save(state);
        }

        // 5. 更新计数
        incrementCount(dto, actionType);

        // 6. 实体后置处理
        handler.afterAction(dto.getBizId(), userId);

        // 7. 发布事件（异步写日志）
        eventPublisher.publishEvent(new BehActionEvent(this, userId, dto.getBizId(), dto.getBizType(), actionType));

        log.debug("{} 操作完成: userId={}, bizId={}, bizType={}", actionType.desc(), userId, dto.getBizId(), dto.getBizType());
    }

    /**
     * 执行反向取消行为（取消点赞/取消点踩/取消订阅等）
     *
     * @param dto          请求参数
     * @param userId       操作人ID
     * @param unActionType 反向行为类型
     */
    protected void cancelAction(BehDTO dto, Long userId, BehActionType unActionType) {
        // 1. 实体校验
        BehEntityHandler handler = handlerFactory.getHandler(dto.getBizType());
        handler.validate(dto.getBizId());

        // 2. 查询当前状态
        BehaviorState existingState = getExistingState(userId, dto.getBizId(), dto.getBizType(), getPositiveAction(unActionType));

        if (existingState == null || BehaviorState.BEH_ACTIVE != stateStatus(existingState)) {
            // 没有活跃的行为状态，幂等返回
            log.debug("无可取消的行为，幂等跳过: userId={}, bizId={}", userId, dto.getBizId());
            return;
        }

        // 3. 取消状态
        existingState.setStatus(BehaviorState.BEH_CANCELLED);
        existingState.setUpdateTime(java.time.LocalDateTime.now());
        behaviorStateService.updateById(existingState);

        // 4. 减少计数
        decrementCount(dto, getPositiveAction(unActionType));

        // 5. 实体后置处理（取消回退）
        handler.afterCancelAction(dto.getBizId(), userId);

        // 6. 发布取消事件
        eventPublisher.publishEvent(new BehActionEvent(this, userId, dto.getBizId(), dto.getBizType(), unActionType));

        log.debug("{} 操作完成: userId={}, bizId={}, bizType={}", unActionType.desc(), userId, dto.getBizId(), dto.getBizType());
    }

    // ==================== 子类可覆写的方法 ====================

    /**
     * 处理互斥行为（如点赞时自动取消点踩）
     * 默认空实现，LikeService / DislikeService 覆写
     */
    protected void handleMutex(BehDTO dto, Long userId, BehActionType actionType) {
        // 默认无互斥逻辑
    }

    /**
     * 取消互斥行为后的实体后置处理
     */
    protected void syncAfterMutexCancel(BehDTO dto, Long userId, BehActionType mutexType) {
        BehEntityHandler handler = handlerFactory.getHandler(dto.getBizType());
        handler.afterCancelAction(dto.getBizId(), userId);
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取实体当前状态（子类可扩展，如根据 mutexKey 查询）
     */
    protected BehaviorState getExistingState(Long userId, Long bizId, Integer bizType, BehActionType actionType) {
        LambdaQueryWrapper<BehaviorState> wrapper = Lb.<BehaviorState>q()
                .eq(BehaviorState::getUserId, userId)
                .eq(BehaviorState::getBizId, bizId)
                .eq(BehaviorState::getBizType, bizType)
                .eq(BehaviorState::getAction, actionType.code());
        return behaviorStateService.getOne(wrapper);
    }

    /**
     * 查询指定用户、bizId、bizType 下指定行为类型的活跃状态（不区分 actionType）
     * 用于互斥逻辑中查询"对方"的活跃状态
     */
    protected BehaviorState getActiveState(Long userId, Long bizId, Integer bizType) {
        LambdaQueryWrapper<BehaviorState> wrapper = Lb.<BehaviorState>q()
                .eq(BehaviorState::getUserId, userId)
                .eq(BehaviorState::getBizId, bizId)
                .eq(BehaviorState::getBizType, bizType)
                .eq(BehaviorState::getStatus, BehaviorState.BEH_ACTIVE)
                .last("limit 1");
        return behaviorStateService.getOne(wrapper);
    }

    protected int stateStatus(BehaviorState state) {
        return state != null ? state.getStatus() : 0;
    }

    /**
     * 递增计数（protected 供子类复用，如 ViewServiceImpl）
     */
    protected void incrementCount(BehDTO dto, BehActionType actionType) {
        BehaviorCnt existingCnt = behCntService.getOne(Lb.<BehaviorCnt>q()
                .eq(BehaviorCnt::getBizId, dto.getBizId())
                .eq(BehaviorCnt::getBizType, dto.getBizType())
                .eq(BehaviorCnt::getAction, actionType.code()));
        if (existingCnt == null) {
            BehaviorCnt cnt = new BehaviorCnt();
            cnt.setBizId(dto.getBizId());
            cnt.setBizType(dto.getBizType());
            cnt.setAction(actionType.code());
            cnt.setCount(1);
            behCntService.save(cnt);
        } else {
            existingCnt.setCount(existingCnt.getCount() + 1);
            behCntService.updateById(existingCnt);
        }
    }

    /**
     * 递减计数（protected 供子类复用）
     */
    protected void decrementCount(BehDTO dto, BehActionType actionType) {
        BehaviorCnt existingCnt = behCntService.getOne(Lb.<BehaviorCnt>q()
                .eq(BehaviorCnt::getBizId, dto.getBizId())
                .eq(BehaviorCnt::getBizType, dto.getBizType())
                .eq(BehaviorCnt::getAction, actionType.code()));
        if (existingCnt != null && existingCnt.getCount() > 0) {
            existingCnt.setCount(existingCnt.getCount() - 1);
            behCntService.updateById(existingCnt);
        }
    }

    /**
     * 根据取消类型获取对应的正向行为类型
     */
    protected BehActionType getPositiveAction(BehActionType unActionType) {
        return switch (unActionType) {
            case UN_LIKE -> BehActionType.LIKE;
            case UN_DISLIKE -> BehActionType.DISLIKE;
            case UN_SUBSCRIBE -> BehActionType.SUBSCRIBE;
            case UN_FORWARD -> BehActionType.FORWARD;
            case UN_COMMENT -> BehActionType.COMMENT;
            default -> throw new IllegalArgumentException("未知的取消操作类型: " + unActionType);
        };
    }
}
