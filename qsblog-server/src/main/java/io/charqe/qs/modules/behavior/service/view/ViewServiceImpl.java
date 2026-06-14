package io.charqe.qs.modules.behavior.service.view;

import io.charqe.qs.modules.behavior.controller.web.dto.BehDTO;
import io.charqe.qs.modules.behavior.enums.BehActionType;
import io.charqe.qs.modules.behavior.service.AbstractBehService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 浏览服务实现
 * 浏览独立于其他行为，异步处理且无取消操作
 *
 * @author Joy
 * @since 2026-06-12
 */
@Slf4j
@Service
public class ViewServiceImpl extends AbstractBehService implements ViewService {

    @Override
    @Async
    public void view(BehDTO dto, Long userId) {
        // 浏览不做实体校验（游客也可浏览），直接更新计数 + 发事件
        Long effectiveUserId = userId != null ? userId : 0L;

        // 使用父类的 incrementCount（已重构为 protected）
        incrementCount(dto, BehActionType.VIEW);

        // 发布事件（异步写日志）
        eventPublisher.publishEvent(
                new io.charqe.qs.modules.behavior.service.event.BehActionEvent(
                        this, effectiveUserId, dto.getBizId(), dto.getBizType(), BehActionType.VIEW));

        log.debug("浏览记录完成: bizId={}, bizType={}", dto.getBizId(), dto.getBizType());
    }
}
