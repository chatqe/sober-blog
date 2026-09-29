package io.charqe.qs.modules.behavior.service.comment;

import io.charqe.qs.modules.behavior.controller.web.dto.BehDTO;
import io.charqe.qs.modules.behavior.enums.BehActionType;
import io.charqe.qs.modules.behavior.service.AbstractBehService;
import io.charqe.qs.modules.behavior.service.IActionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 评论行为服务实现
 * 注意：此服务仅记录行为，评论实体的 CRUD 由 CommentService 负责
 *
 * @author Joy
 * @since 2026-06-12
 */
@Slf4j
@Service
public class CommentActionServiceImpl extends AbstractBehService implements CommentActionService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void comment(BehDTO dto, Long userId) {
        doAction(dto, userId, BehActionType.COMMENT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uncomment(BehDTO dto, Long userId) {
        cancelAction(dto, userId, BehActionType.UN_COMMENT);
    }
}
