package io.charqe.qs.modules.behavior.strategy.impl;

import io.charqe.qs.modules.behavior.domain.pojo.Behavior;
import io.charqe.qs.modules.behavior.strategy.BehStrategy;
import io.charqe.qs.modules.file.enums.BizTypeEnum;
import org.springframework.stereotype.Component;

/**
 * 文章策略实现类
 *
 * @author Joy
 * @since 2026-02-10 21:01
 */
@Component
public class ArticleBehStrategy implements BehStrategy {

    @Override
    public Integer getBizType() {
        return BizTypeEnum.ARTICLE.code(); // 返回 1，表示文章
    }

    @Override
    public void handleBehavior(Behavior behavior) {

    }

    @Override
    public boolean supportsAction(Integer action) {
        return false;
    }
}
