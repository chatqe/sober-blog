package io.charqe.qs.modules.behavior.service;

import io.charqe.qs.modules.behavior.enums.BehActionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 互斥策略注册中心
 * 管理行为类型之间的互斥关系，如 LIKE ↔ DISLIKE
 *
 * @author Joy
 * @since 2026-06-15
 */
@Slf4j
@Component
public class MutexRegistry {

    /**
     * 行为类型 → 互斥行为类型
     * key: 当前行为类型, value: 需要被取消的互斥行为类型
     */
    private final Map<BehActionType, BehActionType> mutexMap = new HashMap<>();

    @PostConstruct
    public void init() {
        // 点赞时自动取消点踩
        mutexMap.put(BehActionType.LIKE, BehActionType.DISLIKE);
        // 点踩时自动取消点赞
        mutexMap.put(BehActionType.DISLIKE, BehActionType.LIKE);

        log.info("互斥策略初始化完成: {} 条规则", mutexMap.size());
    }

    /**
     * 获取当前行为的互斥行为类型
     *
     * @param actionType 当前行为类型
     * @return 互斥行为类型，无互斥则返回 null
     */
    public BehActionType getMutexType(BehActionType actionType) {
        return mutexMap.get(actionType);
    }

    /**
     * 判断两个行为类型是否互斥
     *
     * @param a 行为类型A
     * @param b 行为类型B
     * @return true-互斥
     */
    public boolean isMutualExclusion(BehActionType a, BehActionType b) {
        BehActionType mutexA = mutexMap.get(a);
        return mutexA != null && mutexA.equals(b);
    }
}
