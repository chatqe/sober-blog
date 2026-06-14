package io.charqe.qs.modules.behavior.service.forward;

import io.charqe.qs.modules.behavior.controller.web.dto.BehDTO;
import io.charqe.qs.modules.behavior.enums.BehActionType;
import io.charqe.qs.modules.behavior.service.AbstractBehService;
import io.charqe.qs.modules.behavior.service.IActionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 转发服务实现
 * 无互斥逻辑，纯状态翻转 + 计数 + 日志
 * 如需扩展（如记录转发平台、附言），可在 ForwardService 接口增加重载方法
 *
 * @author Joy
 * @since 2026-06-12
 */
@Slf4j
@Service
public class ForwardServiceImpl extends AbstractBehService implements ForwardService, IActionService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forward(BehDTO dto, Long userId) {
        doAction(dto, userId, BehActionType.FORWARD);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unforward(BehDTO dto, Long userId) {
        cancelAction(dto, userId, BehActionType.UN_FORWARD);
    }
}
