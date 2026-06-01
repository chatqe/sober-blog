package io.charqe.qs.modules.behavior.service;

import io.charqe.qs.common.model.mp.CoreService;
import io.charqe.qs.modules.behavior.domain.pojo.BehaviorLog;
import io.charqe.qs.modules.behavior.domain.pojo.BehaviorState;

/**
 * 用户行为表 服务类
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
public interface BehaviorLogService extends CoreService<BehaviorLog> {

    /**
     * 处理用户行为
     *
     * @param beh 行为请求参数
     * @return 行为结果
     */
    // boolean handleBeh(BehaviorLog beh);

    /**
     * 检查用户行为 是否存在
     * 例: 用户是否已点赞
     *
     * @param behaviorState 行为请求参数
     * @return 是否存在
     */
    // boolean checkHasBeh(BehaviorState behaviorState);
}
