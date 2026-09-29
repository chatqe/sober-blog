package io.charqe.qs.modules.behavior.handler;

/**
 * 行为-实体处理器接口
 * 各内容实体（文章、动态、评论、分类等）实现此接口，
 * 提供实体特有的校验和后置逻辑。
 *
 * @author Joy
 * @since 2026-06-12
 */
public interface BehEntityHandler {

    /**
     * 判断是否支持该 bizType
     *
     * @param bizType 业务类型编码
     * @return true-支持
     */
    boolean supports(Integer bizType);

    /**
     * 校验实体是否存在及可操作状态
     *
     * @param bizId 业务主键ID
     */
    void validate(Long bizId);

    /**
     * 行为执行后的后置处理（如通知作者、清除缓存）
     *
     * @param bizId  业务主键ID
     * @param userId 操作人ID
     * @param action 行为编码（来自 BehActionType）
     */
    default void afterAction(Long bizId, Long userId, Integer action) {
        // 默认空实现
    }

    /**
     * 行为取消后的后置处理（如同步计数回退）
     *
     * @param bizId  业务主键ID
     * @param userId 操作人ID
     * @param action 正向行为编码（来自 BehActionType，用于计算回退量）
     */
    default void afterCancelAction(Long bizId, Long userId, Integer action) {
        // 默认空实现
    }
}
