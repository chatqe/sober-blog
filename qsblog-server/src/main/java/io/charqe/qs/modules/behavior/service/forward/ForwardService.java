package io.charqe.qs.modules.behavior.service.forward;

import io.charqe.qs.modules.behavior.controller.web.dto.BehDTO;
import io.charqe.qs.modules.behavior.service.IActionService;

/**
 * 转发服务接口
 *
 * @author Joy
 * @since 2026-06-12
 */
public interface ForwardService extends IActionService {

    /**
     * 转发（委托给 IActionService.doAction）
     */
    void forward(BehDTO dto, Long userId);

    /**
     * 取消转发（委托给 IActionService.cancelAction）
     */
    void unforward(BehDTO dto, Long userId);
}
