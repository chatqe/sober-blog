package io.charqe.qs.modules.behavior.service.dislike;

import io.charqe.qs.modules.behavior.controller.web.dto.BehDTO;
import io.charqe.qs.modules.behavior.service.IActionService;

/**
 * 点踩服务接口
 *
 * @author Joy
 * @since 2026-06-12
 */
public interface DislikeService extends IActionService {

    /**
     * 点踩（委托给 IActionService.doAction）
     */
    void dislike(BehDTO dto, Long userId);

    /**
     * 取消点踩（委托给 IActionService.cancelAction）
     */
    void undislike(BehDTO dto, Long userId);
}
