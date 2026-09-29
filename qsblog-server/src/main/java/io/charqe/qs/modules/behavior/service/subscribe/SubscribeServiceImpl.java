package io.charqe.qs.modules.behavior.service.subscribe;

import io.charqe.qs.modules.behavior.controller.web.dto.BehDTO;
import io.charqe.qs.modules.behavior.enums.BehActionType;
import io.charqe.qs.modules.behavior.service.AbstractBehService;
import io.charqe.qs.modules.behavior.service.IActionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订阅服务实现
 * 无互斥逻辑，纯状态翻转 + 计数 + 日志
 *
 * @author Joy
 * @since 2026-06-12
 */
@Slf4j
@Service
public class SubscribeServiceImpl extends AbstractBehService implements SubscribeService, IActionService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void subscribe(BehDTO dto, Long userId) {
        doAction(dto, userId, BehActionType.SUBSCRIBE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unsubscribe(BehDTO dto, Long userId) {
        cancelAction(dto, userId, BehActionType.UN_SUBSCRIBE);
    }
}
