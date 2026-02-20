package io.charqe.qs.modules.behavior.strategy;

import io.charqe.qs.modules.behavior.domain.pojo.Behavior;

/**
 * 行为策略
 *
 * @author Joy
 * @since 2026-02-10 21:01
 */
public interface BehStrategy {

    /**
     * 支持的业务类型
     */
    Integer getBizType();

    /**
     * 处理行为
     * @param behavior 行为对象
     */
    void handleBehavior(Behavior behavior);

    /**
     * 支持的行为类型
     */
    boolean supportsAction(Integer action);
}
