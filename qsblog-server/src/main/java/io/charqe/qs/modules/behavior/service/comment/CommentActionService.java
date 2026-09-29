package io.charqe.qs.modules.behavior.service.comment;

import io.charqe.qs.modules.behavior.controller.web.dto.BehDTO;
import io.charqe.qs.modules.behavior.service.IActionService;

/**
 * 评论行为服务接口
 * 注意：此接口仅记录评论行为（状态+计数+日志），
 * 评论内容的创建/删除由 CommentService 负责。
 *
 * @author Joy
 * @since 2026-06-12
 */
public interface CommentActionService extends IActionService {

    /**
     * 记录评论行为（委托给 IActionService.doAction）
     */
    void comment(BehDTO dto, Long userId);

    /**
     * 取消评论记录（委托给 IActionService.cancelAction）
     */
    void uncomment(BehDTO dto, Long userId);
}
