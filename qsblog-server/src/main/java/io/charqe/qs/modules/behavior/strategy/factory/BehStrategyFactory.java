package io.charqe.qs.modules.behavior.strategy.factory;

import io.charqe.qs.common.constants.ErrorConst;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.modules.behavior.strategy.BehStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 行为策略工厂
 *
 * @author Joy
 * @since 2026-02-10 21:00
 */
@Component
public class BehStrategyFactory {
    private final Map<Integer, BehStrategy> strategyMap = new ConcurrentHashMap<>();

    @Autowired
    public BehStrategyFactory(List<BehStrategy> strategies) {
        strategies.forEach(strategy ->
                strategyMap.put(strategy.getBizType(), strategy)
        );
    }

    public BehStrategy getStrategy(Integer bizType) {
        BehStrategy strategy = strategyMap.get(bizType);
        if (strategy == null) {
            throw new BizException(ErrorConst.BEH_BIZ_TYPE_NOT_SUPPORT + bizType,"500");
        }
        return strategy;
    }
}
