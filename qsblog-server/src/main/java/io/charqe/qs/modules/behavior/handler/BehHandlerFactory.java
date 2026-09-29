package io.charqe.qs.modules.behavior.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 行为实体处理器工厂
 * 根据 bizType 返回对应的 BehEntityHandler
 *
 * @author Joy
 * @since 2026-06-12
 */
@Slf4j
@Component
public class BehHandlerFactory {

    /**
     * 所有 BehEntityHandler 实现会被 Spring 自动注入到此 Map
     * key = bean name, value = handler 实例
     */
    @Resource
    private Map<String, BehEntityHandler> handlerMap;

    /**
     * bizType → handler 缓存
     */
    private final Map<Integer, BehEntityHandler> cache = new ConcurrentHashMap<>();

    /**
     * 根据 bizType 获取对应的实体处理器
     *
     * @param bizType 业务类型编码（BizTypeEnum / CommentTypeEnum）
     * @return 实体处理器，若找不到则返回 NoOpHandler
     */
    public BehEntityHandler getHandler(Integer bizType) {
        if (bizType == null) {
            return NoOpHandler.INSTANCE;
        }
        return cache.computeIfAbsent(bizType, this::resolveHandler);
    }

    private BehEntityHandler resolveHandler(Integer bizType) {
        for (BehEntityHandler handler : handlerMap.values()) {
            if (handler.supports(bizType)) {
                log.debug("找到处理器: {} for bizType={}", handler.getClass().getSimpleName(), bizType);
                return handler;
            }
        }
        log.debug("未找到 bizType={} 的处理器，使用 NoOpHandler", bizType);
        return NoOpHandler.INSTANCE;
    }

    /**
     * 空操作处理器（无实体特有校验的场景）
     */
    static class NoOpHandler implements BehEntityHandler {
        static final NoOpHandler INSTANCE = new NoOpHandler();

        @Override
        public void validate(Long bizId) {
            // no-op
        }

        @Override
        public boolean supports(Integer bizType) {
            return true; // 作为兜底，永远不会被 resolveHandler 循环调用
        }
    }
}
