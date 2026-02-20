package io.charqe.qs.modules.behavior.strategy.impl;

import io.charqe.qs.modules.behavior.domain.pojo.Behavior;
import io.charqe.qs.modules.behavior.strategy.BehStrategy;
import io.charqe.qs.modules.file.enums.BizTypeEnum;
import org.springframework.stereotype.Component;

/**
 * 评论策略实现
 *
 * @author Joy
 * @since 2026-02-10 21:01
 */
@Component
public class CommentBehStrategy implements BehStrategy {

    @Override
    public Integer getBizType() {
        return BizTypeEnum.COMMENT.code();
    }

    @Override
    public void handleBehavior(Behavior behavior) {

    }

    @Override
    public boolean supportsAction(Integer action) {
        return false;
    }
}
