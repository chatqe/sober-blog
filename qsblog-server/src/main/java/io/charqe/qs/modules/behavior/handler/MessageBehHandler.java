package io.charqe.qs.modules.behavior.handler;

import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.modules.behavior.enums.BehActionType;
import io.charqe.qs.modules.file.enums.BizTypeEnum;
import io.charqe.qs.modules.message.domain.dao.MessageMapper;
import io.charqe.qs.modules.message.domain.pojo.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class MessageBehHandler implements BehEntityHandler {

    @Resource
    private MessageMapper messageMapper;

    @Override
    public boolean supports(Integer bizType) {
        return BizTypeEnum.MESSAGE.getCode().equals(bizType);
    }

    @Override
    public void validate(Long bizId) {
        if (bizId == null) {
            throw new BizException("留言ID不能为空");
        }
        Message message = messageMapper.selectById(bizId);
        if (message == null || message.getIsDel() == 1) {
            throw new BizException("留言不存在");
        }
    }

    @Override
    public void afterAction(Long bizId, Long userId, Integer action) {
        if (BehActionType.LIKE.getCode().equals(action)) {
            messageMapper.incrementLikeCount(bizId);
        } else if (BehActionType.DISLIKE.getCode().equals(action)) {
            messageMapper.incrementDislikeCount(bizId);
        }
        log.debug("留言 {} 行为后置处理完成, action={}", bizId, action);
    }

    @Override
    public void afterCancelAction(Long bizId, Long userId, Integer action) {
        if (BehActionType.LIKE.getCode().equals(action)) {
            messageMapper.decrementLikeCount(bizId);
        } else if (BehActionType.DISLIKE.getCode().equals(action)) {
            messageMapper.decrementDislikeCount(bizId);
        }
        log.debug("留言 {} 取消行为后置处理完成, action={}", bizId, action);
    }
}
