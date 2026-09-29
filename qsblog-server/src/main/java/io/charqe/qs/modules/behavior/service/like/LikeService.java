package io.charqe.qs.modules.behavior.service.like;

import io.charqe.qs.modules.behavior.controller.web.dto.BehDTO;
import io.charqe.qs.modules.behavior.service.IActionService;

/**
 * 点赞服务接口
 *
 * @author Joy
 * @since 2026-06-12
 */
public interface LikeService extends IActionService {

    /**
     * 点赞（委托给 IActionService.doAction）
     */
    void like(BehDTO dto, Long userId);

    /**
     * 取消点赞（委托给 IActionService.cancelAction）
     */
    void unlike(BehDTO dto, Long userId);
}
