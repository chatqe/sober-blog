package io.charqe.qs.modules.behavior.service.event;

import io.charqe.qs.modules.behavior.enums.BehActionType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 行为操作事件（发布后由 BehLogEventListener 异步消费）
 *
 * @author Joy
 * @since 2026-06-12
 */
@Getter
public class BehActionEvent extends ApplicationEvent {

    private final Long userId;
    private final Long bizId;
    private final Integer bizType;
    private final Integer actionCode;

    public BehActionEvent(Object source, Long userId, Long bizId, Integer bizType, BehActionType actionType) {
        super(source);
        this.userId = userId;
        this.bizId = bizId;
        this.bizType = bizType;
        this.actionCode = actionType.code();
    }

    @Override
    public String toString() {
        return "BehActionEvent{userId=" + userId + ", bizId=" + bizId +
                ", bizType=" + bizType + ", actionCode=" + actionCode + '}';
    }
}
