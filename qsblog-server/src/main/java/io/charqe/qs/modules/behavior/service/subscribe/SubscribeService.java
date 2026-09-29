package io.charqe.qs.modules.behavior.service.subscribe;

import io.charqe.qs.modules.behavior.controller.web.dto.BehDTO;
import io.charqe.qs.modules.behavior.service.IActionService;

/**
 * 订阅服务接口
 *
 * @author Joy
 * @since 2026-06-12
 */
public interface SubscribeService extends IActionService {

    /**
     * 订阅（委托给 IActionService.doAction）
     */
    void subscribe(BehDTO dto, Long userId);

    /**
     * 取消订阅（委托给 IActionService.cancelAction）
     */
    void unsubscribe(BehDTO dto, Long userId);
}
